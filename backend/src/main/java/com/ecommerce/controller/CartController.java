package com.ecommerce.controller;

import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.service.CartService;
import com.ecommerce.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public Result<?> addToCart(@Valid @RequestBody CartItemRequest request) {
        return cartService.addToCart(request.getProductId(), request.getQuantity());
    }

    @GetMapping
    public Result<?> getCartList() {
        return cartService.getCartList();
    }

    @PutMapping("/update")
    public Result<?> updateCartItem(@Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateCartItem(request.getId(), request.getQuantity());
    }

    @DeleteMapping("/clear")
    public Result<?> clearCart() {
        return cartService.clearCart();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteCartItem(@PathVariable Long id) {
        return cartService.deleteCartItem(id);
    }
}
