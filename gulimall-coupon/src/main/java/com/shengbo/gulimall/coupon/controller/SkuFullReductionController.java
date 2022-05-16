package com.shengbo.gulimall.coupon.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.shengbo.common.to.SkuReductionTo;
import com.shengbo.gulimall.coupon.entity.SkuLadderEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.shengbo.gulimall.coupon.entity.SkuFullReductionEntity;
import com.shengbo.gulimall.coupon.service.SkuFullReductionService;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.R;



/**
 * 商品满减信息
 *
 * @author shengbo
 * @email shengbo_wang1@163.com
 * @date 2022-04-18 21:11:57
 */
@RestController
@RequestMapping("coupon/skufullreduction")
public class SkuFullReductionController {
    @Autowired
    private SkuFullReductionService skuFullReductionService;

    @PostMapping("/saveinfo")
    //@RequiresPermissions("coupon:seckillskurelation:list")
    public R saveInfo(@RequestBody SkuReductionTo skuReductionTo){
        skuFullReductionService.saveSkuReduction(skuReductionTo);
        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("coupon:skufullreduction:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = skuFullReductionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("coupon:skufullreduction:info")
    public R info(@PathVariable("id") Long id){
		SkuFullReductionEntity skuFullReduction = skuFullReductionService.getById(id);

        return R.ok().put("skuFullReduction", skuFullReduction);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("coupon:skufullreduction:save")
    public R save(@RequestBody SkuFullReductionEntity reductionEntity){
		skuFullReductionService.save(reductionEntity);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("coupon:skufullreduction:update")
    public R update(@RequestBody SkuFullReductionEntity skuFullReduction){
		skuFullReductionService.updateById(skuFullReduction);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("coupon:skufullreduction:delete")
    public R delete(@RequestBody Long[] ids){
		skuFullReductionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
