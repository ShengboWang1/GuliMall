package com.shengbo.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.to.mq.SeckillOrderTo;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.order.entity.OrderEntity;
import com.shengbo.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 21:57:53
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 订单确认页返回需要用的数据
     * @return
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderBySn(String orderSn);

    void closeOrder(OrderEntity entity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handelPayResult(PayAsyncVo vo);

    void createSeckillOrder(SeckillOrderTo to);
}

