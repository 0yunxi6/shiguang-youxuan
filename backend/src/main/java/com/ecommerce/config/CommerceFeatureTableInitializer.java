package com.ecommerce.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class CommerceFeatureTableInitializer {

    @Bean
    public CommandLineRunner ensureCommerceFeatureTables(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
                ensureUserColumns(connection, statement);
                ensureProductImagesColumn(connection, statement);
                ensureProductBrandColumn(connection, statement);
                ensureProductMediumColumns(connection, statement);
                ensureReviewMediumColumns(connection, statement);
                ensureCartSpecColumn(connection, statement);
                ensureOrdersColumns(connection, statement);
                ensureUserAddressTable(statement);
                ensureSearchLogTable(statement);
                ensureMediumFeatureTables(statement);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS `product_favorite` (
                          `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                          `user_id` BIGINT NOT NULL,
                          `product_id` BIGINT NOT NULL,
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          UNIQUE KEY `uk_favorite_user_product` (`user_id`, `product_id`),
                          KEY `idx_favorite_product_time` (`product_id`, `create_time`),
                          CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                          CONSTRAINT `fk_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
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
                          CONSTRAINT `fk_coupon_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.executeUpdate("UPDATE product SET images = image_url WHERE (images IS NULL OR images = '') AND image_url IS NOT NULL AND image_url <> ''");
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to initialize commerce feature tables", ex);
            }
        };
    }

    private void ensureProductImagesColumn(Connection connection, Statement statement) throws SQLException {
        if (!columnExists(connection, "product", "images")) {
            statement.execute("ALTER TABLE `product` ADD COLUMN `images` VARCHAR(2000) DEFAULT NULL AFTER `image_url`");
        }
    }

    private void ensureProductBrandColumn(Connection connection, Statement statement) throws SQLException {
        if (!columnExists(connection, "product", "brand")) {
            statement.execute("ALTER TABLE `product` ADD COLUMN `brand` VARCHAR(100) DEFAULT NULL AFTER `category_id`");
        }
    }

    private void ensureUserColumns(Connection connection, Statement statement) throws SQLException {
        addColumnIfMissing(connection, statement, "user", "status_reason", "ALTER TABLE `user` ADD COLUMN `status_reason` VARCHAR(255) DEFAULT NULL AFTER `status`");
        addColumnIfMissing(connection, statement, "user", "level", "ALTER TABLE `user` ADD COLUMN `level` VARCHAR(20) DEFAULT '普通会员' AFTER `status_reason`");
        addColumnIfMissing(connection, statement, "user", "points", "ALTER TABLE `user` ADD COLUMN `points` INT DEFAULT 0 AFTER `level`");
        addColumnIfMissing(connection, statement, "user", "email_verified", "ALTER TABLE `user` ADD COLUMN `email_verified` TINYINT DEFAULT 0 AFTER `points`");
        addColumnIfMissing(connection, statement, "user", "two_factor_enabled", "ALTER TABLE `user` ADD COLUMN `two_factor_enabled` TINYINT DEFAULT 0 AFTER `email_verified`");
        addColumnIfMissing(connection, statement, "user", "last_login_time", "ALTER TABLE `user` ADD COLUMN `last_login_time` DATETIME DEFAULT NULL AFTER `two_factor_enabled`");
        addColumnIfMissing(connection, statement, "user", "last_login_ip", "ALTER TABLE `user` ADD COLUMN `last_login_ip` VARCHAR(64) DEFAULT NULL AFTER `last_login_time`");
    }

    private void ensureProductMediumColumns(Connection connection, Statement statement) throws SQLException {
        addColumnIfMissing(connection, statement, "product", "specs", "ALTER TABLE `product` ADD COLUMN `specs` VARCHAR(1000) DEFAULT NULL AFTER `images`");
        addColumnIfMissing(connection, statement, "product", "video_url", "ALTER TABLE `product` ADD COLUMN `video_url` VARCHAR(500) DEFAULT NULL AFTER `specs`");
        addColumnIfMissing(connection, statement, "product", "promotion_tag", "ALTER TABLE `product` ADD COLUMN `promotion_tag` VARCHAR(50) DEFAULT NULL AFTER `video_url`");
        addColumnIfMissing(connection, statement, "product", "promotion_price", "ALTER TABLE `product` ADD COLUMN `promotion_price` DECIMAL(10,2) DEFAULT NULL AFTER `promotion_tag`");
        addColumnIfMissing(connection, statement, "product", "promotion_start_time", "ALTER TABLE `product` ADD COLUMN `promotion_start_time` DATETIME DEFAULT NULL AFTER `promotion_price`");
        addColumnIfMissing(connection, statement, "product", "promotion_end_time", "ALTER TABLE `product` ADD COLUMN `promotion_end_time` DATETIME DEFAULT NULL AFTER `promotion_start_time`");
    }

    private void ensureReviewMediumColumns(Connection connection, Statement statement) throws SQLException {
        addColumnIfMissing(connection, statement, "product_review", "append_content", "ALTER TABLE `product_review` ADD COLUMN `append_content` VARCHAR(500) DEFAULT NULL AFTER `images`");
        addColumnIfMissing(connection, statement, "product_review", "append_time", "ALTER TABLE `product_review` ADD COLUMN `append_time` DATETIME DEFAULT NULL AFTER `append_content`");
        addColumnIfMissing(connection, statement, "product_review", "admin_reply", "ALTER TABLE `product_review` ADD COLUMN `admin_reply` VARCHAR(500) DEFAULT NULL AFTER `append_time`");
        addColumnIfMissing(connection, statement, "product_review", "admin_reply_time", "ALTER TABLE `product_review` ADD COLUMN `admin_reply_time` DATETIME DEFAULT NULL AFTER `admin_reply`");
    }

    private void ensureCartSpecColumn(Connection connection, Statement statement) throws SQLException {
        addColumnIfMissing(connection, statement, "cart", "product_spec", "ALTER TABLE `cart` ADD COLUMN `product_spec` VARCHAR(200) NOT NULL DEFAULT '' AFTER `product_id`");
        statement.executeUpdate("UPDATE `cart` SET `product_spec` = '' WHERE `product_spec` IS NULL");
        if (indexExists(connection, "cart", "uk_cart_user_product")) {
            statement.execute("ALTER TABLE `cart` DROP INDEX `uk_cart_user_product`");
        }
        if (!indexExists(connection, "cart", "uk_cart_user_product_spec")) {
            statement.execute("ALTER TABLE `cart` ADD UNIQUE KEY `uk_cart_user_product_spec` (`user_id`, `product_id`, `product_spec`)");
        }
    }

    private void ensureOrdersColumns(Connection connection, Statement statement) throws SQLException {
        addColumnIfMissing(connection, statement, "orders", "original_amount", "ALTER TABLE `orders` ADD COLUMN `original_amount` DECIMAL(10,2) DEFAULT NULL AFTER `total_amount`");
        addColumnIfMissing(connection, statement, "orders", "discount_amount", "ALTER TABLE `orders` ADD COLUMN `discount_amount` DECIMAL(10,2) DEFAULT 0 AFTER `original_amount`");
        addColumnIfMissing(connection, statement, "orders", "shipping_fee", "ALTER TABLE `orders` ADD COLUMN `shipping_fee` DECIMAL(10,2) DEFAULT 0 AFTER `discount_amount`");
        addColumnIfMissing(connection, statement, "orders", "coupon_id", "ALTER TABLE `orders` ADD COLUMN `coupon_id` BIGINT DEFAULT NULL AFTER `shipping_fee`");
        addColumnIfMissing(connection, statement, "orders", "coupon_name", "ALTER TABLE `orders` ADD COLUMN `coupon_name` VARCHAR(100) DEFAULT NULL AFTER `coupon_id`");
        addColumnIfMissing(connection, statement, "orders", "payment_method", "ALTER TABLE `orders` ADD COLUMN `payment_method` VARCHAR(50) DEFAULT NULL AFTER `remark`");
        addColumnIfMissing(connection, statement, "orders", "invoice_title", "ALTER TABLE `orders` ADD COLUMN `invoice_title` VARCHAR(100) DEFAULT NULL AFTER `payment_method`");
        addColumnIfMissing(connection, statement, "orders", "invoice_tax_no", "ALTER TABLE `orders` ADD COLUMN `invoice_tax_no` VARCHAR(50) DEFAULT NULL AFTER `invoice_title`");
        addColumnIfMissing(connection, statement, "orders", "shipped_time", "ALTER TABLE `orders` ADD COLUMN `shipped_time` DATETIME DEFAULT NULL AFTER `update_time`");
        addColumnIfMissing(connection, statement, "orders", "completed_time", "ALTER TABLE `orders` ADD COLUMN `completed_time` DATETIME DEFAULT NULL AFTER `shipped_time`");
    }

    private void ensureUserAddressTable(Statement statement) throws SQLException {
        statement.execute("""
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
                  CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void ensureSearchLogTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `search_log` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `keyword` VARCHAR(100) NOT NULL,
                  `search_count` INT DEFAULT 1,
                  `last_search_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  UNIQUE KEY `uk_search_keyword` (`keyword`),
                  KEY `idx_search_count_time` (`search_count`, `last_search_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void ensureMediumFeatureTables(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `user_message` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `type` VARCHAR(30) DEFAULT 'system',
                  `title` VARCHAR(100) NOT NULL,
                  `content` VARCHAR(500) NOT NULL,
                  `read_status` TINYINT DEFAULT 0,
                  `read_time` DATETIME DEFAULT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  KEY `idx_message_user_read_time` (`user_id`, `read_status`, `create_time`),
                  CONSTRAINT `fk_message_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `product_view_history` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `product_id` BIGINT NOT NULL,
                  `view_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY `uk_view_user_product` (`user_id`, `product_id`),
                  KEY `idx_view_user_time` (`user_id`, `view_time`),
                  CONSTRAINT `fk_view_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                  CONSTRAINT `fk_view_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `after_sale` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `order_id` BIGINT NOT NULL,
                  `order_no` VARCHAR(50) NOT NULL,
                  `type` VARCHAR(20) NOT NULL,
                  `reason` VARCHAR(500) NOT NULL,
                  `amount` DECIMAL(10,2) DEFAULT 0,
                  `status` TINYINT DEFAULT 0 COMMENT '0:待审核 1:已同意 2:已拒绝 3:已完成',
                  `audit_remark` VARCHAR(500) DEFAULT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY `idx_after_sale_user_time` (`user_id`, `create_time`),
                  KEY `idx_after_sale_order` (`order_id`),
                  CONSTRAINT `fk_after_sale_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                  CONSTRAINT `fk_after_sale_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `coupon_template` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `name` VARCHAR(100) NOT NULL,
                  `description` VARCHAR(255),
                  `discount_amount` DECIMAL(10,2) NOT NULL,
                  `min_amount` DECIMAL(10,2) DEFAULT 0,
                  `valid_days` INT DEFAULT 30,
                  `status` TINYINT DEFAULT 1,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY `idx_coupon_template_status` (`status`, `create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `user_invoice` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `title` VARCHAR(100) NOT NULL,
                  `tax_no` VARCHAR(50) DEFAULT NULL,
                  `is_default` TINYINT DEFAULT 0,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY `idx_invoice_user_default` (`user_id`, `is_default`),
                  CONSTRAINT `fk_invoice_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `ai_knowledge_base` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `category` VARCHAR(50) NOT NULL,
                  `title` VARCHAR(120) NOT NULL,
                  `question` VARCHAR(255) DEFAULT NULL,
                  `answer` TEXT NOT NULL,
                  `keywords` VARCHAR(255) DEFAULT NULL,
                  `status` TINYINT DEFAULT 1,
                  `sort_order` INT DEFAULT 100,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY `idx_kb_status_sort` (`status`, `sort_order`),
                  KEY `idx_kb_category` (`category`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `customer_ticket` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `assignee_id` BIGINT DEFAULT NULL,
                  `ticket_no` VARCHAR(40) NOT NULL,
                  `source` VARCHAR(30) DEFAULT 'user',
                  `type` VARCHAR(30) DEFAULT 'general',
                  `title` VARCHAR(150) NOT NULL,
                  `content` TEXT NOT NULL,
                  `order_no` VARCHAR(50) DEFAULT NULL,
                  `priority` TINYINT DEFAULT 1,
                  `status` TINYINT DEFAULT 0,
                  `resolution` TEXT,
                  `satisfaction` TINYINT DEFAULT NULL,
                  `satisfaction_remark` VARCHAR(500) DEFAULT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `closed_time` DATETIME DEFAULT NULL,
                  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
                  KEY `idx_ticket_user_time` (`user_id`, `create_time`),
                  KEY `idx_ticket_status_time` (`status`, `create_time`),
                  KEY `idx_ticket_assignee` (`assignee_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `media_asset` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT DEFAULT NULL,
                  `provider` VARCHAR(30) NOT NULL DEFAULT 'local',
                  `bucket` VARCHAR(120) DEFAULT NULL,
                  `object_key` VARCHAR(500) NOT NULL,
                  `url` VARCHAR(800) NOT NULL,
                  `original_name` VARCHAR(255) DEFAULT NULL,
                  `content_type` VARCHAR(100) DEFAULT NULL,
                  `size` BIGINT DEFAULT 0,
                  `biz_type` VARCHAR(50) DEFAULT 'media',
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  KEY `idx_media_biz_time` (`biz_type`, `create_time`),
                  KEY `idx_media_provider_time` (`provider`, `create_time`),
                  KEY `idx_media_user_time` (`user_id`, `create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `campaign_event` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT DEFAULT NULL,
                  `campaign_id` BIGINT DEFAULT NULL,
                  `campaign_type` VARCHAR(50) NOT NULL,
                  `campaign_name` VARCHAR(120) DEFAULT NULL,
                  `event_type` VARCHAR(30) NOT NULL,
                  `related_id` BIGINT DEFAULT NULL,
                  `amount` DECIMAL(10,2) DEFAULT 0,
                  `metadata` VARCHAR(1000) DEFAULT NULL,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  KEY `idx_campaign_type_event_time` (`campaign_type`, `event_type`, `create_time`),
                  KEY `idx_campaign_id_time` (`campaign_id`, `create_time`),
                  KEY `idx_campaign_user_time` (`user_id`, `create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS `risk_event` (
                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                  `user_id` BIGINT DEFAULT NULL,
                  `username` VARCHAR(80) DEFAULT NULL,
                  `ip` VARCHAR(64) DEFAULT NULL,
                  `event_type` VARCHAR(50) NOT NULL,
                  `risk_level` VARCHAR(20) DEFAULT 'medium',
                  `description` VARCHAR(500) DEFAULT NULL,
                  `path` VARCHAR(255) DEFAULT NULL,
                  `method` VARCHAR(20) DEFAULT NULL,
                  `metadata` VARCHAR(1000) DEFAULT NULL,
                  `status` TINYINT DEFAULT 0,
                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY `idx_risk_status_time` (`status`, `create_time`),
                  KEY `idx_risk_type_level_time` (`event_type`, `risk_level`, `create_time`),
                  KEY `idx_risk_user_time` (`user_id`, `create_time`),
                  KEY `idx_risk_ip_time` (`ip`, `create_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        seedKnowledgeBase(statement);
    }

    private void seedKnowledgeBase(Statement statement) throws SQLException {
        statement.execute("""
                INSERT INTO `ai_knowledge_base` (`category`, `title`, `question`, `answer`, `keywords`, `status`, `sort_order`)
                SELECT '售后政策', '售后退换政策', '如何申请退款退货换货？',
                       '订单支付后如遇未发货、商品质量问题或规格不符，可在“我的订单”中申请退款、退货或换货。平台会根据订单状态和售后原因进行审核，审核结果可在个人中心的售后进度查看。',
                       '售后,退款,退货,换货,维权,质量问题', 1, 10
                WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_base` WHERE `title` = '售后退换政策')
                """);
        statement.execute("""
                INSERT INTO `ai_knowledge_base` (`category`, `title`, `question`, `answer`, `keywords`, `status`, `sort_order`)
                SELECT '优惠券规则', '优惠券使用规则', '优惠券怎么使用？',
                       '可用优惠券会在结算页自动匹配，满足门槛后可抵扣订单金额。每张券有有效期、使用门槛和状态限制；订单取消后，已使用的优惠券会自动恢复。',
                       '优惠券,满减,领取,使用,抵扣,有效期', 1, 20
                WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_base` WHERE `title` = '优惠券使用规则')
                """);
        statement.execute("""
                INSERT INTO `ai_knowledge_base` (`category`, `title`, `question`, `answer`, `keywords`, `status`, `sort_order`)
                SELECT '物流说明', '配送与物流说明', '什么时候发货，怎么查物流？',
                       '支付成功后平台会进入备货流程，后台发货后订单页会展示物流状态。当前演示环境使用模拟物流，真实快递公司轨迹可在后续接入物流 API 后展示。',
                       '物流,发货,配送,运单,快递,到货', 1, 30
                WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_base` WHERE `title` = '配送与物流说明')
                """);
        statement.execute("""
                INSERT INTO `ai_knowledge_base` (`category`, `title`, `question`, `answer`, `keywords`, `status`, `sort_order`)
                SELECT '发票规则', '发票信息维护', '发票抬头在哪里维护？',
                       '用户可在个人中心维护发票抬头和税号，结算时选择对应发票信息后会写入订单。如订单已提交后需要修改发票信息，建议创建人工客服工单处理。',
                       '发票,抬头,税号,开票,企业', 1, 40
                WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_base` WHERE `title` = '发票信息维护')
                """);
        statement.execute("""
                INSERT INTO `ai_knowledge_base` (`category`, `title`, `question`, `answer`, `keywords`, `status`, `sort_order`)
                SELECT '账号安全', '账号安全与登录提醒', '如何保护账号安全？',
                       '建议定期修改密码，开启两步验证，并关注登录提醒消息。如果发现异常登录或不是本人操作，请及时修改密码并联系人工客服。',
                       '账号安全,密码,两步验证,登录提醒,异常登录', 1, 50
                WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_base` WHERE `title` = '账号安全与登录提醒')
                """);
        statement.execute("""
                INSERT INTO `ai_knowledge_base` (`category`, `title`, `question`, `answer`, `keywords`, `status`, `sort_order`)
                SELECT '会员积分', '会员积分规则', '积分怎么获得和使用？',
                       '确认收货后会根据订单实付金额增加积分，积分可在个人中心兑换优惠券。会员等级会随累计积分提升，后续可扩展更多会员权益。',
                       '会员,积分,等级,兑换,权益', 1, 60
                WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_base` WHERE `title` = '会员积分规则')
                """);
    }

    private void addColumnIfMissing(Connection connection, Statement statement, String tableName, String columnName, String alterSql) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            statement.execute(alterSql);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, columnName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?")) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, indexName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
