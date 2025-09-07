-- 创建数据库 (如果不存在)
CREATE DATABASE IF NOT EXISTS model_service_new CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE model_service_new;

-- 1. 用户表 (替代旧的 admins 表)
CREATE TABLE `users` (
                         `id` INT AUTO_INCREMENT PRIMARY KEY,
                         `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                         `password_hash` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
                         `email` VARCHAR(100) UNIQUE COMMENT '邮箱',
                         `role` ENUM('admin', 'user') NOT NULL DEFAULT 'user' COMMENT '角色: admin-管理员, user-普通用户',
                         `balance` DECIMAL(10, 4) NOT NULL DEFAULT 0.0000 COMMENT '用户账户余额',
                         `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 1-可用, 0-禁用',
                         `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

-- 2. 模型提供商表 (厂商表)
CREATE TABLE `providers` (
                             `id` INT AUTO_INCREMENT PRIMARY KEY,
                             `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '厂商名称, e.g., OpenAI, Aliyun, Volcengine',
                             `api_base_url` VARCHAR(255) COMMENT '厂商官方API的基础URL',
                             `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='模型提供商表';

-- 3. 厂商API密钥表
CREATE TABLE `provider_keys` (
                                 `id` INT AUTO_INCREMENT PRIMARY KEY,
                                 `provider_id` INT NOT NULL COMMENT '关联的厂商ID',
                                 `api_key` VARCHAR(512) NOT NULL COMMENT '厂商提供的API Key (建议加密存储)',
                                 `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 1-可用, 0-禁用',
                                 `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='厂商API密钥表';

-- 4. 大语言模型表
CREATE TABLE `llm_models` (
                              `id` INT AUTO_INCREMENT PRIMARY KEY,
                              `provider_id` INT NOT NULL COMMENT '该模型所属的厂商ID',
                              `display_name` VARCHAR(100) NOT NULL COMMENT '模型显示名称, e.g., GPT-4o',
                              `model_identifier` VARCHAR(100) NOT NULL UNIQUE COMMENT '模型唯一标识, e.g., gpt-4o',
                              `capabilities` JSON NOT NULL COMMENT '模型能力, e.g., ["chat", "vision", "text-to-image"]',
                              `pricing` JSON NOT NULL COMMENT '计费标准, e.g., {"input_tokens": 0.05, "output_tokens": 0.15, "image_generation": 2.0}',
                              `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 1-上线, 0-下线',
                              `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='大语言模型表';

-- 5. 用户访问凭证表
CREATE TABLE `access_keys` (
                               `id` INT AUTO_INCREMENT PRIMARY KEY,
                               `key_value` VARCHAR(255) NOT NULL UNIQUE COMMENT 'Access Key的值, e.g., sk-xxxx',
                               `user_id` INT NOT NULL COMMENT '创建该Key的用户ID',
                               `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '密钥状态: 1-可用, 0-不可用',
                               `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户API访问凭证表';

-- 6. 订单表 (用于充值)
CREATE TABLE `orders` (
                          `id` INT AUTO_INCREMENT PRIMARY KEY,
                          `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
                          `user_id` INT NOT NULL COMMENT '关联的用户ID',
                          `amount` DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
                          `status` ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
                          `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='充值订单表';

-- 7. 异步任务表
CREATE TABLE `async_jobs` (
                              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                              `job_uid` VARCHAR(64) NOT NULL UNIQUE COMMENT '任务的唯一ID，用于用户查询',
                              `user_id` INT NOT NULL COMMENT '提交任务的用户ID',
                              `model_id` INT NOT NULL COMMENT '任务使用的模型ID',
                              `status` ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
                              `request_payload` JSON NOT NULL COMMENT '原始的请求体',
                              `result` JSON COMMENT '任务成功的结果或失败的错误信息',
                              `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='异步任务表';

-- 8. 同步调用用量日志表
CREATE TABLE `usage_logs` (
                              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                              `user_id` INT NOT NULL,
                              `access_key_id` INT NOT NULL,
                              `model_id` INT NOT NULL,
                              `prompt_tokens` INT DEFAULT 0 COMMENT '输入Token数',
                              `completion_tokens` INT DEFAULT 0 COMMENT '输出Token数',
                              `image_count` INT DEFAULT 0 COMMENT '生成图片数',
                              `cost` DECIMAL(10, 6) NOT NULL COMMENT '本次调用花费',
                              `is_success` TINYINT(1) NOT NULL COMMENT '调用是否成功',
                              `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='同步调用用量日志表';