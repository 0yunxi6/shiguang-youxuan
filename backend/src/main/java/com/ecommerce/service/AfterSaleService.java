package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.AfterSale;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.AfterSaleMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AfterSaleService {
    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Transactional
    public Result<?> create(Long orderId, String type, String reason) {
        if (orderId == null || orderId <= 0) {
            return Result.error("订单不合法");
        }
        if (!StringUtils.hasText(reason) || reason.trim().length() < 4) {
            return Result.error("请填写至少4个字的售后原因");
        }
        String safeType = StringUtils.hasText(type) ? type.trim() : "refund";
        if (!List.of("refund", "return", "exchange").contains(safeType)) {
            return Result.error("售后类型不合法");
        }
        User user = getCurrentUser();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() == null || (order.getStatus() < 1 || order.getStatus() > 3)) {
            return Result.error("当前订单状态暂不支持售后");
        }
        Long exists = afterSaleMapper.selectCount(new QueryWrapper<AfterSale>()
                .eq("user_id", user.getId())
                .eq("order_id", orderId)
                .in("status", List.of(0, 1)));
        if (exists > 0) {
            return Result.error("该订单已有处理中售后申请");
        }
        AfterSale afterSale = new AfterSale();
        afterSale.setUserId(user.getId());
        afterSale.setOrderId(orderId);
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setType(safeType);
        afterSale.setReason(reason.trim());
        afterSale.setAmount(order.getTotalAmount());
        afterSale.setStatus(0);
        afterSale.setCreateTime(LocalDateTime.now());
        afterSaleMapper.insert(afterSale);
        messageService.create(user.getId(), "after_sale", "售后申请已提交", "订单 " + order.getOrderNo() + " 的售后申请已提交，请等待审核。");
        return Result.success("售后申请已提交", afterSale.getId());
    }

    public Result<?> myList() {
        User user = getCurrentUser();
        QueryWrapper<AfterSale> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).orderByDesc("create_time");
        return Result.success(afterSaleMapper.selectList(wrapper));
    }

    public Result<?> adminList(int page, int size, Integer status, String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        QueryWrapper<AfterSale> wrapper = new QueryWrapper<>();
        if (status != null && status >= 0 && status <= 3) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("order_no", keyword.trim()).or().like("reason", keyword.trim()));
        }
        wrapper.orderByDesc("create_time");
        return Result.success(afterSaleMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Transactional
    public Result<?> audit(Long id, Integer status, String remark) {
        if (status == null || status < 1 || status > 3) {
            return Result.error("售后状态不合法");
        }
        AfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null) {
            return Result.error("售后申请不存在");
        }
        afterSale.setStatus(status);
        afterSale.setAuditRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);
        messageService.create(afterSale.getUserId(), "after_sale", "售后进度更新",
                "订单 " + afterSale.getOrderNo() + " 的售后状态已更新为：" + statusText(status));
        return Result.success("售后状态已更新");
    }

    private String statusText(Integer status) {
        return switch (status) {
            case 1 -> "已同意";
            case 2 -> "已拒绝";
            case 3 -> "已完成";
            default -> "待审核";
        };
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }
}
