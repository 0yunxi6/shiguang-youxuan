package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.AiKnowledgeBase;
import com.ecommerce.mapper.AiKnowledgeBaseMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiKnowledgeBaseService {
    private static final int STATUS_ENABLED = 1;

    private final AiKnowledgeBaseMapper aiKnowledgeBaseMapper;

    public Result<?> page(int page, int size, String category, String keyword, Integer status) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<AiKnowledgeBase> pageParam = new Page<>(page, size);
        QueryWrapper<AiKnowledgeBase> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("title", kw)
                    .or().like("question", kw)
                    .or().like("answer", kw)
                    .or().like("keywords", kw));
        }
        wrapper.orderByAsc("sort_order").orderByDesc("id");
        Page<AiKnowledgeBase> result = aiKnowledgeBaseMapper.selectPage(pageParam, wrapper);
        return Result.success(buildPageData(result, result.getRecords()));
    }

    public Result<?> publicList(String keyword) {
        QueryWrapper<AiKnowledgeBase> wrapper = new QueryWrapper<>();
        wrapper.eq("status", STATUS_ENABLED);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("title", kw)
                    .or().like("question", kw)
                    .or().like("keywords", kw));
        }
        wrapper.orderByAsc("sort_order").orderByDesc("id").last("LIMIT 20");
        return Result.success(aiKnowledgeBaseMapper.selectList(wrapper));
    }

    public Result<?> create(AiKnowledgeBase knowledge) {
        Result<?> validation = validate(knowledge);
        if (validation != null) {
            return validation;
        }
        knowledge.setStatus(knowledge.getStatus() == null ? STATUS_ENABLED : knowledge.getStatus());
        knowledge.setSortOrder(knowledge.getSortOrder() == null ? 100 : knowledge.getSortOrder());
        knowledge.setCreateTime(LocalDateTime.now());
        knowledge.setUpdateTime(LocalDateTime.now());
        aiKnowledgeBaseMapper.insert(knowledge);
        return Result.success("知识库条目已创建", knowledge);
    }

    public Result<?> update(Long id, AiKnowledgeBase payload) {
        AiKnowledgeBase knowledge = aiKnowledgeBaseMapper.selectById(id);
        if (knowledge == null) {
            return Result.error("知识库条目不存在");
        }
        if (StringUtils.hasText(payload.getCategory())) {
            knowledge.setCategory(payload.getCategory().trim());
        }
        if (StringUtils.hasText(payload.getTitle())) {
            knowledge.setTitle(payload.getTitle().trim());
        }
        if (StringUtils.hasText(payload.getQuestion())) {
            knowledge.setQuestion(payload.getQuestion().trim());
        }
        if (StringUtils.hasText(payload.getAnswer())) {
            knowledge.setAnswer(payload.getAnswer().trim());
        }
        knowledge.setKeywords(StringUtils.hasText(payload.getKeywords()) ? payload.getKeywords().trim() : knowledge.getKeywords());
        if (payload.getStatus() != null) {
            knowledge.setStatus(payload.getStatus());
        }
        if (payload.getSortOrder() != null) {
            knowledge.setSortOrder(payload.getSortOrder());
        }
        knowledge.setUpdateTime(LocalDateTime.now());
        Result<?> validation = validate(knowledge);
        if (validation != null) {
            return validation;
        }
        aiKnowledgeBaseMapper.updateById(knowledge);
        return Result.success("知识库条目已更新", knowledge);
    }

    public Result<?> delete(Long id) {
        AiKnowledgeBase knowledge = aiKnowledgeBaseMapper.selectById(id);
        if (knowledge == null) {
            return Result.error("知识库条目不存在");
        }
        aiKnowledgeBaseMapper.deleteById(id);
        return Result.success("知识库条目已删除");
    }

    public List<AiKnowledgeBase> searchForQuestion(String message, int limit) {
        if (!StringUtils.hasText(message)) {
            return List.of();
        }
        String normalized = normalize(message);
        List<AiKnowledgeBase> all = aiKnowledgeBaseMapper.selectList(new QueryWrapper<AiKnowledgeBase>()
                .eq("status", STATUS_ENABLED)
                .orderByAsc("sort_order")
                .orderByDesc("id")
                .last("LIMIT 200"));
        return all.stream()
                .map(item -> Map.entry(item, score(item, normalized)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<AiKnowledgeBase, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().getSortOrder() == null ? 100 : entry.getKey().getSortOrder()))
                .limit(Math.max(1, limit))
                .map(Map.Entry::getKey)
                .toList();
    }

    public String buildPromptText(List<AiKnowledgeBase> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("匹配到的平台规则/FAQ：\n");
        for (AiKnowledgeBase item : items) {
            sb.append("- 【").append(nullToEmpty(item.getCategory())).append("】")
                    .append(item.getTitle()).append("：")
                    .append(item.getAnswer()).append("\n");
        }
        return sb.toString();
    }

    public String directAnswerIfStrongMatch(String message, List<AiKnowledgeBase> items) {
        if (!StringUtils.hasText(message) || items == null || items.isEmpty()) {
            return null;
        }
        String normalized = normalize(message);
        AiKnowledgeBase first = items.get(0);
        int firstScore = score(first, normalized);
        if (firstScore >= 8 || normalize(first.getTitle()).contains(normalized) || normalized.contains(normalize(first.getTitle()))) {
            return "根据拾光平台规则「" + first.getTitle() + "」：\n" + first.getAnswer();
        }
        return null;
    }

    private int score(AiKnowledgeBase item, String normalizedMessage) {
        if (item == null || !StringUtils.hasText(normalizedMessage)) {
            return 0;
        }
        int score = 0;
        score += scoreText(item.getTitle(), normalizedMessage, 5);
        score += scoreText(item.getQuestion(), normalizedMessage, 4);
        score += scoreText(item.getCategory(), normalizedMessage, 2);
        score += scoreText(item.getKeywords(), normalizedMessage, 3);
        score += scoreText(item.getAnswer(), normalizedMessage, 1);
        return score;
    }

    private int scoreText(String source, String normalizedMessage, int weight) {
        if (!StringUtils.hasText(source)) {
            return 0;
        }
        String normalizedSource = normalize(source);
        if (normalizedSource.contains(normalizedMessage) || normalizedMessage.contains(normalizedSource)) {
            return weight * 3;
        }
        int score = 0;
        for (String token : normalizedSource.split("[,，、\\s]+")) {
            if (token.length() >= 2 && normalizedMessage.contains(token)) {
                score += weight;
            }
        }
        return score;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private Result<?> validate(AiKnowledgeBase knowledge) {
        if (knowledge == null || !StringUtils.hasText(knowledge.getCategory())) {
            return Result.error("分类不能为空");
        }
        if (!StringUtils.hasText(knowledge.getTitle())) {
            return Result.error("标题不能为空");
        }
        if (!StringUtils.hasText(knowledge.getAnswer())) {
            return Result.error("答案不能为空");
        }
        knowledge.setCategory(knowledge.getCategory().trim());
        knowledge.setTitle(knowledge.getTitle().trim());
        knowledge.setQuestion(StringUtils.hasText(knowledge.getQuestion()) ? knowledge.getQuestion().trim() : knowledge.getTitle());
        knowledge.setAnswer(knowledge.getAnswer().trim());
        knowledge.setKeywords(StringUtils.hasText(knowledge.getKeywords()) ? knowledge.getKeywords().trim() : "");
        return null;
    }

    private Map<String, Object> buildPageData(Page<?> page, List<?> records) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return data;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
