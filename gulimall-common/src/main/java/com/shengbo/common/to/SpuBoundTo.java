package com.shengbo.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpuBoundTo {
    private Long spuId;
    private BigDecimal bounds;
    private BigDecimal growBounds;
}
