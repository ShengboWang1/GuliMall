package com.shengbo.gulimall.search.vo;

import com.shengbo.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {

    private List<SkuEsModel> product;//查询到的所有商品信息
    /**
     * 以下是分页信息
     */
    private Integer pageNum; //当前页码
    private Long total;// 总记录数
    private Integer totalPages;
    //private List<Integer> pageNavs;

    private List<BrandVo> brands;//当前查询到的结果 所有涉及到的品牌
    private List<CatalogVo> catalogs;//当前查询到的结果 所有涉及到的分类
    private List<AttrVo> attrs;//当前查询到的结果 所有涉及到的属性

    //private List<NavVo> navs;

//    @Data
//    public static class NavVo{
//        private String navName;
//        private String navValue;
//        private String link;
//    }

    //==========以上是返回给咱们页面的所有信息===========
    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandImg;
        private String brandName;
    }

    @Data
    public static class CatalogVo{
        private Long CatalogId;
        private String CatalogName;
    }

    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValues;
    }
}
