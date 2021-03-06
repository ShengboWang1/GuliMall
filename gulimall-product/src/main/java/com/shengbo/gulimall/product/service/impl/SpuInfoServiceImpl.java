package com.shengbo.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.shengbo.common.constant.ProductConstant;
import com.shengbo.common.to.SkuReductionTo;
import com.shengbo.common.to.SpuBoundTo;
import com.shengbo.common.to.es.SkuEsModel;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.product.entity.*;
import com.shengbo.gulimall.product.feign.CouponFeignService;
import com.shengbo.gulimall.product.feign.SearchFeignService;
import com.shengbo.gulimall.product.feign.WareFeignService;
import com.shengbo.gulimall.product.service.*;
import com.shengbo.gulimall.product.vo.*;
import com.shengbo.gulimall.ware.vo.SkuHasStockVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
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
    @Autowired
    BrandService brandService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    SearchFeignService searchFeignService;
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
        // 1??????spu???????????? pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        // 2??????spu???????????? pms_spu_info_desc
        List<String> descript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", descript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        // 3??????spu????????? pms_spu_images
        List<String> images = vo.getImages();
        SpuImagesEntity imagesEntity = new SpuImagesEntity();
        spuImagesService.saveImages(infoEntity.getId(), images);

        // 4??????spu??????????????? pms_product_attr_value
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
        // 5??????spu??????????????? sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("????????????spu??????????????????");
        }


        // 5????????????spu???????????????sku??????
        // 5.1 sku???????????? pms_sku_info
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
                    //??????true????????????
                    return StringUtils.hasText(entity.getImgUrl());
                }).collect(Collectors.toList());
                // 5.2 sku???????????? pms_sku_images
                skuImagesService.saveBatch(imagesEntities);
                //TODO ?????????????????????????????????

                // 5.3 sku????????????????????? pms_sku_sale_attr_value
                List<Attr> attr = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, attrValueEntity);
                    attrValueEntity.setSkuId(skuId);
                    return attrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);
                // 5.4 sku????????????????????? gulimall-sms ->sms_sku_ladder sms_sku_full_reduction
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                if(skuReductionTo.getFullCount() >0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    skuReductionTo.setSkuId(skuId);
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("????????????sku??????????????????");
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
         * catelogId: 6,//????????????id
         *    brandId: 1,//??????id
         *    status: 0,//????????????
         */
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    //????????????
    @Override
    public void up(Long spuId) {
        //???????????????????????????

        //1.????????????spuId???????????????sku?????? ???????????????
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIds = skus.stream().map(sku -> {
            return sku.getSkuId();
        }).collect(Collectors.toList());
        //TODO ????????????sku????????????????????????????????????
        List<ProductAttrValueEntity> entities = attrValueService.bastAttrListForSpu(spuId);
        List<Long> attrIds = entities.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrsList = entities.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs);
            return attrs;
        }).collect(Collectors.toList());

        //TODO ?????????????????? ?????????????????????????????????
        Map<Long, Boolean> stockMap =null;
        try{
            R r = wareFeignService.getSkuHasStock(skuIds);
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {
            };
            List<SkuHasStockVo> data = r.getData(typeReference);
            stockMap = data.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        }catch (Exception e){
            log.error("????????????????????????:??????{}", e);
        }



        //2.????????????sku?????????
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            //??????????????????
            if (finalStockMap == null){
                esModel.setHasStock(true);
            }else{
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }

            //TODO ????????????
            esModel.setHotScore(0L);
            //TODO ????????????????????????????????????
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());
            //??????????????????
            esModel.setAttrs(attrsList);

            return esModel;
        }).collect(Collectors.toList());

        //TODO ??????????????????es???????????? gulimall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0){
            //??????????????????
            //TODO ????????????SPU???????????????
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else{
            //??????????????????
            //TODO ?????????????????????????????????
        }
    }

}