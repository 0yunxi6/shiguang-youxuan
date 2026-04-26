package com.ecommerce.controller;

import com.ecommerce.service.AiKnowledgeBaseService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final AiKnowledgeBaseService aiKnowledgeBaseService;

    @GetMapping("/faq")
    public Result<?> faq(@RequestParam(required = false) String keyword) {
        return aiKnowledgeBaseService.publicList(keyword);
    }
}
