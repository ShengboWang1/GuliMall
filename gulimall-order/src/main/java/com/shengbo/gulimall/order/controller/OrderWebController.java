package com.shengbo.gulimall.order.controller;

import com.shengbo.common.exception.NoStockException;
import com.shengbo.gulimall.order.entity.OrderEntity;
import com.shengbo.gulimall.order.service.OrderService;
import com.shengbo.gulimall.order.vo.OrderConfirmVo;
import com.shengbo.gulimall.order.vo.OrderSubmitVo;
import com.shengbo.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {
    @Autowired
    OrderService orderService;

    /**
     * 往订单确认页里面扔数据
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo vo = orderService.confirmOrder();
        model.addAttribute("confirmOrderData", vo);
        return "confirm";
    }
    /**
     * 下单功能
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        try{
            SubmitOrderResponseVo submitOrderResponseVo = orderService.submitOrder(vo);
            if(submitOrderResponseVo.getCode() == 0){
                //下单成功 来到支付选择页
                model.addAttribute("submitOrderResp", submitOrderResponseVo);
                return "pay";
            }else{
                // 下单失败 来到订单确认页重新确认订单信息
                String msg = "下单失败,";
                switch (submitOrderResponseVo.getCode()){
                    case 1: msg += "订单信息过期， 请刷新再次提交"; break;
                    case 2: msg += "订单价格发生变化， 请确认后再次提交"; break;
                    case 3: msg += "商品存在无库存情况， 请确认后再次提交"; break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        }catch (Exception e){
            if (e instanceof NoStockException) {
                String message = ((NoStockException)e).getMessage();
                redirectAttributes.addFlashAttribute("msg",message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }


    }




}
