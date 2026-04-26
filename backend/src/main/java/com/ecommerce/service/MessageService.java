package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserMessage;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.mapper.UserMessageMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final UserMessageMapper messageMapper;
    private final UserMapper userMapper;

    public void create(Long userId, String type, String title, String content) {
        if (userId == null || !StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return;
        }
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setType(StringUtils.hasText(type) ? type.trim() : "system");
        message.setTitle(title.trim());
        message.setContent(content.trim());
        message.setReadStatus(0);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    public Result<?> list(Integer readStatus, String type) {
        User user = getCurrentUser();
        QueryWrapper<UserMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());
        if (readStatus != null && (readStatus == 0 || readStatus == 1)) {
            wrapper.eq("read_status", readStatus);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq("type", type.trim());
        }
        wrapper.orderByDesc("create_time");
        return Result.success(messageMapper.selectList(wrapper));
    }

    public Result<?> unreadCount() {
        User user = getCurrentUser();
        long count = messageMapper.selectCount(new QueryWrapper<UserMessage>()
                .eq("user_id", user.getId())
                .eq("read_status", 0));
        return Result.success(count);
    }

    public Result<?> markRead(Long id) {
        User user = getCurrentUser();
        UserMessage message = messageMapper.selectById(id);
        if (message == null || !user.getId().equals(message.getUserId())) {
            return Result.error("消息不存在");
        }
        message.setReadStatus(1);
        message.setReadTime(LocalDateTime.now());
        messageMapper.updateById(message);
        return Result.success("消息已读");
    }

    public Result<?> markAllRead() {
        User user = getCurrentUser();
        List<UserMessage> messages = messageMapper.selectList(new QueryWrapper<UserMessage>()
                .eq("user_id", user.getId())
                .eq("read_status", 0));
        for (UserMessage message : messages) {
            message.setReadStatus(1);
            message.setReadTime(LocalDateTime.now());
            messageMapper.updateById(message);
        }
        return Result.success("全部消息已读");
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }
}
