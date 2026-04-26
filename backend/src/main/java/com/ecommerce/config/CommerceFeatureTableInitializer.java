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
                ensureProductImagesColumn(connection, statement);
                ensureProductBrandColumn(connection, statement);
                ensureOrdersColumns(connection, statement);
                ensureUserAddressTable(statement);
                ensureSearchLogTable(statement);
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

    private void ensureOrdersColumns(Connection connection, Statement statement) throws SQLException {
        addColumnIfMissing(connection, statement, "orders", "original_amount", "ALTER TABLE `orders` ADD COLUMN `original_amount` DECIMAL(10,2) DEFAULT NULL AFTER `total_amount`");
        addColumnIfMissing(connection, statement, "orders", "discount_amount", "ALTER TABLE `orders` ADD COLUMN `discount_amount` DECIMAL(10,2) DEFAULT 0 AFTER `original_amount`");
        addColumnIfMissing(connection, statement, "orders", "shipping_fee", "ALTER TABLE `orders` ADD COLUMN `shipping_fee` DECIMAL(10,2) DEFAULT 0 AFTER `discount_amount`");
        addColumnIfMissing(connection, statement, "orders", "coupon_id", "ALTER TABLE `orders` ADD COLUMN `coupon_id` BIGINT DEFAULT NULL AFTER `shipping_fee`");
        addColumnIfMissing(connection, statement, "orders", "coupon_name", "ALTER TABLE `orders` ADD COLUMN `coupon_name` VARCHAR(100) DEFAULT NULL AFTER `coupon_id`");
        addColumnIfMissing(connection, statement, "orders", "payment_method", "ALTER TABLE `orders` ADD COLUMN `payment_method` VARCHAR(50) DEFAULT NULL AFTER `remark`");
        addColumnIfMissing(connection, statement, "orders", "invoice_title", "ALTER TABLE `orders` ADD COLUMN `invoice_title` VARCHAR(100) DEFAULT NULL AFTER `payment_method`");
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
}
