# LLM Proxy API 参考文档

**版本**: 1.0.0
**面向对象**: 前端开发人员、第三方集成商

## 1\. 简介

LLM Proxy 提供了一套统一的 RESTful API，用于聚合和管理多种大语言模型（如 OpenAI, 阿里 DashScope, 火山 Ark 等）。本服务支持标准的 HTTP 请求以及基于 SSE (Server-Sent Events) 的流式响应。

### 1.1 服务入口

* **Base URL**: `http://<host>:8060`
* **协议**: HTTP/1.1 (常规), EventStream (流式)
* **数据格式**: 所有请求和响应 Body 均为 `application/json` (除 SSE 外)。

### 1.2 鉴权机制

本服务通过 HTTP Header 进行鉴权，分为两种模式：

1.  **管理端鉴权 (Bearer Token)**

    * **适用接口**: 用户管理、后台管理、密钥生成、历史记录查询。
    * **方式**: Header 中携带 `Authorization: Bearer <token>`。
    * **Token 获取**: 通过 `/v1/auth/login` 接口获取。

2.  **能力调用鉴权 (Access Key)**

    * **适用接口**: `/v1/chat`, `/v1/v2/*` (OpenAI 兼容接口), `/v1/generate-image` 等推理接口。
    * **方式**: Header 中携带 `ACCESS-KEY: <your_api_key>`。
    * **Key 获取**: 在管理端通过 `/v1/access-keys` 生成。

### 1.3 通用响应格式

除流式接口和 OpenAI 兼容接口外，所有接口遵循以下统一响应结构：

```json
{
  "code": 200,         // 业务状态码，200 表示成功
  "message": "操作成功", // 状态描述
  "data": { ... }      // 业务数据
}
```

### 1.4 分页数据结构

分页接口 `data` 字段结构如下：

```json
{
  "records": [ ... ], // 数据列表
  "total": 100,       // 总条数
  "size": 10,         // 每页大小
  "current": 1,       // 当前页码
  "pages": 10         // 总页数
}
```

-----

## 2\. 认证与账户管理 (Authentication)

### 2.1 用户登录

获取管理端 Token。

* **Method**: `POST /v1/auth/login`
* **Auth**: None

**Request Body:**

```json
{
  "username": "myuser",
  "password": "mypassword123"
}
```

**Response (Success):**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...", // JWT Token
    "username": "myuser",
    "role": "ROLE_USER"
  }
}
```

### 2.2 用户注册

注册新账户。

* **Method**: `POST /v1/auth/register`
* **Auth**: None

**Request Body:**

```json
{
  "username": "newuser",
  "password": "newpassword123",
  "email": "user@example.com" // 可选
}
```

### 2.3 获取当前用户信息

* **Method**: `GET /v1/auth/me`
* **Auth**: `Authorization: Bearer <token>`

**Response (Success):**

```json
{
  "code": 200,
  "data": {
    "id": 101,
    "username": "myuser",
    "email": "user@example.com",
    "role": "ROLE_USER",
    "balance": 99.50, // 当前账户余额
    "createdAt": "2023-10-27T10:00:00"
  }
}
```

-----

## 3\. 密钥管理 (Access Keys)

用于前端自行管理调用推理接口所需的 API Key。

### 3.1 生成 Access Key

* **Method**: `POST /v1/access-keys`
* **Auth**: `Authorization: Bearer <token>`

**Response (Success):**

```json
{
  "code": 200,
  "data": {
    "id": 5,
    "keyValue": "ak-a1b2c3d4-...", // 仅在此次响应中明文显示，请妥善保存
    "isActive": true,
    "createdAt": "2023-11-20T12:00:00"
  }
}
```

### 3.2 获取密钥列表

* **Method**: `GET /v1/access-keys`
* **Auth**: `Authorization: Bearer <token>`

**Response (Success):**

```json
{
  "code": 200,
  "data": [
    {
      "id": 5,
      "keyValue": "********", // 列表不返回明文
      "isActive": true,
      "createdAt": "2023-11-20T12:00:00"
    }
  ]
}
```

-----

## 4\. 模型推理接口 (Native API)

本节接口使用 **Access Key** 鉴权。Header 需包含 `ACCESS-KEY: <key>`。

### 4.1 通用对话 (Chat)

统一的对话接口，支持多轮对话上下文。

* **Method**: `POST /v1/chat`
* **Auth**: `ACCESS-KEY`

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `modelIdentifier` | String | 否 | 模型唯一标识 (如 `qwen-max`)，若不填则根据能力自动路由 |
| `userMessage` | String | 否 | 单条用户指令。若需多轮对话，请使用 `history` |
| `history` | List | 否 | 历史对话列表 (见示例) |
| `images` | List | 否 | 图片输入，用于多模态模型 |
| `options` | Map | 否 | 模型参数 (temperature, top\_p 等) |

**请求示例:**

```json
{
  "modelIdentifier": "qwen-max",
  "history": [
    { "role": "user", "content": "你好，请扮演一个助手。" },
    { "role": "assistant", "content": "好的，我是一个助手。" }
  ],
  "userMessage": "请帮我写一首诗。",
  "options": {
    "temperature": 0.7,
    "max_tokens": 1024
  }
}
```

**Response (Success):**

```json
{
  "code": 200,
  "data": {
    "response": "当然，这是为你写的一首诗...", // AI 回复内容
    "usedModel": "qwen-max",              // 实际调用的模型
    "promptTokens": 50,
    "completionTokens": 100,
    "totalTokens": 150
  }
}
```

### 4.2 图像生成 (Text to Image)

* **Method**: `POST /v1/generate-image`
* **Auth**: `ACCESS-KEY`

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `prompt` | String | 是 | 提示词 |
| `modelIdentifier` | String | 否 | 指定模型 (如 `wanx-v1`) |
| `options` | Map | 否 | 生成参数 (size, n, style 等) |

**请求示例:**

```json
{
  "prompt": "一只在太空中骑自行车的猫，赛博朋克风格",
  "modelIdentifier": "wanx-v1",
  "options": {
    "size": "1024*1024",
    "n": 1
  }
}
```

**Response (Success):**

```json
{
  "code": 200,
  "data": {
    "imageUrls": [
      "https://oss.example.com/generated/img_123.png"
    ],
    "actualPrompt": "一只在太空中骑自行车的猫...",
    "usedModelIdentifier": "wanx-v1"
  }
}
```

-----

## 5\. OpenAI 兼容接口

本服务提供与 OpenAI API 格式兼容的接口，方便现有应用（如 NextChat, LangChain 等）直接接入。
**Auth**: Header 需包含 `ACCESS-KEY: <key>` (作为 API Key)。

### 5.1 对话补全 (Chat Completions)

* **Method**: `POST /v1/v2/chat`
* **Auth**: `ACCESS-KEY`

**Request Body (OpenAI Format):**

```json
{
  "model": "gpt-3.5-turbo", // 对应系统的 modelIdentifier
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "temperature": 0.7,
  "stream": false // 设为 true 时进入流式模式
}
```

**Response (JSON Mode):**
完全兼容 OpenAI `chat.completions` 响应结构。

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-3.5-turbo",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "Hello there, how may I assist you today?"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 9,
    "completion_tokens": 12,
    "total_tokens": 21
  }
}
```

### 5.2 流式对话 (Server-Sent Events)

当请求体中 `stream: true` 时，接口将返回 MIME 类型为 `text/event-stream` 的数据。

**Stream Response Protocol:**
服务端会推送以 ` data:  ` 开头的 JSON 字符串，以 `[DONE]` 结束。

```http
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1694268190,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

-----

## 6\. 异步任务接口 (Async API)

用于处理耗时较长的推理任务，避免 HTTP 超时。
**Auth**: `ACCESS-KEY`

### 6.1 提交异步对话任务

* **Method**: `POST /v1/async/chat`
* **Request Body**: 同 `POST /v1/chat` (Native API)

**Response (Success):**

```json
{
  "code": 200,
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PENDING",
    "message": "Async job accepted"
  }
}
```

### 6.2 查询任务结果

* **Method**: `GET /v1/async/jobs/{jobId}`
* **Auth**: `Authorization: Bearer <token>` (注意：查询结果需使用管理端 Token，而非 Access Key)

**Response (Success):**

```json
{
  "code": 200,
  "data": {
    "jobId": "550e8400-...",
    "status": "COMPLETED", // 状态: PENDING, PROCESSING, COMPLETED, FAILED
    "resultPayload": {     // 任务完成后此处会有数据，结构同同步接口的 data
      "response": "异步任务的处理结果...",
      "usedModel": "qwen-max"
    },
    "errorMessage": null,
    "createdAt": "..."
  }
}
```

-----

## 附录

### A. 状态码字典 (ResultCode)

| 状态码 | 说明 | 建议处理 |
| :--- | :--- | :--- |
| `200` | 成功 | 正常处理业务数据 |
| `400xxx` | 请求参数错误 | 检查必填项、格式或 JSON 结构 |
| `401xxx` | 未认证 / Token 无效 | 重新登录或检查 Authorization 头 |
| `403xxx` | 权限不足 | 检查用户角色或 Access Key 是否有权访问 |
| `600xxx` | 模型服务错误 | 指定模型可能已下线或配置错误 |
| `700xxx` | 第三方服务异常 | 上游 API 调用失败，建议稍后重试 |

### B. 模型能力枚举 (ModelCapabilityEnum)

在创建模型或过滤模型时使用。

* `text-to-text`: 文本生成文本 (标准 LLM)
* `text-to-image`: 文生图
* `image-to-text`: 图像理解 (Vision)
* `image-to-image`: 图生图

### C. 订单状态

* `PENDING`: 待支付/处理中
* `COMPLETED`: 充值成功，余额已增加
* `FAILED`: 支付失败或已取消