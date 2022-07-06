package com.shengbo.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.cart.feign.ProductFeignService;
import com.shengbo.gulimall.cart.intercepter.CartIntercepter;
import com.shengbo.gulimall.cart.service.CartService;
import com.shengbo.gulimall.cart.vo.Cart;
import com.shengbo.gulimall.cart.vo.CartItem;
import com.shengbo.gulimall.cart.vo.SkuInfoVo;
import com.shengbo.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    /**
     * 获取购物车中的某个购物项目
     * @param skuId
     * @return
     */
    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();

        //区分是否登录
        UserInfoTo userInfoTo = CartIntercepter.threadLocal.get();
        if (userInfoTo.getUserId()!= null){
            //登录状态
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            String tempCartkey = CART_PREFIX + userInfoTo.getUserKey();
            //如果临时购物车的数据还没有进行合并
            List<CartItem> tempCartItems = getCartItems(tempCartkey);
            if (tempCartItems != null){
                //临时购物车有数据 需要将其添加到用户购物车中
                for (CartItem tempCartItem : tempCartItems) {
                    //合并购物车
                    addToCart(tempCartItem.getSkuId(), tempCartItem.getCount());
                }
                clearCart(tempCartkey);
            }
            // 再获取登陆后的购物车数据【包含合并过来的临时购物车的数据和登录后的购物车的数据】
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }else{
            //没登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车的所有购物项目
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    /**
     * 添加到购物车
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //获取我们要操作的购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //添加新商品到购物车
        String s = (String) cartOps.get(skuId.toString());
        if (StringUtils.hasText(s)) {
            //有这个商品
            CartItem cartItem = JSON.parseObject(s, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {
            CartItem cartItem = new CartItem();
            //1。根据skuId远程查询 要添加的商品信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });

                //2。将商品添加到购物车
                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(data.getPrice());
            }, executor);

            //远程查询sku的组合信息
            CompletableFuture<Void> getSkuSaleAttrTask = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(skuSaleAttrValues);
            }, executor);
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrTask).get();
            String s1 = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s1);
            return cartItem;
        }



    }


    /**
     * 获取我们要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartIntercepter.threadLocal.get();
        // 看是临时购物车否
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = stringRedisTemplate.boundHashOps(cartKey);
        return operations;
    }

    private List<CartItem> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if(values!= null && values.size() != 0){
            List<CartItem> collect = values.stream().map(item -> {
                String str = (String) item;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    @Override
    public void clearCart(String cartKey){
        stringRedisTemplate.delete(cartKey);
    }

    //勾选购物项目
    @Override
    public void checkItem(Long skuId, Integer checked) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(checked==1?true:false);
        String jsonString = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), jsonString);

    }

    @Override
    public void countItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String jsonString = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), jsonString);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }
}
