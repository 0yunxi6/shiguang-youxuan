package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductViewHistory;
import com.ecommerce.entity.SearchLog;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.ProductFavoriteMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductViewHistoryMapper;
import com.ecommerce.mapper.SearchLogMapper;
import com.ecommerce.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductFavoriteMapper productFavoriteMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final SearchLogMapper searchLogMapper;
    private final ProductViewHistoryMapper productViewHistoryMapper;

    public Page<Product> getProductList(int page, int size, Long categoryId, String keyword,
                                         String sort, BigDecimal minPrice, BigDecimal maxPrice,
                                         Boolean inStockOnly, String brand, Integer minRating) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 60);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        if (page == 1 && StringUtils.hasText(normalizedKeyword)) {
            recordSearchKeyword(normalizedKeyword);
        }
        Page<Product> pageParam = new Page<>(page, size);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (StringUtils.hasText(brand)) {
            wrapper.like("brand", brand.trim());
        }

        if (StringUtils.hasText(normalizedKeyword)) {
            String kw = normalizedKeyword;
            // Find categories matching the keyword
            QueryWrapper<Category> catWrapper = new QueryWrapper<>();
            catWrapper.like("name", kw);
            List<Long> matchedCatIds = categoryMapper.selectList(catWrapper)
                    .stream().map(Category::getId).collect(Collectors.toList());

            wrapper.and(w -> {
                w.like("name", kw).or().like("description", kw).or().like("brand", kw);
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
        if (Boolean.TRUE.equals(inStockOnly)) {
            wrapper.gt("stock", 0);
        }
        if (minRating != null && minRating >= 1 && minRating <= 5) {
            wrapper.apply("(SELECT COALESCE(AVG(pr.rating), 0) FROM product_review pr WHERE pr.product_id = product.id AND pr.status = 1 AND pr.deleted = 0) >= {0}", minRating);
        }

        // Sorting
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc("price");
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc("price");
        } else if ("newest".equals(sort)) {
            wrapper.orderByDesc("create_time");
        } else if ("sales".equals(sort)) {
            wrapper.orderByDesc("(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi WHERE oi.product_id = product.id)");
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
        recordProductView(product.getId());
        return product;
    }

    public List<Product> getPersonalRecommendations(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        Long userId = getCurrentUserId();
        if (userId != null) {
            QueryWrapper<ProductViewHistory> historyWrapper = new QueryWrapper<>();
            historyWrapper.eq("user_id", userId).orderByDesc("view_time").last("LIMIT 1");
            ProductViewHistory latest = productViewHistoryMapper.selectList(historyWrapper).stream().findFirst().orElse(null);
            if (latest != null) {
                List<Product> related = getRecommendations(latest.getProductId(), safeLimit);
                if (!related.isEmpty()) {
                    return related;
                }
            }
        }
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .gt("stock", 0)
                .orderByDesc("(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi WHERE oi.product_id = product.id)")
                .orderByDesc("create_time")
                .last("LIMIT " + safeLimit);
        List<Product> products = productMapper.selectList(wrapper);
        enrichProducts(products);
        return products;
    }

    public List<ProductViewHistory> getMyViewHistory(int limit) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        QueryWrapper<ProductViewHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("view_time").last("LIMIT " + safeLimit);
        List<ProductViewHistory> rows = productViewHistoryMapper.selectList(wrapper);
        if (rows.isEmpty()) {
            return rows;
        }
        List<Long> productIds = rows.stream().map(ProductViewHistory::getProductId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        for (ProductViewHistory row : rows) {
            Product product = productMap.get(row.getProductId());
            if (product != null) {
                row.setProductName(product.getName());
                row.setProductImage(product.getImageUrl());
                row.setPrice(product.getPrice());
            }
        }
        return rows;
    }

    public List<Product> getRecommendations(Long productId, int limit) {
        if (productId == null || productId <= 0) {
            return List.of();
        }
        Product current = productMapper.selectById(productId);
        if (current == null || current.getStatus() == null || current.getStatus() != 1) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 12);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .ne("id", productId)
                .gt("stock", 0);
        if (current.getCategoryId() != null) {
            wrapper.eq("category_id", current.getCategoryId());
        }
        wrapper.orderByDesc("(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi WHERE oi.product_id = product.id)")
                .orderByDesc("create_time")
                .last("LIMIT " + safeLimit);
        List<Product> products = productMapper.selectList(wrapper);

        if (products.size() < safeLimit && current.getCategoryId() != null) {
            List<Long> existingIds = products.stream().map(Product::getId).collect(Collectors.toList());
            QueryWrapper<Product> fallback = new QueryWrapper<>();
            fallback.eq("status", 1)
                    .ne("id", productId)
                    .gt("stock", 0);
            if (!existingIds.isEmpty()) {
                fallback.notIn("id", existingIds);
            }
            fallback.orderByDesc("(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi WHERE oi.product_id = product.id)")
                    .orderByDesc("create_time")
                    .last("LIMIT " + (safeLimit - products.size()));
            products = new java.util.ArrayList<>(products);
            products.addAll(productMapper.selectList(fallback));
        }
        enrichProducts(products);
        return products;
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        List<Product> products = productMapper.findByCategoryId(categoryId);
        enrichProducts(products);
        return products;
    }

    public List<Map<String, Object>> getHotSearchKeywords(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        try {
            QueryWrapper<SearchLog> wrapper = new QueryWrapper<>();
            wrapper.select("keyword", "search_count")
                    .orderByDesc("search_count")
                    .orderByDesc("last_search_time")
                    .last("LIMIT " + safeLimit);
            return searchLogMapper.selectList(wrapper).stream()
                    .filter(item -> StringUtils.hasText(item.getKeyword()))
                    .map(item -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("keyword", item.getKeyword());
                        row.put("count", item.getSearchCount() == null ? 0 : item.getSearchCount());
                        return row;
                    })
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public List<String> getSearchSuggestions(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 12);
        String kw = normalizeSearchKeyword(keyword);
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        if (StringUtils.hasText(kw)) {
            QueryWrapper<Product> productWrapper = new QueryWrapper<>();
            productWrapper.select("name", "brand")
                    .eq("status", 1)
                    .and(w -> w.like("name", kw).or().like("brand", kw))
                    .orderByDesc("create_time")
                    .last("LIMIT " + (safeLimit * 2));
            for (Product product : productMapper.selectList(productWrapper)) {
                addSuggestion(suggestions, product.getBrand(), kw);
                addSuggestion(suggestions, product.getName(), kw);
                if (suggestions.size() >= safeLimit) {
                    break;
                }
            }

            if (suggestions.size() < safeLimit) {
                for (Map<String, Object> row : getHotSearchKeywords(safeLimit * 2)) {
                    Object value = row.get("keyword");
                    if (value instanceof String text) {
                        addSuggestion(suggestions, text, kw);
                    }
                    if (suggestions.size() >= safeLimit) {
                        break;
                    }
                }
            }
        } else {
            for (Map<String, Object> row : getHotSearchKeywords(safeLimit)) {
                Object value = row.get("keyword");
                if (value instanceof String text && StringUtils.hasText(text)) {
                    suggestions.add(text.trim());
                }
            }
        }

        return suggestions.stream().limit(safeLimit).toList();
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
            boolean promotionActive = isPromotionActive(product);
            product.setPromotionActive(promotionActive);
            product.setEffectivePrice(promotionActive && product.getPromotionPrice() != null
                    ? product.getPromotionPrice()
                    : product.getPrice());
        }
    }

    private boolean isPromotionActive(Product product) {
        if (product == null || product.getPromotionPrice() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (product.getPromotionStartTime() == null || !product.getPromotionStartTime().isAfter(now))
                && (product.getPromotionEndTime() == null || !product.getPromotionEndTime().isBefore(now));
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

    private String normalizeSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }

    private void recordSearchKeyword(String keyword) {
        try {
            searchLogMapper.upsertKeyword(keyword);
        } catch (RuntimeException ignored) {
            // Search logging is non-critical; product search should not fail because statistics failed.
        }
    }

    private void recordProductView(Long productId) {
        Long userId = getCurrentUserId();
        if (userId == null || productId == null) {
            return;
        }
        try {
            QueryWrapper<ProductViewHistory> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId).eq("product_id", productId);
            ProductViewHistory existing = productViewHistoryMapper.selectOne(wrapper);
            if (existing == null) {
                ProductViewHistory history = new ProductViewHistory();
                history.setUserId(userId);
                history.setProductId(productId);
                history.setViewTime(java.time.LocalDateTime.now());
                productViewHistoryMapper.insert(history);
            } else {
                existing.setViewTime(java.time.LocalDateTime.now());
                productViewHistoryMapper.updateById(existing);
            }
        } catch (RuntimeException ignored) {
            // Browsing history is best-effort and should never break product detail.
        }
    }

    private void addSuggestion(Set<String> suggestions, String value, String keyword) {
        if (!StringUtils.hasText(value) || !containsIgnoreCase(value, keyword)) {
            return;
        }
        suggestions.add(value.trim());
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(target)) {
            return false;
        }
        return source.toLowerCase().contains(target.toLowerCase());
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
