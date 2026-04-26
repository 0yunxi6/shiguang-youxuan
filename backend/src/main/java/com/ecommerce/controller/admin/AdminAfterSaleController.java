package com.ecommerce.controller.admin;

import com.ecommerce.service.AfterSaleService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/after-sales")
@RequiredArgsConstructor
public class AdminAfterSaleController {
    private final AfterSaleService afterSaleService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String keyword) {
        return afterSaleService.adminList(page, size, status, keyword);
    }

    @PutMapping("/{id}/status")
    public Result<?> audit(@PathVariable Long id,
                           @RequestParam Integer status,
                           @RequestParam(required = false) String remark) {
        return afterSaleService.audit(id, status, remark);
    }
}
