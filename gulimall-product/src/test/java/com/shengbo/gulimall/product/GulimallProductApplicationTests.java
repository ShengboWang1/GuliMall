package com.shengbo.gulimall.product;

import com.aliyun.oss.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shengbo.gulimall.product.dao.AttrGroupDao;
import com.shengbo.gulimall.product.dao.SkuSaleAttrValueDao;
import com.shengbo.gulimall.product.entity.BrandEntity;
import com.shengbo.gulimall.product.service.BrandService;
import com.shengbo.gulimall.product.service.CategoryService;
import com.shengbo.gulimall.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.swing.text.html.parser.Entity;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.runner.RunWith;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallProductApplicationTests {
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;
    @Test
    public void test(){
        List<SkuItemVo.SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(19L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);
        List<SkuItemVo.SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(20L);
        System.out.println(saleAttrsBySpuId);
    }
    // 创建OSSClient实例。
    //    @Autowired
    //    OSSClient ossClient;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void teststringRedisTemplate(){
        //key:hello value:world
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello", "world_" + UUID.randomUUID().toString());
    }

    @Test
    public void redisson(){
        System.out.println(redissonClient);
    }

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
