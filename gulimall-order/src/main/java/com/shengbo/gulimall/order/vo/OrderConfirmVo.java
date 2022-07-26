package com.shengbo.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//订单确认页需要的数据
public class OrderConfirmVo {
    //收获地址 ums_member_receive_address表
    @Setter @Getter
    List<MemberAddressVo> addressVos;

    //所有选中的购物项
    @Setter @Getter
    List<OrderItemVo> itemVos;

    //优惠券信息
    @Setter @Getter
    Integer integeration;

    @Setter @Getter
    Map<Long, Boolean> stocks;


    //防止重复提交令牌
    @Setter @Getter
    String orderToken;

    public Integer getCount(){
        Integer i = 0;
        if(itemVos != null){
            for (OrderItemVo itemVo : itemVos) {
                i += itemVo.getCount();
            }
        }
        return i;
    }
    //积分信息
    //BigDecimal total;//订单总额
    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if(itemVos != null){
            for (OrderItemVo itemVo : itemVos) {
                BigDecimal multiply = itemVo.getPrice().multiply(new BigDecimal(itemVo.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

    BigDecimal payPrice; //应该付款的价格

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
