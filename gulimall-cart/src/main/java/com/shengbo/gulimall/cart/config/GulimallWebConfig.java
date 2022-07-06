package com.shengbo.gulimall.cart.config;

import com.shengbo.gulimall.cart.intercepter.CartIntercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    @Override
    //设置CartIntercepter拦截器的作用域
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new CartIntercepter()).addPathPatterns("/**");
    }
}
