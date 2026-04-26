package com.ecommerce.controller.admin;

import com.ecommerce.service.ProductReviewService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ProductReviewService reviewService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Long productId,
                          @RequestParam(required = false) String keyword) {
        return reviewService.getAdminReviews(page, size, status, productId, keyword);
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        return reviewService.getAdminStats();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return reviewService.updateStatus(id, status);
    }

    @PutMapping("/{id}/reply")
    public Result<?> reply(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return reviewService.replyReview(id, body == null ? null : body.get("reply"));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return reviewService.deleteReview(id);
    }
}
