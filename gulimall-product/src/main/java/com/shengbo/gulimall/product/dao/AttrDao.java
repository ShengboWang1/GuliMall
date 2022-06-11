package com.shengbo.gulimall.product.dao;

import com.shengbo.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 16:19:54
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    List<Long> selectAttrIds(@Param("attrIds") List<Long> attrIds);
}
