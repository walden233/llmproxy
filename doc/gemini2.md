当然！结合您提出的异步模型调用和订单充值功能，这是一个非常全面且有深度的改造方案。这会让您的项目从一个简单的代理服务，演进为一个具备异步处理能力、模拟计费闭环的微型AI PaaS平台。

以下是为您量身定制的整体改造方案，分为六个清晰的步骤。

---

### 整体架构概览 (改造后)

```
                                     +-------------------------+
                                     |         User / Client   |
                                     +-------------------------+
                                                |
                                                | (REST API Calls)
                                                v
+--------------------------------------------------------------------------------------------------+
|                                        LLM Proxy Backend (Your Application)                      |
|                                                                                                  |
|  +---------------------+   +-----------------------+   +----------------------+   +-------------+  |
|  |   Security/Auth     |-->|     API Gateway       |-->|   Controller Layer   |-->|   Service   |  |
|  | (Spring Security)   |   | (Role-based Routing)  |   | (Endpoints)          |   |   Layer     |  |
|  +---------------------+   +-----------------------+   +----------------------+   +-------------+  |
|                                                                                       |     |      |
|                               +-------------------------------------------------------+     |      |
|                               | (Sync)                                                      | (Async)
|                               v                                                             v      |
|  +--------------------------------------------------+        +-----------------------------------+ |
|  |  Synchronous Flow (e.g., /v1/chat)               |        |  Asynchronous Flow                | |
|  |  1. Pre-check Balance                            |        |  1. Create Job in DB              | |
|  |  2. Proxy to LLM                                 |        |  2. Publish to RabbitMQ           | |
|  |  3. Get Response                                 |        |  3. Return JobID immediately      | |
|  |  4. Publish Billing Message to RabbitMQ          |        +-----------------------------------+ |
|  +--------------------------------------------------+                                                |
+------------------------------------------------|---------------------------------------------------+
                                                 |
                                                 v
+--------------------------------------------------------------------------------------------------+
|                                        Messaging & Worker Layer                                  |
|                                                                                                  |
|                               +----------------------------------+                               |
|                               |            RabbitMQ              |                               |
|                               |                                  |                               |
|                               |  [model_invocation_queue]        |                               |
|                               |  [billing_log_queue]             |                               |
|                               +----------------------------------+                               |
|                                      ^                ^                                          |
|                               (Consume)|                |(Consume)                               |
|                                      |                |                                          |
|  +-----------------------------------+                +------------------------------------+     |
|  |  Model Invocation Worker           |                |  Billing & Logging Worker          |     |
|  |  - Calls external LLM API          |                |  - Updates User Balance            |     |
|  |  - Updates Job Status in DB        |                |  - Writes to usage_logs            |     |
|  |  - Publishes to Billing Queue      |                +------------------------------------+     |
|  +-----------------------------------+                                                         |
+--------------------------------------------------------------------------------------------------+
```

---

### 改造方案步骤

#### Phase 1: 数据库模型大修 (Foundation)

这是所有改造的基础。您需要添加 `orders` 和 `async_jobs` 表，并重构现有表。

1.  **`users` 表**: 新增 `role` (ENUM 'admin', 'user') 和 `balance` (DECIMAL) 字段。
2.  **`providers`, `provider_keys`, `llm_models` 表**: 按上一轮建议进行解耦设计，`llm_models` 表增加 `pricing` (JSON) 字段。
3.  **`access_keys` 表**: 关联到 `user_id` 而非 `admin_id`。
4.  **`usage_logs` 表**: 记录每次**成功调用**的成本和用量。
5.  **新增 `orders` 表**: 用于模拟充值。
    *   `id` (PK)
    *   `order_no` (VARCHAR, UNIQUE) - 订单号
    *   `user_id` (FK -> users.id)
    *   `amount` (DECIMAL) - 充值金额
    *   `status` (ENUM 'PENDING', 'COMPLETED', 'FAILED')
    *   `created_at`, `updated_at`
6.  **新增 `async_jobs` 表**: 用于跟踪异步任务。
    *   `id` (PK)
    *   `job_id` (VARCHAR, UNIQUE) - 公开给用户的任务ID (建议用UUID)
    *   `user_id` (FK -> users.id)
    *   `model_id` (FK -> llm_models.id)
    *   `status` (ENUM 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    *   `request_payload` (JSON) - 存储原始请求体
    *   `result_payload` (JSON, NULLABLE) - 存储成功的结果
    *   `error_message` (TEXT, NULLABLE) - 存储失败原因
    *   `created_at`, `updated_at`

#### Phase 2: 核心逻辑与安全重构 (Core Logic)

1.  **引入 Spring Security**:
    *   配置JWT认证，从Token中解析出 `userId` 和 `role`。
    *   配置基于角色的授权。使用 `@PreAuthorize("hasRole('ADMIN')")` 和 `@PreAuthorize("hasRole('USER')")` 注解保护相应的Controller方法。
2.  **重构认证模块**:
    *   `login` 接口返回用户信息，包括角色和余额。
3.  **开发管理员后台API**:
    *   实现对 `users`, `providers`, `provider_keys`, `llm_models` 的完整CRUD操作。
    *   提供一个接口为用户手动修改余额（用于测试和管理）。

#### Phase 3: 同步代理流程与计费集成 (Sync Flow)

在这一步，我们先让同步调用（如 `/v1/chat`）跑通新的计费逻辑，但计费本身是异步的。

1.  **重构 `/v1/chat` 接口**:
    *   **认证**: 通过请求头中的 `ACCESS-KEY` 找到 `user_id`。
    *   **余额预检**: 查询用户余额，如果不足则直接拒绝。
    *   **模型与密钥选择**: 根据请求选择模型，并从 `provider_keys` 中获取一个可用的 `api_key`。
    *   **请求转发**: 调用上游大模型服务。
    *   **异步计费**:
        *   如果调用成功，从响应中解析用量（tokens等）。
        *   根据用量和 `llm_models.pricing` 计算费用 `cost`。
        *   **将计费信息（`user_id`, `cost`, `model_id`, 用量详情等）打包成一个消息，发送到 RabbitMQ 的 `billing_log_queue` 队列。**
        *   立即将模型响应返回给用户。

2.  **创建计费日志消费者 (Billing Worker)**:
    *   这是一个独立的监听服务。
    *   它消费 `billing_log_queue` 中的消息。
    *   收到消息后，执行一个数据库事务：
        1.  `UPDATE users SET balance = balance - ? WHERE id = ?`
        2.  `INSERT INTO usage_logs (...) VALUES (...)`

#### Phase 4: 异步任务API与处理流程 (Async Flow)

这是本次改造的核心亮点。

1.  **创建任务提交API**: `POST /v1/jobs/image-generation`
    *   **认证与余额预检**: 同上。
    *   **创建任务记录**: 在 `async_jobs` 表中插入一条新记录，`status` 为 `PENDING`，并生成一个唯一的 `job_id`。
    *   **发送任务消息**: 将 `job_id` 作为消息体，发送到 RabbitMQ 的 `model_invocation_queue` 队列。
    *   **立即响应**: 向用户返回 `202 Accepted` 状态码，并在响应体中包含 `job_id`。

2.  **创建模型调用消费者 (Invocation Worker)**:
    *   监听 `model_invocation_queue` 队列。
    *   收到 `job_id` 后：
        1.  查询 `async_jobs` 表，获取任务详情，并将 `status` 更新为 `PROCESSING`。
        2.  执行对上游大模型（如文生图）的API调用。
        3.  **调用成功**:
            *   将返回的图片URL等结果存入 `async_jobs.result_payload` 字段。
            *   将 `status` 更新为 `COMPLETED`。
            *   **触发计费**: 计算费用，并向 `billing_log_queue` 发送一条计费消息。
        4.  **调用失败**:
            *   将错误信息存入 `async_jobs.error_message` 字段。
            *   将 `status` 更新为 `FAILED`。

3.  **创建任务状态查询API**: `GET /v1/jobs/{jobId}`
    *   用户通过此接口轮询任务结果。
    *   接口根据 `jobId` 查询 `async_jobs` 表。
    *   返回任务的当前状态 (`status`)。如果状态是 `COMPLETED`，则同时返回 `result_payload`；如果是 `FAILED`，则返回 `error_message`。

#### Phase 5: 用户充值与订单流程 (Billing Cycle)

1.  **创建订单API**: `POST /v1/me/orders`
    *   需要 `role=USER` 权限。
    *   请求体包含 `amount`。
    *   在 `orders` 表中创建一条 `status='PENDING'` 的记录。
    *   返回 `order_no`。

2.  **模拟支付成功API**: `POST /v1/orders/{orderNo}/complete`
    *   这是一个**模拟接口**，用于测试。在实际生产中，这应该是一个由支付网关回调的内部接口。
    *   为方便测试，可以设置为 `role=ADMIN` 才能调用。
    *   执行一个数据库事务：
        1.  `UPDATE orders SET status = 'COMPLETED' WHERE order_no = ?`
        2.  `UPDATE users SET balance = balance + ? WHERE id = ?` (根据订单信息)

3.  **查询订单历史API**: `GET /v1/me/orders`
    *   需要 `role=USER` 权限。
    *   查询并返回当前用户的所有订单记录。

#### Phase 6: API文档与收尾 (Finalization)

1.  **全面更新API文档**:
    *   添加所有新接口的说明（管理员API、用户订单API、异步任务API）。
    *   修改现有接口的说明，特别是认证方式（Bearer Token vs Access Key）和响应体变化。
    *   明确区分同步API和异步API的调用方式和预期响应。
2.  **配置与测试**:
    *   配置 RabbitMQ 连接信息。
    *   编写单元测试和集成测试，覆盖新流程，特别是事务和异步消息的正确性。

通过以上六个阶段的改造，您的项目将具备一个清晰、健壮且功能丰富的架构，足以在简历上作为一个高质量的代表作。