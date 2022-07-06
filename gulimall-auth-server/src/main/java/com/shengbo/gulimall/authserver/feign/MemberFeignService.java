package com.shengbo.gulimall.authserver.feign;

import com.shengbo.common.to.SocialUser;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.authserver.vo.UserLoginVo;
import com.shengbo.gulimall.authserver.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    R oauthlogin(@RequestBody SocialUser user) throws Exception;
}
