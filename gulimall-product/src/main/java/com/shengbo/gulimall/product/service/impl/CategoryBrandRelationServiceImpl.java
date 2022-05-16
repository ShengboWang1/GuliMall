package com.shengbo.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.shengbo.gulimall.product.dao.BrandDao;
import com.shengbo.gulimall.product.dao.CategoryDao;
import com.shengbo.gulimall.product.entity.BrandEntity;
import com.shengbo.gulimall.product.entity.CategoryEntity;
import com.shengbo.gulimall.product.service.BrandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.product.dao.CategoryBrandRelationDao;
import com.shengbo.gulimall.product.entity.CategoryBrandRelationEntity;
import com.shengbo.gulimall.product.service.CategoryBrandRelationService;

@Slf4j
@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    BrandDao brandDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationDao relationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);

        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());

        this.save(categoryBrandRelation);
    }

    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);
        this.update(relationEntity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id",brandId));
    }

    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategory(catId,name);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {
        List<CategoryBrandRelationEntity> brandRelationEntities = relationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq(
                "catelog_id", catId));

        List<BrandEntity> brandEntities = brandRelationEntities.stream().map(item -> {
            BrandEntity brandEntity = brandDao.selectOne(new QueryWrapper<BrandEntity>().eq("brand_id", item.getBrandId()));
            return brandEntity;
        }).collect(Collectors.toList());

        return brandEntities;
    }


}