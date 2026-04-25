package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE category_id = #{categoryId} AND status = 1 AND deleted = 0")
    List<Product> findByCategoryId(Long categoryId);

    @Update("UPDATE product SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE id = #{productId} AND status = 1 AND deleted = 0 AND stock >= #{quantity}")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET stock = stock + #{quantity}, update_time = NOW() WHERE id = #{productId} AND deleted = 0")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
