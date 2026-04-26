package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.SearchLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface SearchLogMapper extends BaseMapper<SearchLog> {

    @Insert("""
            INSERT INTO search_log (keyword, search_count, last_search_time, create_time, update_time)
            VALUES (#{keyword}, 1, NOW(), NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                search_count = search_count + 1,
                last_search_time = NOW(),
                update_time = NOW()
            """)
    int upsertKeyword(@Param("keyword") String keyword);
}
