package com.ecommerce.service;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.ProductFavoriteMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ProductFavoriteMapper productFavoriteMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductById_existingProduct_returnsProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("测试商品");
        product.setCategoryId(1L);
        product.setPrice(new BigDecimal("99.99"));
        product.setStatus(1);
        Category category = new Category();
        category.setId(1L);
        category.setName("测试分类");
        when(productMapper.selectById(1L)).thenReturn(product);
        when(categoryMapper.selectBatchIds(anyList())).thenReturn(List.of(category));
        when(productFavoriteMapper.selectMaps(any())).thenReturn(List.of(Map.of("product_id", 1L, "total", 2L)));
        when(orderItemMapper.selectMaps(any())).thenReturn(List.of(Map.of("PRODUCT_ID", 1L, "TOTAL", new BigDecimal("5"))));

        Product result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals("测试商品", result.getName());
        assertEquals(new BigDecimal("99.99"), result.getPrice());
        assertEquals(2L, result.getFavoriteCount());
        assertEquals(5L, result.getSales());
    }

    @Test
    void getProductById_nonExistingProduct_returnsNull() {
        when(productMapper.selectById(999L)).thenReturn(null);

        Product result = productService.getProductById(999L);

        assertNull(result);
    }
}
