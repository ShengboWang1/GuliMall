package com.shengbo.gulimall.order;

import com.shengbo.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Test
    void sendMessageTest(){
        OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
        orderReturnReasonEntity.setCreateTime(new Date());
        orderReturnReasonEntity.setId(1L);
        orderReturnReasonEntity.setSort(0);
        orderReturnReasonEntity.setName("11");
        orderReturnReasonEntity.setStatus(0);
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderReturnReasonEntity);
        log.info("消息{}发送完成", orderReturnReasonEntity);
    }

    @Test
    void createExchange() {
        /**
         * public DirectExchange(String name) {
         *         super(name);
         *     }
         *
         *     public DirectExchange(String name, boolean durable, boolean autoDelete) {
         *         super(name, durable, autoDelete);
         *     }
         */
        DirectExchange directExchange = new DirectExchange("hello-java-exchange"
                ,true, false);

        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", "hello-java-exchange");
    }

    @Test
    void createQueue() {
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
    }

    @Test
    void createBindung(){
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",
                null);
        amqpAdmin.declareBinding(binding);
    }
}
