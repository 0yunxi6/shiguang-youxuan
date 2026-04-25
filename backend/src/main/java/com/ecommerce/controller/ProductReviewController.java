package com.ecommerce.controller;

import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.service.ProductReviewService;
import com.ecommerce.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public Result<?> list(@PathVariable Long productId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "5") int size,
                          @RequestParam(required = false) Integer rating) {
        return reviewService.getPublicReviews(productId, page, size, rating);
    }

    @GetMapping("/products/{productId}/reviews/summary")
    public Result<?> summary(@PathVariable Long productId) {
        return reviewService.getProductSummary(productId);
    }

    @PostMapping("/reviews")
    public Result<?> create(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.createReview(request);
    }
}
