package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String role,
                          @RequestParam(required = false) Integer status) {
        if (status != null && isInvalidBinaryStatus(status)) {
            return Result.error("用户状态不合法");
        }
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<User> pageParam = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("username", keyword).or().like("email", keyword).or().like("nickname", keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq("role", role);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        Page<User> result = userMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (isInvalidBinaryStatus(status)) {
            return Result.error("用户状态不合法");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            return Result.error("管理员账号不能禁用");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            return Result.error("管理员账号不能删除");
        }
        userMapper.deleteById(id);
        return Result.success("用户删除成功");
    }

    private boolean isInvalidBinaryStatus(Integer status) {
        return status == null || (status != 0 && status != 1);
    }
}
