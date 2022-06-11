package com.shengbo.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递的查询条件
 *
 */
@Data
public class SearchParam {
    private String keyword;//全文匹配关键字
    private Long catalog3Id;//三级分类Id
    /**
     * sort = saleCount_asc/desc
     * sort = skuPrice_asc/desc
     * sort = hotScore_asc/desc
     */
    private String sort;//排序条件
    /**
     * 好多的过滤条件
     * hasStock = 0/1 是否有货
     * skuPrice = 1_500 或 _500 或 500_
     * brandId = 1
     * attrs=1_其他：安卓 // 1号属性 值为其他或安卓 可以有多个属性 多个值
     */
    private Integer hasStock;//是否有货 0/1
    private String skuPrice; //价格区间查询
    private List<Long> brandIds;//按照品牌进行查询 可以多选
    private List<String> attrs;//按照属性进行筛选
    private Integer pageNum = 1;//页码


}
