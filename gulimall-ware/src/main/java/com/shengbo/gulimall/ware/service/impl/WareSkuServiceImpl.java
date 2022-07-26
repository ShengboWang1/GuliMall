package com.shengbo.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.shengbo.common.to.OrderTo;
import com.shengbo.common.to.mq.StockDetailTo;
import com.shengbo.common.to.mq.StockLockedTo;
import com.shengbo.common.utils.R;
import com.shengbo.common.exception.NoStockException;
import com.shengbo.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.shengbo.gulimall.ware.entity.WareOrderTaskEntity;
import com.shengbo.gulimall.ware.feign.OrderFeignService;
import com.shengbo.gulimall.ware.feign.ProductFeignService;
import com.shengbo.gulimall.ware.service.WareOrderTaskDetailService;
import com.shengbo.gulimall.ware.service.WareOrderTaskService;
import com.shengbo.gulimall.ware.vo.OrderItemVo;
import com.shengbo.gulimall.ware.vo.OrderVo;
import com.shengbo.gulimall.ware.vo.SkuHasStockVo;
import com.shengbo.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.ware.dao.WareSkuDao;
import com.shengbo.gulimall.ware.entity.WareSkuEntity;
import com.shengbo.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    WareSkuDao wareSkuDao;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    WareOrderTaskService orderTaskService;
    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;
    @Autowired
    OrderFeignService orderFeignService;


    private void unlockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        //库存解锁
        wareSkuDao.unlockStock(skuId, wareId, num);
        //更新库存工作单的状态
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
        wareOrderTaskDetailEntity.setId(taskDetailId);
        wareOrderTaskDetailEntity.setLockStatus(2);//变为已解锁
        orderTaskDetailService.updateById(wareOrderTaskDetailEntity);
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        /**
         * wareId: 123,//仓库id
         *    skuId: 123//商品id
         */
        String skuId = (String) params.get("skuId");
        if (StringUtils.hasText(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (StringUtils.hasText(wareId)) {
            wrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1。判断如果还没有这个库存记录就是新增
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setStock(skuNum);
            skuEntity.setSkuId(skuId);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //远程查询sku的名字 如果失败 事务无需回滚
            //1 自己catch
            //2 TODO 还可以什么办法让异常出现以后不回滚？
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }

            this.baseMapper.insert(skuEntity);
        } else {
            //2。没有则是更新操作
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId(skuId);
            Long count = baseMapper.getSkuStock(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;

    }

    /**
     * 为某一个订单进行库存锁定
     * Transactional 默认运行时异常都会回滚
     * 啥时候解锁库存？
     * 1。订单下了 然后用户手动取消订单 或者过期没有被支付
     * 2。下订单成功 库存锁定成功 接下来的业务调用失败 导致订单回滚 之前锁定的库存要自动解锁
     *
     * @param vo
     * @return
     */
    @Override
    @Transactional
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 0.
         * 保存库存工作单的详情
         * 追溯到哪一个仓库干嘛了
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(wareOrderTaskEntity);

        //1. 找到每个商品在哪个仓库有库存
        List<OrderItemVo> locks = vo.getLocks();//想要被锁住的商品
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            //想要锁的商品
            Long skuId = item.getSkuId();
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            skuWareHasStock.setSkuId(skuId);
            List<Long> wareId = wareSkuDao.listWareIdHasSkuStock(skuId);
            skuWareHasStock.setWareId(wareId);
            skuWareHasStock.setNum(item.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());
        Boolean allLock = true;
        //锁定库存
        for (SkuWareHasStock skuWareHasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = skuWareHasStock.getSkuId();
            List<Long> wareIds = skuWareHasStock.getWareId();
            Integer num = skuWareHasStock.getNum();
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }
            //1。如果商品锁定成功 就将带库存工作单的id 和 详情工作单的消息发给mq
            //2。如果锁定失败 前面保存的工作单信息需要回滚 发送出去的消息即使要解锁记录 由于对面查不到id
            for (Long wareId : wareIds) {
                //返回1成功 0失败
                Long count = wareSkuDao.lockSkuDao(wareId, skuId, num);
                if (count == 0) {
                    //当前仓库锁失败 尝试下一个仓库
                } else {
                    //锁成功了
                    skuStocked = true;
                    //TODO 给MQ发一个消息 告诉他解锁成功
                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", skuWareHasStock.getNum(), wareOrderTaskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(wareOrderTaskDetailEntity);
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(wareOrderTaskDetailEntity, stockDetailTo);
                    lockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                }
            }
            //发现对这个sku 所有的仓库都没锁住
            if (!skuStocked) {
                throw new NoStockException(skuId);
            }

        }
        //到这里就是能锁定成功的咯
        return true;

    }

    @Override
    public void unlockStock(StockLockedTo to) {
        System.out.println("收到解锁库存的消息");
        StockDetailTo detailTo = to.getDetailTo();
        Long skuId = detailTo.getSkuId();
        Long detailId = detailTo.getId();
        //1.查询数据库中关于这个订单的锁定库存信息
        //有 需要解锁
        //没有 库存锁定失败了 库存回滚 无需解锁
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        //查询数据库关于这个订单的锁定库存信息 detail表
        // 1。有信息：证明库存锁定成功了
        //          那么要查询订单状况
        //          1。1没有这个订单 那么必须解锁
        //          1。2有这个订单：
        //               1。2。1那么如果订单状态为已取消 解锁库存
        //               1。2。2订单状态没取消 这种情况不解锁
        // 2。没有信息： 库存锁定失败了 库存回滚了已经 不用解锁
        if (byId != null) {
            //解锁
            Long id = to.getId();//库存工作单的id
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                //订单数据返回成功
                OrderVo orderVo = r.getData(new TypeReference<OrderVo>() {
                });
                if (orderVo == null || orderVo.getStatus() == 4) {
                    //订单已经被取消了 解锁库存
                    if(byId.getLockStatus() == 1){
                        //当前库存工作单状态详情 状态1 一锁定但是未解锁才可以解锁
                        unlockStock(detailTo.getSkuId(), detailTo.getWareId(), detailTo.getSkuNum(), detailId);
                        //手动ACK
                    }

                }
            } else {
                //消息拒绝以后重新放到队列里面 让别人继续消费解锁
                throw new RuntimeException("远程服务失败");
            }
            List<Integer> a = new ArrayList<>();
            a.stream().toArray();

        } else {
            //无需解锁
        }
    }

    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查询最新库存的状态 防止被重复解锁
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        //按照工作单找到所有么得解锁的库存
        List<WareOrderTaskDetailEntity> list = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id).eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : list) {
            unlockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }

    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}