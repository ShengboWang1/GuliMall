package com.shengbo.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.gulimall.ware.entity.WareInfoEntity;
import com.shengbo.gulimall.ware.vo.FareResponseVo;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 22:03:14
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareResponseVo getFare(Long addrId);
}

