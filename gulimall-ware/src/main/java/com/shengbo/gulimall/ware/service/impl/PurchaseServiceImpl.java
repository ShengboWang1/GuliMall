package com.shengbo.gulimall.ware.service.impl;

import com.shengbo.common.constant.ProductConstant;
import com.shengbo.common.constant.WareConstant;
import com.shengbo.gulimall.ware.dao.PurchaseDetailDao;
import com.shengbo.gulimall.ware.entity.PurchaseDetailEntity;
import com.shengbo.gulimall.ware.entity.WareSkuEntity;
import com.shengbo.gulimall.ware.service.PurchaseDetailService;
import com.shengbo.gulimall.ware.service.WareSkuService;
import com.shengbo.gulimall.ware.vo.MergeVo;
import com.shengbo.gulimall.ware.vo.PurchaseDoneVo;
import com.shengbo.gulimall.ware.vo.PurchaseItemDoneVo;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.ware.dao.PurchaseDao;
import com.shengbo.gulimall.ware.entity.PurchaseEntity;
import com.shengbo.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService wareSkuService;
    @Autowired
    PurchaseDetailDao purchaseDetailDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.hasText(key)){
            wrapper.and(w->{
                w.eq("id", key).or().eq("assignee_id",key).or().like("assignee_name",key);
            });
        }
        String status = (String) params.get("status");
        if(StringUtils.hasText(status)){
            wrapper.and(w->{
                w.eq("status", status);
            });
        }
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status",0).or().eq("status",1);
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if(purchaseId == null){
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        //TODO 确定状态为0或者1才能合并
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(item -> {
            PurchaseDetailEntity entity = new PurchaseDetailEntity();
            entity.setId(item);
            entity.setPurchaseId(finalPurchaseId);
            entity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return entity;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(collect);
        PurchaseEntity entity = new PurchaseEntity();
        entity.setId(finalPurchaseId);
        entity.setUpdateTime(new Date());
        this.updateById(entity);
    }

    @Override
    public void received(List<Long> ids) {
        //1.确认当前采购单是新建或者已分配状态
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item -> {
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());
        //2。改变采购单的状态
        this.updateBatchById(collect);
        //3。改变采购单对应采购项目的状态
        collect.forEach(item->{
            List<PurchaseDetailEntity> entities = purchaseDetailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
                entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return entity;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntities);
        });

    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {
        //1.改变采购单状态
        Long purchaseId = doneVo.getId();

        //2。改变采购项状态
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        Boolean flag = true;

        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item: items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag = false;
                detailEntity.setStatus(item.getStatus());
            }else{
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //3。将成功采购的入库
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                //PurchaseDetailEntity entity = purchaseDetailDao.selectOne(new QueryWrapper<PurchaseDetailEntity>().eq("sku_id", item.getItemId()));
                wareSkuService.addStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        purchaseDetailService.updateBatchById(updates);

        //1.改变采购单状态
        PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
        purchaseDetailEntity.setPurchaseId(purchaseId);
        purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
        purchaseDetailService.updateById(purchaseDetailEntity);

        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);



    }

}