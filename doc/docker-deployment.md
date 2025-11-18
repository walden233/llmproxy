# Docker 部署指引

本项目提供 `Dockerfile` + `docker-compose.yml` 的一键环境，包含
Spring Boot 服务、MySQL、Redis、RabbitMQ 与 MongoDB。

## 1. 依赖与准备

- 安装 Docker Engine (24+) 与 Docker Compose v2。
- 克隆/同步代码到服务器。
- 可选：在仓库根目录创建 `.env` 覆盖默认密码，例如：

```env
DB_PASSWORD=change-me
RABBITMQ_PASSWORD=change-me
MONGO_PASSWORD=change-me
```

不配置时默认使用 `docker-compose.yml` 内的 `:-` 后值。

## 2. 构建镜像

```bash
docker compose build springboot-app
```

该步骤触发多阶段构建：`maven:3.8.5-openjdk-17` 编译 Jar，
`eclipse-temurin:17-jre` 作为运行时镜像。

## 3. 启动所有服务

```bash
docker compose up -d
```

Compose 将依次启动：

| 服务         | 说明 / 端口                          |
|--------------|-------------------------------------|
| mysql-db     | MySQL 8.0 (`3306`)，执行 `mysql-init` 下脚本 |
| redis        | Redis 7 (`6379`)                    |
| rabbitmq     | RabbitMQ 3.13 + 管理端口 `15672`    |
| mongodb      | MongoDB 6 (`27017`)                 |
| springboot-app | LLM Proxy 后端 (`8060`)           |

可通过 `docker compose ps` 查看状态。

## 4. 查看日志 / 首次验证

```bash
docker compose logs -f springboot-app
curl -I http://localhost:8060
```

日志中出现 `Started LlmproxyApplication` 即代表应用已启动。
随后可使用 `test_scripts/chat_v2.sh` 或 Python 测试脚本配合
有效的 `ACCESS-KEY` 做健康验证。

## 5. 停止与清理

```bash
docker compose down          # 停止并保留数据卷
docker compose down -v       # 停止并删除所有卷（清空数据）
```

日志与数据库：

- `./logs` 会挂载到容器 `/app/logs`
- `mysql-data` / `redis-data` / `rabbitmq-data` / `mongo-data`
  均为命名卷，位于 Docker 数据目录

## 6. 环境变量速查

| 变量 | 说明 (默认) |
|------|-------------|
| `DB_NAME` / `DB_PASSWORD` | Spring Boot 使用的数据库与密码 (`model_service` / `123456`) |
| `REDIS_DATABASE` / `REDIS_PASSWORD` | Redis 库序号及密码 (默认 0 / 空) |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST` | 队列鉴权 (`llmproxy` / `llmproxy` / `/llm-proxy`) |
| `MONGO_USERNAME` / `MONGO_PASSWORD` / `MONGO_DATABASE` | MongoDB root 用户/密码/默认库 (`llmproxy` / `llmproxy` / `llm-proxy`) |
| `MONGODB_URI` | 覆盖整个 Mongo 连接串，若设置则忽略上述单项 |

按需在 `.env` 或 CI/部署管线中覆盖上述变量即可完成自定义。
