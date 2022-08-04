package com.shengbo.gulimall.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.shengbo.common.to.mq.SeckillOrderTo;
import com.shengbo.common.utils.R;
import com.shengbo.common.vo.MemberResponseVo;
import com.shengbo.gulimall.seckill.feign.CouponFeignService;
import com.shengbo.gulimall.seckill.feign.ProductFeignService;
import com.shengbo.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.shengbo.gulimall.seckill.service.SeckillService;
import com.shengbo.gulimall.seckill.to.SeckillSkuRedisTo;
import com.shengbo.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.shengbo.gulimall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";
    private final String STOCK_CACHE_PREFIX = "seckill:stock:";//后面是商品的随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.去扫描最近三天需要参与秒杀的活动
        R r = couponFeignService.getLatest3DaysSession();
        if (r.getCode() == 0) {
            //调用远程接口查到活动
            List<SeckillSessionsWithSkus> sessionData = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //上架到redis中去
            // 1.缓存活动信息
            saveSessionInfos(sessionData);

            // 2。缓存活动的关联商品信息
            saveSessionSkuInfos(sessionData);
        }
    }


    public List<SeckillSkuRedisTo> blockHandler(BlockException e) {
        log.error("getCurrentSeckillSkusResource被限流了。。。");
        return null;
    }

    /**
     * 从缓存中查询当前可以参加的商品信息 用于返回给前端页面
     */
    @SentinelResource(value = "getCurrentSeckillSkusResource", blockHandler = "blockHandler")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1.确定当前时间属于哪个秒杀场次
        //是当前时间与1970年时间的差值
        long time = new Date().getTime();
        Set<String> keys = stringRedisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSION_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long start = Long.parseLong(s[0]);
            long end = Long.parseLong(s[1]);
            if (time >= start && time <= end) {
                //2，获取这个秒杀场次需要的所有sku信息
                List<String> range = stringRedisTemplate.opsForList().range(key, 0, -1);
                BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redisTo = JSON.parseObject(item.toString(), SeckillSkuRedisTo.class);
                        return redisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }
        return null;
    }

    /**
     * 根据skuId查询是否有秒杀信息 并且给到product服务去
     *
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //1。找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                String[] s = key.split("_");
                if (skuId == Long.parseLong(s[1])) {
                    String info = hashOps.get(key);
                    SeckillSkuRedisTo redisTo = JSON.parseObject(info, SeckillSkuRedisTo.class);
                    long time = new Date().getTime();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    if (time >= startTime && time <= endTime) {
                    } else {
                        redisTo.setRandomCode(null);
                    }
                    return redisTo;
                }
            }
        }
        return null;
    }

    /**
     * 判断是否符合秒杀条件 成功后给MQ发消息
     *
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        long s1 = System.currentTimeMillis();
        MemberResponseVo respVo = LoginUserInterceptor.loginUser.get();

        //1.获取当前商品的秒杀信息
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(killId);
        if (StringUtils.hasText(s)) {
            SeckillSkuRedisTo redisTo = JSON.parseObject(s, SeckillSkuRedisTo.class);
            //2.校验合法性
            //2.1时间合法性
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long time = new Date().getTime();
            if (time < startTime || time > endTime) return null;
            else {
                //2.2随机码合法性和商品id合法性
                String randomCode = redisTo.getRandomCode();
                String id = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if (randomCode.equals(key) && killId.equals(id)) {
                    //2.3 验证商品数量是否合理
                    if (num <= redisTo.getSeckillLimit().intValue()) {
                        //2.4 验证这个人是否已经购买过 幂等性 只要秒杀成功 就去占位 userId_SessionId_skuId
                        //SETNX
                        String redisKey = respVo.getId() + "_" + id;
                        //自动过期 过期时间为活动结束时间-当前时间
                        long ttl = endTime - time;
                        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MICROSECONDS);
                        if (aBoolean) {
                            //这个人没买过
                            RSemaphore semaphore = redissonClient.getSemaphore(STOCK_CACHE_PREFIX + randomCode);
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功 快速下单 发送MQ消息
                                String timeId = IdWorker.getTimeId();
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(timeId);
                                orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                orderTo.setNum(num);
                                orderTo.setSkuId(redisTo.getSkuId());
                                orderTo.setMemberId(respVo.getId());
                                orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                long s2 = System.currentTimeMillis();
                                log.info("耗时。。" + (s2 - s1));
                                return timeId;
                            }
                        }
                    }
                }
            }

        }
        return null;
    }

    /**
     * / 1.缓存活动信息
     *
     * @param sessions
     */
    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session -> {
                Long startTime = session.getStartTime().getTime();
                Long endTime = session.getEndTime().getTime();
                //key是活动的时间 value是活动对应商品的id
                String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
                Boolean hasKey = stringRedisTemplate.hasKey(key);
                if (Boolean.FALSE.equals(hasKey)) {
                    List<String> collect = session.getRelationSkus().stream().map(item -> {
                        return item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString();
                    }).collect(Collectors.toList());
                    //缓存活动信息
                    stringRedisTemplate.opsForList().leftPushAll(key, collect);
                }
            });
        }

    }

    /**
     * // 2。缓存活动的关联商品信息
     *
     * @param sessions
     */
    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            //准备哈希操作
            BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            // 商品ID 对应JSON详细信息进行存储
            session.getRelationSkus().stream().forEach(sku -> {
                String token = UUID.randomUUID().toString().replace("-", "");
                Boolean hasKey = hashOps.hasKey(sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString());
                if (Boolean.FALSE.equals(hasKey)) {
                    //缓存商品信息
                    SeckillSkuRedisTo seckillSkuRedisTo = new SeckillSkuRedisTo();
                    //1.sku的基本数据 调用product进行查询
                    R r = productFeignService.getSkuInfo(sku.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo infoVo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        seckillSkuRedisTo.setSkuInfoVo(infoVo);
                    }

                    //2。sku的秒杀信息
                    BeanUtils.copyProperties(sku, seckillSkuRedisTo);
                    //3. 设置当前商品的秒杀时间信息
                    Long startTime = session.getStartTime().getTime();
                    Long endTime = session.getEndTime().getTime();
                    seckillSkuRedisTo.setStartTime(startTime);
                    seckillSkuRedisTo.setEndTime(endTime);

                    //4。随机码 seckill?skuId=1&key=dadlasfdsaf; 得随机码对了才能访问 防止秒杀开始的一瞬间存在恶意攻击 只有带着随机码才能去信号量那里减库存

                    seckillSkuRedisTo.setRandomCode(token);
                    String s = JSON.toJSONString(seckillSkuRedisTo);
                    hashOps.put(sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString(), s);
                    //引入分布式的信号量 来表示库存 ->限流：库存减完 咱们就上锁
                    RSemaphore semaphore = redissonClient.getSemaphore(STOCK_CACHE_PREFIX + token);
                    //商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(sku.getSeckillCount().intValue());
                }
            });
        });
    }
}
