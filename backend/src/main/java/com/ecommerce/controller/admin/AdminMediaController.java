package com.ecommerce.controller.admin;

import com.ecommerce.service.StorageService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {
    private final StorageService storageService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String bizType,
                          @RequestParam(required = false) String provider,
                          @RequestParam(required = false) String keyword) {
        return storageService.page(page, size, bizType, provider, keyword);
    }
}
