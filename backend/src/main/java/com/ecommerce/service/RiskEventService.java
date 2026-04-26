package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.RiskEvent;
import com.ecommerce.mapper.RiskEventMapper;
import com.ecommerce.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskEventService {
    public static final int STATUS_OPEN = 0;
    public static final int STATUS_HANDLED = 1;
    public static final int STATUS_IGNORED = 2;

    private final RiskEventMapper riskEventMapper;

    public void record(Long userId, String username, String ip, String eventType, String riskLevel,
                       String description, String path, String method, String metadata) {
        if (!StringUtils.hasText(eventType)) {
            return;
        }
        try {
            RiskEvent event = new RiskEvent();
            event.setUserId(userId);
            event.setUsername(trim(username, 80));
            event.setIp(trim(ip, 64));
            event.setEventType(trim(eventType, 50));
            event.setRiskLevel(trim(defaultString(riskLevel, "medium"), 20));
            event.setDescription(trim(description, 500));
            event.setPath(trim(path, 255));
            event.setMethod(trim(method, 20));
            event.setMetadata(trim(metadata, 1000));
            event.setStatus(STATUS_OPEN);
            event.setCreateTime(LocalDateTime.now());
            event.setUpdateTime(LocalDateTime.now());
            riskEventMapper.insert(event);
        } catch (Exception ignored) {
            // 风控记录不能影响正常业务
        }
    }

    public void recordRequest(HttpServletRequest request, String eventType, String riskLevel, String description, String metadata) {
        record(null, null, clientIp(request), eventType, riskLevel, description,
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getMethod(),
                metadata);
    }

    public long countRecent(Long userId, String eventType, int minutes) {
        if (userId == null || !StringUtils.hasText(eventType)) {
            return 0L;
        }
        try {
            return riskEventMapper.selectCount(new QueryWrapper<RiskEvent>()
                    .eq("user_id", userId)
                    .eq("event_type", eventType)
                    .ge("create_time", LocalDateTime.now().minusMinutes(Math.max(minutes, 1))));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public Result<?> page(int page, int size, String eventType, String riskLevel, Integer status, String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        QueryWrapper<RiskEvent> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(eventType)) {
            wrapper.eq("event_type", eventType.trim());
        }
        if (StringUtils.hasText(riskLevel)) {
            wrapper.eq("risk_level", riskLevel.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("username", kw)
                    .or().like("ip", kw)
                    .or().like("description", kw)
                    .or().like("path", kw));
        }
        wrapper.orderByAsc("status").orderByDesc("id");
        Page<RiskEvent> result = riskEventMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        data.put("pages", result.getPages());
        data.put("overview", overview());
        return Result.success(data);
    }

    public Result<?> updateStatus(Long id, Integer status) {
        RiskEvent event = riskEventMapper.selectById(id);
        if (event == null) {
            return Result.error("风控事件不存在");
        }
        event.setStatus(status == null ? STATUS_HANDLED : Math.min(Math.max(status, STATUS_OPEN), STATUS_IGNORED));
        event.setUpdateTime(LocalDateTime.now());
        riskEventMapper.updateById(event);
        return Result.success("风控事件状态已更新", event);
    }

    private Map<String, Object> overview() {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("open", riskEventMapper.selectCount(new QueryWrapper<RiskEvent>().eq("status", STATUS_OPEN)));
            map.put("high", riskEventMapper.selectCount(new QueryWrapper<RiskEvent>().eq("risk_level", "high").eq("status", STATUS_OPEN)));
            map.put("today", riskEventMapper.selectCount(new QueryWrapper<RiskEvent>().ge("create_time", LocalDateTime.now().toLocalDate().atStartOfDay())));
            List<RiskEvent> latest = riskEventMapper.selectList(new QueryWrapper<RiskEvent>().orderByDesc("id").last("LIMIT 5"));
            map.put("latest", latest);
        } catch (Exception ignored) {
            map.put("open", 0);
            map.put("high", 0);
            map.put("today", 0);
            map.put("latest", List.of());
        }
        return map;
    }

    public String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }

    private String trim(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        return text.length() > max ? text.substring(0, max) : text;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
