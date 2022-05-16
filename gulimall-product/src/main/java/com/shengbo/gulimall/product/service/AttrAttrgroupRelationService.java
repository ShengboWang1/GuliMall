package com.shengbo.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.shengbo.gulimall.product.vo.AttrGroupRelationVo;

import java.util.List;
import java.util.Map;

/**
 * 属性&属性分组关联
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 16:19:54
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void SaveBatchRelation(List<AttrGroupRelationVo> vos);
}

