package com.shengbo.gulimall.seckill.controller;

import com.shengbo.common.utils.R;
import com.shengbo.gulimall.seckill.service.SeckillService;
import com.shengbo.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 返回当前时间可以参与的秒杀商品信息
     * @return
     */
    @ResponseBody
    @GetMapping("/getCurrentSeckillSkus")
    public R getCurrentSeckillSkus(){
        List<SeckillSkuRedisTo> tos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(tos);
    }
    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId){
        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);

    }

    /**
     * 执行秒杀抢购功能
     * @param killId sessionId+skuId
     * @param key randomCode
     * @param num 数量
     * @return
     */

    @GetMapping("/kill")
    public String secKill(@RequestParam("killId") String killId,
                     @RequestParam("key") String key,
                     @RequestParam("num") Integer num,
                          Model model){
        //1.判断是否登录 拦截器
        //2.秒杀
        String orderSn = seckillService.kill(killId, key, num);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }


}
