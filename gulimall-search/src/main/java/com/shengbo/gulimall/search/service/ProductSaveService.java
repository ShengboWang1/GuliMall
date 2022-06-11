package com.shengbo.gulimall.search.service;

import com.shengbo.common.to.es.SkuEsModel;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {

    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
