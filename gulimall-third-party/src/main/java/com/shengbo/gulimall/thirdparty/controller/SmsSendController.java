package com.shengbo.gulimall.thirdparty.controller;

import com.shengbo.common.utils.R;
import com.shengbo.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sms")
public class SmsSendController {
    @Autowired
    SmsComponent smsComponent;
    /**
     * t提供给别的微服务进行校验
     * @param phone
     * @param code
     * @return
     */
    @ResponseBody
    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code, @RequestParam("expireTime") String expireTime){
        smsComponent.sendCode(phone, code, expireTime);
        return R.ok();
    }

}
