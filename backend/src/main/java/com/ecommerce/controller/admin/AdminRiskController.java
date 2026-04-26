package com.ecommerce.controller.admin;

import com.ecommerce.service.RiskEventService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/risk")
@RequiredArgsConstructor
public class AdminRiskController {
    private final RiskEventService riskEventService;

    @GetMapping("/events")
    public Result<?> page(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String eventType,
                          @RequestParam(required = false) String riskLevel,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String keyword) {
        return riskEventService.page(page, size, eventType, riskLevel, status, keyword);
    }

    @PutMapping("/events/{id}/status")
    public Result<?> status(@PathVariable Long id, @RequestParam Integer status) {
        return riskEventService.updateStatus(id, status);
    }
}
