package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String orderNo) {
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
        if (StringUtils.hasText(orderNo)) {
            wrapper.like("order_no", orderNo);
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
        itemWrapper.eq("order_id", id);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (isInvalidOrderStatus(status)) {
            return Result.error("订单状态不合法");
        }
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        order.setStatus(status);
        if (status == 2 && order.getShippedTime() == null) {
            order.setShippedTime(LocalDateTime.now());
        }
        if (status == 3 && order.getCompletedTime() == null) {
            order.setCompletedTime(LocalDateTime.now());
        }
        orderMapper.updateById(order);
        return Result.success("订单状态更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        orderMapper.deleteById(id);
        return Result.success("订单删除成功");
    }

    private boolean isInvalidOrderStatus(Integer status) {
        return status == null || status < 0 || status > 4;
    }
}
