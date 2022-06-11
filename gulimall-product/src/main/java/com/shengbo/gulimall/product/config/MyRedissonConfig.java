package com.shengbo.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {

    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException{
        Config config = new Config();
        config.useSingleServer().setAddress("redis://10.211.55.6:6379");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
