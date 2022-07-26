package com.shengbo.gulimall.order.config;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

    private RabbitTemplate rabbitTemplate;
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(messageConverter());
        initRabbitTemplate();
        return rabbitTemplate;
    }

    //@PostConstruct
    public void initRabbitTemplate(){
//        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback(){
//            //消息没有投递给queue 就失败
//            @Override
//            public void returnedMessage(ReturnedMessage returnedMessage) {
//                System.out.println("失败了。。"  + returnedMessage);
//            }
//        });
        RabbitTemplate.ReturnsCallback returnsCallback = returnedMessage -> System.out.println("失败了。。" + returnedMessage);
        rabbitTemplate.setReturnsCallback(returnsCallback);
    }
}
