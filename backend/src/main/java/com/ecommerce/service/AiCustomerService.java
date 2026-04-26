package com.ecommerce.service;

import com.ecommerce.dto.AiChatRequest;
import com.ecommerce.util.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiCustomerService {
    private static final String SYSTEM_PROMPT = """
            你是“拾光”电商平台的 AI 客服小光。
            你要用简洁、温和、可信的中文回答用户问题。
            你可以解答：商品推荐、优惠券、下单支付、物流时效、售后退换货、发票、会员积分、账号安全。
            如涉及真实支付、物流单号、退款到账等实时数据，提醒用户以订单页/售后页显示为准。
            不要编造不存在的订单号、金额或承诺；需要人工处理时，引导用户提交售后或联系平台客服。
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${ai.glm.api-key:}")
    private String apiKey;

    @Value("${ai.glm.endpoint:https://api.z.ai/api/paas/v4/chat/completions}")
    private String endpoint;

    @Value("${ai.glm.model:glm-5.1}")
    private String model;

    public Result<?> chat(AiChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (!StringUtils.hasText(message)) {
            return Result.error("消息不能为空");
        }
        if (!StringUtils.hasText(apiKey)) {
            return Result.success(buildResponse(fallbackReply(message), true));
        }
        try {
            Map<String, Object> payload = buildPayload(request, message);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Result.success(buildResponse(fallbackReply(message), true));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String reply = root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(reply)) {
                reply = fallbackReply(message);
            }
            return Result.success(buildResponse(reply.trim(), false));
        } catch (Exception ignored) {
            return Result.success(buildResponse(fallbackReply(message), true));
        }
    }

    private Map<String, Object> buildPayload(AiChatRequest request, String message) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
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
        payload.put("temperature", 0.65);
        payload.put("max_tokens", 1024);
        payload.put("stream", false);
        payload.put("thinking", Map.of("type", "enabled"));
        return payload;
    }

    private String trimContent(String content, int max) {
        String text = content == null ? "" : content.trim();
        return text.length() > max ? text.substring(0, max) : text;
    }

    private Map<String, Object> buildResponse(String reply, boolean fallback) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", reply);
        data.put("fallback", fallback);
        data.put("model", fallback ? "local-fallback" : model);
        return data;
    }

    private String fallbackReply(String message) {
        String text = message.toLowerCase(Locale.ROOT);
        if (text.contains("优惠") || text.contains("券")) {
            return "可以在「个人中心-我的优惠券」查看优惠券；结算页会自动选择当前最省钱的一张，你也可以手动切换或选择不使用。";
        }
        if (text.contains("退") || text.contains("售后") || text.contains("换货")) {
            return "订单支付后可在「我的订单」点击“申请售后”，填写原因后提交；平台会在后台审核，进度会同步到「个人中心-售后进度」和消息中心。";
        }
        if (text.contains("物流") || text.contains("发货") || text.contains("多久")) {
            return "模拟订单支付后会进入待发货状态，后台发货后可确认收货；真实物流 API 暂未接入，具体状态以订单页展示为准。";
        }
        if (text.contains("发票") || text.contains("税号")) {
            return "你可以在「个人中心-发票信息」维护发票抬头和税号，结算时选择后会写入订单。";
        }
        if (text.contains("积分") || text.contains("会员")) {
            return "确认收货后会按实付金额获得积分，积分会提升会员等级，也可以在个人中心兑换优惠券。";
        }
        return "我是拾光 AI 客服小光。你可以问我商品推荐、优惠券、下单支付、发票、物流和售后问题；如果涉及具体订单，请前往订单页查看最新状态。";
    }
}
