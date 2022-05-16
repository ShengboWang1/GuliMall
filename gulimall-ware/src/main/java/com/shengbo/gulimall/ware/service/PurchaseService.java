package com.shengbo.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.ware.entity.PurchaseEntity;
import com.shengbo.gulimall.ware.vo.MergeVo;
import com.shengbo.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 22:03:14
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceive(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo);

    void received(List<Long> ids);

    void done(PurchaseDoneVo doneVo);
}

