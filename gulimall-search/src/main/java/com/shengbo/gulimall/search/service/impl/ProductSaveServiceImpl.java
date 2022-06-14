package com.shengbo.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.alibaba.fastjson.JSONObject;
import com.shengbo.common.to.es.SkuEsModel;
import com.shengbo.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.shengbo.gulimall.search.constant.EsConstant.PRODUCT_INDEX;

@Service
@Slf4j
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    ElasticsearchClient esClient;
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //保存到ES
        //1。给es建立一个索引 product建立好映射关系(属性）
        //在es的客户段中 put命令见product-mapping

        //给es保存数据
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (SkuEsModel skuEsModel : skuEsModels) {
            //bulk 批量操作
            String s = JSONObject.toJSONString(skuEsModel);
            br.operations(op -> op
                    .index(idx -> idx
                            .index(PRODUCT_INDEX)
                            .id(skuEsModel.getSkuId().toString())
                            .document(skuEsModel)
                    )
            );
        }
        BulkResponse result = esClient.bulk(br.build());
        // Log errors, if any
        if (result.errors()) {
            log.error("Bulk had errors");
            for (BulkResponseItem item: result.items()) {
                if (item.error() != null) {
                    log.error(item.error().reason());
                }
            }
            return false;
        }
        List<Long> collect = skuEsModels.stream().map(sku -> {
            return sku.getSkuId();
        }).collect(Collectors.toList());
        log.info("商品上架完成{}", collect);
        return true;
    }
}
