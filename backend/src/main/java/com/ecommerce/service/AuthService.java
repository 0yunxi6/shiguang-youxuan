package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.dto.ForgotPasswordResetRequest;
import com.ecommerce.dto.ForgotPasswordVerifyRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final CouponService couponService;

    public Result<?> login(LoginRequest request) {
        String username = request.getUsername().trim();
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );
        String token = jwtTokenProvider.generateToken(username);
        User user = userMapper.findByUsername(username);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        if (user != null) {
            user.setPassword(null);
            data.put("user", user);
        }
        return Result.success(data);
    }

    public Result<?> register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone() == null ? null : request.getPhone().trim();

        if (userMapper.findByUsername(username) != null) {
            return Result.error(409, "用户名已被占用");
        }
        Long emailCount = userMapper.selectCount(new QueryWrapper<User>().eq("email", email));
        if (emailCount > 0) {
            return Result.error(409, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        couponService.issueWelcomeCouponsIfAbsent(user);
        return Result.success("注册成功");
    }

    public Result<?> forgotPasswordVerify(ForgotPasswordVerifyRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();
        User user = userMapper.findByUsername(username);
        if (user == null || user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            return Result.error("用户名或邮箱不匹配");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("resetToken", jwtTokenProvider.generateResetPasswordToken(user.getUsername()));
        data.put("expiresIn", 600);
        return Result.success("校验通过", data);
    }

    public Result<?> forgotPasswordReset(ForgotPasswordResetRequest request) {
        if (!jwtTokenProvider.validateResetPasswordToken(request.getResetToken())) {
            return Result.error("重置链接已失效，请重新验证身份");
        }

        String username = jwtTokenProvider.getUsernameFromToken(request.getResetToken());
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success("密码重置成功");
    }

    public Result<?> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userMapper.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }
}
