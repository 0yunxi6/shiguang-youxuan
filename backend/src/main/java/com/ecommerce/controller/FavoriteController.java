package com.ecommerce.controller;

import com.ecommerce.service.ProductFavoriteService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final ProductFavoriteService favoriteService;

    @GetMapping
    public Result<?> list() {
        return favoriteService.listFavorites();
    }

    @PostMapping("/{productId}")
    public Result<?> add(@PathVariable Long productId) {
        return favoriteService.addFavorite(productId);
    }

    @DeleteMapping("/{productId}")
    public Result<?> remove(@PathVariable Long productId) {
        return favoriteService.removeFavorite(productId);
    }
}
