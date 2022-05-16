package com.shengbo.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.product.entity.AttrEntity;
import com.shengbo.gulimall.product.vo.AttrGroupRelationVo;
import com.shengbo.gulimall.product.vo.AttrRespVo;
import com.shengbo.gulimall.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 16:19:54
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    void deleteRelation(AttrGroupRelationVo[] vos);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

}

