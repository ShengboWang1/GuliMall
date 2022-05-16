package com.shengbo.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.ware.entity.WareSkuEntity;

import java.util.Map;

/**
 * 商品库存
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 22:03:14
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);
}

