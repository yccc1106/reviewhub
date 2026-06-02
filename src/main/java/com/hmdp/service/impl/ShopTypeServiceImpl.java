package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.interfaces.Func;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = CACHE_SHOP_TYPE_KEY + "list";
        //1.从redis查询
        String typeListJson = stringRedisTemplate.opsForValue().get(key);
        //2.存在，直接返回
        if (StrUtil.isNotBlank(typeListJson)) {
            List<ShopType> shopTypes = JSONUtil.toList(typeListJson, ShopType.class);
            return Result.ok(shopTypes);
        }
        //3.不存在，查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        //4.不存在，返回错误
        if (shopTypes==null||shopTypes.isEmpty()) {
            return Result.fail("店铺类型不存在");
        }

        //5.存在，写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes));

        //6.返回
        return Result.ok(shopTypes);
    }
}
