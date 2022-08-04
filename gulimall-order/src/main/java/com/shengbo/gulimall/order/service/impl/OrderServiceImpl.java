package com.shengbo.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.shengbo.common.exception.NoStockException;
import com.shengbo.common.to.OrderTo;
import com.shengbo.common.to.mq.SeckillOrderTo;
import com.shengbo.common.utils.R;
import com.shengbo.common.vo.MemberResponseVo;
import com.shengbo.gulimall.order.constant.OrderConstant;
import com.shengbo.gulimall.order.dao.OrderItemDao;
import com.shengbo.gulimall.order.entity.OrderItemEntity;
import com.shengbo.gulimall.order.entity.PaymentInfoEntity;
import com.shengbo.gulimall.order.enume.OrderStatusEnum;
import com.shengbo.gulimall.order.feign.CartFeignService;
import com.shengbo.gulimall.order.feign.MemberFeignService;
import com.shengbo.gulimall.order.feign.ProductFeignService;
import com.shengbo.gulimall.order.feign.WmsFeignService;
import com.shengbo.gulimall.order.interceptor.LoginUserInterceptor;
import com.shengbo.gulimall.order.service.OrderItemService;
import com.shengbo.gulimall.order.service.PaymentInfoService;
import com.shengbo.gulimall.order.service.to.OrderCreateTo;
import com.shengbo.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.order.dao.OrderDao;
import com.shengbo.gulimall.order.entity.OrderEntity;
import com.shengbo.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();
    @Autowired
    OrderItemService orderItemService;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //从拦截器中获取id
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        //Feign异步问题：获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //1。远程查询所有收货地址列表
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //每一个异步线程都同步之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            confirmVo.setAddressVos(address);
        }, executor);

        //2. 远程查询购物车所有选中的购物项目
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItemVos(currentUserCartItems);
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> itemVos = confirmVo.getItemVos();
            List<Long> collect = itemVos.stream().map(itemVo -> {
                return itemVo.getSkuId();
            }).collect(Collectors.toList());
            R skuHasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = skuHasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if(data != null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        //3. 查询用户的积分
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegeration(integration);

        //4。 价格等数据自动计算

        //TODO 5。防重令牌 给服务器和页面各存储一个
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }

//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        submitVoThreadLocal.set(vo);
        //验证令牌： 令牌的对比和删除必须得有原子性！！！
        // 0删除失败 1删除成功
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        String orderToken = vo.getOrderToken();
        //原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()),
                        orderToken);

        if(result == 0L){
            //失败咯
            responseVo.setCode(1);
            return responseVo;
        }
        //验证成功
        else{
            //下单：创建订单 验证令牌 验证价格 锁库存。。。
            //1.创建订单 订单项
            OrderCreateTo order = createOrder();
            //2。验价
            BigDecimal payAmount = order.getOrderEntity().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01){
                //金额对比成功
                //3.保存订单
                saveOrder(order);
                //4.锁定库存 只要有异常 就回滚订单数据
                //订单号 订单项目（skuId， skuName, num）
                WareSkuLockVo lockVo = new WareSkuLockVo();
                // 4。1 订单号
                lockVo.setOrderSn(order.getOrderEntity().getOrderSn());
                // 4。2订单项目
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //TODO 远程锁库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if(r.getCode() == 0){
                    //库存锁定成功
                    responseVo.setOrderEntity(order.getOrderEntity());
                    //TODO 远程扣减积分出异常
                    //int i = 10/0;
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrderEntity());
                    return responseVo;
                }else{
                    //库存锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                    //responseVo.setCode(3);
//                    return responseVo;
                }
            }else{
                responseVo.setCode(2);
                return responseVo;
            }
        }


    }

    @Override
    public OrderEntity getOrderBySn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        //先来查询订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if(orderEntity == null){
            System.out.println("这tm是个空的。。");
        }
        if(orderEntity.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())){
            //关单
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            orderTo.setStatus(OrderStatusEnum.CANCLED.getCode());
            //再发给MQ一个消息 告诉他订单取消咯
            try{
                //TODO 保证消息一定发送出去 ，每个消息都做好日志记录（给数据库保存消息的状态信息）
                //TODO 定期扫描数据库 将失败的消息重新发送
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            }catch (Exception e){
                //TODO 将没发送成功的消息 进行重试发送
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            }

        }


    }

    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderBySn(orderSn);
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(orderSn);

        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity orderItemEntity = order_sn.get(0);
        payVo.setSubject(orderItemEntity.getSkuName());
        payVo.setBody(orderItemEntity.getSkuAttrsVals());
        return payVo;


    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        Long id = memberResponseVo.getId();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", id).orderByDesc("id")
        );
        List<OrderEntity> collect = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(collect);
        return new PageUtils(page);

    }

    //处理支付宝的支付结果
    @Override
    public String handelPayResult(PayAsyncVo vo) {
        //1。保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);

        //2.修改订单的状态信息
        if(vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")){
            //支付成功 按照订单号修改订单
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * 创建秒杀单信息
     * @param orderTo 从MQ拿到的To
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {
        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        //保存订单
        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setRealAmount(totalPrice);

        orderItem.setSkuQuantity(orderTo.getNum());

        //保存商品的spu信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItem.setSpuId(spuInfoData.getId());
        orderItem.setSpuName(spuInfoData.getSpuName());
        orderItem.setSpuBrand(spuInfoData.getBrandId().toString());
        orderItem.setCategoryId(spuInfoData.getCatalogId());

        //保存订单项数据
        orderItemService.save(orderItem);
    }



    /**
     * 保存订单数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrderEntity();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);

    }

    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo = new OrderCreateTo();

        //1.生成一个订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);
        //2.获取到所有的订单项目
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        //3.计算价格
        computePrice(orderEntity, itemEntities);
        orderCreateTo.setOrderEntity(orderEntity);
        orderCreateTo.setOrderItems(itemEntities);
        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        //1。订单中价格相关的：
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal giftIntegration = new BigDecimal("0.0");
        BigDecimal giftGrowth = new BigDecimal("0.0");
        //订单的总额=每一个订单项的总额
        for (OrderItemEntity itemEntity : itemEntities) {
            coupon = coupon.add(itemEntity.getRealAmount());
            integration = integration.add(itemEntity.getIntegrationAmount());
            promotion = promotion.add(itemEntity.getPromotionAmount());

            total = total.add(itemEntity.getRealAmount());

            giftIntegration = giftIntegration.add(new BigDecimal(itemEntity.getGiftIntegration().toString()));
            giftGrowth = giftGrowth.add(new BigDecimal(itemEntity.getGiftGrowth().toString()));
        }
        orderEntity.setTotalAmount(total);
        //总额加上运费 为应付多少钱
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);

        //设置积分和成长值信息
        orderEntity.setIntegration(giftIntegration.intValue());
        orderEntity.setGrowth(giftGrowth.intValue());

        orderEntity.setDeleteStatus(0);//未删除


    }

    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(memberResponseVo.getId());

        //获取库存服务中邮费
        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //设置运费信息
        orderEntity.setFreightAmount(fareResp.getFare());

        //设置收货人信息
        MemberAddressVo address = fareResp.getAddress();
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverRegion(address.getRegion());

        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        return orderEntity;
    }

    /**
     * 构建订单项数据
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确认每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems!=null && currentUserCartItems.size()>0){
            List<OrderItemEntity> collect = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 构建每一个订单项目
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //1.订单信息 订单号 已经有了
        //商品spu信息
        R r = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(data.getId());
        orderItemEntity.setSpuBrand(data.getBrandId().toString());
        orderItemEntity.setSpuName(data.getSpuName());
        orderItemEntity.setCategoryId(data.getCatalogId());
        //商品sku信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        //优惠信息
        //积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());

        //订单项目的价格信息
        // 3种优惠
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单的实际金额
        BigDecimal orginal = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal finalAmount = orginal.subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(finalAmount);


        return orderItemEntity;
    }
}