package com.shengbo.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareResponseVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}
