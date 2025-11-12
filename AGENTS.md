# AGENTS

## 项目速览
- **定位**：一个面向大模型 API 聚合代理的 Spring Boot 后端，统一接入聊天/图像能力，负责鉴权、计费、异步任务、模型编排及统计。
- **特性**：JWT + Access Key 双重鉴权、Redis 缓存、MyBatis-Plus 操作 MySQL、MongoDB 保存详细用量、RabbitMQ 处理订单超时与异步生成、LangChain4j 统一调用模型、按能力自动路由模型，支持图片输入/输出。
- **数据流**：HTTP → 控制器 → 服务层（模型选择/鉴权/扣费）→ 外部模型/消息队列 → 统计（MySQL/Mongo）→ 结果返回；失败场景通过熔断和异常处理回写。

## 技术栈
- Spring Boot 3、Spring Security、Spring Validation、Spring Cache (Redis)、Spring AMQP、Spring Data MongoDB
- MyBatis-Plus（分页、乐观锁、XML 自定义 upsert）
- LangChain4j (OpenAI Chat)、Volcengine Ark / Alibaba Dashscope 图像 SDK
- Redis、RabbitMQ、MySQL、MongoDB

## `src/main` 目录结构
```text
src/main
├── java/cn/tyt/llmproxy
│   ├── LlmproxyApplication.java                 # 应用入口，启用缓存
│   ├── aspect                                   # AOP：访问日志与统计写回
│   ├── common                                   # 注解、通用枚举/异常/DTO/工具
│   ├── config                                   # 安全、缓存、序列化、RabbitMQ、MyBatis、WebMvc
│   ├── context                                  # ThreadLocal 模型使用上下文
│   ├── controller                               # 对外 REST API（鉴权、模型、订单、异步代理等）
│   ├── dto                                      # 请求/响应 DTO、Mongo 文档模型
│   ├── entity                                   # MyBatis-Plus 实体（模型、订单、用户、统计等）
│   ├── filter                                   # AccessKey 拦截器 & JWT 过滤器
│   ├── image                                    # 图像生成服务抽象与厂商实现
│   ├── listener                                 # RabbitMQ 监听器（异步生成、订单超时）
│   ├── mapper                                   # MyBatis-Plus Mapper 接口
│   ├── repository                               # MongoRepository 封装
│   ├── runner                                   # 启动时创建 ROLE_ROOT_ADMIN
│   ├── security                                 # 认证失败/权限不足响应
│   └── service                                  # Service 接口 & 实现、密钥选择、异步任务编排
└── resources
    ├── application.yml                          # MySQL/Redis/Rabbit/Mongo/JWT 配置
    ├── logback-spring.xml
    ├── mapper/ModelDailyStatMapper.xml          # 自定义 upsert SQL
    ├── static                                   # (空，预留)
    └── templates                                # (空，预留)
```

## 核心包说明

### aspect
- `LoggingAspect`：环绕 Controller/Service 记录 traceId、请求体（做敏感过滤）、UA/IP、执行耗时/异常。
- `StatisticsAspect`：围绕 `ILangchainProxyService` 公开方法工作，在 `ModelUsageContext` 写入后自动调用 `IStatisticsService`，成功写 MySQL 日统计，失败写 Mongo 并清理上下文。

### common
- `annotation/LogExecution` 供自定义切面。
- `domain/Result` & `LoginUser`：统一返回结构与自定义 `UserDetails`。
- `enums`：封装角色、模型能力、结果码、状态。
- `exception/GlobalExceptionHandler`：覆盖参数校验、SQL 约束、未授权、NPE 等，返回 `ResultCode`。
- `utils/JwtTokenUtil`：生成/校验 JWT，配合 `JwtAuthenticationTokenFilter` 使用。

### config
- `SecurityConfig`：JWT 过滤、CORS、鉴权规则（开放注册/登录/公开推理，模型管理限管理员，其余需认证）。
- `WebMvcConfig`：注册 `AccessKeyInterceptor` 仅拦截代理/异步推理接口。
- `RedisCacheConfig`：自定义 `RedisCacheManager`，复制全局 `ObjectMapper`，开启类型信息与安全模块序列化。
- `RabbitMQConfig`：定义订单延迟/死信交换机 + 异步聊天/图像队列及 JSON 消息转换器。
- `MyBatisPlusConfig`：分页 + 乐观锁 + Mapper 扫描；`JacksonConfig` 统一 JavaTime 格式。

### controller
- `AuthController`：注册、登录、改密、ROLE_ROOT_ADMIN 分配角色。
- `AccessKeyController`：当前用户 key 的 CRUD（创建随机 `ak-*` 前缀、删除前校验归属）。
- `ModelController`：模型增删改查、状态切换、分页筛选及调用统计（调用 `IStatisticsService.getModelUsage`）。
- `ProviderController`/`OrderController`/`StatisticsController`：厂商管理、充值订单（生成订单 → RabbitMQ 延迟单 → 超时取消 / 支付成功入账）、统计接口（模型日汇总、用户/模型日志列表、自定义 `/logs/query` 支持 `UsageLogQueryDto` 多条件检索 Mongo 用量）。
- `ProxyController`：核心代理；同步/异步聊天+图像，注入 `AccessKeyInterceptor` 写入的 `userId/accessKeyId`；异步请求时创建 `AsyncJob` → 生产 MQ 消息 → 查询任务状态。

### dto
- `request`：Chat/Image/Model/Order/User 等输入校验，支持多模态字段（history、images、options、originImage）；`UsageLogQueryDto` 允许按用户/模型/Key/时间范围/成功状态等组合条件查询 Mongo 用量日志。
- `response`：`ChatResponse_dto`, `ImageGenerationResponse`, `ModelResponse`, `OrderResponse`, `UserLoginResponse`, `ModelStatisticsDto` 等。
- `LlmModelConfigDto`：LangChain4j 执行所需配置（API Key、能力、价格等）；`UsageLogDocument`：MongoDB 使用记录结构。

### entity & mapper
- 通过 `@TableName` + `JacksonTypeHandler`（如 `LlmModel.capabilities/pricing`, `AsyncJob.requestPayload`）映射 JSON 列。
- 关注实体：`LlmModel`, `Provider`, `ProviderKey`, `AccessKey`, `Order`, `User`, `AsyncJob`, `ModelDailyStat`, `UsageLog`。
- Mapper 接口继承 `BaseMapper<>`，`ModelDailyStatMapper.xml` 通过 `INSERT … ON DUPLICATE KEY UPDATE` 聚合日数据。

### filter & security
- `AccessKeyInterceptor`：读取 `ACCESS-KEY` header → 查询缓存/DB → 校验余额，向 request attribute 写 `USER_ID/ACCESS_KEY_ID`，不足或无效直接 401。
- `JwtAuthenticationTokenFilter`：提取 `Authorization: Bearer xxx` 校验后写入 `SecurityContextHolder`；`RestAuthenticationEntryPoint` & `RestfulAccessDeniedHandler` 定制未登录/权限不足响应。
- `Roles`：集中维护 `ROLE_*` 常量，供 Spring Security/业务逻辑共享。

### image
- `ImageGeneratorService` 抽象同步/编辑接口。
- `ImageGeneratorFactory` 根据 `modelIdentifier` 前缀决定生成器。
- `AliImageGenerator`（阿里云 DashScope）与 `ArkImageGenerator`（火山引擎 Ark）实现不同 SDK、参数适配、Base64 处理和错误提示。

### listener & async 服务
- `AsyncTaskProducerServiceImpl`：把 `AsyncJob` 元数据 + 请求体发到 `async.task.exchange`。
- `AsyncTaskListener`：监听聊天/图像队列，调用 `IAsyncTaskConsumerService`。
- `AsyncTaskConsumerServiceImpl`：开始时更新状态为 PROCESSING，调用 `LangchainProxyServiceImpl`（设置 `isAsync=true`），成功写 resultPayload/模型名，失败写 errorMessage。
- `OrderCancelListener`：监听死信队列，调用 `IOrderService.cancelTtlOrder` 自动关闭超时订单。

### service 层亮点
- `LangchainProxyServiceImpl`：根据请求能力/指定模型 → `selectModel` + `KeySelectionService` 轮询活跃 Key（Redis 计数器、熔断键），构建 `OpenAiChatModel` / 图像生成器，调用 LangChain4j / 厂商 SDK；捕获认证/限速异常更新 key 状态或设置熔断；根据 token 用量计算成本、扣费、写统计（MySQL+Mongo），并在 `ModelUsageContext` 中记录当前模型供切面使用。
- `KeySelectionService`：Redis 维护轮询计数与熔断 TTL（429/密钥失效时 `reportKeyFailure`），确保高可用。
- `AccessKeyServiceImpl`：包装 `AccessKeyRepository`（带 cache 的 self-invocation workaround）查询、余额校验、key CRUD。
- `UserServiceImpl`：整合 Spring Security 登录、`LoginUser` 缓存、余额增减（充值/扣费）、ROLE_ROOT_ADMIN 初始化由 `AdminInitializer` 完成。
- `OrderServiceImpl`：创建订单后发送延迟消息；支付成功幂等校验＋乐观锁＋余额充值；TTL 取消用于自动关单。
- `IStatisticsService`：`recordUsageMysql` 日志聚合、`recordUsageMongo`/`recordFailMongo` 保存详细记录、`getModelUsage` 组合查询；`queryUsageLogs` 通过 `MongoTemplate` 支持更细粒度（用户/模型/Key/异步、时间区间、排序、limit）筛选；`UsageLogRepository` 走 MongoRepository。

### resources
- `application.yml`：集中配置各数据源及 JWT；`spring.cache.redis.key-prefix` 与 `RedisCacheConfig` 对应；启用 LangChain4j 依赖的日志等级。
- `logback-spring.xml`：日志配置（未展开）。

## 关键流程速查
- **同步聊天/图像**：AccessKey 拦截 → `ProxyController` → `LangchainProxyServiceImpl`（模型选择、限流熔断、LangChain4j 调用、统计、扣费）→ `Result.success`.
- **异步任务**：`ProxyController` 创建 `AsyncJob` + MQ 消息 → `AsyncTaskListener` → `AsyncTaskConsumerServiceImpl` → 处理完成后客户轮询 `/v1/async/jobs/{jobId}`。
- **模型统计**：AOP 把请求成败写 `model_daily_stats`，详细 token/费用写 Mongo；`ModelController` + `StatisticsController` 提供查询接口。
- **订单充值**：`OrderController` 创建订单（消息进入延迟队列）→ 支付后 `paySuccess` 加余额 → 未支付到期由 `OrderCancelListener` 自动取消。

## 扩展建议
1. 新模型/厂商：新增 `Provider/ProviderKey`，在 `ImageGeneratorFactory` 或 `LangchainProxyServiceImpl` 中扩展构建逻辑即可。
2. 更细粒度监控：可在 `UsageLogRepository` 基础上增加聚合查询或导出任务。
3. 接入新鉴权方式：复用 `AccessKeyInterceptor` 机制，只需在 `IAccessKeyService` 中增添校验策略。
