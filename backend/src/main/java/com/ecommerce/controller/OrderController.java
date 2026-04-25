package com.ecommerce.controller;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getReceiverAddress(),
                request.getRemark(),
                request.getCouponId(),
                request.getPaymentMethod(),
                request.getInvoiceTitle(),
                request.getCartItemIds()
        );
    }

    @GetMapping
    public Result<?> getOrderList() {
        return orderService.getOrderList();
    }

    @GetMapping("/{id}")
    public Result<?> getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetail(id);
    }

    @PutMapping("/{id}/cancel")
    public Result<?> cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @PutMapping("/{id}/confirm")
    public Result<?> confirmOrder(@PathVariable Long id) {
        return orderService.confirmReceipt(id);
    }
}
