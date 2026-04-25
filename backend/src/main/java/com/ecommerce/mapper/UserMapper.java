package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.User;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
    User findByUsername(String username);
}
