package com.ecommerce.controller;

import com.ecommerce.service.CouponService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) BigDecimal orderAmount,
                          @RequestParam(defaultValue = "false") boolean availableOnly) {
        return couponService.getMyCoupons(orderAmount, availableOnly);
    }
}
