package com.shengbo.gulimall.order.dao;

import com.shengbo.gulimall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 21:57:53
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
