package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTimeoutService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CouponService couponService;
    private final MessageService messageService;

    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    @Transactional
    public void cancelExpiredUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("status", 0)
                .lt("create_time", deadline)
                .last("LIMIT 50"));
        for (Order order : orders) {
            cancelOrder(order);
        }
    }

    private void cancelOrder(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                .eq("order_id", order.getId()));
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }
        order.setStatus(4);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        couponService.restoreCoupon(order.getCouponId(), order.getUserId());
        messageService.create(order.getUserId(), "order", "订单超时已取消",
                "订单 " + order.getOrderNo() + " 超过30分钟未支付，系统已自动取消。");
    }
}
