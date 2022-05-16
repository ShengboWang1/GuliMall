package com.shengbo.gulimall.product.service.impl;

import com.shengbo.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.product.dao.CategoryDao;
import com.shengbo.gulimall.product.entity.CategoryEntity;
import com.shengbo.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryDao categoryDao;
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //要查出所有的分类
        List<CategoryEntity> categoryEntities = categoryDao.selectList(null);
        //组装成父子的树形结构
        //2.1找到所有的1级分类
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return menu1.getSort() - menu2.getSort();
        }).collect(Collectors.toList());
        return level1Menus;
    }

    @Override
    public void removeMenusByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单是否被别的地方引用
        //逻辑删除 不是真的物理删除 而是给他添加一个标志位 1表示显示 不删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPaths = findParentPath(catelogId, paths);
        Collections.reverse(parentPaths);
        return (Long[]) parentPaths.toArray(new Long[0]);

    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
        this.update();
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths){
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid() != 0){
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());

        }).collect(Collectors.toList());
        return children;
    }

}