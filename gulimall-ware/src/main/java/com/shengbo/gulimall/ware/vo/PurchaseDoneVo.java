package com.shengbo.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class PurchaseDoneVo {

    @NotNull
    private Long id;//123 采购单id
    private List<PurchaseItemDoneVo> items; //完成/失败的需求详情
}
