package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
    List<Cart> findByUserId(Long userId);
}
