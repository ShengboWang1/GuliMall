package com.shengbo.gulimall.coupon.dao;

import com.shengbo.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 21:11:57
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
