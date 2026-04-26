package com.ecommerce.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
@CrossOrigin
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    
    @GetMapping
    public Result<Page<Product>> getProductList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(required = false) String brand) {
        return Result.success(productService.getProductList(page, size, categoryId, keyword, sort, minPrice, maxPrice, inStockOnly, brand));
    }

    @GetMapping("/search")
    public Result<Page<Product>> searchProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(required = false) String brand) {
        return getProductList(page, size, categoryId, keyword, sort, minPrice, maxPrice, inStockOnly, brand);
    }

    @GetMapping("/search/hot")
    public Result<?> getHotSearchKeywords(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(productService.getHotSearchKeywords(limit));
    }

    @GetMapping("/search/suggestions")
    public Result<?> getSearchSuggestions(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.getSearchSuggestions(keyword, limit));
    }

    @GetMapping("/{id}/recommendations")
    public Result<List<Product>> getRecommendations(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "4") int limit) {
        return Result.success(productService.getRecommendations(id, limit));
    }
    
    @GetMapping("/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.error("商品不存在或已下架");
        }
        return Result.success(product);
    }
}
