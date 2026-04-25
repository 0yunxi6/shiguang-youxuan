package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.mapper.UserCouponMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {
    private static final int STATUS_UNUSED = 0;
    private static final int STATUS_USED = 1;
    private static final int STATUS_EXPIRED = 2;

    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;

    public Result<?> getMyCoupons(BigDecimal orderAmount, boolean availableOnly) {
        User user = getCurrentUser();
        issueWelcomeCouponsIfAbsent(user.getId());
        refreshExpiredCoupons(user.getId());

        QueryWrapper<UserCoupon> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).orderByAsc("status").orderByAsc("end_time").orderByDesc("id");
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        coupons.forEach(coupon -> coupon.setCanUse(isCouponUsable(coupon, orderAmount)));
        if (availableOnly) {
            coupons = coupons.stream().filter(coupon -> Boolean.TRUE.equals(coupon.getCanUse())).toList();
        }
        coupons = coupons.stream()
                .sorted(Comparator.comparing((UserCoupon coupon) -> !Boolean.TRUE.equals(coupon.getCanUse()))
                        .thenComparing(UserCoupon::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return Result.success(coupons);
    }

    public UserCoupon validateCouponForOrder(Long userId, Long couponId, BigDecimal orderAmount) {
        if (couponId == null) {
            return null;
        }
        refreshExpiredCoupons(userId);
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || !userId.equals(coupon.getUserId())) {
            throw new BusinessException(404, "优惠券不存在");
        }
        if (!isCouponUsable(coupon, orderAmount)) {
            throw new BusinessException(400, "优惠券当前不可用");
        }
        return coupon;
    }

    public void markCouponUsed(UserCoupon coupon) {
        if (coupon == null) {
            return;
        }
        coupon.setStatus(STATUS_USED);
        coupon.setUseTime(LocalDateTime.now());
        coupon.setUpdateTime(LocalDateTime.now());
        userCouponMapper.updateById(coupon);
    }

    public void restoreCoupon(Long couponId, Long userId) {
        if (couponId == null || userId == null) {
            return;
        }
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || !userId.equals(coupon.getUserId())) {
            return;
        }
        if (coupon.getStatus() != null && coupon.getStatus() == STATUS_USED) {
            coupon.setStatus(STATUS_UNUSED);
            coupon.setUseTime(null);
            coupon.setUpdateTime(LocalDateTime.now());
            userCouponMapper.updateById(coupon);
        }
    }

    public void issueWelcomeCouponsIfAbsent(Long userId) {
        QueryWrapper<UserCoupon> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (userCouponMapper.selectCount(wrapper) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        insertCoupon(userId, "新人专享券", "满50减10", new BigDecimal("10"), new BigDecimal("50"), now, now.plusDays(30));
        insertCoupon(userId, "限时满减券", "满100减20", new BigDecimal("20"), new BigDecimal("100"), now, now.plusDays(15));
        insertCoupon(userId, "会员专享券", "满300减50", new BigDecimal("50"), new BigDecimal("300"), now, now.plusDays(45));
    }

    public void issueWelcomeCouponsIfAbsent(User user) {
        if (user != null && user.getId() != null) {
            issueWelcomeCouponsIfAbsent(user.getId());
        }
    }

    public String buildCouponDisplay(UserCoupon coupon) {
        if (coupon == null) {
            return null;
        }
        String endText = coupon.getEndTime() == null ? "长期有效" : coupon.getEndTime().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        return String.format(Locale.ROOT, "%s（满%s减%s，%s到期）",
                coupon.getName(),
                coupon.getMinAmount().stripTrailingZeros().toPlainString(),
                coupon.getDiscountAmount().stripTrailingZeros().toPlainString(),
                endText);
    }

    private void insertCoupon(Long userId, String name, String description, BigDecimal discountAmount,
                              BigDecimal minAmount, LocalDateTime startTime, LocalDateTime endTime) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setCouponCode(generateCouponCode(name));
        coupon.setName(name);
        coupon.setDescription(description);
        coupon.setDiscountAmount(discountAmount);
        coupon.setMinAmount(minAmount);
        coupon.setStatus(STATUS_UNUSED);
        coupon.setStartTime(startTime);
        coupon.setEndTime(endTime);
        coupon.setCreateTime(LocalDateTime.now());
        coupon.setUpdateTime(LocalDateTime.now());
        userCouponMapper.insert(coupon);
    }

    private void refreshExpiredCoupons(Long userId) {
        QueryWrapper<UserCoupon> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", STATUS_UNUSED).lt("end_time", LocalDateTime.now());
        List<UserCoupon> expiredCoupons = userCouponMapper.selectList(wrapper);
        expiredCoupons.forEach(coupon -> {
            coupon.setStatus(STATUS_EXPIRED);
            coupon.setUpdateTime(LocalDateTime.now());
            userCouponMapper.updateById(coupon);
        });
    }

    private boolean isCouponUsable(UserCoupon coupon, BigDecimal orderAmount) {
        if (coupon == null) {
            return false;
        }
        if (coupon.getStatus() == null || coupon.getStatus() != STATUS_UNUSED) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(now)) {
            return false;
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(now)) {
            return false;
        }
        if (orderAmount != null && coupon.getMinAmount() != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            return false;
        }
        return true;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }

    private String generateCouponCode(String name) {
        String normalized = name == null ? "" : Normalizer.normalize(name, Normalizer.Form.NFKD);
        String prefix = normalized.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        prefix = prefix.isEmpty() ? "CP" : prefix.substring(0, Math.min(prefix.length(), 2));
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }
}
