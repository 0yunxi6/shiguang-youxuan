package com.ecommerce.controller;

import com.ecommerce.dto.AiChatRequest;
import com.ecommerce.service.AiCustomerService;
import com.ecommerce.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiCustomerController {
    private final AiCustomerService aiCustomerService;

    @GetMapping("/context")
    public Result<?> context() {
        return aiCustomerService.context();
    }

    @PostMapping("/chat")
    public Result<?> chat(@Valid @RequestBody AiChatRequest request) {
        return aiCustomerService.chat(request);
    }
}
