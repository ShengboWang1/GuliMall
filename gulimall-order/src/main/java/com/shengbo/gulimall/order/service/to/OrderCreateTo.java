package com.shengbo.gulimall.order.service.to;

import com.shengbo.gulimall.order.entity.OrderEntity;
import com.shengbo.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {
    private OrderEntity orderEntity;
    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;//订单计算的应付价格

    private BigDecimal fare;//运费

}
