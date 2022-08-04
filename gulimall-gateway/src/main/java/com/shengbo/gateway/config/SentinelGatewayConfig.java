package com.shengbo.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.shengbo.common.exception.BizCodeEnume;
import com.shengbo.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.Model;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class SentinelGatewayConfig{
    public SentinelGatewayConfig(){
        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            //网关相应了请求 就会调用此回调 Mono Flux
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange serverWebExchange, Throwable throwable) {
                R error = R.error(BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getCode(), BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getMsg());
                String s = JSON.toJSONString(error);
                Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(s), String.class);
                return body;
            }
        });
    }
}
