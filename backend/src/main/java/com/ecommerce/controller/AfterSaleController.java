package com.ecommerce.controller;

import com.ecommerce.service.AfterSaleService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/after-sales")
@RequiredArgsConstructor
public class AfterSaleController {
    private final AfterSaleService afterSaleService;

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> body) {
        Long orderId = body.get("orderId") instanceof Number number ? number.longValue() : null;
        String type = body.get("type") == null ? null : String.valueOf(body.get("type"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return afterSaleService.create(orderId, type, reason);
    }

    @GetMapping
    public Result<?> list() {
        return afterSaleService.myList();
    }
}
