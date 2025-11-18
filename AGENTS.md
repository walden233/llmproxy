# AGENTS

## 项目全景

- **定位**：面向多模型聚合的 Spring Boot 后端，整合 LangChain4j、阿里 Dashscope、火山 Ark 等能力，对外提供统一的聊天/图像/异步 API，并负责鉴权、余额、统计与订单。
- **核心职责**：  
  1. 统一鉴权（JWT + Access Key）、模型/Key 管理、成本结算；  
  2. 根据能力与优先级选择模型，处理多模态输入、工具调用与 SSE 流式输出；  
  3. 通过 RabbitMQ 驱动异步聊天/图像任务，MySQL+Mongo 双存储统计与风控数据；  
  4. 提供管理端（模型/供应商/订单/统计）接口与配套测试脚本。
- **数据流**：HTTP → Controller → Service（模型选择、扣费、统计）→ LangChain4j / Dashscope / Ark / MQ → 统计入库 → Result/ SSE 返回。失败场景配合熔断与异常追踪。

## 技术与运行环境

| 领域 | 选型 |
|------|------|
| 核心框架 | Spring Boot 3.x、Spring Security、Spring Validation |
| 数据访问 | MyBatis-Plus、Jackson、MongoRepository |
| 模型调用 | LangChain4j (Chat + Streaming)、Dashscope、Volcengine Ark |
| 中间件 | MySQL、Redis、RabbitMQ、MongoDB |
| 监控/日志 | Logback、AOP (LoggingAspect / StatisticsAspect) |
| 构建交付 | Maven、多阶段 Dockerfile、Docker Compose (`doc/docker-deployment.md`) |

关键环境变量可通过 `application.yml` 的 `${VAR:default}` 覆盖，例如 `DB_HOST`、`REDIS_HOST`、`RABBIT_HOST`、`MONGODB_URI` 等。运行端口默认 8060。

## 目录与模块

```
src/main/java/cn/tyt/llmproxy
├── aspect          # Logging / Statistics AOP，写 traceId、统计 MySQL/Mongo
├── common          # Result、异常、枚举、JWT、工具
├── config          # Security、Redis Cache、RabbitMQ、MyBatis、Jackson、WebMvc
├── context         # ModelUsageContext（供切面写统计）
├── controller      # Auth / AccessKey / Provider / Model / Order / Proxy / Statistics
├── dto             # 请求/响应 DTO、OpenAI 风格消息、Mongo 文档
├── entity          # MyBatis-Plus 实体（User、Order、LlmModel、AsyncJob...）
├── filter          # `AccessKeyInterceptor`、`JwtAuthenticationTokenFilter`
├── image           # 图像生成抽象与厂商实现 (`AliImageGenerator`, `ArkImageGenerator`)
├── listener        # MQ 监听器（异步任务、订单 TTL）
├── mapper          # MyBatis-Plus Mapper + `ModelDailyStatMapper.xml`
├── repository      # Mongo Repository 封装
├── service         # 业务接口与实现（LangchainProxy、KeySelection、Statistics...）
└── runner          # `AdminInitializer` 启动时创建 ROOT 角色
```

### 控制器速览
- `AuthController`：注册/登录/改密/分配角色（ROOT）。
- `AccessKeyController`：Access Key CRUD，`ACCESS-KEY` 拦截器依赖于此。
- `ProviderController`：供应商及其 Keys 管理。
- `ModelController`：模型 CRUD、状态切换、能力筛选、统计查询。
- `OrderController`：充值订单创建、支付成功、取消。
- `ProxyController`：聊天/图像/异步/流式接口（`/v1/chat`、`/v2/chat`、`/v2/chat/stream`、`/async/*`、图像生成）。
- `StatisticsController`：模型日统计、用户/模型日志、`/logs/query`。

## 关键流程

1. **同步聊天（`/v1/chat`）**  
   AccessKeyInterceptor 校验 Header → `LangchainProxyServiceImpl.chat` 根据能力选模型 → LangChain4j 执行 → 统计/扣费写入 MySQL+Mongo。

2. **OpenAI 风格聊天（`/v2/chat` / `/v2/chat/stream`）**  
   使用 `OpenAiChatRequest` 与 `OpenAiStreamingChatModel`，支持工具调用、图文混输、SSE `[DONE]` 结束、错误 chunk 回写。

3. **异步任务**  
   `/v1/async/{chat|generate-image}` 将请求 + 元数据投递至 RabbitMQ；`AsyncTaskListener` 消费后调用 LangchainProxy，写入 `AsyncJob`（Mongo/MySQL）并由客户通过 `/v1/async/jobs/{jobId}` 轮询。

4. **图像生成**  
   `ImageGeneratorFactory` 根据 `modelIdentifier` 选择 Dashscope/Ark，支持文本与编辑模式，返回 Base64/URL。

5. **统计/计费**  
   `StatisticsAspect` 读取 `ModelUsageContext`，成功写 `model_daily_stats`（MySQL upsert），失败写 Mongo；`StatisticsController` 允许按模型/用户/时间/成功状态一系列条件查询。

6. **订单/余额**  
   订单创建后写 RabbitMQ 延迟队列，`OrderCancelListener` 到期自动关闭；支付回调 `paySuccess` 触发余额充值与幂等校验。

## 部署与运维

- **Docker**：多阶段 `Dockerfile` + `docker-compose.yml`，一次性拉起 MySQL/Redis/RabbitMQ/Mongo + Spring Boot。详见 `doc/docker-deployment.md`。
- **日志**：容器日志挂载至 `./logs`；Logback 通过 `traceId` 串联调用链。
- **配置**：依赖环境变量覆盖 `application.yml`，同时 `test_scripts` 可帮助验证 Access Key/模型链路。

## 调试与脚本

- `test_scripts/*.sh`：用户注册、登录、模型 CRUD、聊天/图像、订单等 curl 示例。
- `test_scripts/python/`：`chat_v2.py` / `chat_v2_stream.py` 等，模拟 OpenAI client。
- `doc/llmproxy_pytest`：图像多模态 Python 样例。

## 扩展建议

1. **新增模型/厂商**：在 `Provider`/`ProviderKey` 中注册，扩展 `LangchainProxyServiceImpl` 或 `ImageGeneratorFactory` 解析 `modelIdentifier` 即可。
2. **能力扩展**：`ModelCapabilityEnum` 与 `LlmModel.capabilities` 驱动，若新增语音等能力，可在 `selectModel` / `KeySelectionService` 增加策略。
3. **监控对接**：可在 `StatisticsServiceImpl` 基础上追加 Aggregation / 导出，或在 `LoggingAspect` 中写入 APM。
4. **认证策略**：Access Key 机制可扩展为子账号/配额，只需调整 `IAccessKeyService` 与拦截器即可。

如需更多上下文，请结合 `README.md`、`doc/api_v2.md` 与 `doc/docker-deployment.md`，这些文档描述了运行、API、部署等维度。
