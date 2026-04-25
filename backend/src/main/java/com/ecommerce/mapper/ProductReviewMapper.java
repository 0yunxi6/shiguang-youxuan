package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.ProductReview;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    @Select("""
            <script>
            SELECT r.*,
                   u.username AS username,
                   u.nickname AS nickname,
                   u.avatar AS avatar,
                   p.name AS product_name,
                   p.image_url AS product_image
            FROM product_review r
            LEFT JOIN `user` u ON u.id = r.user_id
            LEFT JOIN product p ON p.id = r.product_id
            WHERE r.deleted = 0
              AND r.product_id = #{productId}
              AND r.status = 1
            <if test="rating != null">
              AND r.rating = #{rating}
            </if>
            ORDER BY r.create_time DESC
            </script>
            """)
    Page<ProductReview> selectPublicPage(Page<ProductReview> page,
                                         @Param("productId") Long productId,
                                         @Param("rating") Integer rating);

    @Select("""
            <script>
            SELECT r.*,
                   u.username AS username,
                   u.nickname AS nickname,
                   u.avatar AS avatar,
                   p.name AS product_name,
                   p.image_url AS product_image
            FROM product_review r
            LEFT JOIN `user` u ON u.id = r.user_id
            LEFT JOIN product p ON p.id = r.product_id
            WHERE r.deleted = 0
            <if test="status != null">
              AND r.status = #{status}
            </if>
            <if test="productId != null">
              AND r.product_id = #{productId}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                r.content LIKE CONCAT('%', #{keyword}, '%')
                OR p.name LIKE CONCAT('%', #{keyword}, '%')
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY r.create_time DESC
            </script>
            """)
    Page<ProductReview> selectAdminPage(Page<ProductReview> page,
                                        @Param("status") Integer status,
                                        @Param("productId") Long productId,
                                        @Param("keyword") String keyword);

    @Select("""
            SELECT COALESCE(ROUND(AVG(rating), 1), 0) AS avgRating,
                   COUNT(*) AS reviewCount
            FROM product_review
            WHERE product_id = #{productId}
              AND status = 1
              AND deleted = 0
            """)
    Map<String, Object> selectProductSummary(@Param("productId") Long productId);
}
