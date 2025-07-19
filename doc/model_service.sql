CREATE TABLE admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL COMMENT 'Stores Bcrypt or Argon2 hash of the password',
    email VARCHAR(100) NULL UNIQUE,
    status INT NOT NULL DEFAULT 1 COMMENT '0: unavailable, 1: available',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE llm_models (
    id INT AUTO_INCREMENT PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL COMMENT 'User-friendly name shown in UI',
    model_identifier VARCHAR(100) NOT NULL UNIQUE COMMENT 'Actual model ID for API calls (e.g., gpt-4o)',
    url_base VARCHAR(255) COMMENT 'Base URL for the model API endpoint',
    api_key VARCHAR(255) NOT NULL COMMENT 'API key for accessing the model. Consider encrypting at rest.',
    capabilities JSON NOT NULL COMMENT 'JSON array of supported features, e.g., ["text-to-text"]',
    priority INT NOT NULL DEFAULT 99 COMMENT 'Lower number means higher priority',
    status INT NOT NULL DEFAULT 1 COMMENT '0: offline, 1: online',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `access_keys` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `key_value` VARCHAR(255) NOT NULL COMMENT 'Access Key的值，必须唯一',
    `admin_id` INT NOT NULL COMMENT '创建该Key的管理员ID',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '密钥状态: 1=可用, 0=不可用',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key_value` (`key_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API访问密钥表';