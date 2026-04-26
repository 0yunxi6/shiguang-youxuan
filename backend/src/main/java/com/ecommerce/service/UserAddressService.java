package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserAddress;
import com.ecommerce.mapper.UserAddressMapper;
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
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;
    private final UserMapper userMapper;

    public Result<?> listMyAddresses() {
        User user = getCurrentUser();
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId())
                .eq("status", 1)
                .orderByDesc("is_default")
                .orderByDesc("update_time")
                .orderByDesc("id");
        return Result.success(userAddressMapper.selectList(wrapper));
    }

    @Transactional
    public Result<?> createAddress(UserAddress request) {
        User user = getCurrentUser();
        UserAddress address = new UserAddress();
        copyEditableFields(request, address);
        Result<?> validation = validateAddress(address);
        if (validation != null) {
            return validation;
        }

        boolean firstAddress = userAddressMapper.selectCount(new QueryWrapper<UserAddress>()
                .eq("user_id", user.getId())
                .eq("status", 1)) == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            clearDefault(user.getId());
        }

        address.setUserId(user.getId());
        address.setIsDefault(makeDefault);
        address.setStatus(1);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        userAddressMapper.insert(address);
        return Result.success("地址创建成功", address);
    }

    @Transactional
    public Result<?> updateAddress(Long id, UserAddress request) {
        User user = getCurrentUser();
        UserAddress existing = findOwnedAddress(id, user.getId());
        if (existing == null) {
            return Result.error(404, "收货地址不存在");
        }

        copyEditableFields(request, existing);
        Result<?> validation = validateAddress(existing);
        if (validation != null) {
            return validation;
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(user.getId());
            existing.setIsDefault(true);
        }
        existing.setUpdateTime(LocalDateTime.now());
        userAddressMapper.updateById(existing);
        return Result.success("地址更新成功", existing);
    }

    @Transactional
    public Result<?> deleteAddress(Long id) {
        User user = getCurrentUser();
        UserAddress existing = findOwnedAddress(id, user.getId());
        if (existing == null) {
            return Result.error(404, "收货地址不存在");
        }
        boolean wasDefault = Boolean.TRUE.equals(existing.getIsDefault());
        userAddressMapper.deleteById(id);
        if (wasDefault) {
            promoteLatestAddressToDefault(user.getId());
        }
        return Result.success("地址删除成功");
    }

    @Transactional
    public Result<?> setDefault(Long id) {
        User user = getCurrentUser();
        UserAddress existing = findOwnedAddress(id, user.getId());
        if (existing == null) {
            return Result.error(404, "收货地址不存在");
        }
        clearDefault(user.getId());
        existing.setIsDefault(true);
        existing.setUpdateTime(LocalDateTime.now());
        userAddressMapper.updateById(existing);
        return Result.success("默认地址已更新", existing);
    }

    private UserAddress findOwnedAddress(Long id, Long userId) {
        if (id == null || id <= 0 || userId == null) {
            return null;
        }
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !userId.equals(address.getUserId()) || address.getStatus() == null || address.getStatus() != 1) {
            return null;
        }
        return address;
    }

    private void copyEditableFields(UserAddress source, UserAddress target) {
        if (source == null || target == null) {
            return;
        }
        target.setReceiverName(trimToNull(source.getReceiverName()));
        target.setReceiverPhone(trimToNull(source.getReceiverPhone()));
        target.setProvince(trimToNull(source.getProvince()));
        target.setCity(trimToNull(source.getCity()));
        target.setDistrict(trimToNull(source.getDistrict()));
        target.setDetail(trimToNull(source.getDetail()));
    }

    private Result<?> validateAddress(UserAddress address) {
        if (address == null
                || !StringUtils.hasText(address.getReceiverName())
                || !StringUtils.hasText(address.getReceiverPhone())
                || !StringUtils.hasText(address.getProvince())
                || !StringUtils.hasText(address.getCity())
                || !StringUtils.hasText(address.getDetail())) {
            return Result.error("请完善收货地址信息");
        }
        if (!address.getReceiverPhone().matches("^1[3-9]\\d{9}$")) {
            return Result.error("联系电话格式不正确");
        }
        if (address.getReceiverName().length() > 50 || address.getReceiverPhone().length() > 20) {
            return Result.error("收货人或联系电话过长");
        }
        if (length(address.getProvince()) > 50 || length(address.getCity()) > 50 || length(address.getDistrict()) > 50 || length(address.getDetail()) > 255) {
            return Result.error("地址内容过长");
        }
        return null;
    }

    private void clearDefault(Long userId) {
        UserAddress update = new UserAddress();
        update.setIsDefault(false);
        update.setUpdateTime(LocalDateTime.now());
        userAddressMapper.update(update, new QueryWrapper<UserAddress>()
                .eq("user_id", userId)
                .eq("status", 1)
                .eq("is_default", true));
    }

    private void promoteLatestAddressToDefault(Long userId) {
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", 1)
                .orderByDesc("update_time")
                .last("LIMIT 1");
        List<UserAddress> addresses = userAddressMapper.selectList(wrapper);
        if (!addresses.isEmpty()) {
            UserAddress latest = addresses.get(0);
            latest.setIsDefault(true);
            latest.setUpdateTime(LocalDateTime.now());
            userAddressMapper.updateById(latest);
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
