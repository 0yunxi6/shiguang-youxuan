package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

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
                          @RequestParam(required = false) Integer status) {
        if (status != null && isInvalidBinaryStatus(status)) {
            return Result.error("商品状态不合法");
        }
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
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        return Result.success(productMapper.selectPage(pageParam, wrapper));
    }

    @PostMapping
    public Result<?> create(@RequestBody Product product) {
        normalizeProductImages(product);
        productMapper.insert(product);
        return Result.success("商品创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        normalizeProductImages(product);
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

    private boolean isInvalidBinaryStatus(Integer status) {
        return status == null || (status != 0 && status != 1);
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
