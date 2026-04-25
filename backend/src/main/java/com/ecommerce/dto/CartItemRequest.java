package com.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartItemRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量不能小于1")
    @Max(value = 99, message = "单件商品最多购买99件")
    private Integer quantity = 1;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
