package com.shengbo.gulimall.product.service.impl;

import com.shengbo.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.shengbo.gulimall.product.dao.AttrDao;
import com.shengbo.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.shengbo.gulimall.product.entity.AttrEntity;
import com.shengbo.gulimall.product.service.AttrService;
import com.shengbo.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.BeanUtils;
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

import com.shengbo.gulimall.product.dao.AttrGroupDao;
import com.shengbo.gulimall.product.entity.AttrGroupEntity;
import com.shengbo.gulimall.product.service.AttrGroupService;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrDao attrDao;

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        /**
         * select * from pms_attr_group where catelog_id=? and (attr_group_id=key
         * or attr_group_name like %key%
         */
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(StringUtils.hasText(key)){
            wrapper.and((obj)->{
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        };
        //查询所有
        if(catelogId == 0){
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        }else{
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);

        }

    }

    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        //1 查询分组信息
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        //2 查询所有属性
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().filter(entity ->
            attrService.getRelationAttr(entity.getAttrGroupId()) != null
        ).map(item -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(item, attrsVo);
            List<AttrEntity> attrs = attrService.getRelationAttr(attrsVo.getAttrGroupId());
            attrsVo.setAttrs(attrs);
            return attrsVo;
        }).collect(Collectors.toList());
        return collect;


    }

}