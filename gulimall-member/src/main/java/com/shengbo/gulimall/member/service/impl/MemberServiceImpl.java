package com.shengbo.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shengbo.common.to.SocialUser;
import com.shengbo.common.utils.HttpUtils;
import com.shengbo.gulimall.member.dao.MemberLevelDao;
import com.shengbo.gulimall.member.entity.MemberLevelEntity;
import com.shengbo.gulimall.member.exception.PhoneExistException;
import com.shengbo.gulimall.member.exception.UserNameExistException;
import com.shengbo.gulimall.member.vo.MemberLoginVo;
import com.shengbo.gulimall.member.vo.MemberRegistVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.member.dao.MemberDao;
import com.shengbo.gulimall.member.entity.MemberEntity;
import com.shengbo.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    MemberLevelDao memberLevelDao;

    //    @Autowired
//    MemberService memberService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberDao baseMapper = this.baseMapper;
        MemberEntity memberEntity = new MemberEntity();
        //设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());
        //
        //检查用户名和手机号是否唯一 为了让controller感受异常 可以加入异常机制
        this.checkPhoneUnique(vo.getPhone());
        this.checkUserNameUnique(vo.getUserName());

        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());
        memberEntity.setMobile(vo.getPhone());

        //密码要进行加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(vo.getPassword());
        memberEntity.setPassword(encodedPassword);

        //保存
        baseMapper.insert(memberEntity);

    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        MemberDao baseMapper = this.baseMapper;
        Integer cnt = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (cnt > 0) {
            throw new PhoneExistException();
        }

    }

    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistException {
        MemberDao baseMapper = this.baseMapper;
        Integer cnt = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (cnt > 0) {
            throw new UserNameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginAccount = vo.getLoginAccount();
        String password = vo.getPassword();
        MemberDao baseMapper = this.baseMapper;
        //1.去数据库查询
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginAccount).or().eq("mobile", loginAccount));

        if (entity == null) {
            //返回null意味着失败
            return null;
        } else {
            String passwordDb = entity.getPassword();
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            boolean matches = bCryptPasswordEncoder.matches(password, passwordDb);
            if (matches) {
                return entity;
            } else {
                return null;
            }

        }

    }

    @Override
    public MemberEntity login(SocialUser user) throws Exception {
        MemberDao baseMapper = this.baseMapper;
        //具有登陆和注册合并逻辑

        String uid = user.getUid();
        //先判断是否用过没
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (entity != null) {
            //已经注册过
            MemberEntity newEntity = new MemberEntity();
            newEntity.setId(entity.getId());
            newEntity.setAccessToken(user.getAccess_token());
            newEntity.setExpiresIn(user.getExpires_in());

            baseMapper.updateById(newEntity);

            entity.setAccessToken(user.getAccess_token());
            entity.setExpiresIn(user.getExpires_in());
            return entity;
        } else {
            //没查到 注册一个
            MemberEntity regist = new MemberEntity();
            try {
                //向微博发送请求 查询当前社交用户的社交账号信息：昵称 性别等
                Map<String, String> query = new HashMap<>();
                query.put("access_token", user.getAccess_token());
                query.put("uid", user.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    //查询成功
                    String json = EntityUtils.toString(response.getEntity());

                    JSONObject jsonObject = JSON.parseObject(json);
                    String name = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");
                    String profile_image_url = jsonObject.getString("profile_image_url");


                    regist.setNickname(name);
                    regist.setGender("m".equals(gender) ? 1 : 0);
                    regist.setHeader(profile_image_url);
                    regist.setCreateTime(new Date());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            regist.setAccessToken(user.getAccess_token());
            regist.setExpiresIn(user.getExpires_in());
            regist.setSocialUid(user.getUid());

            baseMapper.insert(regist);

            return regist;
        }
    }
}