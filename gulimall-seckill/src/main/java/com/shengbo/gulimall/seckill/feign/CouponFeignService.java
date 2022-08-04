package com.shengbo.gulimall.seckill.feign;

import com.shengbo.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    @GetMapping("/coupon/seckillsession/getLatest3DaysSession")
    R getLatest3DaysSession();
}
