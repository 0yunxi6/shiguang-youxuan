package com.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateCartItemRequest {
    @NotNull(message = "购物车项ID不能为空")
    private Long id;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量不能小于1")
    @Max(value = 99, message = "单件商品最多购买99件")
    private Integer quantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
