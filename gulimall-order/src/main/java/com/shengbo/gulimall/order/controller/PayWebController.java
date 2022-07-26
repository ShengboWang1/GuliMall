package com.shengbo.gulimall.order.controller;

import com.alipay.api.AlipayApiException;
import com.shengbo.gulimall.order.config.AlipayTemplate;
import com.shengbo.gulimall.order.service.OrderService;
import com.shengbo.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {
    @Autowired
    AlipayTemplate alipayTemplate;
    @Autowired
    OrderService orderService;
    @ResponseBody
    @GetMapping(value = "/aliPayOrder", produces = "text/html")
    public String payOrder(@RequestParam("orderSn")  String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPay(orderSn);
        String pay = alipayTemplate.pay(payVo);
        System.out.println(pay);
        return pay;
    }
}
