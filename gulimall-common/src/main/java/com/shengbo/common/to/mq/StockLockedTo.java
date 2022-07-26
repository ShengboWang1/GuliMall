package com.shengbo.common.to.mq;

import lombok.Data;

import java.util.List;

@Data
public class StockLockedTo {
    //2张表的id
    private Long id;//库存工作单的id

    private StockDetailTo detailTo;//库存详情工作单


}
