package com.shengbo.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shengbo.common.utils.HttpUtils;
import com.shengbo.common.to.SocialUser;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.authserver.feign.MemberFeignService;
import com.shengbo.common.vo.MemberResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        //1。根据code换Access token
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "671596388");
        map.put("client_secret", "8e632e0e28accb6c9ffa6d9eef2df405");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", new HashMap<>(), map, new HashMap<>());

        //2。处理
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode == 200){
            //获取到了access token
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //知道当前是哪个社交用户
            //1）当前用户如果是第一次进该网站 就自动注册进来 即为当前社交用户生成一个会员信息账号 以后这个账号就对应指定的会员
            //登陆或者注册这个社交用户
            R oauthlogin = memberFeignService.oauthlogin(socialUser);
            if(oauthlogin.getCode() == 0){
                //登陆成功跳回首页
                MemberResponseVo data = oauthlogin.getData("data", new TypeReference<MemberResponseVo>() {
                });
                log.info("登陆成功， 用户信息为：" + data.toString());
                //TODO 1.session的作用域是 auth.gulimall.com子域 需要解决子域session共享问题
                //TODO 2.使用JSON的序列化方式来序列化整个对象数据到redis
                session.setAttribute("loginUser", data);
                return "redirect:http://gulimall.com";
            }else{
                return "redirect:http://auth.gulimall.com/login.html";
            }

        }else{
            return "redirect:http://auth.gulimall.com/login.html";
        }


    }
}
