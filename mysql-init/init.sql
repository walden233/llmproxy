-- 如果数据库不存在则创建
CREATE DATABASE IF NOT EXISTS `model_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 切换到新数据库
USE `model_service`;

-- 1. 用户表 (替代原 admins 表)
CREATE TABLE `users` (
                         `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                         `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                         `password_hash` VARCHAR(255) NOT NULL COMMENT 'Bcrypt加密后的密码哈希',
                         `email` VARCHAR(100) UNIQUE COMMENT '电子邮箱',
                         `role` VARCHAR(100) NOT NULL COMMENT '用户角色: ADMIN-管理员, USER-普通用户',
                         `balance` DECIMAL(10, 4) NOT NULL DEFAULT 0.0000 COMMENT '用户账户余额',
                         `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账户状态: 0-禁用, 1-可用',
                         `version` INT NOT NULL DEFAULT 1 COMMENT '版本号，用于乐观锁',
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

-- 6.1 计费流水表
CREATE TABLE `billing_ledger` (
                                  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                  `request_id` VARCHAR(128) NOT NULL COMMENT '幂等请求ID',
                                  `user_id` INT NOT NULL COMMENT '用户ID',
                                  `access_key_id` INT DEFAULT NULL COMMENT 'Access Key ID',
                                  `model_id` INT DEFAULT NULL COMMENT '模型ID',
                                  `prompt_tokens` INT UNSIGNED DEFAULT NULL COMMENT '输入Token数',
                                  `completion_tokens` INT UNSIGNED DEFAULT NULL COMMENT '输出Token数',
                                  `image_count` INT UNSIGNED DEFAULT NULL COMMENT '生成图片数',
                                  `amount` DECIMAL(10, 6) NOT NULL COMMENT '扣费/充值金额，正为充值，负为扣费',
                                  `status` VARCHAR(32) NOT NULL COMMENT '状态: INIT/SETTLED',
                                  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型: CHAT/IMAGE/TOPUP',
                                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  UNIQUE KEY `uk_request_id` (`request_id`),
                                  KEY `idx_user_id` (`user_id`),
                                  KEY `idx_access_key_id` (`access_key_id`)
) ENGINE=InnoDB COMMENT='计费流水表';

-- 7. 订单表
CREATE TABLE `orders` (
                          `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                          `order_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
                          `user_id` INT NOT NULL COMMENT '用户ID',
                          `amount` DECIMAL(10, 2) NOT NULL COMMENT '充值金额',
                          `status` ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
                          `version` INT NOT NULL DEFAULT 1 COMMENT '版本号，用于乐观锁',
                          `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户充值订单表';

-- 8. 异步任务表
CREATE TABLE `async_jobs` (
                              `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                              `job_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '公开的任务ID (UUID)',
                              `user_id` INT NOT NULL COMMENT '用户ID',
                              `access_key_id` INT COMMENT '使用的Access Key ID',
                              `model_name` VARCHAR(100) COMMENT '使用的模型名',
                              `status` ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
                              `request_payload` JSON COMMENT '原始请求体',
                              `result_payload` JSON COMMENT '成功的结果',
                              `error_message` TEXT COMMENT '失败原因',
                              `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='异步任务表';

-- 8.1 异步任务 Outbox 表
CREATE TABLE `async_task_outbox` (
                                     `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                     `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
                                     `exchange` VARCHAR(128) NOT NULL COMMENT '交换机',
                                     `routing_key` VARCHAR(128) NOT NULL COMMENT '路由键',
                                     `payload` TEXT NOT NULL COMMENT '消息体JSON',
                                     `status` VARCHAR(32) NOT NULL COMMENT '状态: PENDING/SENT/FAILED',
                                     `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
                                     `next_retry_at` DATETIME NOT NULL COMMENT '下次重试时间',
                                     `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     KEY `idx_job_id` (`job_id`),
                                     KEY `idx_status_retry` (`status`, `next_retry_at`)
) ENGINE=InnoDB COMMENT='异步任务 Outbox 表';

CREATE TABLE `model_daily_stats` (
                                     `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                     `model_id` INT NOT NULL COMMENT '模型ID，关联模型表的主键',
                                     `model_identifier` VARCHAR(100) NOT NULL COMMENT '模型唯一标识，冗余字段方便查询',
                                     `stat_date` DATE NOT NULL COMMENT '统计日期',
                                     `total_requests` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当天总请求数',
                                     `success_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当天成功请求数',
                                     `failure_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当天失败请求数',
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     UNIQUE KEY `uk_model_date` (`model_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型每日用量统计表';

-- 9. 用户会话元数据表（用于最近会话列表）
CREATE TABLE `conversation_sessions` (
                                         `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                         `conversation_id` VARCHAR(64) NOT NULL COMMENT '对外暴露的会话ID',
                                         `user_id` INT NOT NULL COMMENT '所属用户ID',
                                         `access_key_id` INT DEFAULT NULL COMMENT '最近一次使用的AccessKey ID',
                                         `title` VARCHAR(255) DEFAULT NULL COMMENT '会话标题，可空',
                                         `pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
                                         `last_model_identifier` VARCHAR(100) DEFAULT NULL COMMENT '最近一次使用的模型标识',
                                         `last_message_summary` VARCHAR(500) DEFAULT NULL COMMENT '最近一条消息摘要，便于列表展示',
                                         `message_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计消息条数（用户+助手）',
                                         `last_active_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近活跃时间，用于排序',
                                         `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-删除',
                                         UNIQUE KEY `uk_conversation_id` (`conversation_id`),
                                         KEY `idx_user_last_active` (`user_id`, `last_active_at`),
                                         KEY `idx_access_key` (`access_key_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话元数据表';
