package com.shengbo.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 */
@ToString
public class Cart {
    private List<CartItem> items;

    private Integer countNum;//商品数量

    private Integer countType;//商品类型的数量

    private BigDecimal totalAmount;//商品总价

    private BigDecimal reduce = new BigDecimal("0.00");//减免价格

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int countNum = 0;
        if(items!= null && items.size()>0){
            for (CartItem item : items) {
                countNum += item.getCount();
            }
        }
        return countNum;
    }


    public Integer getCountType() {
        if(items!= null){
            return items.size();
        }else{
            return 0;
        }
    }


    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        if(items!= null && items.size()>0){
            for (CartItem item : items) {
                BigDecimal totalPrice = item.getTotalPrice();
                amount = amount.add(totalPrice);
            }
        }
        return amount.subtract(getReduce());
    }


    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
