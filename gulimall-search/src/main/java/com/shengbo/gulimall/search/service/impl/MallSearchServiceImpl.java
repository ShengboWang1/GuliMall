package com.shengbo.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import com.shengbo.common.to.es.SkuEsModel;
import com.shengbo.gulimall.search.service.MallSearchService;
import com.shengbo.gulimall.search.vo.SearchParam;
import com.shengbo.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.shengbo.gulimall.search.constant.EsConstant.PRODUCT_INDEX;
import static com.shengbo.gulimall.search.constant.EsConstant.PRODUCT_PAGESIZE;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private ElasticsearchClient esClient;

    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult result = null;
        // 检索：
        //1.模糊匹配
        //2. 过滤 按照属性 分类 品牌 价格区间 库存
        //3.排序 分页 高亮
        //4.聚合分析
        try {
            SearchResponse<SkuEsModel> response = buildSearchResponse(searchParam);
            show_result(response);
            result = buildSearchResult(response, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void show_result(SearchResponse<SkuEsModel> response) {
        List<Hit<SkuEsModel>> hits = response.hits().hits();

        List<LongTermsBucket> brand_aggs = response.aggregations().get("brand_agg").lterms().buckets().array();
        for (LongTermsBucket brand_agg : brand_aggs) {
            List<StringTermsBucket> brand_name_aggs = brand_agg.aggregations().get("brand_name_agg").sterms().buckets().array();
            for (StringTermsBucket brand_name_agg : brand_name_aggs) {
                System.out.println("brand_name...");
                System.out.println(brand_name_agg.key());
            }
            List<StringTermsBucket> brand_img_aggs = brand_agg.aggregations().get("brand_img_agg").sterms().buckets().array();
            for (StringTermsBucket brand_img_agg : brand_img_aggs) {
                System.out.println("brand_img...");
                System.out.println(brand_img_agg.key());
            }
        }

        List<LongTermsBucket> catalog_aggs = response.aggregations().get("catalog_agg").lterms().buckets().array();
        for (LongTermsBucket catalog_agg : catalog_aggs) {
            List<StringTermsBucket> catalog_name_aggs = catalog_agg.aggregations().get("catalog_name_agg").sterms().buckets().array();
            for (StringTermsBucket catalog_name_agg : catalog_name_aggs) {
                System.out.println("catalog_name...");
                System.out.println(catalog_name_agg.key());
            }
        }

        NestedAggregate attr_agg = response.aggregations().get("attr_agg").nested();
        List<LongTermsBucket> attr_id_aggs = attr_agg.aggregations().get("attr_id_agg").lterms().buckets().array();
        for (LongTermsBucket attr_id_agg : attr_id_aggs) {
            List<StringTermsBucket> attr_name_aggs = attr_id_agg.aggregations().get("attr_name_agg").sterms().buckets().array();
            for (StringTermsBucket attr_name_agg : attr_name_aggs) {
                System.out.println("attr_name...");
                System.out.println(attr_name_agg.key());
            }
            List<StringTermsBucket> attr_value_aggs = attr_id_agg.aggregations().get("attr_value_agg").sterms().buckets().array();
            for (StringTermsBucket attr_value_agg : attr_value_aggs) {
                System.out.println("attr_value...");
                System.out.println(attr_value_agg.key());
            }
        }


        for (Hit<SkuEsModel> hit : hits) {
            SkuEsModel skuEsModel = hit.source();
            System.out.println("Found product " + skuEsModel.toString() + ", score " + hit.score());
        }
    }

    private SearchResponse<SkuEsModel> buildSearchResponse(SearchParam searchParam) throws IOException {
        SearchResponse<SkuEsModel> response = null;
        response = esClient.search(s -> {
                    //指定数据库
                    s.index(PRODUCT_INDEX);
                    s.query(q -> q
                            .bool(b -> {
                                        Query catalogIdQuery1 = TermQuery.of(t -> t.field("catalogId").value(FieldValue.of(225)))._toQuery();
                                        b.must(catalogIdQuery1);
                                        // 1。1 模糊检索关键字
                                        if (StringUtils.hasText(searchParam.getKeyword())) {
                                            Query skuTitleQuery = getSkuTitleQuery(searchParam);
                                            b.must(skuTitleQuery);
                                        }
                                        // 1。2 三级分类
                                        if (null != searchParam.getCatalog3Id()) {
                                            Query catalogIdQuery = getCatalogIdQuery(searchParam);
                                            b.filter(catalogIdQuery);
                                        }
                                        // 1。3 品牌
                                        if (null != searchParam.getBrandIds() && searchParam.getBrandIds().size() > 0) {
                                            Query brandIdQuery = getBrandIdQuery(searchParam);
                                            b.filter(brandIdQuery);
                                        }
                                        // 1。4 nested Attr
                                        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {
                                            searchParam.getAttrs().forEach(item -> {
                                                Query attrsQuery = getAttrsQuery(item);
                                                b.filter(attrsQuery);
                                            });
                                        }
                                        // 1。5 库存
                                        if (null != searchParam.getHasStock()) {
                                            Query hasStockQuery = getHasStockQuery(searchParam);
                                            b.filter(hasStockQuery);
                                        }
                                        //价格区间
                                        if (StringUtils.hasText(searchParam.getSkuPrice())) {
                                            Query SkuPriceQuery = getSkuPriceQuery(searchParam);
                                            b.filter(SkuPriceQuery);
                                        }
                                        return b;
                                    }
                            )
                    );
                    if (StringUtils.hasText(searchParam.getSort())) {
                        // 2。1排序
                        String str = searchParam.getSort();
                        //sort=hotScore_asc/desc
                        String[] strs = str.split("_");
                        SortOrder order = strs[1].equalsIgnoreCase("asc") ? SortOrder.Asc : SortOrder.Desc;
                        SortOptions sortOptions = new SortOptions.Builder().field(t -> t.field(strs[0]).order(order)).build();
                        s.sort(sortOptions);
                    }

                    // 2。2 分页 如果pageSize = 5
                    // pageNum：1 from 0 size 5 【0，1，2，3，4】
                    // pageNum：2 from 5 size 5 【5，6，7，8，9】
                    s.from((searchParam.getPageNum() - 1) * PRODUCT_PAGESIZE);
                    s.size(PRODUCT_PAGESIZE);

                    //2。3 高亮 在有关键词进行模糊匹配的时候
                    if (StringUtils.hasText(searchParam.getKeyword())) {
                        Highlight skuTitle = new Highlight.Builder().fields("skuTitle", new HighlightField.Builder().build())
                                .preTags("<b style='color.red'>").postTags("</b").build();
                        s.highlight(skuTitle);
                    }
                    //聚合分析
                    s.aggregations("brand_agg", a ->
                            a.terms(t -> t.field("brandId").size(50))
                                    .aggregations("brand_name_agg", name -> name.terms(t -> t.field("brandName").size(1)))
                                    .aggregations("brand_img_agg", img -> img.terms(t -> t.field("brandImg").size(1)))
                    );
                    s.aggregations("catalog_agg", a ->
                            a.terms(t -> t.field("catalogId").size(20))
                                    .aggregations("catalog_name_agg", name -> name.terms(t -> t.field("catalogName").size(1)))
                    );
//                    瞎jb写的 不好用 先留着
//                    NestedAggregation.Builder nested1 = new NestedAggregation.Builder().name("attr_agg").path("attrs");
//                    TermsAggregation attrIdBuilder = AggregationBuilders.terms().field("attrs.attrId").size(1).build();
//
//                    TermsAggregation attrValueBuilder = AggregationBuilders.terms().field("attrs.attrValue").size(1).build();
//                    NestedAggregation build = new NestedAggregation.Builder().path("attrs").build();
                    s.aggregations("attr_agg", a ->
                            a.nested(n -> n.path("attrs"))
                                    .aggregations("attr_id_agg", attrId -> attrId
                                            .terms(t -> t.field("attrs.attrId"))
                                            .aggregations("attr_name_agg", attrValue -> attrValue.terms(t -> t.field("attrs.attrName").size(1)))
                                            .aggregations("attr_value_agg", attrValue -> attrValue.terms(t -> t.field("attrs.attrValue").size(50))))
                    );
                    return s;
                }
                ,
                SkuEsModel.class
        );

        return response;
    }

    private Query getAttrsQuery(String attrItem) {
        String[] s = attrItem.split("_");
        String attrId = s[0];
        String[] attrValues = s[1].split(":");//这个属性检索用的值
        Query attrIdQuery = TermQuery.of(t -> t.field("attrs.attrId").value(FieldValue.of(attrId)))._toQuery();
        FieldValue[] attrFieldValues = new FieldValue[attrValues.length];
        for (int i = 0; i < attrValues.length; i++) {
            attrFieldValues[i] = FieldValue.of(attrValues[i]);
        }

        TermsQueryField termsQueryField = new TermsQueryField.Builder()
                .value(Arrays.asList(attrFieldValues))
                .build();
        Query attrValueQuery = TermsQuery.of(t -> t.field("attrs.attrValue").terms(termsQueryField))._toQuery();
        Query nestAttrQuery = NestedQuery.of(t -> t.path("attrs")
                .query(q -> q.bool(b -> b
                        .must(attrIdQuery)
                        .must(attrValueQuery)
                )))._toQuery();
        return nestAttrQuery;
    }

    private Query getSkuPriceQuery(SearchParam searchParam) {
        String[] price = searchParam.getSkuPrice().split("_");
        //System.out.println("price数组的长度为。。。。。。。。。。。。。");
        //System.out.println(price.length);
        Query skuPriceQuery = null;
        if (price.length == 2) {
            skuPriceQuery = RangeQuery.of(t -> t.field("skuPrice").gte(JsonData.of(price[0])).lte(JsonData.of(price[1])))._toQuery();
        } else if (price.length == 1) {
            if (searchParam.getSkuPrice().startsWith("_")) {
                skuPriceQuery = RangeQuery.of(t -> t.field("skuPrice").lte(JsonData.of(price[0])))._toQuery();
            } else if (searchParam.getSkuPrice().endsWith("_")) {
                skuPriceQuery = RangeQuery.of(t -> t.field("skuPrice").gte(JsonData.of(price[0])))._toQuery();
            }
        }
        return skuPriceQuery;
    }

    private Query getHasStockQuery(SearchParam searchParam) {
        Query hasStockQuery = TermQuery.of(t -> t.field("hasStock").value(FieldValue.of(searchParam.getHasStock() == 1)))._toQuery();
        return hasStockQuery;
    }

    private Query getBrandIdQuery(SearchParam searchParam) {
        Query brandIdQuery = TermsQuery.of(t -> t.field("brandId").terms((TermsQueryField) searchParam.getBrandIds()))._toQuery();
        return brandIdQuery;
    }

    private Query getCatalogIdQuery(SearchParam searchParam) {
        Query catalogIdQuery = TermQuery.of(t -> t.field("catalogId").value(FieldValue.of(searchParam.getCatalog3Id())))._toQuery();
        return catalogIdQuery;
    }

    private Query getSkuTitleQuery(SearchParam searchParam) {
        Query skuTitleQuery = MatchQuery.of(t -> t.field("skuTitle").query(FieldValue.of(searchParam.getKeyword())))._toQuery();
        return skuTitleQuery;
    }

    /**
     * 构建结果数据
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse<SkuEsModel> response, SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();
        List<Hit<SkuEsModel>> hits = response.hits().hits();
        List<SkuEsModel> skuEsModels = new ArrayList<>();
        if (hits != null && hits.size()>0){
            for (Hit<SkuEsModel> hit:hits){
                skuEsModels.add(hit.source());
            }
        }
        //返回商品 从hits中获取
        searchResult.setProduct(skuEsModels);

        //属性 品牌 分类 从聚合中获取
        Map<String, Aggregate> aggregations = response.aggregations();

        //attr的聚合。。。
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        NestedAggregate attr_agg = aggregations.get("attr_agg").nested();
        List<LongTermsBucket> attr_id_aggs = attr_agg.aggregations().get("attr_id_agg").lterms().buckets().array();
        for (LongTermsBucket attr_id_agg : attr_id_aggs) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            attrVo.setAttrId(Long.valueOf(attr_id_agg.key()));
            List<StringTermsBucket> attr_name_aggs = attr_id_agg.aggregations().get("attr_name_agg").sterms().buckets().array();
            attrVo.setAttrName(attr_name_aggs.get(0).key());
            List<StringTermsBucket> attr_value_aggs = attr_id_agg.aggregations().get("attr_value_agg").sterms().buckets().array();
            List<String> attrValues = new ArrayList<>();
            for (StringTermsBucket attr_value_agg : attr_value_aggs) {
                attrValues.add(attr_value_agg.key());
            }
            attrVo.setAttrValues(attrValues);
            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);
        //Brand的聚合。。。
        List<LongTermsBucket> brand_aggs = aggregations.get("brand_agg").lterms().buckets().array();
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        for (LongTermsBucket brand_agg:brand_aggs){
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            String key = brand_agg.key();
            brandVo.setBrandId(Long.valueOf(key));
            List<StringTermsBucket> brand_name_agg = brand_agg.aggregations().get("brand_name_agg").sterms().buckets().array();
            brandVo.setBrandName(brand_name_agg.get(0).key());
            List<StringTermsBucket> brand_img_agg = brand_agg.aggregations().get("brand_img_agg").sterms().buckets().array();
            brandVo.setBrandImg(brand_img_agg.get(0).key());
            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);
        // Catalog的聚合。。。
        List<LongTermsBucket> catalog_aggs = aggregations.get("catalog_agg").lterms().buckets().array();
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for (LongTermsBucket catalog_agg:catalog_aggs){
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String key = catalog_agg.key();
            catalogVo.setCatalogId(Long.valueOf(key));
            List<StringTermsBucket> attr_name_agg = catalog_agg.aggregations().get("catalog_name_agg").sterms().buckets().array();
            catalogVo.setCatalogName(attr_name_agg.get(0).key());
            catalogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catalogVos);

        //分页信息
        searchResult.setPageNum(searchParam.getPageNum());
        //总记录数 total个数
        long total = response.hits().total().value();
        searchResult.setTotal(total);
        //总页码 计算得到
        searchResult.setTotalPages((int) Math.ceil(total/PRODUCT_PAGESIZE));

//        List<Integer> pageNavs = new ArrayList<>();
//        for (int i = 1; i < total; i++) {
//            pageNavs.add(i);
//        }
//        searchResult.setPageNavs(pageNavs);

        //构建面包屑导航
        //不整了
        return searchResult;
    }
}
