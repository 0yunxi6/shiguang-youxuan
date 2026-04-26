package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.User;
import com.ecommerce.entity.CouponTemplate;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.mapper.CouponTemplateMapper;
import com.ecommerce.mapper.UserCouponMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.service.CouponService;
import com.ecommerce.service.MessageService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponService couponService;
    private final MessageService messageService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);

        Page<UserCoupon> pageParam = new Page<>(page, size);
        QueryWrapper<UserCoupon> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> matchedUserIds = findMatchedUserIds(kw);
            wrapper.and(w -> {
                w.like("coupon_code", kw)
                        .or().like("name", kw)
                        .or().like("description", kw);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in("user_id", matchedUserIds);
                }
            });
        }
        wrapper.orderByDesc("id");

        Page<UserCoupon> result = userCouponMapper.selectPage(pageParam, wrapper);
        List<UserCoupon> coupons = result.getRecords();
        if (coupons.isEmpty()) {
            return Result.success(buildPageData(result, List.of()));
        }

        List<Long> userIds = coupons.stream().map(UserCoupon::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Map<String, Object>> records = coupons.stream()
                .map(coupon -> buildCouponRow(coupon, userMap.get(coupon.getUserId())))
                .toList();
        return Result.success(buildPageData(result, records));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        UserCoupon coupon = userCouponMapper.selectById(id);
        if (coupon == null) {
            return Result.error("优惠券不存在");
        }
        userCouponMapper.deleteById(id);
        return Result.success("优惠券删除成功");
    }

    @GetMapping("/templates")
    public Result<?> listTemplates() {
        return Result.success(couponTemplateMapper.selectList(new QueryWrapper<CouponTemplate>().orderByDesc("id")));
    }

    @PostMapping("/templates")
    public Result<?> createTemplate(@RequestBody CouponTemplate template) {
        Result<?> validation = validateTemplate(template);
        if (validation != null) {
            return validation;
        }
        template.setStatus(template.getStatus() == null ? 1 : template.getStatus());
        couponTemplateMapper.insert(template);
        return Result.success("优惠券模板已创建", template.getId());
    }

    @PostMapping("/templates/{id}/issue")
    public Result<?> issueTemplate(@PathVariable Long id,
                                   @RequestParam(defaultValue = "all") String target,
                                   @RequestParam(required = false) Long userId) {
        CouponTemplate template = couponTemplateMapper.selectById(id);
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            return Result.error("优惠券模板不存在或已停用");
        }
        List<User> users;
        if ("user".equalsIgnoreCase(target) && userId != null) {
            User user = userMapper.selectById(userId);
            users = user == null ? List.of() : List.of(user);
        } else {
            users = userMapper.selectList(new QueryWrapper<User>().eq("status", 1));
        }
        int count = 0;
        for (User user : users) {
            couponService.issueCoupon(user.getId(), template.getName(), template.getDescription(),
                    template.getDiscountAmount(), template.getMinAmount(), template.getValidDays());
            messageService.create(user.getId(), "coupon", "收到新优惠券", "你收到了一张「" + template.getName() + "」，可在结算时使用。");
            count++;
        }
        return Result.success("发券完成", Map.of("issued", count));
    }

    private Result<?> validateTemplate(CouponTemplate template) {
        if (template == null || !StringUtils.hasText(template.getName())) {
            return Result.error("模板名称不能为空");
        }
        if (template.getDiscountAmount() == null || template.getDiscountAmount().signum() <= 0) {
            return Result.error("优惠金额必须大于0");
        }
        if (template.getMinAmount() == null || template.getMinAmount().signum() < 0) {
            return Result.error("使用门槛不能小于0");
        }
        template.setName(template.getName().trim());
        template.setDescription(StringUtils.hasText(template.getDescription()) ? template.getDescription().trim() : null);
        template.setValidDays(Math.min(Math.max(template.getValidDays() == null ? 30 : template.getValidDays(), 1), 365));
        return null;
    }

    private List<Long> findMatchedUserIds(String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("id")
                .and(w -> w.like("username", keyword)
                        .or().like("nickname", keyword)
                        .or().like("email", keyword)
                        .or().like("phone", keyword));
        return userMapper.selectList(wrapper).stream().map(User::getId).toList();
    }

    private Map<String, Object> buildCouponRow(UserCoupon coupon, User user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", coupon.getId());
        row.put("userId", coupon.getUserId());
        row.put("username", user == null ? null : user.getUsername());
        row.put("nickname", user == null ? null : user.getNickname());
        row.put("email", user == null ? null : user.getEmail());
        row.put("couponCode", coupon.getCouponCode());
        row.put("name", coupon.getName());
        row.put("description", coupon.getDescription());
        row.put("discountAmount", coupon.getDiscountAmount());
        row.put("minAmount", coupon.getMinAmount());
        row.put("status", coupon.getStatus());
        row.put("statusText", buildStatusText(coupon.getStatus()));
        row.put("startTime", coupon.getStartTime());
        row.put("endTime", coupon.getEndTime());
        row.put("useTime", coupon.getUseTime());
        row.put("createTime", coupon.getCreateTime());
        row.put("updateTime", coupon.getUpdateTime());
        return row;
    }

    private Map<String, Object> buildPageData(Page<?> page, List<?> records) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return data;
    }

    private String buildStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "未使用";
            case 1 -> "已使用";
            case 2 -> "已过期";
            default -> "未知";
        };
    }
}
