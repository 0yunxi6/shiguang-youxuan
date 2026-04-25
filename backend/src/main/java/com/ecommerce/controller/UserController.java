package com.ecommerce.controller;

import com.ecommerce.dto.UpdatePasswordRequest;
import com.ecommerce.dto.UpdateProfileRequest;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
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
        return Result.success("密码修改成功");
    }
}
