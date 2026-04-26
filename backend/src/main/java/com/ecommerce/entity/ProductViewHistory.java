package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("product_view_history")
public class ProductViewHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private LocalDateTime viewTime;

    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String productImage;
    @TableField(exist = false)
    private java.math.BigDecimal price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public LocalDateTime getViewTime() { return viewTime; }
    public void setViewTime(LocalDateTime viewTime) { this.viewTime = viewTime; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
}
