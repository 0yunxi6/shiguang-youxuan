package com.ecommerce.controller;

import com.ecommerce.dto.UpdatePasswordRequest;
import com.ecommerce.dto.UpdateProfileRequest;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.service.MessageService;
import com.ecommerce.service.TokenBlacklistService;
import com.ecommerce.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final Map<Long, EmailCode> emailCodes = new ConcurrentHashMap<>();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MessageService messageService;

    @GetMapping("/profile")
    public Result<?> getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateUser) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (updateUser.getNickname() != null) user.setNickname(updateUser.getNickname().trim());
        if (updateUser.getEmail() != null) user.setEmail(updateUser.getEmail().trim().toLowerCase());
        if (updateUser.getPhone() != null) user.setPhone(updateUser.getPhone().trim());
        if (updateUser.getAvatar() != null) user.setAvatar(updateUser.getAvatar().trim());
        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/password")
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest request,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        messageService.create(user.getId(), "security", "密码已修改", "你的账号密码已修改，当前登录会话已失效，请重新登录。");
        String token = extractToken(authorization);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            tokenBlacklistService.blacklist(token, jwtTokenProvider.getExpirationMillis(token));
        }
        return Result.success("密码修改成功");
    }

    @PostMapping("/email/verification-code")
    public Result<?> requestEmailCode() {
        User user = currentUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return Result.error("请先设置邮箱");
        }
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        emailCodes.put(user.getId(), new EmailCode(code, LocalDateTime.now().plusMinutes(10)));
        messageService.create(user.getId(), "security", "邮箱验证码", "你的邮箱验证模拟验证码是：" + code + "，10分钟内有效。");
        return Result.success("验证码已生成（模拟发送）", Map.of("code", code, "expiresIn", 600));
    }

    @PostMapping("/email/verify")
    public Result<?> verifyEmail(@RequestBody Map<String, String> body) {
        User user = currentUser();
        EmailCode code = emailCodes.get(user.getId());
        String submitted = body == null ? null : body.get("code");
        if (code == null || code.expireAt().isBefore(LocalDateTime.now()) || submitted == null || !submitted.equals(code.code())) {
            return Result.error("验证码不正确或已过期");
        }
        user.setEmailVerified(1);
        userMapper.updateById(user);
        emailCodes.remove(user.getId());
        messageService.create(user.getId(), "security", "邮箱验证成功", "你的邮箱已完成验证。");
        return Result.success("邮箱验证成功");
    }

    @PutMapping("/2fa")
    public Result<?> toggle2fa(@RequestParam Boolean enabled) {
        User user = currentUser();
        user.setTwoFactorEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        userMapper.updateById(user);
        messageService.create(user.getId(), "security", "两步验证设置变更",
                Boolean.TRUE.equals(enabled) ? "你已开启两步验证基础开关。" : "你已关闭两步验证基础开关。");
        return Result.success("两步验证设置已更新");
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }

    private record EmailCode(String code, LocalDateTime expireAt) {}
}
