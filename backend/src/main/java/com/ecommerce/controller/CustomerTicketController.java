package com.ecommerce.controller;

import com.ecommerce.service.CustomerTicketService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class CustomerTicketController {
    private final CustomerTicketService customerTicketService;

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> payload) {
        return customerTicketService.createTicket(payload);
    }

    @GetMapping("/my")
    public Result<?> myTickets() {
        return customerTicketService.myTickets();
    }

    @PutMapping("/{id}/satisfaction")
    public Result<?> satisfaction(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return customerTicketService.satisfaction(id, payload);
    }
}
