USE ecommerce;

CREATE TABLE IF NOT EXISTS `product_review` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `rating` TINYINT NOT NULL COMMENT '1-5星',
  `content` VARCHAR(500) NOT NULL,
  `images` VARCHAR(1000),
  `status` TINYINT DEFAULT 1 COMMENT '0:隐藏 1:展示',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_review_user_product` (`user_id`, `product_id`),
  KEY `idx_review_product_status` (`product_id`, `status`, `deleted`, `create_time`),
  KEY `idx_review_status_time` (`status`, `create_time`),
  CONSTRAINT `fk_review_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`),
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
