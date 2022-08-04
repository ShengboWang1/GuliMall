package com.shengbo.gulimall.coupon.service.impl;

import com.shengbo.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.shengbo.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.coupon.dao.SeckillSessionDao;
import com.shengbo.gulimall.coupon.entity.SeckillSessionEntity;
import com.shengbo.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {
    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 从数据库中查出最近三天的秒杀活动 并将每个活动关联的商品也查询好放入其中
     */
    @Override
    public List<SeckillSessionEntity> getLatest3DaysSession() {
        //计算最近3天的日期 拼接日期和时间
        String startTime = getStartTime();
        String endTime = getEndTime();
        List<SeckillSessionEntity> list = this.list(new QueryWrapper<SeckillSessionEntity>().between("start_time", startTime, endTime));
        if(list!=null && list.size()>0){
            List<SeckillSessionEntity> collect = list.stream().map(session -> {
                Long id = session.getId();
                List<SeckillSkuRelationEntity> skus = seckillSkuRelationService.list(new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", id));
                session.setRelationSkus(skus);
                return session;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 计算时间
     */
    private String getStartTime(){
        LocalDate now = LocalDate.now();
        LocalTime minTime = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, minTime);
        String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

    /**
     * 计算时间
     */
    private String getEndTime(){
        LocalDate now = LocalDate.now();
        LocalDate plus = now.plusDays(2);
        LocalTime maxTime = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(plus, maxTime);
        String format = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }
}