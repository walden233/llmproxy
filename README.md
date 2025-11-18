# LLM Proxy

一个面向企业/团队场景的 **大模型 API 聚合代理**。项目通过统一的权限体系、模型编排与计费规则，将不同厂商的大语言/多模态模型能力暴露为一致的 REST API，并提供异步任务、统计与订单管理等周边能力。

## 功能特性

- **统一鉴权**：JWT 用户体系 + Access Key 双重保护，Access Key 拦截器会校验余额、熔断状态并将 userId 注入上下文。
- **模型治理**：支持模型/供应商/密钥的 CRUD、优先级、能力维度（text-to-text、image-to-text、text-to-image、image-to-image）与价格配置。
- **高可用调用链**：LangChain4j 统一调用，`KeySelectionService` 结合 Redis 做轮询与熔断，支持同步 `/v1/chat`、OpenAI 风格 `/v2/chat` 以及 `/v2/chat/stream` SSE。
- **多模态**：文生文、文+图生文、文生图、图生图；图像能力分别接入阿里 DashScope、火山 Ark。
- **异步任务**：RabbitMQ + MongoDB 记录，支持 `/v1/async/chat`、`/v1/async/generate-image`，客户通过 `/v1/async/jobs/{jobId}` 轮询。
- **计费与统计**：MySQL 写日汇总，MongoDB 存细粒度 token/cost；`StatisticsController`/`ModelController`/`UsageLogQueryDto` 提供多维查询。
- **订单与余额**：`OrderController` 负责充值订单、TTL 自动取消、支付成功幂等入账。
- **测试脚本**：`test_scripts/` 内含 curl 与 Python 测试（新增 `/v2/chat`, `/v2/chat/stream` 等）。
- **一键部署**：提供 `Dockerfile` + `docker-compose.yml`，见 `doc/docker-deployment.md`。

## 技术栈

- **后端框架**：Spring Boot 3.x、Spring Security、Spring Validation、Spring Cache (Redis)、Spring AMQP、Spring Data MongoDB
- **ORM/数据访问**：MyBatis-Plus、MongoRepository
- **模型调用**：LangChain4j (OpenAI Chat)、Volcengine Ark、Alibaba Dashscope
- **中间件**：MySQL 8、Redis 7、RabbitMQ 3、MongoDB 6
- **构建与部署**：Maven、多阶段 Dockerfile、Docker Compose

## 目录速览

```
.
├── src/main/java/cn/tyt/llmproxy
│   ├── aspect/              # 日志与统计 AOP
│   ├── common/              # Result、异常、枚举、工具
│   ├── config/              # Security、Redis、RabbitMQ、MyBatis 等配置
│   ├── controller/          # Auth / Model / Provider / Order / Proxy / Statistics
│   ├── dto/, entity/, mapper/, service/ ... # 业务层
│   └── image/, listener/, runner/, filter/  # 图像接入、MQ 监听、初始化
├── src/main/resources
│   ├── application.yml      # 可通过环境变量覆盖的配置
│   └── mapper/              # 自定义 SQL
├── test_scripts/            # Shell 与 Python 调试脚本
├── doc/                     # 架构说明、API 文档、部署文档
├── Dockerfile               # 多阶段构建
└── docker-compose.yml       # 含 MySQL/Redis/RabbitMQ/Mongo 的一站式环境
```

## 快速开始

### 1. Docker Compose

```bash
# 复制 .env (可选) 覆盖数据库/队列密码
cp .env.example .env

# 构建后端镜像
docker compose build springboot-app

# 启动所有服务
docker compose up -d

# 查看日志
docker compose logs -f springboot-app
```

默认暴露端口：

| 服务 | 端口 |
|---|---|
| Spring Boot | 8060 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ (AMQP/管理) | 5672 / 15672 |
| MongoDB | 27017 |

更多细节见 `doc/docker-deployment.md`。

### 2. 本地开发

1. 安装 MySQL、Redis、RabbitMQ、MongoDB 并确保与 `application.yml` 中默认配置一致（或设置相应环境变量）。
2. 执行数据库初始化脚本 `mysql-init/init.sql`。
3. 运行应用：

```bash
./mvnw spring-boot:run
# 或
mvn clean package && java -jar target/llmproxy-0.0.1-SNAPSHOT.jar
```

4. 使用 `test_scripts/login.sh` / `register.sh` 获取 JWT，再调用 `test_scripts/chat*.sh` 或 `test_scripts/python/` 中的脚本验证能力。

## 配置与环境变量

`application.yml` 中大部分配置都可通过环境变量覆盖，例如：

| 变量 | 默认值 | 用途 |
|------|--------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `3306` / `model_service` / `root` / `123456` | MySQL 连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` / `REDIS_PASSWORD` | `localhost` / `6379` / `0` / 空 | Redis 缓存 |
| `MONGODB_URI` | `mongodb://llmproxy:llmproxy@localhost:27017/llm-proxy?authSource=admin` | MongoDB 数据源 |
| `RABBIT_HOST` / `RABBIT_PORT` / `RABBIT_USERNAME` / `RABBIT_PASSWORD` / `RABBIT_VHOST` | `localhost` / `5672` / `llmproxy` / `llmproxy` / `/llm-proxy` | RabbitMQ |
| `JWT_SECRET` (`jwt.secret`) | `SecretKeyForHS256...` | JWT 加解密 |

## 文档 & 调试

- `AGENTS.md`：项目速览、目录、关键流程说明。
- `doc/api_v2.md`：最新 API 说明（参考 `doc/大模型代理后端API-250728.md` 重写）。
- `doc/docker-deployment.md`：容器化部署步骤。
- `doc/llmproxy_pytest/proxytest.py`：Python 端到端示例。
- `test_scripts/`：涵盖鉴权、模型管理、聊天/图像（含 `/v2/chat`、SSE）的示例脚本。

如需新增模型或厂商，请参考 `AGENTS.md` 中的扩展建议，主要涉及 `Provider`、`ProviderKey`、`LangchainProxyServiceImpl` 以及 `ImageGeneratorFactory`。
