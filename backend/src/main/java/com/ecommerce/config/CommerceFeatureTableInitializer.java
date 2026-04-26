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
