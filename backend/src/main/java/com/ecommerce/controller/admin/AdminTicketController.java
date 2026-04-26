package com.ecommerce.controller.admin;

import com.ecommerce.service.CustomerTicketService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketController {
    private final CustomerTicketService customerTicketService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String keyword) {
        return customerTicketService.adminPage(page, size, status, keyword);
    }

    @PutMapping("/{id}/assign")
    public Result<?> assign(@PathVariable Long id, @RequestParam(required = false) Long assigneeId) {
        return customerTicketService.assign(id, assigneeId);
    }

    @PutMapping("/{id}/status")
    public Result<?> status(@PathVariable Long id,
                            @RequestParam Integer status,
                            @RequestParam(required = false) String resolution) {
        return customerTicketService.updateStatus(id, status, resolution);
    }

    @PutMapping("/{id}/reply")
    public Result<?> reply(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return customerTicketService.reply(id, payload);
    }
}
