-- 如果数据库不存在则创建
CREATE DATABASE IF NOT EXISTS `model_service_new` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 切换到新数据库
USE `model_service_new`;

-- 1. 用户表 (替代原 admins 表)
CREATE TABLE `users` (
                         `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                         `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                         `password_hash` VARCHAR(255) NOT NULL COMMENT 'Bcrypt加密后的密码哈希',
                         `email` VARCHAR(100) UNIQUE COMMENT '电子邮箱',
                         `role` ENUM('ADMIN', 'USER') NOT NULL COMMENT '用户角色: ADMIN-管理员, USER-普通用户',
                         `balance` DECIMAL(10, 4) NOT NULL DEFAULT 0.0000 COMMENT '用户账户余额',
                         `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账户状态: 0-禁用, 1-可用',
                         `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户表';

-- 2. 模型提供商表 (厂商表)
CREATE TABLE `providers` (
                             `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                             `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '提供商名称, 如 OpenAI, Aliyun',
                             `url_base` VARCHAR(255) COMMENT '提供商API的基础URL',
                             `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='模型提供商表';

-- 3. 提供商密钥表
CREATE TABLE `provider_keys` (
                                 `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                 `provider_id` INT NOT NULL COMMENT '关联的提供商ID',
                                 `api_key` VARCHAR(255) NOT NULL COMMENT '提供商的API Key (建议生产环境加密存储)',
                                 `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '密钥状态: 0-禁用, 1-可用',
                                 `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='提供商密钥表';

-- 4. 大语言模型表
CREATE TABLE `llm_models` (
                              `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                              `provider_id` INT NOT NULL COMMENT '关联的提供商ID',
                              `display_name` VARCHAR(100) NOT NULL COMMENT '模型显示名称',
                              `model_identifier` VARCHAR(100) NOT NULL UNIQUE COMMENT '模型唯一标识符 (用于API调用)',
                              `priority` TINYINT NOT NULL DEFAULT 99 COMMENT '优先级, 1-99, 数字越小优先级越高',
                              `capabilities` JSON NOT NULL COMMENT '模型能力, e.g., ["text-to-text", "text-to-image","image-to-text"]',
                              `pricing` JSON NOT NULL COMMENT '计费标准, e.g., {"input_tokens": 0.05, "output_tokens": 0.15, "image_generation": 2.0}',
                              `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '模型状态: 0-下线, 1-上线',
                              `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='大语言模型表';

-- 5. 用户访问密钥表 (Access Keys)
CREATE TABLE `access_keys` (
                               `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                               `key_value` VARCHAR(255) NOT NULL UNIQUE COMMENT 'Access Key的值',
                               `user_id` INT NOT NULL COMMENT '所属用户ID',
                               `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '密钥状态: 0-禁用, 1-可用',
                               `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='用户访问密钥表';

-- 6. 用量与计费日志表
CREATE TABLE `usage_logs` (
                              `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                              `user_id` INT NOT NULL COMMENT '用户ID',
                              `access_key_id` INT NOT NULL COMMENT '使用的Access Key ID',
                              `model_id` INT NOT NULL COMMENT '使用的模型ID',
                              `prompt_tokens` INT UNSIGNED DEFAULT 0 COMMENT '输入Token数',
                              `completion_tokens` INT UNSIGNED DEFAULT 0 COMMENT '输出Token数',
                              `image_count` INT UNSIGNED DEFAULT 0 COMMENT '生成图片数',
                              `cost` DECIMAL(10, 6) NOT NULL COMMENT '本次调用花费',
                              `request_timestamp` DATETIME(3) NOT NULL COMMENT '请求开始时间',
                              `response_timestamp` DATETIME(3) NOT NULL COMMENT '收到响应时间',
                              `is_success` TINYINT(1) NOT NULL COMMENT '是否成功: 0-失败, 1-成功',
                              `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间'
) ENGINE=InnoDB COMMENT='用量与计费日志表';

-- 7. 订单表
CREATE TABLE `orders` (
                          `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                          `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
                          `user_id` INT NOT NULL COMMENT '用户ID',
                          `amount` DECIMAL(10, 2) NOT NULL COMMENT '充值金额',
                          `status` ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
                          `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户充值订单表';

-- 8. 异步任务表
CREATE TABLE `async_jobs` (
                              `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                              `job_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '公开的任务ID (UUID)',
                              `user_id` INT NOT NULL COMMENT '用户ID',
                              `model_id` INT NOT NULL COMMENT '使用的模型ID',
                              `status` ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
                              `request_payload` JSON COMMENT '原始请求体',
                              `result_payload` JSON COMMENT '成功的结果',
                              `error_message` TEXT COMMENT '失败原因',
                              `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='异步任务表';