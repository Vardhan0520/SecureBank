-- =====================================================
-- Banking System Database Schema
-- =====================================================

CREATE DATABASE IF NOT EXISTS banking_system;
USE banking_system;

-- ---------------------------------------------------
-- USERS  (login credentials, separate from account holder profile)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,   -- salted SHA-256 hash
    salt          VARCHAR(64)  NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) UNIQUE,
    phone         VARCHAR(15),
    role          ENUM('CUSTOMER', 'ADMIN') DEFAULT 'CUSTOMER',
    failed_attempts INT DEFAULT 0,
    locked        BOOLEAN DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------
-- ACCOUNTS  (a user can hold multiple accounts)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    account_id     INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    user_id        INT NOT NULL,
    account_type   ENUM('SAVINGS', 'CURRENT') DEFAULT 'SAVINGS',
    balance        DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status         ENUM('ACTIVE', 'CLOSED', 'FROZEN') DEFAULT 'ACTIVE',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ---------------------------------------------------
-- TRANSACTIONS  (deposits, withdrawals, transfers)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   INT AUTO_INCREMENT PRIMARY KEY,
    account_id       INT NOT NULL,
    related_account_id INT DEFAULT NULL,  -- for transfers
    type             ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_OUT', 'TRANSFER_IN') NOT NULL,
    amount           DECIMAL(15,2) NOT NULL,
    balance_after    DECIMAL(15,2) NOT NULL,
    description      VARCHAR(255),
    timestamp        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (related_account_id) REFERENCES accounts(account_id) ON DELETE SET NULL
);

-- ---------------------------------------------------
-- AUDIT LOG (login attempts, security-relevant events)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    action      VARCHAR(100),
    detail      VARCHAR(255),
    timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Helpful indexes
CREATE INDEX idx_accounts_user ON accounts(user_id);
CREATE INDEX idx_transactions_account ON transactions(account_id);
