package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.ProductFavoriteMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductFavoriteMapper productFavoriteMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    public Page<Product> getProductList(int page, int size, Long categoryId, String keyword,
                                         String sort, BigDecimal minPrice, BigDecimal maxPrice) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 60);
        Page<Product> pageParam = new Page<>(page, size);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // Find categories matching the keyword
            QueryWrapper<Category> catWrapper = new QueryWrapper<>();
            catWrapper.like("name", kw);
            List<Long> matchedCatIds = categoryMapper.selectList(catWrapper)
                    .stream().map(Category::getId).collect(Collectors.toList());

            wrapper.and(w -> {
                w.like("name", kw).or().like("description", kw);
                if (!matchedCatIds.isEmpty()) {
                    w.or().in("category_id", matchedCatIds);
                }
            });
        }

        // Price range filter
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0) {
            wrapper.ge("price", minPrice);
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) >= 0) {
            wrapper.le("price", maxPrice);
        }

        // Sorting
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc("price");
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc("price");
        } else if ("newest".equals(sort)) {
            wrapper.orderByDesc("create_time");
        } else if ("sales".equals(sort)) {
            wrapper.orderByDesc("stock"); // fallback: no sales field yet
        } else {
            wrapper.orderByDesc("create_time");
        }

        Page<Product> result = productMapper.selectPage(pageParam, wrapper);
        enrichProducts(result.getRecords());
        return result;
    }

    public Product getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            return null;
        }
        enrichProducts(List.of(product));
        return product;
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        List<Product> products = productMapper.findByCategoryId(categoryId);
        enrichProducts(products);
        return products;
    }

    private void enrichProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> categoryNameMap = buildCategoryNameMap(products);
        Map<Long, Long> favoriteCountMap = buildAggregateMap(productFavoriteMapper.selectMaps(
                new QueryWrapper<com.ecommerce.entity.ProductFavorite>()
                        .select("product_id AS productId", "COUNT(*) AS total")
                        .in("product_id", productIds)
                        .groupBy("product_id")
        ));
        Map<Long, Long> salesMap = buildAggregateMap(orderItemMapper.selectMaps(
                new QueryWrapper<com.ecommerce.entity.OrderItem>()
                        .select("product_id AS productId", "COALESCE(SUM(quantity), 0) AS total")
                        .in("product_id", productIds)
                        .groupBy("product_id")
        ));
        Set<Long> favoritedIds = resolveFavoritedIds(productIds);

        for (Product product : products) {
            List<String> imageList = normalizeImages(product.getImages(), product.getImageUrl());
            product.setImageList(imageList);
            if (!imageList.isEmpty()) {
                product.setImageUrl(imageList.get(0));
                product.setImages(String.join(",", imageList));
            }
            product.setCategoryName(categoryNameMap.get(product.getCategoryId()));
            product.setFavoriteCount(favoriteCountMap.getOrDefault(product.getId(), 0L));
            product.setSales(salesMap.getOrDefault(product.getId(), 0L));
            product.setFavorited(favoritedIds.contains(product.getId()));
        }
    }

    private Map<Long, String> buildCategoryNameMap(List<Product> products) {
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private Map<Long, Long> buildAggregateMap(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long productId = getLongValue(row, "productId", "product_id");
            Long total = getLongValue(row, "total");
            if (productId != null && total != null) {
                result.merge(productId, total, Long::sum);
            }
        }
        return result;
    }

    private Long getLongValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Object value = null;
        for (String key : keys) {
            value = row.get(key);
            if (value != null) {
                break;
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
            if (value != null) {
                break;
            }
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Set<Long> resolveFavoritedIds(List<Long> productIds) {
        Long userId = getCurrentUserId();
        if (userId == null || productIds == null || productIds.isEmpty()) {
            return Set.of();
        }
        return productFavoriteMapper.selectList(new QueryWrapper<com.ecommerce.entity.ProductFavorite>()
                        .select("id", "product_id")
                        .eq("user_id", userId)
                        .in("product_id", productIds))
                .stream()
                .map(com.ecommerce.entity.ProductFavorite::getProductId)
                .collect(Collectors.toSet());
    }

    private List<String> normalizeImages(String images, String imageUrl) {
        Set<String> normalized = new LinkedHashSet<>();
        if (StringUtils.hasText(images)) {
            Arrays.stream(images.split("[,，\\n\\r]+"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(normalized::add);
        }
        if (StringUtils.hasText(imageUrl)) {
            normalized.add(imageUrl.trim());
        }
        return List.copyOf(normalized);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if (!StringUtils.hasText(username) || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }
        User user = userMapper.findByUsername(username);
        return user == null ? null : user.getId();
    }
}
