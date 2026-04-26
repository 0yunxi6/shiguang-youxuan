package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.CustomerTicket;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.CustomerTicketMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerTicketService {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_RESOLVED = 2;
    public static final int STATUS_CLOSED = 3;

    private final CustomerTicketMapper customerTicketMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    public Result<?> createTicket(Map<String, Object> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        String content = stringValue(payload, "content");
        String title = stringValue(payload, "title");
        if (!StringUtils.hasText(content)) {
            return Result.error("请填写需要人工客服协助的问题");
        }
        if (!StringUtils.hasText(title)) {
            title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
        }

        CustomerTicket ticket = new CustomerTicket();
        ticket.setUserId(user.getId());
        ticket.setTicketNo(generateTicketNo());
        ticket.setSource(defaultString(stringValue(payload, "source"), "user"));
        ticket.setType(defaultString(stringValue(payload, "type"), "general"));
        ticket.setTitle(title.trim());
        ticket.setContent(content.trim());
        ticket.setOrderNo(trimToNull(stringValue(payload, "orderNo")));
        ticket.setPriority(parseInt(payload == null ? null : payload.get("priority"), 1));
        ticket.setStatus(STATUS_PENDING);
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        customerTicketMapper.insert(ticket);
        messageService.create(user.getId(), "ticket", "人工客服工单已创建",
                "你的工单 " + ticket.getTicketNo() + " 已提交，客服会尽快处理。");
        return Result.success("工单已创建", ticket);
    }

    public Result<?> myTickets() {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        List<CustomerTicket> tickets = customerTicketMapper.selectList(new QueryWrapper<CustomerTicket>()
                .eq("user_id", user.getId())
                .orderByDesc("id"));
        return Result.success(tickets);
    }

    public Result<?> satisfaction(Long id, Map<String, Object> payload) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        CustomerTicket ticket = customerTicketMapper.selectById(id);
        if (ticket == null || !user.getId().equals(ticket.getUserId())) {
            return Result.error("工单不存在");
        }
        int score = parseInt(payload == null ? null : payload.get("satisfaction"), 5);
        score = Math.min(Math.max(score, 1), 5);
        ticket.setSatisfaction(score);
        ticket.setSatisfactionRemark(trimToNull(stringValue(payload, "remark")));
        ticket.setStatus(STATUS_CLOSED);
        ticket.setClosedTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        customerTicketMapper.updateById(ticket);
        return Result.success("感谢你的反馈", ticket);
    }

    public Result<?> adminPage(int page, int size, Integer status, String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        QueryWrapper<CustomerTicket> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("ticket_no", kw)
                    .or().like("title", kw)
                    .or().like("content", kw)
                    .or().like("order_no", kw));
        }
        wrapper.orderByAsc("status").orderByDesc("id");
        Page<CustomerTicket> result = customerTicketMapper.selectPage(new Page<>(page, size), wrapper);
        List<CustomerTicket> tickets = result.getRecords();
        Map<Long, User> userMap = loadUsers(tickets.stream().map(CustomerTicket::getUserId).filter(Objects::nonNull).toList());
        Map<Long, User> assigneeMap = loadUsers(tickets.stream().map(CustomerTicket::getAssigneeId).filter(Objects::nonNull).toList());
        List<Map<String, Object>> rows = tickets.stream()
                .map(ticket -> row(ticket, userMap.get(ticket.getUserId()), assigneeMap.get(ticket.getAssigneeId())))
                .toList();
        return Result.success(buildPageData(result, rows));
    }

    public Result<?> assign(Long id, Long assigneeId) {
        CustomerTicket ticket = customerTicketMapper.selectById(id);
        if (ticket == null) {
            return Result.error("工单不存在");
        }
        User assignee = assigneeId == null ? getCurrentUser() : userMapper.selectById(assigneeId);
        if (assignee == null) {
            return Result.error("客服账号不存在");
        }
        ticket.setAssigneeId(assignee.getId());
        if (ticket.getStatus() == null || ticket.getStatus() == STATUS_PENDING) {
            ticket.setStatus(STATUS_PROCESSING);
        }
        ticket.setUpdateTime(LocalDateTime.now());
        customerTicketMapper.updateById(ticket);
        return Result.success("工单已分配", ticket);
    }

    public Result<?> updateStatus(Long id, Integer status, String resolution) {
        CustomerTicket ticket = customerTicketMapper.selectById(id);
        if (ticket == null) {
            return Result.error("工单不存在");
        }
        int safeStatus = status == null ? STATUS_PROCESSING : Math.min(Math.max(status, STATUS_PENDING), STATUS_CLOSED);
        ticket.setStatus(safeStatus);
        if (StringUtils.hasText(resolution)) {
            ticket.setResolution(resolution.trim());
        }
        if (safeStatus >= STATUS_RESOLVED) {
            ticket.setClosedTime(LocalDateTime.now());
        }
        ticket.setUpdateTime(LocalDateTime.now());
        customerTicketMapper.updateById(ticket);
        notifyTicketUser(ticket, safeStatus);
        return Result.success("工单状态已更新", ticket);
    }

    public Result<?> reply(Long id, Map<String, Object> payload) {
        String resolution = stringValue(payload, "resolution");
        if (!StringUtils.hasText(resolution)) {
            resolution = stringValue(payload, "content");
        }
        if (!StringUtils.hasText(resolution)) {
            return Result.error("请填写处理结果");
        }
        Integer status = parseInt(payload == null ? null : payload.get("status"), STATUS_RESOLVED);
        return updateStatus(id, status, resolution);
    }

    private void notifyTicketUser(CustomerTicket ticket, int status) {
        if (ticket == null || ticket.getUserId() == null) {
            return;
        }
        if (status == STATUS_RESOLVED) {
            messageService.create(ticket.getUserId(), "ticket", "工单已处理",
                    "你的工单 " + ticket.getTicketNo() + " 已处理：" + defaultString(ticket.getResolution(), "请查看处理结果。"));
        } else if (status == STATUS_CLOSED) {
            messageService.create(ticket.getUserId(), "ticket", "工单已关闭",
                    "你的工单 " + ticket.getTicketNo() + " 已关闭，感谢反馈。");
        }
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Object> row(CustomerTicket ticket, User user, User assignee) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", ticket.getId());
        row.put("ticketNo", ticket.getTicketNo());
        row.put("userId", ticket.getUserId());
        row.put("username", user == null ? null : user.getUsername());
        row.put("nickname", user == null ? null : user.getNickname());
        row.put("assigneeId", ticket.getAssigneeId());
        row.put("assigneeName", assignee == null ? null : assignee.getUsername());
        row.put("source", ticket.getSource());
        row.put("type", ticket.getType());
        row.put("title", ticket.getTitle());
        row.put("content", ticket.getContent());
        row.put("orderNo", ticket.getOrderNo());
        row.put("priority", ticket.getPriority());
        row.put("status", ticket.getStatus());
        row.put("statusText", statusText(ticket.getStatus()));
        row.put("resolution", ticket.getResolution());
        row.put("satisfaction", ticket.getSatisfaction());
        row.put("satisfactionRemark", ticket.getSatisfactionRemark());
        row.put("createTime", ticket.getCreateTime());
        row.put("updateTime", ticket.getUpdateTime());
        row.put("closedTime", ticket.getClosedTime());
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

    private String statusText(Integer status) {
        if (status == null) {
            return "待处理";
        }
        return switch (status) {
            case STATUS_PROCESSING -> "处理中";
            case STATUS_RESOLVED -> "已解决";
            case STATUS_CLOSED -> "已关闭";
            default -> "待处理";
        };
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName()) || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userMapper.findByUsername(authentication.getName());
    }

    private String generateTicketNo() {
        return "TK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String stringValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return "";
        }
        return String.valueOf(map.get(key));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
