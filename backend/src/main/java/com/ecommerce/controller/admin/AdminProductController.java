package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductMapper productMapper;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) String brand,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "false") Boolean lowStockOnly,
                          @RequestParam(defaultValue = "10") Integer maxStock) {
        if (status != null && isInvalidBinaryStatus(status)) {
            return Result.error("商品状态不合法");
        }
        int lowStockThreshold = Math.min(Math.max(maxStock == null ? 10 : maxStock, 0), 99999);
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<Product> pageParam = new Page<>(page, size);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like("name", keyword);
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (StringUtils.hasText(brand)) {
            wrapper.like("brand", brand.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (Boolean.TRUE.equals(lowStockOnly)) {
            wrapper.le("stock", lowStockThreshold);
            wrapper.orderByAsc("stock").orderByDesc("create_time");
        } else {
            wrapper.orderByDesc("create_time");
        }
        return Result.success(productMapper.selectPage(pageParam, wrapper));
    }

    @PostMapping
    public Result<?> create(@RequestBody Product product) {
        normalizeProductImages(product);
        Result<?> validation = validateProduct(product);
        if (validation != null) {
            return validation;
        }
        productMapper.insert(product);
        return Result.success("商品创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        normalizeProductImages(product);
        Result<?> validation = validateProduct(product);
        if (validation != null) {
            return validation;
        }
        productMapper.updateById(product);
        return Result.success("商品更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        productMapper.deleteById(id);
        return Result.success("商品删除成功");
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (isInvalidBinaryStatus(status)) {
            return Result.error("商品状态不合法");
        }
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        return Result.success("状态更新成功");
    }

    @PutMapping("/status/batch")
    public Result<?> batchUpdateStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        if (isInvalidBinaryStatus(status)) {
            return Result.error("商品状态不合法");
        }
        List<Long> validIds = ids == null ? List.of() : ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            return Result.error("请选择要操作的商品");
        }
        Product product = new Product();
        product.setStatus(status);
        productMapper.update(product, new QueryWrapper<Product>().in("id", validIds));
        return Result.success("批量更新成功");
    }

    private boolean isInvalidBinaryStatus(Integer status) {
        return status == null || (status != 0 && status != 1);
    }

    private Result<?> validateProduct(Product product) {
        if (product == null) {
            return Result.error("商品信息不能为空");
        }
        product.setName(StringUtils.hasText(product.getName()) ? product.getName().trim() : null);
        product.setBrand(StringUtils.hasText(product.getBrand()) ? product.getBrand().trim() : null);
        product.setDescription(StringUtils.hasText(product.getDescription()) ? product.getDescription().trim() : null);
        if (!StringUtils.hasText(product.getName())) {
            return Result.error("商品名称不能为空");
        }
        if (product.getName().length() > 100) {
            return Result.error("商品名称不能超过100个字符");
        }
        if (product.getBrand() != null && product.getBrand().length() > 100) {
            return Result.error("品牌名称不能超过100个字符");
        }
        if (product.getCategoryId() == null || product.getCategoryId() <= 0) {
            return Result.error("请选择商品分类");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.error("商品价格不能小于0");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            return Result.error("商品库存不能小于0");
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (isInvalidBinaryStatus(product.getStatus())) {
            return Result.error("商品状态不合法");
        }
        return null;
    }

    private void normalizeProductImages(Product product) {
        if (product == null) {
            return;
        }
        LinkedHashSet<String> images = new LinkedHashSet<>();
        if (product.getImageList() != null) {
            product.getImageList().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(images::add);
        }
        if (StringUtils.hasText(product.getImages())) {
            Arrays.stream(product.getImages().split("[,，\\n\\r]+"))
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(images::add);
        }
        if (StringUtils.hasText(product.getImageUrl())) {
            images.add(product.getImageUrl().trim());
        }
        List<String> normalized = images.stream().toList();
        product.setImageList(normalized);
        product.setImages(normalized.isEmpty() ? null : String.join(",", normalized));
        if (!normalized.isEmpty()) {
            product.setImageUrl(normalized.get(0));
        }
    }
}
