package com.shengbo.gulimall.product.service.impl;

import com.shengbo.common.to.SkuReductionTo;
import com.shengbo.common.to.SpuBoundTo;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.product.entity.*;
import com.shengbo.gulimall.product.feign.CouponFeignService;
import com.shengbo.gulimall.product.service.*;
import com.shengbo.gulimall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    SpuImagesService spuImagesService;
    @Autowired
    AttrService attrService;
    @Autowired
    ProductAttrValueService attrValueService;
    @Autowired
    SkuInfoService skuInfoService;
    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    CouponFeignService couponFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1保存spu基本信息 pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        // 2保存spu描述图片 pms_spu_info_desc
        List<String> descript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", descript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        // 3保存spu图片集 pms_spu_images
        List<String> images = vo.getImages();
        SpuImagesEntity imagesEntity = new SpuImagesEntity();
        spuImagesService.saveImages(infoEntity.getId(), images);

        // 4保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity id = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(id.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(infoEntity.getId());
            return valueEntity;
        }).collect(Collectors.toList());
        attrValueService.saveProductAttr(collect);
        // 5保存spu的积分信息 sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程保存spu积分信息失败");
        }


        // 5保存当前spu对应的所有sku信息
        // 5.1 sku基本信息 pms_sku_info
        List<Skus> skus = vo.getSkus();
        if(skus!=null && skus.size()>0){
            skus.forEach(sku->{
                String defaultImg = "";
                for (Images img: sku.getImages()){
                    if (img.getDefaultImg() == 1){
                        defaultImg = img.getImgUrl();
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> imagesEntities = sku.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    //返回true就是需要
                    return StringUtils.hasText(entity.getImgUrl());
                }).collect(Collectors.toList());
                // 5.2 sku图片信息 pms_sku_images
                skuImagesService.saveBatch(imagesEntities);
                //TODO 没有图片路径的无需保存

                // 5.3 sku的销售属性信息 pms_sku_sale_attr_value
                List<Attr> attr = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, attrValueEntity);
                    attrValueEntity.setSkuId(skuId);
                    return attrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);
                // 5.4 sku的优惠满减信息 gulimall-sms ->sms_sku_ladder sms_sku_full_reduction
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                if(skuReductionTo.getFullCount() >0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    skuReductionTo.setSkuId(skuId);
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("远程保存sku优惠信息失败");
                    }
                }

            });
        }


    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
        this.baseMapper.insert(infoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.hasText(key)){
            wrapper.and(w->{
                w.eq("id", key).or().like("spu_name",key);
            });
        }
        String catelogId = (String) params.get("catelogId");
        if(StringUtils.hasText(catelogId)&&! "0".equalsIgnoreCase(catelogId)){
            wrapper.and(w->{
                w.eq("catalog_id", catelogId);
            });
        }
        String brandId = (String) params.get("brandId");
        if(StringUtils.hasText(brandId) &&! "0".equalsIgnoreCase(brandId)){
            wrapper.and(w->{
                w.eq("brand_id", brandId);
            });
        }
        String status = (String) params.get("status");
        if(StringUtils.hasText(status)){
            wrapper.and(w->{
                w.eq("publish_status", status);
            });
        }
        /**
         * catelogId: 6,//三级分类id
         *    brandId: 1,//品牌id
         *    status: 0,//商品状态
         */
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}