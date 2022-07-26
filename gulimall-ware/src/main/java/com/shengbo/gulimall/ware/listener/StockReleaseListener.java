package com.shengbo.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.shengbo.common.to.OrderTo;
import com.shengbo.common.to.mq.StockDetailTo;
import com.shengbo.common.to.mq.StockLockedTo;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.shengbo.gulimall.ware.entity.WareOrderTaskEntity;
import com.shengbo.gulimall.ware.feign.OrderFeignService;
import com.shengbo.gulimall.ware.service.WareOrderTaskDetailService;
import com.shengbo.gulimall.ware.service.WareOrderTaskService;
import com.shengbo.gulimall.ware.service.WareSkuService;
import com.shengbo.gulimall.ware.service.impl.WareSkuServiceImpl;
import com.shengbo.gulimall.ware.vo.OrderVo;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    WareOrderTaskService orderTaskService;
    @Autowired
    WareSkuService wareSkuService;
    /**
     * 库存自动解锁
     * 下订单成功 库存锁定成功 接下来的业务调用失败 导致订单回滚 之前锁定的库存要自动解锁
     * @param to
     * @param message
     */
    @RabbitHandler
    public void handleStockLockRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息。。");
        try{
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭 准备解锁库存。。");
        try{
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
