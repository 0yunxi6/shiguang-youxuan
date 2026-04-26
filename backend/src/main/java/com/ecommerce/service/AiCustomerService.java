package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.dto.AiChatRequest;
import com.ecommerce.entity.AfterSale;
import com.ecommerce.entity.AiKnowledgeBase;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.mapper.AfterSaleMapper;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserCouponMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCustomerService {
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("\\bORD[0-9A-Z]{8,}\\b", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String SYSTEM_PROMPT = """
            你是“拾光”电商平台的 AI 客服小光。
            请用简洁、温和、可信的中文回答用户问题。
            你可以解答商品推荐、优惠券、下单支付、物流时效、售后退换货、发票、会员积分、账号安全等问题。
            如果上下文中提供了订单、优惠券、售后或商品数据，必须优先依据这些真实数据回答。
            不要编造不存在的订单号、物流单号、金额、退款到账时间或平台承诺；没有真实数据时要明确说明，并引导用户去订单页或售后页操作。
            当前系统的物流为项目内模拟物流，真实快递公司 API 暂未接入。
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserCouponMapper userCouponMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final ProductMapper productMapper;
    private final AiKnowledgeBaseService aiKnowledgeBaseService;

    @Value("${ai.glm.api-key:}")
    private String apiKey;

    @Value("${ai.glm.endpoint:https://api.z.ai/api/paas/v4/chat/completions}")
    private String endpoint;

    @Value("${ai.glm.model:glm-5.1}")
    private String model;

    public Result<?> context() {
        return Result.success(buildContext().toMap());
    }

    public Result<?> chat(AiChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (!StringUtils.hasText(message)) {
            return Result.error("消息不能为空");
        }

        AiContext context = buildContext();
        List<AiKnowledgeBase> matchedKnowledge = aiKnowledgeBaseService.searchForQuestion(message, 5);
        String deterministicReply = buildDeterministicReply(message, context);
        if (StringUtils.hasText(deterministicReply)) {
            return Result.success(buildResponse(deterministicReply, false, "context-assistant", context, matchedKnowledge));
        }
        String knowledgeReply = aiKnowledgeBaseService.directAnswerIfStrongMatch(message, matchedKnowledge);
        if (StringUtils.hasText(knowledgeReply)) {
            return Result.success(buildResponse(knowledgeReply, false, "knowledge-base", context, matchedKnowledge));
        }

        if (!StringUtils.hasText(apiKey)) {
            return Result.success(buildResponse(fallbackReply(message, context, matchedKnowledge), true, "local-fallback", context, matchedKnowledge));
        }

        try {
            Map<String, Object> payload = buildPayload(request, message, context, matchedKnowledge);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Result.success(buildResponse(fallbackReply(message, context, matchedKnowledge), true, "local-fallback", context, matchedKnowledge));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String reply = root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(reply)) {
                reply = fallbackReply(message, context, matchedKnowledge);
            }
            return Result.success(buildResponse(reply.trim(), false, model, context, matchedKnowledge));
        } catch (Exception ignored) {
            return Result.success(buildResponse(fallbackReply(message, context, matchedKnowledge), true, "local-fallback", context, matchedKnowledge));
        }
    }

    private AiContext buildContext() {
        User user = getCurrentUser();
        if (user == null || user.getId() == null) {
            return new AiContext(null, List.of(), List.of(), List.of(), List.of());
        }

        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("user_id", user.getId())
                .orderByDesc("create_time")
                .last("LIMIT 10"));
        List<Long> orderIds = orders.stream().map(Order::getId).filter(Objects::nonNull).toList();
        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            itemsByOrder = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                            .in("order_id", orderIds)
                            .orderByAsc("id"))
                    .stream()
                    .collect(Collectors.groupingBy(OrderItem::getOrderId));
        }
        Map<Long, List<OrderItem>> finalItemsByOrder = itemsByOrder;
        List<OrderInfo> orderInfos = orders.stream()
                .map(order -> new OrderInfo(order, finalItemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();

        List<UserCoupon> coupons = userCouponMapper.selectList(new QueryWrapper<UserCoupon>()
                .eq("user_id", user.getId())
                .orderByAsc("status")
                .orderByAsc("end_time")
                .last("LIMIT 20"));

        List<AfterSale> afterSales = afterSaleMapper.selectList(new QueryWrapper<AfterSale>()
                .eq("user_id", user.getId())
                .orderByDesc("create_time")
                .last("LIMIT 10"));

        List<Product> products = productMapper.selectList(new QueryWrapper<Product>()
                .eq("status", 1)
                .eq("deleted", 0)
                .orderByDesc("update_time")
                .last("LIMIT 8"));

        return new AiContext(user, orderInfos, coupons, afterSales, products);
    }

    private String buildDeterministicReply(String message, AiContext context) {
        String lower = message.toLowerCase(Locale.ROOT);
        OrderInfo targetOrder = findTargetOrder(message, context);

        if (containsAny(lower, "物流", "快递", "发货", "运单", "到哪", "多久到", "tracking")) {
            OrderInfo order = targetOrder != null ? targetOrder : latestOrder(context);
            return order == null ? "当前账号暂时没有可查询的订单。你可以先下单，支付后订单页会展示物流进度。" : buildLogisticsReply(order);
        }

        if (containsAny(lower, "订单", "order", "ord") || extractOrderNo(message) != null) {
            OrderInfo order = targetOrder != null ? targetOrder : latestOrder(context);
            if (order == null) {
                return "当前账号暂时没有订单记录。你可以先去首页挑选商品，下单后我就能在这里帮你查看订单状态。";
            }
            return buildOrderReply(order);
        }

        if (containsAny(lower, "优惠", "优惠券", "券", "coupon", "满减")) {
            return buildCouponReply(context);
        }

        if (containsAny(lower, "售后", "退款", "退货", "换货", "退换")) {
            return buildAfterSaleReply(context, targetOrder);
        }

        if (containsAny(lower, "发票", "税号", "抬头")) {
            return "发票信息可以在「个人中心 - 发票信息」维护；结算页选择发票抬头后，订单会记录发票抬头和税号。若订单已经提交，需要修改发票信息，建议联系平台客服人工处理。";
        }

        if (containsAny(lower, "积分", "会员", "等级", "成长")) {
            User user = context.user();
            int points = user == null || user.getPoints() == null ? 0 : user.getPoints();
            String level = user == null || !StringUtils.hasText(user.getLevel()) ? "普通会员" : user.getLevel();
            return "你当前是「" + level + "」，可用积分为 " + points + "。确认收货后会按实付金额获得积分，积分也可以在个人中心兑换优惠券。";
        }

        if (containsAny(lower, "推荐", "商品", "有货", "买什么", "热卖", "好物")) {
            return buildProductReply(context);
        }

        return null;
    }

    private String buildOrderReply(OrderInfo info) {
        Order order = info.order();
        StringBuilder sb = new StringBuilder();
        sb.append("查到了，订单 ").append(order.getOrderNo()).append(" 的当前状态是「")
                .append(statusText(order.getStatus())).append("」。\n");
        sb.append("- 下单时间：").append(formatTime(order.getCreateTime())).append("\n");
        sb.append("- 商品明细：").append(itemSummary(info.items())).append("\n");
        sb.append("- 金额明细：商品 ¥").append(amount(order.getOriginalAmount()))
                .append("，优惠 -¥").append(amount(order.getDiscountAmount()))
                .append("，运费 ").append(order.getShippingFee() == null || order.getShippingFee().compareTo(BigDecimal.ZERO) == 0 ? "免运费" : "¥" + amount(order.getShippingFee()))
                .append("，实付 ¥").append(amount(order.getTotalAmount())).append("\n");
        if (StringUtils.hasText(order.getCouponName())) {
            sb.append("- 已用优惠：").append(order.getCouponName()).append("\n");
        }
        if (StringUtils.hasText(order.getPaymentMethod())) {
            sb.append("- 支付方式：").append(paymentText(order.getPaymentMethod())).append("\n");
        }
        sb.append("- 收货信息：").append(order.getReceiverName()).append(" · ")
                .append(maskPhone(order.getReceiverPhone())).append(" · ")
                .append(order.getReceiverAddress()).append("\n");
        sb.append(buildLogisticsLine(order));
        return sb.toString();
    }

    private String buildLogisticsReply(OrderInfo info) {
        Order order = info.order();
        return "订单 " + order.getOrderNo() + " 的物流状态：\n"
                + buildLogisticsLine(order)
                + "\n商品：" + itemSummary(info.items())
                + "\n说明：当前项目使用模拟物流状态，真实快递公司 API 后续接入后会展示运单轨迹。";
    }

    private String buildLogisticsLine(Order order) {
        Integer status = order.getStatus();
        if (status == null) {
            return "- 物流：订单状态异常，请联系平台客服核对。";
        }
        return switch (status) {
            case 0 -> "- 物流：订单待付款，暂未进入仓库履约；完成支付后会安排发货。";
            case 1 -> "- 物流：订单已支付，仓库正在拣货打包，预计 24 小时内发货。";
            case 2 -> "- 物流：订单已发货，模拟运单号 SG" + order.getId() + "，发货时间 " + formatTime(order.getShippedTime()) + "，预计 2-3 天送达。";
            case 3 -> "- 物流：订单已完成，签收/完成时间 " + formatTime(order.getCompletedTime()) + "。";
            case 4 -> "- 物流：订单已取消，不会继续发货。";
            default -> "- 物流：当前状态暂未识别，请以订单页展示为准。";
        };
    }

    private String buildCouponReply(AiContext context) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> unused = context.coupons().stream()
                .filter(coupon -> coupon.getStatus() != null && coupon.getStatus() == 0)
                .filter(coupon -> coupon.getEndTime() == null || !coupon.getEndTime().isBefore(now))
                .toList();
        if (unused.isEmpty()) {
            return "当前账号暂无可用优惠券。你可以在个人中心使用积分兑换优惠券，或关注首页活动和新人礼包。";
        }
        UserCoupon best = unused.stream()
                .max(Comparator.comparing(coupon -> Optional.ofNullable(coupon.getDiscountAmount()).orElse(BigDecimal.ZERO)))
                .orElse(unused.get(0));
        return "你当前有 " + unused.size() + " 张可用优惠券。推荐优先使用「" + best.getName() + "」：满 "
                + amount(best.getMinAmount()) + " 减 " + amount(best.getDiscountAmount())
                + "，有效期至 " + formatDate(best.getEndTime())
                + "。结算页会自动选择当前最省钱的一张，你也可以手动切换或选择不使用。";
    }

    private String buildAfterSaleReply(AiContext context, OrderInfo targetOrder) {
        if (targetOrder != null) {
            String orderNo = targetOrder.order().getOrderNo();
            Optional<AfterSale> exists = context.afterSales().stream()
                    .filter(item -> orderNo.equalsIgnoreCase(item.getOrderNo()))
                    .findFirst();
            if (exists.isPresent()) {
                AfterSale afterSale = exists.get();
                return "订单 " + orderNo + " 已有售后记录：类型「" + afterSaleTypeText(afterSale.getType())
                        + "」，状态「" + afterSaleStatusText(afterSale.getStatus()) + "」。"
                        + (StringUtils.hasText(afterSale.getAuditRemark()) ? "处理备注：" + afterSale.getAuditRemark() : "请在个人中心 - 售后进度查看最新处理结果。");
            }
            Integer status = targetOrder.order().getStatus();
            if (status != null && status >= 1 && status <= 3) {
                return "订单 " + orderNo + " 当前支持发起售后。请在「我的订单」点击“申请售后”，选择退款/退货/换货并填写原因；提交后可在「个人中心 - 售后进度」查看审核结果。";
            }
            return "订单 " + orderNo + " 当前状态为「" + statusText(status) + "」，暂不支持发起售后。";
        }
        if (context.afterSales().isEmpty()) {
            return "当前账号暂无售后记录。支付后的订单如遇到未发货、商品问题或需要退换货，可在「我的订单」里申请售后。";
        }
        AfterSale latest = context.afterSales().get(0);
        return "最近一条售后：订单 " + latest.getOrderNo() + "，类型「" + afterSaleTypeText(latest.getType())
                + "」，状态「" + afterSaleStatusText(latest.getStatus()) + "」。你可以在个人中心 - 售后进度查看全部记录。";
    }

    private String buildProductReply(AiContext context) {
        if (context.products().isEmpty()) {
            return "当前暂无可推荐商品。你可以先浏览首页分类，后续我会根据浏览、收藏和订单记录为你推荐更合适的好物。";
        }
        String products = context.products().stream()
                .limit(5)
                .map(product -> "「" + product.getName() + "」¥" + amount(effectivePrice(product)) + "（库存 " + (product.getStock() == null ? 0 : product.getStock()) + "）")
                .collect(Collectors.joining("、"));
        return "为你推荐这些拾光好物：" + products + "。你可以复制商品名到搜索框，或告诉我预算/品牌/用途，我再帮你缩小范围。";
    }

    private Map<String, Object> buildPayload(AiChatRequest request, String message, AiContext context, List<AiKnowledgeBase> matchedKnowledge) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "system", "content", "当前登录用户上下文：\n" + context.toPromptText()));
        String knowledgePrompt = aiKnowledgeBaseService.buildPromptText(matchedKnowledge);
        if (StringUtils.hasText(knowledgePrompt)) {
            messages.add(Map.of("role", "system", "content", knowledgePrompt));
        }
        if (request.getHistory() != null) {
            request.getHistory().stream()
                    .filter(item -> item != null && StringUtils.hasText(item.getContent()))
                    .filter(item -> "user".equals(item.getRole()) || "assistant".equals(item.getRole()))
                    .skip(Math.max(0, request.getHistory().size() - 8L))
                    .forEach(item -> messages.add(Map.of(
                            "role", item.getRole(),
                            "content", trimContent(item.getContent(), 1000)
                    )));
        }
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.55);
        payload.put("max_tokens", 1024);
        payload.put("stream", false);
        return payload;
    }

    private String fallbackReply(String message, AiContext context, List<AiKnowledgeBase> matchedKnowledge) {
        String direct = buildDeterministicReply(message, context);
        if (StringUtils.hasText(direct)) {
            return direct;
        }
        String knowledgeReply = aiKnowledgeBaseService.directAnswerIfStrongMatch(message, matchedKnowledge);
        if (StringUtils.hasText(knowledgeReply)) {
            return knowledgeReply;
        }
        if (matchedKnowledge != null && !matchedKnowledge.isEmpty()) {
            AiKnowledgeBase item = matchedKnowledge.get(0);
            return "我在知识库里找到了相关规则「" + item.getTitle() + "」：\n" + item.getAnswer()
                    + "\n如果仍未解决，可以点击右侧“人工协助”创建工单。";
        }
        return "我是拾光 AI 客服小光。你可以问我商品推荐、优惠券、下单支付、发票、物流和售后问题；如果涉及具体订单，请直接发送订单号，我会基于当前账号的订单数据帮你查看。";
    }

    private OrderInfo findTargetOrder(String message, AiContext context) {
        String orderNo = extractOrderNo(message);
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        for (OrderInfo info : context.orders()) {
            if (orderNo.equalsIgnoreCase(info.order().getOrderNo())) {
                return info;
            }
        }
        if (context.user() == null) {
            return null;
        }
        Order order = orderMapper.selectOne(new QueryWrapper<Order>()
                .eq("user_id", context.user().getId())
                .eq("order_no", orderNo.toUpperCase(Locale.ROOT))
                .last("LIMIT 1"));
        if (order == null) {
            return null;
        }
        List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                .eq("order_id", order.getId())
                .orderByAsc("id"));
        return new OrderInfo(order, items);
    }

    private String extractOrderNo(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = ORDER_NO_PATTERN.matcher(message.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group() : null;
    }

    private OrderInfo latestOrder(AiContext context) {
        return context.orders().isEmpty() ? null : context.orders().get(0);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> buildResponse(String reply, boolean fallback, String modelName, AiContext context, List<AiKnowledgeBase> matchedKnowledge) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", reply);
        data.put("fallback", fallback);
        data.put("model", modelName);
        data.put("context", context.toMap());
        if (matchedKnowledge != null && !matchedKnowledge.isEmpty()) {
            data.put("knowledge", matchedKnowledge.stream().map(item -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", item.getId());
                row.put("category", item.getCategory());
                row.put("title", item.getTitle());
                row.put("keywords", item.getKeywords());
                return row;
            }).toList());
        }
        return data;
    }

    private String trimContent(String content, int max) {
        String text = content == null ? "" : content.trim();
        return text.length() > max ? text.substring(0, max) : text;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName()) || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userMapper.findByUsername(authentication.getName());
    }

    private String itemSummary(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "暂无商品明细";
        }
        return items.stream()
                .map(item -> item.getProductName() + (StringUtils.hasText(item.getProductSpec()) ? "（" + item.getProductSpec() + "）" : "") + " × " + item.getQuantity())
                .collect(Collectors.joining("；"));
    }

    private String statusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String afterSaleStatusText(Integer status) {
        if (status == null) return "待审核";
        return switch (status) {
            case 1 -> "已同意";
            case 2 -> "已拒绝";
            case 3 -> "已完成";
            default -> "待审核";
        };
    }

    private String afterSaleTypeText(String type) {
        if ("return".equals(type)) return "退货";
        if ("exchange".equals(type)) return "换货";
        return "退款";
    }

    private String paymentText(String value) {
        if (!StringUtils.hasText(value)) return "未记录";
        return switch (value) {
            case "wechat" -> "微信支付";
            case "alipay" -> "支付宝";
            case "card" -> "银行卡";
            case "cod" -> "货到付款";
            default -> value;
        };
    }

    private BigDecimal effectivePrice(Product product) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        LocalDateTime now = LocalDateTime.now();
        if (product.getPromotionPrice() != null
                && (product.getPromotionStartTime() == null || !product.getPromotionStartTime().isAfter(now))
                && (product.getPromotionEndTime() == null || !product.getPromotionEndTime().isBefore(now))) {
            return product.getPromotionPrice();
        }
        return product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
    }

    private String amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "暂未记录" : value.format(TIME_FORMATTER);
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "长期有效" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return StringUtils.hasText(phone) ? phone : "未设置";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private record OrderInfo(Order order, List<OrderItem> items) {}

    private record AiContext(User user, List<OrderInfo> orders, List<UserCoupon> coupons, List<AfterSale> afterSales, List<Product> products) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderCount", orders == null ? 0 : orders.size());
            map.put("couponCount", coupons == null ? 0 : coupons.size());
            map.put("afterSaleCount", afterSales == null ? 0 : afterSales.size());
            map.put("productCount", products == null ? 0 : products.size());
            if (user != null) {
                Map<String, Object> userMap = new LinkedHashMap<>();
                userMap.put("username", user.getUsername());
                userMap.put("nickname", user.getNickname());
                userMap.put("level", user.getLevel());
                userMap.put("points", user.getPoints());
                map.put("user", userMap);
            }
            if (orders != null && !orders.isEmpty()) {
                map.put("latestOrderNo", orders.get(0).order().getOrderNo());
                map.put("latestOrderStatus", orders.get(0).order().getStatus());
            }
            return map;
        }

        String toPromptText() {
            StringBuilder sb = new StringBuilder();
            if (user == null) {
                return "未读取到登录用户上下文。";
            }
            sb.append("用户：").append(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                    .append("，会员：").append(StringUtils.hasText(user.getLevel()) ? user.getLevel() : "普通会员")
                    .append("，积分：").append(user.getPoints() == null ? 0 : user.getPoints()).append("\n");
            sb.append("最近订单：\n");
            if (orders == null || orders.isEmpty()) {
                sb.append("- 暂无订单\n");
            } else {
                for (OrderInfo info : orders.stream().limit(5).toList()) {
                    Order order = info.order();
                    sb.append("- ").append(order.getOrderNo())
                            .append("，状态码 ").append(order.getStatus())
                            .append("，实付 ").append(order.getTotalAmount())
                            .append("，商品 ").append(info.items().stream().map(OrderItem::getProductName).collect(Collectors.joining("/")))
                            .append("\n");
                }
            }
            sb.append("优惠券数量：").append(coupons == null ? 0 : coupons.size()).append("；售后记录数量：").append(afterSales == null ? 0 : afterSales.size()).append("\n");
            if (products != null && !products.isEmpty()) {
                sb.append("可推荐商品：").append(products.stream().limit(5).map(Product::getName).collect(Collectors.joining("、"))).append("\n");
            }
            return sb.toString();
        }
    }
}
