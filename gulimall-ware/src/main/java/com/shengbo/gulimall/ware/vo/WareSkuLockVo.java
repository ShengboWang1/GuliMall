package com.shengbo.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class WareSkuLockVo {
    private String orderSn;
    private List<OrderItemVo> locks;//需要锁住的库存信息

}
