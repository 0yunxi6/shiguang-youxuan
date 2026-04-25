package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductFavorite;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductFavoriteMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductFavoriteService {
    private final ProductFavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    public Result<?> listFavorites() {
        User user = getCurrentUser();
        QueryWrapper<ProductFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).orderByDesc("create_time");
        List<ProductFavorite> favorites = favoriteMapper.selectList(wrapper);
        if (favorites.isEmpty()) {
            return Result.success(List.of());
        }
        List<Long> productIds = favorites.stream().map(ProductFavorite::getProductId).distinct().toList();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        Map<Long, String> categoryNameMap = buildCategoryNameMap(productMap.values().stream().toList());
        List<Product> result = favorites.stream()
                .map(favorite -> {
                    Product product = productMap.get(favorite.getProductId());
                    if (product == null) {
                        return null;
                    }
                    product.setFavorited(true);
                    product.setFavoriteTime(favorite.getCreateTime());
                    product.setCategoryName(categoryNameMap.get(product.getCategoryId()));
                    List<String> images = normalizeImages(product.getImages(), product.getImageUrl());
                    product.setImageList(images);
                    if (!images.isEmpty()) {
                        product.setImageUrl(images.get(0));
                    }
                    return product;
                })
                .filter(product -> product != null)
                .toList();
        return Result.success(result);
    }

    public Result<?> addFavorite(Long productId) {
        User user = getCurrentUser();
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            return Result.error("商品不存在或已下架");
        }
        QueryWrapper<ProductFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).eq("product_id", productId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return Result.success("已加入收藏");
        }
        ProductFavorite favorite = new ProductFavorite();
        favorite.setUserId(user.getId());
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);
        return Result.success("收藏成功");
    }

    public Result<?> removeFavorite(Long productId) {
        User user = getCurrentUser();
        QueryWrapper<ProductFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).eq("product_id", productId);
        favoriteMapper.delete(wrapper);
        return Result.success("已取消收藏");
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }

    private Map<Long, String> buildCategoryNameMap(List<Product> products) {
        List<Long> categoryIds = products.stream().map(Product::getCategoryId).filter(id -> id != null).distinct().toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> categoryNameMap = new HashMap<>();
        for (Category category : categoryMapper.selectBatchIds(categoryIds)) {
            categoryNameMap.put(category.getId(), category.getName());
        }
        return categoryNameMap;
    }

    private List<String> normalizeImages(String images, String imageUrl) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (StringUtils.hasText(images)) {
            Arrays.stream(images.split("[,，\\n\\r]+"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(normalized::add);
        }
        if (StringUtils.hasText(imageUrl)) {
            normalized.add(imageUrl.trim());
        }
        return normalized.stream().toList();
    }
}
