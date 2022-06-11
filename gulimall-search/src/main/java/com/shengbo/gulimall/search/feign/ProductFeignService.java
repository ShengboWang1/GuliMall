package com.shengbo.gulimall.search.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("gulimall-product")
public interface ProductFeignService {

}
