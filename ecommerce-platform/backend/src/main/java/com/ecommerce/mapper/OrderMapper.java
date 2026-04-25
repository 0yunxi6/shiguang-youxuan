package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    List<Order> findByUserId(Long userId);
}
