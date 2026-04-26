-- 创建数据库
CREATE DATABASE IF NOT EXISTS ecommerce DEFAULT CHARACTER SET utf8mb4;

USE ecommerce;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50),
  `email` VARCHAR(100),
  `phone` VARCHAR(20),
  `avatar` VARCHAR(500),
  `role` VARCHAR(20) DEFAULT 'USER' COMMENT 'USER/ADMIN',
  `status` TINYINT DEFAULT 1 COMMENT '0:禁用 1:正常',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_role_status` (`role`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `parent_id` BIGINT DEFAULT 0,
  `level` INT DEFAULT 1,
  `sort` INT DEFAULT 0,
  `icon` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_category_parent_status` (`parent_id`, `status`, `deleted`),
  KEY `idx_category_sort` (`sort`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `category_id` BIGINT,
  `price` DECIMAL(10,2) NOT NULL,
  `stock` INT DEFAULT 0,
  `description` TEXT,
  `image_url` VARCHAR(500),
  `images` VARCHAR(2000),
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_product_category_status` (`category_id`, `status`, `deleted`),
  KEY `idx_product_status_time` (`status`, `create_time`),
  KEY `idx_product_price` (`price`),
  FOREIGN KEY (`category_id`) REFERENCES `category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 购物车表
CREATE TABLE IF NOT EXISTS `cart` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `quantity` INT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_cart_user_product` (`user_id`, `product_id`),
  KEY `idx_cart_user_update` (`user_id`, `update_time`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户收货地址表
CREATE TABLE IF NOT EXISTS `user_address` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `receiver_phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50) NOT NULL,
  `city` VARCHAR(50) NOT NULL,
  `district` VARCHAR(50) DEFAULT NULL,
  `detail` VARCHAR(255) NOT NULL,
  `is_default` TINYINT(1) DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_address_user_default` (`user_id`, `is_default`, `deleted`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `order_no` VARCHAR(50) NOT NULL UNIQUE,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `original_amount` DECIMAL(10,2) DEFAULT NULL,
  `discount_amount` DECIMAL(10,2) DEFAULT 0,
  `shipping_fee` DECIMAL(10,2) DEFAULT 0,
  `coupon_id` BIGINT DEFAULT NULL,
  `coupon_name` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 0 COMMENT '0:待支付 1:已支付 2:已发货 3:已完成 4:已取消',
  `receiver_name` VARCHAR(50),
  `receiver_phone` VARCHAR(20),
  `receiver_address` VARCHAR(500),
  `remark` VARCHAR(500),
  `payment_method` VARCHAR(50) DEFAULT NULL,
  `invoice_title` VARCHAR(100) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `shipped_time` DATETIME DEFAULT NULL,
  `completed_time` DATETIME DEFAULT NULL,
  KEY `idx_orders_user_time` (`user_id`, `create_time`),
  KEY `idx_orders_status_time` (`status`, `create_time`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单项表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `product_name` VARCHAR(200),
  `product_image` VARCHAR(500),
  `product_spec` VARCHAR(200),
  `quantity` INT NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `total_amount` DECIMAL(10,2),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_order_item_order` (`order_id`),
  KEY `idx_order_item_product` (`product_id`),
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品评价表
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
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品收藏表
CREATE TABLE IF NOT EXISTS `product_favorite` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_favorite_user_product` (`user_id`, `product_id`),
  KEY `idx_favorite_product_time` (`product_id`, `create_time`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `coupon_code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255),
  `discount_amount` DECIMAL(10,2) NOT NULL,
  `min_amount` DECIMAL(10,2) DEFAULT 0,
  `status` TINYINT DEFAULT 0,
  `start_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `end_time` DATETIME DEFAULT NULL,
  `use_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_coupon_user_status` (`user_id`, `status`, `end_time`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'ADMIN');

-- 测试用户 (密码: 123456, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `phone`) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 'test@example.com', '13800138000');

INSERT INTO `category` (`name`, `parent_id`, `sort`) VALUES
('手机数码', 0, 1),
('电脑办公', 0, 2),
('家用电器', 0, 3),
('服饰鞋包', 0, 4),
('图书文具', 0, 5),
('食品生鲜', 0, 6);

INSERT INTO `product` (`name`, `category_id`, `price`, `stock`, `description`, `image_url`, `images`) VALUES
('iPhone 15 Pro Max 256GB', 1, 9999.00, 86, '全新A17 Pro芯片，钛金属设计，4800万像素主摄，USB-C接口', 'https://picsum.photos/seed/iphone/400/400', 'https://picsum.photos/seed/iphone/400/400,https://picsum.photos/seed/iphone-side/400/400,https://picsum.photos/seed/iphone-back/400/400'),
('华为 Mate 60 Pro', 1, 6999.00, 120, '麒麟9000S芯片，卫星通话，超可靠玄武架构', 'https://picsum.photos/seed/huawei/400/400', 'https://picsum.photos/seed/huawei/400/400,https://picsum.photos/seed/huawei-side/400/400,https://picsum.photos/seed/huawei-back/400/400'),
('小米14 Ultra', 1, 5999.00, 200, '骁龙8 Gen3，徕卡光学镜头，专业影像旗舰', 'https://picsum.photos/seed/xiaomi/400/400', 'https://picsum.photos/seed/xiaomi/400/400,https://picsum.photos/seed/xiaomi-side/400/400,https://picsum.photos/seed/xiaomi-back/400/400'),
('MacBook Pro 14英寸 M3 Pro', 2, 14999.00, 45, 'M3 Pro芯片，18GB统一内存，Liquid Retina XDR显示屏', 'https://picsum.photos/seed/macbook/400/400', 'https://picsum.photos/seed/macbook/400/400,https://picsum.photos/seed/macbook-open/400/400,https://picsum.photos/seed/macbook-keyboard/400/400'),
('联想ThinkPad X1 Carbon', 2, 9999.00, 60, '第14代酷睿处理器，2.8K OLED屏，1.12kg轻薄商务本', 'https://picsum.photos/seed/thinkpad/400/400', 'https://picsum.photos/seed/thinkpad/400/400,https://picsum.photos/seed/thinkpad-open/400/400,https://picsum.photos/seed/thinkpad-keyboard/400/400'),
('AirPods Pro 第二代', 1, 1799.00, 300, '自适应降噪，个性化空间音频，USB-C充电盒', 'https://picsum.photos/seed/airpods/400/400', 'https://picsum.photos/seed/airpods/400/400,https://picsum.photos/seed/airpods-case/400/400,https://picsum.photos/seed/airpods-detail/400/400'),
('索尼WH-1000XM5', 1, 2499.00, 150, '行业领先降噪，30小时续航，多点连接', 'https://picsum.photos/seed/sony/400/400', 'https://picsum.photos/seed/sony/400/400,https://picsum.photos/seed/sony-side/400/400,https://picsum.photos/seed/sony-detail/400/400'),
('戴森V15 Detect', 3, 4990.00, 80, '激光探测灰尘，240AW强劲吸力，LCD屏幕实时显示', 'https://picsum.photos/seed/dyson/400/400', 'https://picsum.photos/seed/dyson/400/400,https://picsum.photos/seed/dyson-head/400/400,https://picsum.photos/seed/dyson-detail/400/400'),
('海尔冰箱 BCD-510', 3, 3999.00, 40, '510升大容量，风冷无霜，一级能效', 'https://picsum.photos/seed/haier/400/400', 'https://picsum.photos/seed/haier/400/400,https://picsum.photos/seed/haier-open/400/400,https://picsum.photos/seed/haier-detail/400/400'),
('优衣库 纯棉圆领T恤', 4, 79.00, 500, '100%新疆棉，舒适透气，多色可选', 'https://picsum.photos/seed/tshirt/400/400', 'https://picsum.photos/seed/tshirt/400/400,https://picsum.photos/seed/tshirt-back/400/400,https://picsum.photos/seed/tshirt-detail/400/400'),
('Nike Air Force 1', 4, 799.00, 180, '经典白色低帮，Air缓震科技，百搭潮流', 'https://picsum.photos/seed/nike/400/400', 'https://picsum.photos/seed/nike/400/400,https://picsum.photos/seed/nike-side/400/400,https://picsum.photos/seed/nike-sole/400/400'),
('深入理解计算机系统 第3版', 5, 139.00, 100, 'CSAPP经典教材，程序员进阶必读', 'https://picsum.photos/seed/csapp/400/400', 'https://picsum.photos/seed/csapp/400/400,https://picsum.photos/seed/csapp-open/400/400,https://picsum.photos/seed/csapp-detail/400/400');
