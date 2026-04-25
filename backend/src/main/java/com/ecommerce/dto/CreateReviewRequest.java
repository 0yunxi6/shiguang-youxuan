package com.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReviewRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private Long orderId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于1星")
    @Max(value = 5, message = "评分不能高于5星")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(min = 5, max = 500, message = "评价内容长度需在5-500个字符之间")
    private String content;

    @Size(max = 1000, message = "图片地址内容过长")
    private String images;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
}
