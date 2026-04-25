package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductFavorite;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.ProductFavoriteMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/favorites")
@RequiredArgsConstructor
public class AdminFavoriteController {

    private final ProductFavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);

        Page<ProductFavorite> pageParam = new Page<>(page, size);
        QueryWrapper<ProductFavorite> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> matchedUserIds = findMatchedUserIds(kw);
            List<Long> matchedProductIds = findMatchedProductIds(kw);
            if (matchedUserIds.isEmpty() && matchedProductIds.isEmpty()) {
                return Result.success(buildPageData(pageParam, List.of()));
            }
            if (!matchedUserIds.isEmpty() && !matchedProductIds.isEmpty()) {
                wrapper.and(w -> w.in("user_id", matchedUserIds).or().in("product_id", matchedProductIds));
            } else if (!matchedUserIds.isEmpty()) {
                wrapper.in("user_id", matchedUserIds);
            } else {
                wrapper.in("product_id", matchedProductIds);
            }
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");

        Page<ProductFavorite> result = favoriteMapper.selectPage(pageParam, wrapper);
        List<ProductFavorite> favorites = result.getRecords();
        if (favorites.isEmpty()) {
            return Result.success(buildPageData(result, List.of()));
        }

        List<Long> userIds = favorites.stream().map(ProductFavorite::getUserId).filter(Objects::nonNull).distinct().toList();
        List<Long> productIds = favorites.stream().map(ProductFavorite::getProductId).filter(Objects::nonNull).distinct().toList();

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Map<String, Object>> records = favorites.stream()
                .map(favorite -> buildFavoriteRow(favorite, userMap.get(favorite.getUserId()), productMap.get(favorite.getProductId())))
                .toList();
        return Result.success(buildPageData(result, records));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        ProductFavorite favorite = favoriteMapper.selectById(id);
        if (favorite == null) {
            return Result.error("收藏记录不存在");
        }
        favoriteMapper.deleteById(id);
        return Result.success("收藏记录删除成功");
    }

    private List<Long> findMatchedUserIds(String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("id")
                .and(w -> w.like("username", keyword)
                        .or().like("nickname", keyword)
                        .or().like("email", keyword)
                        .or().like("phone", keyword));
        return userMapper.selectList(wrapper).stream().map(User::getId).toList();
    }

    private List<Long> findMatchedProductIds(String keyword) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.select("id")
                .and(w -> w.like("name", keyword).or().like("description", keyword));
        return productMapper.selectList(wrapper).stream().map(Product::getId).toList();
    }

    private Map<String, Object> buildFavoriteRow(ProductFavorite favorite, User user, Product product) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", favorite.getId());
        row.put("userId", favorite.getUserId());
        row.put("productId", favorite.getProductId());
        row.put("createTime", favorite.getCreateTime());
        row.put("username", user == null ? null : user.getUsername());
        row.put("nickname", user == null ? null : user.getNickname());
        row.put("email", user == null ? null : user.getEmail());
        row.put("productName", product == null ? null : product.getName());
        row.put("productImage", product == null ? null : product.getImageUrl());
        row.put("productPrice", product == null ? null : product.getPrice());
        row.put("productStatus", product == null ? null : product.getStatus());
        return row;
    }

    private Map<String, Object> buildPageData(Page<?> page, List<?> records) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return data;
    }
}
