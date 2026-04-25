package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductReview;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductReviewMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private static final int STATUS_HIDDEN = 0;
    private static final int STATUS_VISIBLE = 1;

    private final ProductReviewMapper reviewMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public Result<?> getPublicReviews(Long productId, int page, int size, Integer rating) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);
        if (rating != null && (rating < 1 || rating > 5)) {
            return Result.error("评分筛选不合法");
        }
        Page<ProductReview> result = reviewMapper.selectPublicPage(new Page<>(page, size), productId, rating);
        result.getRecords().forEach(this::maskUser);
        return Result.success(result);
    }

    public Result<?> getProductSummary(Long productId) {
        Map<String, Object> summary = new HashMap<>(reviewMapper.selectProductSummary(productId));
        for (int rating = 1; rating <= 5; rating++) {
            QueryWrapper<ProductReview> wrapper = new QueryWrapper<>();
            wrapper.eq("product_id", productId).eq("status", STATUS_VISIBLE).eq("rating", rating);
            summary.put("rating" + rating, reviewMapper.selectCount(wrapper));
        }
        return Result.success(summary);
    }

    @Transactional
    public Result<?> createReview(CreateReviewRequest request) {
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            return Result.error("商品不存在或已下架");
        }

        User user = getCurrentUser();
        QueryWrapper<ProductReview> existsWrapper = new QueryWrapper<>();
        existsWrapper.eq("product_id", request.getProductId()).eq("user_id", user.getId());
        if (reviewMapper.selectCount(existsWrapper) > 0) {
            return Result.error(409, "你已评价过该商品");
        }

        ProductReview review = new ProductReview();
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setUserId(user.getId());
        review.setRating(request.getRating());
        review.setContent(request.getContent().trim());
        review.setImages(StringUtils.hasText(request.getImages()) ? request.getImages().trim() : null);
        review.setStatus(STATUS_VISIBLE);
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);
        return Result.success("评价发布成功", review.getId());
    }

    public Result<?> getAdminReviews(int page, int size, Integer status, Long productId, String keyword) {
        if (status != null && status != STATUS_HIDDEN && status != STATUS_VISIBLE) {
            return Result.error("评价状态不合法");
        }
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<ProductReview> result = reviewMapper.selectAdminPage(new Page<>(page, size), status, productId,
                StringUtils.hasText(keyword) ? keyword.trim() : null);
        return Result.success(result);
    }

    public Result<?> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", reviewMapper.selectCount(new QueryWrapper<>()));
        stats.put("visible", reviewMapper.selectCount(new QueryWrapper<ProductReview>().eq("status", STATUS_VISIBLE)));
        stats.put("hidden", reviewMapper.selectCount(new QueryWrapper<ProductReview>().eq("status", STATUS_HIDDEN)));
        return Result.success(stats);
    }

    @Transactional
    public Result<?> updateStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_HIDDEN && status != STATUS_VISIBLE)) {
            return Result.error("评价状态不合法");
        }
        ProductReview review = reviewMapper.selectById(id);
        if (review == null) {
            return Result.error("评价不存在");
        }
        review.setStatus(status);
        review.setUpdateTime(LocalDateTime.now());
        reviewMapper.updateById(review);
        return Result.success(status == STATUS_VISIBLE ? "评价已展示" : "评价已隐藏");
    }

    @Transactional
    public Result<?> deleteReview(Long id) {
        ProductReview review = reviewMapper.selectById(id);
        if (review == null) {
            return Result.error("评价不存在");
        }
        reviewMapper.deleteById(id);
        return Result.success("评价已删除");
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }

    private void maskUser(ProductReview review) {
        String displayName = StringUtils.hasText(review.getNickname()) ? review.getNickname() : review.getUsername();
        if (StringUtils.hasText(displayName) && displayName.length() > 1) {
            review.setNickname(displayName.substring(0, 1) + "**");
        } else {
            review.setNickname("用户");
        }
        review.setUsername(null);
    }
}
