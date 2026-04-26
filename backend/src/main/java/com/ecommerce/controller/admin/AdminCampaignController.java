package com.ecommerce.controller.admin;

import com.ecommerce.service.CampaignEventService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
public class AdminCampaignController {
    private final CampaignEventService campaignEventService;

    @GetMapping("/stats")
    public Result<?> stats(@RequestParam(required = false) String campaignType,
                           @RequestParam(required = false) String startTime,
                           @RequestParam(required = false) String endTime) {
        return campaignEventService.stats(campaignType, startTime, endTime);
    }

    @GetMapping("/events")
    public Result<?> events(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String campaignType,
                            @RequestParam(required = false) String eventType) {
        return campaignEventService.events(page, size, campaignType, eventType);
    }
}
