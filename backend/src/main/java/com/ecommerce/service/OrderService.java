package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final CouponService couponService;
    private final MessageService messageService;

    @Transactional
    public Result<?> createOrder(String receiverName, String receiverPhone, String receiverAddress) {
        return createOrder(receiverName, receiverPhone, receiverAddress, null, null, null, null, null, null);
    }

    @Transactional
    public Result<?> createOrder(String receiverName, String receiverPhone, String receiverAddress, String remark) {
        return createOrder(receiverName, receiverPhone, receiverAddress, remark, null, null, null, null, null);
    }

    @Transactional
    public Result<?> createOrder(String receiverName, String receiverPhone, String receiverAddress, String remark, List<Long> cartItemIds) {
        return createOrder(receiverName, receiverPhone, receiverAddress, remark, null, null, null, null, cartItemIds);
    }

    @Transactional
    public Result<?> createOrder(String receiverName, String receiverPhone, String receiverAddress, String remark,
                                 Long couponId, String paymentMethod, String invoiceTitle, String invoiceTaxNo, List<Long> cartItemIds) {
        if (!StringUtils.hasText(receiverName) || !StringUtils.hasText(receiverPhone) || !StringUtils.hasText(receiverAddress)) {
            return Result.error("请完善收货信息");
        }

        User user = getCurrentUser();
        QueryWrapper<Cart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        Set<Long> selectedIds = normalizeCartItemIds(cartItemIds);
        if (!selectedIds.isEmpty()) {
            wrapper.in("id", selectedIds);
        }
        List<Cart> carts = cartMapper.selectList(wrapper);
        if (carts.isEmpty()) {
            return Result.error("购物车为空");
        }
        if (!selectedIds.isEmpty() && carts.size() != selectedIds.size()) {
            return Result.error("部分购物车商品已失效，请刷新后重试");
        }

        List<Long> productIds = carts.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal originalAmount = BigDecimal.ZERO;
        for (Cart cart : carts) {
            Product product = productMap.get(cart.getProductId());
            validateCartProduct(product, cart.getQuantity());
            originalAmount = originalAmount.add(effectivePrice(product).multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        UserCoupon coupon = couponService.validateCouponForOrder(user.getId(), couponId, originalAmount);
        BigDecimal discountAmount = coupon == null || coupon.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : coupon.getDiscountAmount();
        BigDecimal shippingFee = calculateShippingFee(originalAmount);
        BigDecimal totalAmount = originalAmount.subtract(discountAmount).add(shippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setUserId(user.getId());
        order.setOrderNo(generateOrderNo());
        order.setOriginalAmount(originalAmount);
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(totalAmount);
        order.setCouponId(coupon == null ? null : coupon.getId());
        order.setCouponName(couponService.buildCouponDisplay(coupon));
        order.setStatus(0);
        order.setReceiverName(receiverName.trim());
        order.setReceiverPhone(receiverPhone.trim());
        order.setReceiverAddress(receiverAddress.trim());
        order.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        order.setPaymentMethod(StringUtils.hasText(paymentMethod) ? paymentMethod.trim() : null);
        order.setInvoiceTitle(StringUtils.hasText(invoiceTitle) ? invoiceTitle.trim() : null);
        order.setInvoiceTaxNo(StringUtils.hasText(invoiceTaxNo) ? invoiceTaxNo.trim() : null);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        couponService.markCouponUsed(coupon);

        for (Cart cart : carts) {
            Product product = productMap.get(cart.getProductId());
            int updated = productMapper.decreaseStock(product.getId(), cart.getQuantity());
            if (updated == 0) {
                throw new BusinessException(400, product.getName() + " 库存不足，请刷新后重试");
            }

            BigDecimal itemPrice = effectivePrice(product);
            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(cart.getQuantity()));
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getImageUrl());
            orderItem.setProductSpec(cart.getProductSpec());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(itemPrice);
            orderItem.setTotalAmount(itemTotal);
            orderItem.setCreateTime(LocalDateTime.now());
            orderItemMapper.insert(orderItem);
        }

        cartMapper.delete(wrapper);
        messageService.create(user.getId(), "order", "订单创建成功", "订单 " + order.getOrderNo() + " 已创建，请及时完成支付。");
        return Result.success("订单创建成功", Map.of("orderId", order.getId(), "orderNo", order.getOrderNo()));
    }

    public Result<?> getOrderList(Integer status) {
        User user = getCurrentUser();
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        if (status != null && status >= 0 && status <= 4) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        List<Order> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            return Result.success(List.of());
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.in("order_id", orderIds).orderByAsc("id");
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemMapper.selectList(itemWrapper).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<Map<String, Object>> result = orders.stream()
                .map(order -> buildOrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    public Result<?> getOrderDetail(Long id) {
        User user = getCurrentUser();
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.error("订单不存在");
        }
        return Result.success(buildOrderResponse(order, getOrderItems(id)));
    }

    @Transactional
    public Result<?> payOrder(Long id) {
        User user = getCurrentUser();
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            return Result.error("当前订单状态不支持支付");
        }
        order.setStatus(1);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        messageService.create(user.getId(), "order", "支付成功", "订单 " + order.getOrderNo() + " 已完成模拟支付，等待商家发货。");
        return Result.success("支付成功");
    }

    @Transactional
    public Result<?> cancelOrder(Long id) {
        User user = getCurrentUser();
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() == null || (order.getStatus() != 0 && order.getStatus() != 1)) {
            return Result.error("当前订单状态不支持取消");
        }

        List<OrderItem> items = getOrderItems(id);
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }
        order.setStatus(4);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        couponService.restoreCoupon(order.getCouponId(), user.getId());
        messageService.create(user.getId(), "order", "订单已取消", "订单 " + order.getOrderNo() + " 已取消，库存和优惠券已恢复。");
        return Result.success("订单已取消");
    }

    @Transactional
    public Result<?> confirmReceipt(Long id) {
        User user = getCurrentUser();
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 2) {
            return Result.error("当前订单状态不支持确认收货");
        }
        order.setStatus(3);
        order.setCompletedTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        awardOrderPoints(user, order);
        messageService.create(user.getId(), "order", "确认收货成功", "订单 " + order.getOrderNo() + " 已完成，积分已到账。");
        return Result.success("确认收货成功");
    }

    private void validateCartProduct(Product product, Integer quantity) {
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(400, "部分商品已下架，请刷新购物车");
        }
        if (quantity == null || quantity < 1) {
            throw new BusinessException(400, "商品数量不合法");
        }
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new BusinessException(400, product.getName() + " 库存不足");
        }
    }

    private Map<String, Object> buildOrderResponse(Order order, List<OrderItem> items) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("id", order.getId());
        orderMap.put("orderNo", order.getOrderNo());
        orderMap.put("totalAmount", order.getTotalAmount());
        orderMap.put("originalAmount", order.getOriginalAmount());
        orderMap.put("discountAmount", order.getDiscountAmount());
        orderMap.put("shippingFee", order.getShippingFee());
        orderMap.put("couponId", order.getCouponId());
        orderMap.put("couponName", order.getCouponName());
        orderMap.put("status", order.getStatus());
        orderMap.put("statusText", buildStatusText(order.getStatus()));
        orderMap.put("receiverName", order.getReceiverName());
        orderMap.put("receiverPhone", order.getReceiverPhone());
        orderMap.put("receiverAddress", order.getReceiverAddress());
        orderMap.put("remark", order.getRemark());
        orderMap.put("paymentMethod", order.getPaymentMethod());
        orderMap.put("invoiceTitle", order.getInvoiceTitle());
        orderMap.put("invoiceTaxNo", order.getInvoiceTaxNo());
        orderMap.put("createTime", order.getCreateTime());
        orderMap.put("updateTime", order.getUpdateTime());
        orderMap.put("shippedTime", order.getShippedTime());
        orderMap.put("completedTime", order.getCompletedTime());
        orderMap.put("canCancel", order.getStatus() != null && (order.getStatus() == 0 || order.getStatus() == 1));
        orderMap.put("canConfirm", order.getStatus() != null && order.getStatus() == 2);
        orderMap.put("items", items);
        return orderMap;
    }

    private String buildStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private List<OrderItem> getOrderItems(Long orderId) {
        QueryWrapper<OrderItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("order_id", orderId).orderByAsc("id");
        return orderItemMapper.selectList(itemWrapper);
    }

    private BigDecimal calculateShippingFee(BigDecimal originalAmount) {
        if (originalAmount != null && originalAmount.compareTo(new BigDecimal("99")) >= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("8.00");
    }

    private BigDecimal effectivePrice(Product product) {
        return isPromotionActive(product) && product.getPromotionPrice() != null
                ? product.getPromotionPrice()
                : product.getPrice();
    }

    private boolean isPromotionActive(Product product) {
        if (product == null || product.getPromotionPrice() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (product.getPromotionStartTime() == null || !product.getPromotionStartTime().isAfter(now))
                && (product.getPromotionEndTime() == null || !product.getPromotionEndTime().isBefore(now));
    }

    public void awardOrderPoints(User user, Order order) {
        if (user == null || order == null || order.getTotalAmount() == null) {
            return;
        }
        int points = order.getTotalAmount().max(BigDecimal.ZERO).intValue();
        if (points <= 0) {
            return;
        }
        user.setPoints((user.getPoints() == null ? 0 : user.getPoints()) + points);
        user.setLevel(resolveLevel(user.getPoints()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private String resolveLevel(Integer points) {
        int value = points == null ? 0 : points;
        if (value >= 10000) {
            return "钻石会员";
        }
        if (value >= 5000) {
            return "黄金会员";
        }
        if (value >= 1000) {
            return "白银会员";
        }
        return "普通会员";
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private Set<Long> normalizeCartItemIds(List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return Set.of();
        }
        return cartItemIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
