package com.shengbo.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.to.SocialUser;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.member.entity.MemberEntity;
import com.shengbo.gulimall.member.exception.PhoneExistException;
import com.shengbo.gulimall.member.exception.UserNameExistException;
import com.shengbo.gulimall.member.vo.MemberLoginVo;
import com.shengbo.gulimall.member.vo.MemberRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 21:54:43
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUserNameUnique(String userName) throws UserNameExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser user) throws Exception;
}

