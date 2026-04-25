package com.ecommerce.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String keyword) {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like("name", keyword);
        }
        wrapper.orderByAsc("sort", "id");
        return Result.success(categoryMapper.selectList(wrapper));
    }

    @PostMapping
    public Result<?> create(@RequestBody Category category) {
        categoryMapper.insert(category);
        return Result.success("分类创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success("分类更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", id);
        Long childCount = categoryMapper.selectCount(wrapper);
        if (childCount > 0) {
            return Result.error("该分类下存在子分类，无法删除");
        }
        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("category_id", id);
        Long productCount = productMapper.selectCount(productWrapper);
        if (productCount > 0) {
            return Result.error("该分类下存在商品，无法删除");
        }
        categoryMapper.deleteById(id);
        return Result.success("分类删除成功");
    }
}
