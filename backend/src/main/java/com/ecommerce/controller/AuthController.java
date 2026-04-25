package com.ecommerce.controller;

import com.ecommerce.dto.ForgotPasswordResetRequest;
import com.ecommerce.dto.ForgotPasswordVerifyRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.service.AuthService;
import com.ecommerce.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password/verify")
    public Result<?> forgotPasswordVerify(@Valid @RequestBody ForgotPasswordVerifyRequest request) {
        return authService.forgotPasswordVerify(request);
    }

    @PostMapping("/forgot-password/reset")
    public Result<?> forgotPasswordReset(@Valid @RequestBody ForgotPasswordResetRequest request) {
        return authService.forgotPasswordReset(request);
    }

    @GetMapping("/info")
    public Result<?> getUserInfo() {
        return authService.getUserInfo();
    }
}
