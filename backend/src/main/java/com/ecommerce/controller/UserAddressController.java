package com.ecommerce.controller;

import com.ecommerce.entity.UserAddress;
import com.ecommerce.service.UserAddressService;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public Result<?> list() {
        return userAddressService.listMyAddresses();
    }

    @PostMapping
    public Result<?> create(@RequestBody UserAddress address) {
        return userAddressService.createAddress(address);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody UserAddress address) {
        return userAddressService.updateAddress(id, address);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return userAddressService.deleteAddress(id);
    }

    @PutMapping("/{id}/default")
    public Result<?> setDefault(@PathVariable Long id) {
        return userAddressService.setDefault(id);
    }
}
