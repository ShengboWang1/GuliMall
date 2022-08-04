package com.shengbo.gulimall.product.vo;

import com.shengbo.gulimall.product.entity.SkuImagesEntity;
import com.shengbo.gulimall.product.entity.SkuInfoEntity;
import com.shengbo.gulimall.product.entity.SpuInfoDescEntity;
import com.shengbo.gulimall.product.service.impl.SkuSaleAttrValueServiceImpl;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {
    //1. sku的基本信息获取 pms_sku_info
    private SkuInfoEntity info;
    //1.1 sku是否有货
    private boolean hasStock = true;
    //2. sku的图片信息 pms_sku_images
    private List<SkuImagesEntity> images;
    //3. 获取spu的销售属性组合（多少种sku）
    private List<SkuItemSaleAttrVo> saleAttrs;
    //4. 获取spu的介绍 spu_info
    private SpuInfoDescEntity desp;
    //5. 获取spu的规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;

    private SeckillInfoVo seckillInfoVo;

    @ToString
    @Data
    public static class SkuItemSaleAttrVo{
        private String attrName;
        private Long attrId;
        private List<AttrValueWithSkuIdVo> attrValues;
    }

    @ToString
    @Data
    public static class SpuItemAttrGroupVo{
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    @ToString
    @Data
    public static class SpuBaseAttrVo{
        private String attrName;
        private String attrValue;
    }
}
