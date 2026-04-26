package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.CampaignEvent;
import com.ecommerce.mapper.CampaignEventMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CampaignEventService {
    public static final String TYPE_COUPON = "coupon";
    public static final String TYPE_FULL_REDUCTION = "full_reduction";
    public static final String TYPE_LIMITED_PROMOTION = "limited_promotion";

    public static final String EVENT_ISSUE = "issue";
    public static final String EVENT_USE = "use";
    public static final String EVENT_ORDER = "order";
    public static final String EVENT_VIEW = "view";
    public static final String EVENT_CANCEL = "cancel";

    private final CampaignEventMapper campaignEventMapper;

    public void record(Long userId, Long campaignId, String campaignType, String campaignName,
                       String eventType, Long relatedId, BigDecimal amount, String metadata) {
        if (!StringUtils.hasText(campaignType) || !StringUtils.hasText(eventType)) {
            return;
        }
        try {
            CampaignEvent event = new CampaignEvent();
            event.setUserId(userId);
            event.setCampaignId(campaignId);
            event.setCampaignType(campaignType);
            event.setCampaignName(StringUtils.hasText(campaignName) ? campaignName : campaignType);
            event.setEventType(eventType);
            event.setRelatedId(relatedId);
            event.setAmount(amount == null ? BigDecimal.ZERO : amount);
            event.setMetadata(metadata);
            event.setCreateTime(LocalDateTime.now());
            campaignEventMapper.insert(event);
        } catch (Exception ignored) {
            // 风险/统计日志不影响主交易链路
        }
    }

    public Result<?> stats(String campaignType, String startTime, String endTime) {
        QueryWrapper<CampaignEvent> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(campaignType)) {
            wrapper.eq("campaign_type", campaignType.trim());
        }
        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);
        if (start != null) {
            wrapper.ge("create_time", start);
        }
        if (end != null) {
            wrapper.le("create_time", end);
        }
        wrapper.orderByDesc("id").last("LIMIT 5000");
        List<CampaignEvent> events = campaignEventMapper.selectList(wrapper);
        Map<String, Summary> summaryMap = new LinkedHashMap<>();
        for (CampaignEvent event : events) {
            String key = (event.getCampaignType() == null ? "" : event.getCampaignType())
                    + "#" + (event.getCampaignId() == null ? 0 : event.getCampaignId())
                    + "#" + (event.getCampaignName() == null ? "" : event.getCampaignName());
            Summary summary = summaryMap.computeIfAbsent(key, ignored -> new Summary(event));
            summary.accept(event);
        }
        List<Map<String, Object>> records = summaryMap.values().stream()
                .sorted(Comparator.comparing(Summary::getLastTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Summary::toMap)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", records.size());
        data.put("overview", buildOverview(records));
        return Result.success(data);
    }

    public Result<?> events(int page, int size, String campaignType, String eventType) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        QueryWrapper<CampaignEvent> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(campaignType)) {
            wrapper.eq("campaign_type", campaignType.trim());
        }
        if (StringUtils.hasText(eventType)) {
            wrapper.eq("event_type", eventType.trim());
        }
        wrapper.orderByDesc("id");
        Page<CampaignEvent> result = campaignEventMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        data.put("pages", result.getPages());
        return Result.success(data);
    }

    private Map<String, Object> buildOverview(List<Map<String, Object>> records) {
        long issue = 0;
        long use = 0;
        long order = 0;
        BigDecimal amount = BigDecimal.ZERO;
        for (Map<String, Object> record : records) {
            issue += ((Number) record.get("issueCount")).longValue();
            use += ((Number) record.get("useCount")).longValue();
            order += ((Number) record.get("orderCount")).longValue();
            amount = amount.add((BigDecimal) record.get("amount"));
        }
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("issueCount", issue);
        overview.put("useCount", use);
        overview.put("orderCount", order);
        overview.put("amount", amount);
        overview.put("conversionRate", issue == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(use).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(issue), 2, RoundingMode.HALF_UP));
        return overview;
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            String text = value.trim();
            if (text.length() == 10) {
                return LocalDateTime.parse(text + "T00:00:00");
            }
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class Summary {
        private final Long campaignId;
        private final String campaignType;
        private final String campaignName;
        private long issueCount;
        private long useCount;
        private long orderCount;
        private long viewCount;
        private long cancelCount;
        private BigDecimal amount = BigDecimal.ZERO;
        private LocalDateTime lastTime;

        Summary(CampaignEvent event) {
            this.campaignId = event.getCampaignId();
            this.campaignType = event.getCampaignType();
            this.campaignName = event.getCampaignName();
        }

        void accept(CampaignEvent event) {
            String type = event.getEventType();
            if (EVENT_ISSUE.equals(type)) {
                issueCount++;
            } else if (EVENT_USE.equals(type)) {
                useCount++;
            } else if (EVENT_ORDER.equals(type)) {
                orderCount++;
            } else if (EVENT_VIEW.equals(type)) {
                viewCount++;
            } else if (EVENT_CANCEL.equals(type)) {
                cancelCount++;
            }
            if (event.getAmount() != null) {
                amount = amount.add(event.getAmount());
            }
            if (event.getCreateTime() != null && (lastTime == null || event.getCreateTime().isAfter(lastTime))) {
                lastTime = event.getCreateTime();
            }
        }

        LocalDateTime getLastTime() {
            return lastTime;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("campaignId", campaignId);
            map.put("campaignType", campaignType);
            map.put("campaignName", campaignName);
            map.put("issueCount", issueCount);
            map.put("useCount", useCount);
            map.put("orderCount", orderCount);
            map.put("viewCount", viewCount);
            map.put("cancelCount", cancelCount);
            map.put("amount", amount);
            map.put("conversionRate", issueCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(useCount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(issueCount), 2, RoundingMode.HALF_UP));
            map.put("lastTime", lastTime);
            return map;
        }
    }
}
