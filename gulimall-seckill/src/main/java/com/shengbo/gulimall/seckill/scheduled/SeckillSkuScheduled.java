package com.shengbo.gulimall.seckill.scheduled;


import com.shengbo.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 * 每天晚上3点 上架最近三天需要秒杀的商品SeckillSku
 * 当天晚上00：00：00 -23：59：59
 * 明天晚上00：00：00 -23：59：59
 * 后天晚上00：00：00 -23：59：59
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedissonClient redissonClient;

    private final String upload_lock="seckill:upload:lock";

//    @Scheduled(cron = "*/2 * * * * ?")
//    public void uploadSeckillSkuLatest3Days(){
//        //TODO 幂等性
//        //1。重复上架无需处理
//        //1.1首先保证分布式多个机器的环境下咱们有分布式锁 保证同一时间段只有一个机器进行服务
//        log.info("上架秒杀的商品信息");
//        //分布式锁 锁的业务执行完成了， 状态已经更新完成 释放锁以后其他人获取到就会拿到最新的状态
//        RLock lock = redissonClient.getLock(upload_lock);
//        lock.lock(10, TimeUnit.SECONDS);
//        try{
//            seckillService.uploadSeckillSkuLatest3Days();
//        }
//        finally {
//            lock.unlock();
//        }
//
//    }

}
