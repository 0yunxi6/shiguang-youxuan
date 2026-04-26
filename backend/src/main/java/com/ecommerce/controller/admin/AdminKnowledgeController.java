package com.ecommerce.controller.admin;

import com.ecommerce.entity.AiKnowledgeBase;
import com.ecommerce.service.AiKnowledgeBaseService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {
    private final AiKnowledgeBaseService aiKnowledgeBaseService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return aiKnowledgeBaseService.page(page, size, category, keyword, status);
    }

    @PostMapping
    public Result<?> create(@RequestBody AiKnowledgeBase knowledge) {
        return aiKnowledgeBaseService.create(knowledge);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody AiKnowledgeBase knowledge) {
        return aiKnowledgeBaseService.update(id, knowledge);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return aiKnowledgeBaseService.delete(id);
    }
}
