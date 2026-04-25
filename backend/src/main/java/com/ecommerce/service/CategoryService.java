package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.entity.Category;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public Result<List<Category>> getAllCategories() {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort");
        return Result.success(categoryMapper.selectList(wrapper));
    }

    public Result<Category> getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        return Result.success(category);
    }

    public Result<List<Category>> getSubCategories(Long parentId) {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId).eq("status", 1).orderByAsc("sort");
        return Result.success(categoryMapper.selectList(wrapper));
    }
}
