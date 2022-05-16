package com.shengbo.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 16:19:54
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenusByIds(List<Long> asList);

    Long[] findCatelogPath(Long catelogId);

    void updateCascade(CategoryEntity category);
}

