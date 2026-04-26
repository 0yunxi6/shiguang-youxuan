package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.service.CouponService;
import com.ecommerce.service.MessageService;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CouponService couponService;
    private final MessageService messageService;
    private final OrderService orderService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String orderNo,
                          @RequestParam(required = false) String keyword) {
        if (status != null && isInvalidOrderStatus(status)) {
            return Result.error("订单状态不合法");
        }
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<Order> pageParam = new Page<>(page, size);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        String searchKeyword = StringUtils.hasText(keyword) ? keyword.trim() : orderNo;
        if (StringUtils.hasText(searchKeyword)) {
            wrapper.and(w -> w.like("order_no", searchKeyword)
                    .or().like("receiver_name", searchKeyword)
                    .or().like("receiver_phone", searchKeyword));
        }
        wrapper.orderByDesc("create_time");
        return Result.success(orderMapper.selectPage(pageParam, wrapper));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("order_id", id).orderByAsc("id");
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    @Transactional
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (isInvalidOrderStatus(status)) {
            return Result.error("订单状态不合法");
        }
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        Result<?> validation = validateStatusChange(order, status);
        if (validation != null) {
            return validation;
        }
        applyStatusChange(order, status);
        return Result.success("订单状态更新成功");
    }

    @PutMapping("/status/batch")
    @Transactional
    public Result<?> batchUpdateStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        if (isInvalidOrderStatus(status)) {
            return Result.error("订单状态不合法");
        }
        List<Long> validIds = ids == null ? List.of() : ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            return Result.error("请选择要操作的订单");
        }

        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>().in("id", validIds));
        int updated = 0;
        int skipped = 0;
        for (Order order : orders) {
            if (validateStatusChange(order, status) == null && applyStatusChange(order, status)) {
                updated++;
            } else {
                skipped++;
            }
        }
        return Result.success("订单批量更新成功", Map.of("updated", updated, "skipped", skipped));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        orderMapper.deleteById(id);
        return Result.success("订单删除成功");
    }

    private boolean isInvalidOrderStatus(Integer status) {
        return status == null || status < 0 || status > 4;
    }

    private Result<?> validateStatusChange(Order order, Integer targetStatus) {
        Integer currentStatus = order.getStatus();
        if (Objects.equals(currentStatus, targetStatus)) {
            return null;
        }
        if (currentStatus != null && currentStatus == 4 && targetStatus != 4) {
            return Result.error("已取消订单不能恢复状态");
        }
        if (currentStatus != null && currentStatus == 3 && targetStatus != 3) {
            return Result.error("已完成订单不能再变更状态");
        }
        if (currentStatus != null && currentStatus == 0 && (targetStatus == 2 || targetStatus == 3)) {
            return Result.error("待支付订单需先变更为已支付");
        }
        return null;
    }

    private boolean applyStatusChange(Order order, Integer targetStatus) {
        if (Objects.equals(order.getStatus(), targetStatus)) {
            return false;
        }
        Integer oldStatus = order.getStatus();
        if (targetStatus == 4 && oldStatus != null && oldStatus != 4) {
            restoreOrderResources(order);
        }
        order.setStatus(targetStatus);
        if (targetStatus == 2 && order.getShippedTime() == null) {
            order.setShippedTime(LocalDateTime.now());
        }
        if (targetStatus == 3 && order.getCompletedTime() == null) {
            order.setCompletedTime(LocalDateTime.now());
        }
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        notifyStatusChange(order, targetStatus);
        return true;
    }

    private void restoreOrderResources(Order order) {
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("order_id", order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }
        couponService.restoreCoupon(order.getCouponId(), order.getUserId());
    }

    private void notifyStatusChange(Order order, Integer targetStatus) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        if (targetStatus == 2) {
            messageService.create(order.getUserId(), "order", "订单已发货", "订单 " + order.getOrderNo() + " 已发货，请留意收货。");
        } else if (targetStatus == 3) {
            User user = userMapper.selectById(order.getUserId());
            orderService.awardOrderPoints(user, order);
            messageService.create(order.getUserId(), "order", "订单已完成", "订单 " + order.getOrderNo() + " 已完成，积分已到账。");
        } else if (targetStatus == 4) {
            messageService.create(order.getUserId(), "order", "订单已取消", "订单 " + order.getOrderNo() + " 已由后台取消。");
        }
    }
}
