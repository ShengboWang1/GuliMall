package com.shengbo.gulimall.cart.service;

import com.shengbo.gulimall.cart.vo.Cart;
import com.shengbo.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    //清空购物车数据
    void clearCart(String cartKey);

    void checkItem(Long skuId, Integer checked);

    void countItem(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItem> getCurrentUserCartItems();
}
