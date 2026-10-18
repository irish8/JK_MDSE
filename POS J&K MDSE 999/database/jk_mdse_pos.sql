-- J&K MDSE Store (999) Point of Sale
-- Paste this file into MySQL Workbench or run: mysql -u root -p < jk_mdse_pos.sql
CREATE DATABASE IF NOT EXISTS jk_mdse_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE jk_mdse_pos;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  role ENUM('ADMIN','CASHIER') NOT NULL DEFAULT 'CASHIER',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_users_username (username), KEY ix_users_role_active (role,is_active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  category VARCHAR(100) NOT NULL,
  stock_qty INT UNSIGNED NOT NULL DEFAULT 0,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_products_name (name), KEY ix_products_category (category), KEY ix_products_stock (is_active,stock_qty),
  CONSTRAINT chk_products_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  transaction_no VARCHAR(40) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL,
  discount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total DECIMAL(12,2) NOT NULL,
  amount_paid DECIMAL(12,2) NOT NULL,
  change_due DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_sales_transaction_no (transaction_no),
  KEY ix_sales_created_at (created_at), KEY ix_sales_user_created (user_id,created_at),
  CONSTRAINT fk_sales_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_sales_amounts CHECK (subtotal >= 0 AND discount >= 0 AND total >= 0 AND amount_paid >= total AND change_due >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sale_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NULL,
  product_name VARCHAR(150) NOT NULL,
  quantity INT UNSIGNED NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id), KEY ix_sale_items_sale (sale_id), KEY ix_sale_items_product (product_id),
  CONSTRAINT fk_sale_items_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
  CONSTRAINT fk_sale_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
  CONSTRAINT chk_sale_items_values CHECK (quantity > 0 AND unit_price >= 0 AND line_total >= 0)
) ENGINE=InnoDB;

-- The Java app creates the first admin at startup when users is empty:
-- username: admin   password: Admin999!
-- Sign in and change the password after setup. Passwords are stored as PBKDF2 hashes.
