package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.NonNull;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private RedisWorker redisWorker;

    public VoucherOrderServiceImpl(RedisWorker redisWorker) {
        this.redisWorker = redisWorker;
    }


    @Override
    public Result seckKillVoucher(Long voucherId) {
        //1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        //3.判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        //4.判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        //尝试创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock("order" + userId, stringRedisTemplate);
        //获取锁
        boolean isLock = lock.tryLock(10);
        if (!isLock) {
            //获取锁失败
            return Result.fail("不允许重复下单");
        }
        //synchronized (userId.toString().intern()) {

        try {
            //获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            //释放锁
            lock.unlock();
        }
        //}
    }

    @Transactional
    public @NonNull Result createVoucherOrder(Long voucherId) {
        //4.5一人一单
        Long userId = UserHolder.getUser().getId();
        //4.5.1查询订单
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //4.5.2判断是否存在
        if (count > 0) {
            return Result.fail("用户已经购买过一次");
        }
        //5.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                //.eq("stock",voucher.getStock())//乐观锁的体现，CAS 有弊端：高并发往往是在几毫秒之间，有很多县城都会面临与原stock不一致的情况
                .update();
        if (!success) {
            //扣减失败
            return Result.fail("库存不足");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1订单id
        Long orderId = redisWorker.nextId("order");
        voucherOrder.setId(orderId);
        //6.2用户id

        voucherOrder.setUserId(userId);
        //6.3代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //7.返回订单id
        return Result.ok(orderId);
    }

}
