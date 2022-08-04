package com.shengbo.gulimall.coupon.service.impl;

import com.shengbo.common.to.SkuReductionTo;
import com.shengbo.gulimall.coupon.entity.SkuFullReductionEntity;
import com.shengbo.gulimall.coupon.entity.SkuLadderEntity;
import com.shengbo.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.coupon.dao.SeckillSkuRelationDao;
import com.shengbo.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.shengbo.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.util.StringUtils;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<SeckillSkuRelationEntity> queryWrapper = new QueryWrapper<SeckillSkuRelationEntity>();
        String promotionSessionId = (String) params.get("promotionSessionId");
        if(StringUtils.hasText(promotionSessionId)){
            queryWrapper.eq("promotion_session_id", promotionSessionId);
        }

        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

}