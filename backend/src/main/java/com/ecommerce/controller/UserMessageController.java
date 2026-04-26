package com.ecommerce.controller;

import com.ecommerce.service.MessageService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class UserMessageController {
    private final MessageService messageService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) Integer readStatus,
                          @RequestParam(required = false) String type) {
        return messageService.list(readStatus, type);
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        return messageService.unreadCount();
    }

    @PutMapping("/{id}/read")
    public Result<?> read(@PathVariable Long id) {
        return messageService.markRead(id);
    }

    @PutMapping("/read-all")
    public Result<?> readAll() {
        return messageService.markAllRead();
    }
}
