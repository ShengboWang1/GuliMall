package com.shengbo.gulimall.product.dao;

import com.shengbo.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 16:19:54
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
