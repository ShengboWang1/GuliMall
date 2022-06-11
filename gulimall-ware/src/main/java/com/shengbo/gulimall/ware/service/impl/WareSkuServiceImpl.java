package com.shengbo.gulimall.ware.service.impl;

import com.shengbo.common.utils.R;
import com.shengbo.gulimall.ware.feign.ProductFeignService;
import com.shengbo.gulimall.ware.vo.SkuHasStockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.ware.dao.WareSkuDao;
import com.shengbo.gulimall.ware.entity.WareSkuEntity;
import com.shengbo.gulimall.ware.service.WareSkuService;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    ProductFeignService productFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        /**
         * wareId: 123,//仓库id
         *    skuId: 123//商品id
         */
        String skuId = (String) params.get("skuId");
        if(StringUtils.hasText(skuId)){
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if(StringUtils.hasText(wareId)){
            wrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1。判断如果还没有这个库存记录就是新增
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if(wareSkuEntities == null || wareSkuEntities.size()== 0){
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setStock(skuNum);
            skuEntity.setSkuId(skuId);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //远程查询sku的名字 如果失败 事务无需回滚
            //1 自己catch
            //2 TODO 还可以什么办法让异常出现以后不回滚？
            try{
                R info = productFeignService.info(skuId);
                if(info.getCode() == 0){
                    Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){

            }

            this.baseMapper.insert(skuEntity);
        }else{
            //2。没有则是更新操作
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId(skuId);
            Long count = baseMapper.getSkuStock(skuId);
            vo.setHasStock(count==null?false:count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;

    }

}