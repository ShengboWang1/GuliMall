package com.shengbo.gulimall.authserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import com.shengbo.common.constant.AuthServerConstant;
import com.shengbo.common.exception.BizCodeEnume;
import com.shengbo.common.utils.R;
import com.shengbo.common.vo.MemberResponseVo;
import com.shengbo.gulimall.authserver.feign.MemberFeignService;
import com.shengbo.gulimall.authserver.feign.ThirdPartyFeignService;
import com.shengbo.gulimall.authserver.vo.UserLoginVo;
import com.shengbo.gulimall.authserver.vo.UserRegistVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class LoginController {
    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    MemberFeignService memberFeignService;
    /**
     * 发送一个请求直接跳转到一个页面
     * SpringMVC viewController 将请求和页面映射过来
     *
     * @return
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        //TODO 接口防刷
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.hasText(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                //60s之内不能在发送
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        String code = UUID.randomUUID().toString().substring(0, 5);
        String codeInRedis = code + "_" + System.currentTimeMillis();
        log.info("验证码是。。。" + code);
        String expireTime = "3";

        //验证码的再次校验
        // 存key为 "sms:code:手机号 "
        // value为验证码的值
        //
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, codeInRedis, Integer.parseInt(expireTime), TimeUnit.MINUTES);

        //接口防刷 防止同一个phone在60s内发送验证码
        thirdPartyFeignService.sendCode(phone, code, expireTime);
        return R.ok();
    }

    /**
     * //TODO 重定向携带数据 利用session原理将数据放在session中
     * 只要跳到下一个页面取出这个数据以后 session里面的数据就会删掉
     * //TODO 分布式下的session问题
     * @param vo
     * @param result
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //校验出错 转发到注册页
            //model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //真正的注册 调用远程服务进行注册
        //1. 校验验证码正确
        String code = vo.getCode();
        String s = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        String code1;
        if (StringUtils.hasText(s)){
            code1= s.split("_")[0];
            if (code.equals(code1)){
                // 删除验证码 令牌机制
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                // 验证码通过 调用远程服务进行注册
                R r = memberFeignService.regist(vo);
                if (r.getCode() == 0){
                    //成功
                    return "redirect:http://auth.gulimall.com/login.html";
                }else{
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }

            }else{
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else{
            //校验失败 回到注册页
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "请先用手机发送验证码");
            redirectAttributes.addFlashAttribute("errors", errors);

            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){

        R r = memberFeignService.login(vo);
        if(r.getCode() == 0){
            //成功
            MemberResponseVo data = r.getData("data", new TypeReference<MemberResponseVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://gulimall.com";
        }else{
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute == null){
            return "login";
        }else{
            return "redirect:http://gulimall.com";
        }


    }
}
