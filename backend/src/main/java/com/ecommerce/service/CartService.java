package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private static final int MAX_CART_QUANTITY = 99;

    private final CartMapper cartMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    
    public Result<?> addToCart(Long productId, Integer quantity) {
        return addToCart(productId, quantity, null);
    }

    public Result<?> addToCart(Long productId, Integer quantity, String productSpec) {
        Integer safeQuantity = normalizeQuantity(quantity);
        if (safeQuantity == null) {
            return Result.error("购买数量不合法");
        }
        String safeSpec = normalizeSpec(productSpec);
        Product product = productMapper.selectById(productId);
        if (!isSaleable(product)) {
            return Result.error("商品已下架或不存在");
        }
        
        User user = getCurrentUser();
        QueryWrapper<Cart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId())
                .eq("product_id", productId)
                .eq("product_spec", safeSpec);
        Cart existingCart = cartMapper.selectOne(wrapper);
        int newQuantity = (existingCart == null ? 0 : existingCart.getQuantity()) + safeQuantity;
        if (newQuantity > product.getStock()) {
            return Result.error("库存不足，当前仅剩 " + product.getStock() + " 件");
        }
        if (newQuantity > MAX_CART_QUANTITY) {
            return Result.error("单件商品最多可加入 " + MAX_CART_QUANTITY + " 件");
        }
        
        if (existingCart != null) {
            existingCart.setQuantity(newQuantity);
            existingCart.setUpdateTime(LocalDateTime.now());
            cartMapper.updateById(existingCart);
        } else {
            Cart cart = new Cart();
            cart.setUserId(user.getId());
            cart.setProductId(productId);
            cart.setProductSpec(safeSpec);
            cart.setQuantity(safeQuantity);
            cart.setCreateTime(LocalDateTime.now());
            cartMapper.insert(cart);
        }
        
        return Result.success("添加成功");
    }
    
    public Result<?> getCartList() {
        User user = getCurrentUser();
        
        QueryWrapper<Cart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).orderByDesc("update_time");
        List<Cart> carts = cartMapper.selectList(wrapper);
        if (carts.isEmpty()) {
            return Result.success(List.of());
        }

        List<Long> productIds = carts.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        
        List<Map<String, Object>> result = carts.stream().map(cart -> {
            Product product = productMap.get(cart.getProductId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", cart.getId());
            item.put("productId", cart.getProductId());
            item.put("productSpec", cart.getProductSpec());
            item.put("quantity", cart.getQuantity());
            if (product != null) {
                BigDecimal price = effectivePrice(product);
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(cart.getQuantity()));
                item.put("productName", product.getName());
                item.put("description", product.getDescription());
                item.put("price", price);
                item.put("originalPrice", product.getPrice());
                item.put("promotionActive", isPromotionActive(product));
                item.put("promotionTag", product.getPromotionTag());
                item.put("imageUrl", product.getImageUrl());
                item.put("stock", product.getStock());
                item.put("status", product.getStatus());
                item.put("subtotal", subtotal);
                item.put("available", isSaleable(product) && cart.getQuantity() <= product.getStock());
            } else {
                item.put("productName", "商品已不存在");
                item.put("price", BigDecimal.ZERO);
                item.put("stock", 0);
                item.put("status", 0);
                item.put("available", false);
            }
            return item;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }
    
    public Result<?> updateCartItem(Long id, Integer quantity) {
        Integer safeQuantity = normalizeQuantity(quantity);
        if (safeQuantity == null) {
            return Result.error("购买数量不合法");
        }
        User user = getCurrentUser();
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(user.getId())) {
            return Result.error("购物车项不存在");
        }
        Product product = productMapper.selectById(cart.getProductId());
        if (!isSaleable(product)) {
            return Result.error("商品已下架或不存在");
        }
        if (safeQuantity > product.getStock()) {
            return Result.error("库存不足，当前仅剩 " + product.getStock() + " 件");
        }
        cart.setQuantity(safeQuantity);
        cart.setUpdateTime(LocalDateTime.now());
        cartMapper.updateById(cart);
        return Result.success("更新成功");
    }
    
    public Result<?> deleteCartItem(Long id) {
        User user = getCurrentUser();
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(user.getId())) {
            return Result.error("购物车项不存在");
        }
        cartMapper.deleteById(id);
        return Result.success("删除成功");
    }

    public Result<?> clearCart() {
        User user = getCurrentUser();
        QueryWrapper<Cart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        cartMapper.delete(wrapper);
        return Result.success("购物车已清空");
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }

    private Integer normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_CART_QUANTITY) {
            return null;
        }
        return quantity;
    }

    private String normalizeSpec(String productSpec) {
        if (productSpec == null) {
            return "";
        }
        String normalized = productSpec.trim();
        return normalized.length() > 200 ? normalized.substring(0, 200) : normalized;
    }

    private boolean isSaleable(Product product) {
        return product != null
                && product.getStatus() != null
                && product.getStatus() == 1
                && product.getStock() != null
                && product.getStock() > 0;
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
}
