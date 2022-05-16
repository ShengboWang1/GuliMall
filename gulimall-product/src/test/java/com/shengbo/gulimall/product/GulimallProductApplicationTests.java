package com.shengbo.gulimall.product;

import com.aliyun.oss.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shengbo.gulimall.product.entity.BrandEntity;
import com.shengbo.gulimall.product.service.BrandService;
import com.shengbo.gulimall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.swing.text.html.parser.Entity;
import java.util.Arrays;
import java.util.List;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;
    // 创建OSSClient实例。
//    @Autowired
//    OSSClient ossClient;

    @Test
    public void testFindPath(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径:{}", Arrays.asList(catelogPath));
    }
    @Test
    void contextLoads() {
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//        System.out.println("插入成功。。。");
        brandService.removeById(2);
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id ", 1L));
        for(BrandEntity en: list){
            System.out.println(en);
        }

    }

}
