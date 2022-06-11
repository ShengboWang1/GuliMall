package com.shengbo.gulimall.search.service;

import com.shengbo.gulimall.search.vo.SearchParam;
import com.shengbo.gulimall.search.vo.SearchResult;

import java.io.IOException;

public interface MallSearchService {
    /**
     * @param searchParam 检索的所有参数
     * @return 返回检索的结果 里面包含页面的所有信息
     */
    SearchResult search(SearchParam searchParam);

}
