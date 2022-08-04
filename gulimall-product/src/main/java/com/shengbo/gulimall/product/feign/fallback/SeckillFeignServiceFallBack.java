package com.shengbo.gulimall.product.feign.fallback;

import com.shengbo.common.exception.BizCodeEnume;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.info("熔断方法调用..getSkuSeckillInfo");
        return R.error(BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getCode(), BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getMsg());
    }
}
