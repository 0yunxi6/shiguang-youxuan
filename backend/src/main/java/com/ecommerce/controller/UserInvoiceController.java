package com.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserInvoice;
import com.ecommerce.mapper.UserInvoiceMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class UserInvoiceController {
    private final UserInvoiceMapper invoiceMapper;
    private final UserMapper userMapper;

    @GetMapping
    public Result<?> list() {
        User user = getCurrentUser();
        return Result.success(invoiceMapper.selectList(new QueryWrapper<UserInvoice>()
                .eq("user_id", user.getId())
                .orderByDesc("is_default")
                .orderByDesc("id")));
    }

    @PostMapping
    @Transactional
    public Result<?> create(@RequestBody UserInvoice invoice) {
        User user = getCurrentUser();
        Result<?> validation = validate(invoice);
        if (validation != null) return validation;
        invoice.setUserId(user.getId());
        if (Boolean.TRUE.equals(invoice.getIsDefault() != null && invoice.getIsDefault() == 1)) {
            clearDefault(user.getId());
        }
        invoice.setCreateTime(LocalDateTime.now());
        invoiceMapper.insert(invoice);
        return Result.success("发票信息已保存", invoice.getId());
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<?> update(@PathVariable Long id, @RequestBody UserInvoice invoice) {
        User user = getCurrentUser();
        UserInvoice old = invoiceMapper.selectById(id);
        if (old == null || !user.getId().equals(old.getUserId())) {
            return Result.error("发票信息不存在");
        }
        Result<?> validation = validate(invoice);
        if (validation != null) return validation;
        old.setTitle(invoice.getTitle().trim());
        old.setTaxNo(StringUtils.hasText(invoice.getTaxNo()) ? invoice.getTaxNo().trim() : null);
        old.setIsDefault(invoice.getIsDefault() == null ? 0 : invoice.getIsDefault());
        if (old.getIsDefault() == 1) {
            clearDefault(user.getId());
        }
        old.setUpdateTime(LocalDateTime.now());
        invoiceMapper.updateById(old);
        return Result.success("发票信息已更新");
    }

    @PutMapping("/{id}/default")
    @Transactional
    public Result<?> setDefault(@PathVariable Long id) {
        User user = getCurrentUser();
        UserInvoice invoice = invoiceMapper.selectById(id);
        if (invoice == null || !user.getId().equals(invoice.getUserId())) {
            return Result.error("发票信息不存在");
        }
        clearDefault(user.getId());
        invoice.setIsDefault(1);
        invoice.setUpdateTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        return Result.success("默认发票已更新");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        User user = getCurrentUser();
        UserInvoice invoice = invoiceMapper.selectById(id);
        if (invoice == null || !user.getId().equals(invoice.getUserId())) {
            return Result.error("发票信息不存在");
        }
        invoiceMapper.deleteById(id);
        return Result.success("发票信息已删除");
    }

    private Result<?> validate(UserInvoice invoice) {
        if (invoice == null || !StringUtils.hasText(invoice.getTitle())) {
            return Result.error("发票抬头不能为空");
        }
        invoice.setTitle(invoice.getTitle().trim());
        if (invoice.getTitle().length() > 100) {
            return Result.error("发票抬头不能超过100个字符");
        }
        if (StringUtils.hasText(invoice.getTaxNo()) && invoice.getTaxNo().trim().length() > 50) {
            return Result.error("纳税人识别号不能超过50个字符");
        }
        if (invoice.getIsDefault() == null) {
            invoice.setIsDefault(0);
        }
        return null;
    }

    private void clearDefault(Long userId) {
        UserInvoice update = new UserInvoice();
        update.setIsDefault(0);
        invoiceMapper.update(update, new QueryWrapper<UserInvoice>().eq("user_id", userId));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }
}
