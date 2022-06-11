package com.shengbo.gulimall.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.JsonData;
import com.shengbo.gulimall.search.pojo.Baby;
import com.shengbo.gulimall.search.pojo.Product;
import com.shengbo.gulimall.search.vo.SearchParam;
import com.shengbo.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
class GulimallSearchApplicationTests {

    @Autowired
    private ElasticsearchClient esClient;
    private MatchQuery.Builder bySkuTitle;

    @Test
    public void contextLoads() {
        System.out.println(esClient);
    }

    @Test
    public void testFullDsl() throws IOException {
        // 检索：
        //1.模糊匹配
        //2. 过滤 按照属性 分类 品牌 价格区间 库存
        //3.排序 分页 高亮
        //4.聚合分析
        String keyword = "华为";
        Query bySkuTitle = MatchQuery.of(m -> m
                .field("skuTitle")
                .query(FieldValue.of("华为"))
        )._toQuery();

        SearchResponse<SearchResult> response = esClient.search(s->s
                        .index("gulimall_product")
                        .query(q -> q
                                .bool(b -> b
                                        .must(bySkuTitle))
                        ),
                SearchResult.class
        );
        TotalHits total = response.hits().total();
        boolean isExactResult = total.relation() == TotalHitsRelation.Eq;
        System.out.println("??");

        if (isExactResult) {
            System.out.println("There are " + total.value() + " results");
        } else {
            System.out.println("There are more than " + total.value() + " results");
        }
        List<Hit<SearchResult>> hits = response.hits().hits();
        for (Hit<SearchResult> hit : hits) {
            SearchResult searchResult = hit.source();
            System.out.println("Found product " + searchResult.getCatalogs() + ", score " + hit.score());
        }

    }


    @Test
    public void indexData() throws IOException {
        Baby baby1 = new Baby(1);
        Baby baby2 = new Baby(2);
        List<Baby> BabyList = new ArrayList<>();
        BabyList.add(baby1);
        BabyList.add(baby2);

        Product product = new Product("bk-1", "City Bike", 125.0, BabyList);

        IndexRequest<Product> request = IndexRequest.of(i -> i
                .index("products")
                .id(product.getSku())
                .document(product)
        );
        IndexResponse response = esClient.index(request);

        System.out.println("Indexed with version " + response.version());

    }

    @Test
    public void searchData() throws IOException {
        String searchText = "City Bike";
        double maxPrice = 100;
        Query byName = MatchQuery.of(m -> m
                .field("name")
                .query(FieldValue.of(searchText))
        )._toQuery();

        Query byMaxPrice = RangeQuery.of(r -> r
                .field("price")
                .gte(JsonData.of(maxPrice))
        )._toQuery();

        SearchResponse<Product> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .bool(b->b
                    .must(byMaxPrice)
                    .must(byName))
            ),
            Product.class
        );

        SearchResponse<Product> response1 = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .match(t -> t
                    .field("name")
                    .query(FieldValue.of(searchText))
                )
            ),
            Product.class
        );
        TotalHits total = response.hits().total();
        boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

        if (isExactResult) {
            System.out.println("There are " + total.value() + " results");
        } else {
            System.out.println("There are more than " + total.value() + " results");
        }
        List<Hit<Product>> hits = response.hits().hits();
        for (Hit<Product> hit : hits) {
            Product product = hit.source();
            System.out.println("Found product " + product.getSku() + ", score " + hit.score());
        }
    }

}
