package com.shengbo.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.to.SkuReductionTo;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 21:11:57
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

