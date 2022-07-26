package com.shengbo.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/*
封装订单提交的数据
 */
@Data
public class OrderSubmitVo {
    private Long addrId;
    private Integer payType;
    //购买的商品要去购物车获取一遍
    private String orderToken;//防重令牌
    private BigDecimal payPrice; // 应付价格 验价

    //用户相关信息直接去session中取
    private String note;//备注

}
