当然！结合您最新的需求（异步任务API和订单充值系统），这是一个非常全面和深入的项目改造方案。这会让您的项目在技术栈深度、架构设计和业务完整性上都达到一个新的高度。

以下是详细的整体改造方案步骤和所有新表的SQL创建语句。

---

### 项目整体改造方案

我们将项目改造分为六个核心阶段，建议按顺序实施。

#### 阶段一：数据库模型重建 (Foundation)

这是所有改造的基础。我们将废弃旧的表结构，采用新的、高度解耦的数据库设计来支持多用户、多厂商、计费和异步任务。

1.  **用户表 (`users`):** 统一管理用户和管理员，引入角色和余额。
2.  **厂商与密钥表 (`providers`, `provider_keys`):** 将模型提供商（如OpenAI）及其API Key抽象出来，实现一个厂商对应多个Key。
3.  **模型表 (`llm_models`):** 存储模型元数据，并与厂商关联，最重要的是引入`pricing`字段用于计费。
4.  **访问凭证表 (`access_keys`):** 用户生成的API调用凭证，与用户关联。
5.  **订单表 (`orders`):** 用于模拟用户充值流程。
6.  **异步任务表 (`async_jobs`):** 跟踪异步API调用的状态和结果。
7.  **用量日志表 (`usage_logs`):** 记录每一次**同步**调用的成本和用量。

#### 階段二：核心后台与管理员功能 (Admin Backend)

搭建系统的管理后台API，这是运营整个平台的前提。

1.  **重构认证系统:** 实现基于JWT和角色的认证与授权（RBAC）。推荐使用Spring Security。
2.  **实现管理员API:**
    *   **用户管理 (`/v1/admin/users`):** CRUD用户，特别是**修改用户余额**的功能。
    *   **厂商管理 (`/v1/admin/providers`):** CRUD模型提供商。
    *   **厂商密钥管理 (`/v1/admin/providers/{id}/keys`):** 管理特定厂商的API Keys。
    *   **模型管理 (`/v1/admin/models`):** CRUD模型，并设置其计费价格。
    *   **订单管理 (`/v1/admin/orders`):** 查看所有订单，并提供一个**“审核通过”**的接口，用于模拟支付成功并给用户加款。

#### 阶段三：同步代理API重构 (Synchronous Proxy)

在新的数据模型和计费逻辑之上，重构现有的同步代理接口。

1.  **修改 `/v1/chat` 和 `/v1/generate-image` 接口逻辑:**
    *   **认证:** 通过 `ACCESS-KEY` 找到 `user`。
    *   **余额检查:** 调用前检查 `user.balance` 是否充足。
    *   **模型与密钥选择:** 根据请求选择模型，并从该模型所属的厂商密钥池中选择一个可用Key。
    *   **请求转发:** 调用上游大模型API。
    *   **同步计费:** 收到成功响应后，根据`llm_models.pricing`计算费用，**在数据库事务中扣除用户余额**，并向`usage_logs`表插入记录。
    *   **返回结果:** 将结果返回给用户。

#### 阶段四：异步任务系统实现 (Asynchronous Jobs with RabbitMQ)

这是项目的亮点功能，展示了您对消息队列和异步架构的理解。

1.  **引入RabbitMQ依赖:** 在项目中配置RabbitMQ连接。
2.  **设计API端点:**
    *   `POST /v1/jobs/image-generation`: 提交一个文生图任务。
    *   `POST /v1/jobs/chat`: 提交一个对话任务。
    *   `GET /v1/jobs/{jobId}`: 查询任务状态和结果。
3.  **实现任务提交流程 (Producer):**
    *   当用户调用 `POST /v1/jobs/...` 时：
        1.  执行认证和余额检查。
        2.  在 `async_jobs` 表中创建一条记录，状态为 `PENDING`，生成一个唯一的 `job_uid`。
        3.  将任务所需信息（`user_id`, `model_id`, `request_payload`等）打包成消息，发送到指定的RabbitMQ队列（如 `image_job_queue`）。
        4.  立即向用户返回 `{"jobId": "..."}`。
4.  **实现任务处理逻辑 (Consumer):**
    *   创建一个或多个后台监听器（Worker）消费队列中的消息。
    *   Worker处理流程：
        1.  从队列获取消息。
        2.  更新 `async_jobs` 表中对应任务的状态为 `PROCESSING`。
        3.  执行对上游大模型的调用（可复用同步代理的逻辑）。
        4.  **异步计费:** 调用成功后，计算费用，**在事务中扣除用户余额**。
        5.  将结果（或错误信息）存入 `async_jobs` 表的 `result` 字段，并更新状态为 `COMPLETED` 或 `FAILED`。

#### 阶段五：用户中心与充值流程 (User Center & Billing)

为普通用户提供管理自己账户的接口。

1.  **实现用户中心API (`/v1/me/...`):**
    *   `GET /v1/me/profile`: 获取当前用户信息（特别是余额）。
    *   `GET /v1/me/access-keys`, `POST ...`, `DELETE ...`: 管理自己的Access Keys。
    *   `GET /v1/me/usage`: 查询自己的同步调用历史（查`usage_logs`）。
    *   `GET /v1/me/jobs`: 查询自己的异步任务历史（查`async_jobs`）。
    *   `GET /v1/me/orders`: 查询自己的充值订单历史。
2.  **实现充值流程:**
    *   `POST /v1/me/orders`: 用户调用此接口创建充值订单，传入金额。系统在 `orders` 表创建一条 `PENDING` 状态的记录。
    *   等待管理员在后台通过 `POST /v1/admin/orders/{orderId}/approve` 接口审批。该接口将订单状态改为 `COMPLETED` 并为用户增加余额。

#### 阶段六：性能优化 (Optimization)

在核心功能完成后，加入缓存提升性能。

1.  **引入Redis:**
    *   **缓存Access Key:** 将 `access_key -> user_info` 的映射关系缓存在Redis中，避免每次API调用都查询数据库。
    *   **缓存用户信息:** 缓存用户的基本信息和余额，但在扣款时需注意数据一致性问题（可使用Lua脚本或分布式锁保证原子性，或直接操作数据库）。

---

### 全新数据库表SQL创建语句

```sql
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
```