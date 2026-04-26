package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String brand;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private String images;
    private String specs;
    private String videoUrl;
    private String promotionTag;
    private BigDecimal promotionPrice;
    private LocalDateTime promotionStartTime;
    private LocalDateTime promotionEndTime;
    private Integer status;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private List<String> imageList;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private Boolean favorited;
    @TableField(exist = false)
    private Long favoriteCount;
    @TableField(exist = false)
    private Long sales;
    @TableField(exist = false)
    private LocalDateTime favoriteTime;
    @TableField(exist = false)
    private BigDecimal effectivePrice;
    @TableField(exist = false)
    private Boolean promotionActive;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getPromotionTag() { return promotionTag; }
    public void setPromotionTag(String promotionTag) { this.promotionTag = promotionTag; }
    public BigDecimal getPromotionPrice() { return promotionPrice; }
    public void setPromotionPrice(BigDecimal promotionPrice) { this.promotionPrice = promotionPrice; }
    public LocalDateTime getPromotionStartTime() { return promotionStartTime; }
    public void setPromotionStartTime(LocalDateTime promotionStartTime) { this.promotionStartTime = promotionStartTime; }
    public LocalDateTime getPromotionEndTime() { return promotionEndTime; }
    public void setPromotionEndTime(LocalDateTime promotionEndTime) { this.promotionEndTime = promotionEndTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<String> getImageList() { return imageList; }
    public void setImageList(List<String> imageList) { this.imageList = imageList; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public Long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
    public Long getSales() { return sales; }
    public void setSales(Long sales) { this.sales = sales; }
    public LocalDateTime getFavoriteTime() { return favoriteTime; }
    public void setFavoriteTime(LocalDateTime favoriteTime) { this.favoriteTime = favoriteTime; }
    public BigDecimal getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(BigDecimal effectivePrice) { this.effectivePrice = effectivePrice; }
    public Boolean getPromotionActive() { return promotionActive; }
    public void setPromotionActive(Boolean promotionActive) { this.promotionActive = promotionActive; }
}
