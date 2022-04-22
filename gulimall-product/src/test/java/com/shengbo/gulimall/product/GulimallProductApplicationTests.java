package com.shengbo.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shengbo.gulimall.product.entity.BrandEntity;
import com.shengbo.gulimall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.swing.text.html.parser.Entity;
import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

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
