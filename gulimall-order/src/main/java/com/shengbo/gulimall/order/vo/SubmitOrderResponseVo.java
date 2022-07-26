package com.shengbo.gulimall.order.vo;

import com.shengbo.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {
    private OrderEntity orderEntity;
    private Integer code;//状态码 0 成功 剩下都是错误
}
