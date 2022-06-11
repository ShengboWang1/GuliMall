package com.shengbo.gulimall.search.controller;

import com.shengbo.gulimall.search.service.MallSearchService;
import com.shengbo.gulimall.search.vo.SearchParam;
import com.shengbo.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {
    @Autowired
    MallSearchService mallSearchService;


    /**
     * SpringMVC自动将页面提交过来的所有查询参数封装成指定的对象
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model){
        //页面传来的参数 封装为SearchParam 返回的结果封装成SearchResult
        //根据传递来的页面的查询参数 去es中检索商品
        SearchResult result = mallSearchService.search(searchParam);
        model.addAttribute("result", result);
        return "list";
    }
}
