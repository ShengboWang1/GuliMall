package com.shengbo.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.shengbo.common.utils.R;
import com.shengbo.gulimall.ware.feign.MemberFeignService;
import com.shengbo.gulimall.ware.vo.FareResponseVo;
import com.shengbo.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengbo.common.utils.PageUtils;
import com.shengbo.common.utils.Query;

import com.shengbo.gulimall.ware.dao.WareInfoDao;
import com.shengbo.gulimall.ware.entity.WareInfoEntity;
import com.shengbo.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.hasText(key)){
            wrapper.eq("id", key).or().like("name",key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据收货地址来计算运费
     * @return
     */
    @Override
    public FareResponseVo getFare(Long addrId) {
        R r = memberFeignService.addrInfo(addrId);
        MemberAddressVo data = r.getData("memberReceiveAddress",new TypeReference<MemberAddressVo>(){});
        if(data!=null){
            //赖皮，用手机号最后一位当成是运费了
            FareResponseVo fareResponseVo = new FareResponseVo();
            String phone = data.getPhone();
            String substring = phone.substring(phone.length() - 1, phone.length());

            fareResponseVo.setFare(new BigDecimal(substring));
            fareResponseVo.setAddress(data);

            return fareResponseVo;
        }
        return null;

    }

}