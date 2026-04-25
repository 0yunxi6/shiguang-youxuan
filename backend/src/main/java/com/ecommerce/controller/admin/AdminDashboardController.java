package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.*;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final CategoryMapper categoryMapper;
    private final ProductReviewMapper productReviewMapper;
    private final ProductFavoriteMapper productFavoriteMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderItemMapper orderItemMapper;

    @GetMapping("/stats")
    public Result<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("productCount", productMapper.selectCount(null));
        stats.put("orderCount", orderMapper.selectCount(null));
        stats.put("categoryCount", categoryMapper.selectCount(null));
        stats.put("reviewCount", productReviewMapper.selectCount(null));
        stats.put("favoriteCount", productFavoriteMapper.selectCount(null));
        stats.put("couponCount", userCouponMapper.selectCount(null));
        return Result.success(stats);
    }

    @GetMapping("/overview")
    public Result<?> getOverview(@RequestParam(defaultValue = "7") int days) {
        days = days == 30 ? 30 : 7;

        Map<String, Object> overview = new HashMap<>();
        overview.put("stats", buildStats());
        overview.put("salesTrend", buildSalesTrend(days));
        overview.put("orderStatusCounts", buildOrderStatusCounts());
        overview.put("hotProducts", buildHotProducts());
        overview.put("lowStockProducts", buildLowStockProducts());
        overview.put("recentOrders", getRecentOrders());
        return Result.success(overview);
    }

    private Map<String, Object> buildStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("productCount", productMapper.selectCount(null));
        stats.put("orderCount", orderMapper.selectCount(null));
        stats.put("categoryCount", categoryMapper.selectCount(null));
        stats.put("reviewCount", productReviewMapper.selectCount(null));
        stats.put("favoriteCount", productFavoriteMapper.selectCount(null));
        stats.put("couponCount", userCouponMapper.selectCount(null));

        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.ne("status", 4);
        List<Order> validOrders = orderMapper.selectList(wrapper);
        BigDecimal totalSalesAmount = validOrders.stream()
                .map(Order::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("salesAmount", totalSalesAmount);
        stats.put("completedOrderCount", validOrders.stream().filter(order -> order.getStatus() != null && order.getStatus() == 3).count());
        stats.put("pendingShipmentCount", validOrders.stream().filter(order -> order.getStatus() != null && order.getStatus() == 1).count());
        return stats;
    }

    private Map<String, Object> buildSalesTrend(int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        Map<String, BigDecimal> amountByDate = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            amountByDate.put(startDate.plusDays(i).format(formatter), BigDecimal.ZERO);
        }

        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.ge("create_time", startTime).ne("status", 4);
        List<Order> orders = orderMapper.selectList(wrapper);
        for (Order order : orders) {
            if (order.getCreateTime() == null || order.getTotalAmount() == null) {
                continue;
            }
            String key = order.getCreateTime().toLocalDate().format(formatter);
            if (amountByDate.containsKey(key)) {
                amountByDate.put(key, amountByDate.get(key).add(order.getTotalAmount()));
            }
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("labels", new ArrayList<>(amountByDate.keySet()));
        trend.put("values", new ArrayList<>(amountByDate.values()));
        return trend;
    }

    private List<Long> buildOrderStatusCounts() {
        List<Long> counts = new ArrayList<>();
        for (int status = 0; status <= 4; status++) {
            QueryWrapper<Order> wrapper = new QueryWrapper<>();
            wrapper.eq("status", status);
            counts.add(orderMapper.selectCount(wrapper));
        }
        return counts;
    }

    private List<Order> getRecentOrders() {
        Page<Order> page = new Page<>(1, 5);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return orderMapper.selectPage(page, wrapper).getRecords();
    }

    private List<Map<String, Object>> buildHotProducts() {
        List<Map<String, Object>> rows = orderItemMapper.selectMaps(new QueryWrapper<com.ecommerce.entity.OrderItem>()
                .select("product_id AS productId", "product_name AS productName", "product_image AS productImage", "COALESCE(SUM(quantity), 0) AS sales")
                .groupBy("product_id", "product_name", "product_image")
                .last("ORDER BY sales DESC LIMIT 5"));
        return rows.stream().map(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", row.get("productId"));
            item.put("productName", row.get("productName"));
            item.put("productImage", row.get("productImage"));
            item.put("sales", row.get("sales"));
            return item;
        }).toList();
    }

    private List<Product> buildLowStockProducts() {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.le("stock", 20).eq("status", 1).orderByAsc("stock").last("LIMIT 5");
        return productMapper.selectList(wrapper);
    }
}
