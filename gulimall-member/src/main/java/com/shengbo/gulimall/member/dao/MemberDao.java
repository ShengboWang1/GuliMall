package com.shengbo.gulimall.member.dao;

import com.shengbo.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 21:54:43
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
