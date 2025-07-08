

# 大模型代理后端API 文档

API接口主要分为三部分：**认证模块**、**模型管理模块**和**模型代理模块**。

## 通用说明

### 1. 根路径

所有API的根路径为：`/`

### 2. 通用响应结构

所有API成功或失败时均返回一个标准的JSON对象，结构如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

| 字段      | 类型   | 说明                                   |
| :-------- | :----- | :------------------------------------- |
| `code`    | `int`  | 业务状态码，例如 `200` 代表成功。      |
| `message` | `String` | 响应的描述信息。                       |
| `data`    | `Object` | 具体的响应数据，可能为对象、数组或`null`。 |

### 3. 认证

模型管理模块的所有接口都需要在请求头中携带认证令牌（Token）。请在登录成功后，将获取到的`token`放入后续请求的`Authorization`头中。

```
Authorization: Bearer <your_token>
```

---

## 1. 认证模块 (Auth)

**根路径:** `/v1/auth`

### 1.1 用户注册

- **功能描述:** 注册一个新的管理员账户。
- **请求方法:** `POST`
- **请求路径:** `/v1/auth/register`
- **请求头:** `Content-Type: application/json`

#### 请求体 (`AdminRegisterRequest`)

```json
{
  "username": "newUser",
  "password": "password123",
  "email": "user@example.com"
}
```

| 字段       | 类型     | 是否必填 | 约束                                 |
| :--------- | :------- | :------- | :----------------------------------- |
| `username` | `String` | 是       | 长度必须在3到50之间。                |
| `password` | `String` | 是       | 长度必须在6到100之间。               |
| `email`    | `String` | 否       | 必须是合法的Email格式。              |

#### 成功响应

```json
{
  "code": 200,
  "message": "用户注册成功",
  "data": "newUser"
}
```

### 1.2 用户登录

- **功能描述:** 管理员使用用户名和密码登录，获取认证令牌。
- **请求方法:** `POST`
- **请求路径:** `/v1/auth/login`
- **请求头:** `Content-Type: application/json`

#### 请求体 (`AdminLoginRequest`)

```json
{
  "username": "admin",
  "password": "password123"
}
```

| 字段       | 类型     | 是否必填 | 约束     |
| :--------- | :------- | :------- | :------- |
| `username` | `String` | 是       | -        |
| `password` | `String` | 是       | -        |

#### 成功响应 (`AdminLoginResponse`)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "admin"
  }
}
```

---

## 2. 模型管理模块 (Model Management)

**根路径:** `/v1/models`
**注意:** 此模块所有接口均需要认证。

### 2.1 创建模型

- **功能描述:** 添加一个新的大语言模型配置。
- **请求方法:** `POST`
- **请求路径:** `/v1/models`
- **请求头:**
    - `Content-Type: application/json`
    - `Authorization: Bearer <token>`

#### 请求体 (`ModelCreateRequest`)

```json
{
  "displayName": "GPT-4 模型",
  "modelIdentifier": "gpt-4",
  "urlBase": "https://api.openai.com/v1",
  "apiKey": "sk-xxxxxxxxxxxxxxxxxxxx",
  "capabilities": ["text-to-text", "chat"],
  "priority": 10
}
```

| 字段              | 类型              | 是否必填 | 约束                               |
| :---------------- | :---------------- | :------- | :--------------------------------- |
| `displayName`     | `String`          | 是       | 模型显示名称。                     |
| `modelIdentifier` | `String`          | 是       | 模型唯一标识。                     |
| `urlBase`         | `String`          | 否       | 模型API的基础URL。                 |
| `apiKey`          | `String`          | 是       | 调用模型所需的API Key。            |
| `capabilities`    | `List<String>`    | 是       | 模型能力列表，不能为空。           |
| `priority`        | `Integer`         | 是       | 优先级，数字越小优先级越高，最小为1。 |

#### 成功响应 (`ModelResponse`)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "displayName": "GPT-4 模型",
    "modelIdentifier": "gpt-4",
    "urlBase": "https://api.openai.com/v1",
    "capabilities": ["text-to-text", "chat"],
    "priority": 10,
    "status": 1,
    "createdAt": "2023-10-27T10:00:00",
    "updatedAt": "2023-10-27T10:00:00"
  }
}
```
**注意:** 出于安全考虑，响应中不会返回`apiKey`。

### 2.2 获取模型列表

- **功能描述:** 分页、筛选和排序获取模型列表。
- **请求方法:** `GET`
- **请求路径:** `/v1/models`
- **请求头:** `Authorization: Bearer <token>`

#### 请求参数（查询参数）

| 参数        | 类型     | 是否必填 | 默认值       | 说明                                                         |
| :---------- | :------- | :------- | :----------- | :----------------------------------------------------------- |
| `pageNum`   | `int`    | 否       | `1`          | 页码。                                                       |
| `pageSize`  | `int`    | 否       | `10`         | 每页数量。                                                   |
| `status`    | `String` | 否       | -            | 模型状态: `0` (下线), `1` (上线)。                           |
| `capability`| `String` | 否       | -            | 按模型能力筛选，例如 `text-to-text`。                        |
| `sortBy`    | `String` | 否       | `priority`   | 排序字段，可选值: `priority`, `name`, `createdAt`。          |
| `sortOrder` | `String` | 否       | `asc`        | 排序顺序，可选值: `asc` (升序), `desc` (降序)。              |

#### 成功响应 (`IPage<ModelResponse>`)

响应数据`data`是一个分页对象，结构如下：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "displayName": "GPT-4 模型",
        "modelIdentifier": "gpt-4",
        "urlBase": "https://api.openai.com/v1",
        "capabilities": ["text-to-text", "chat"],
        "priority": 10,
        "status": 1,
        "createdAt": "2023-10-27T10:00:00",
        "updatedAt": "2023-10-27T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 2.3 获取单个模型信息

- **功能描述:** 根据ID获取单个模型的详细信息。
- **请求方法:** `GET`
- **请求路径:** `/v1/models/{id}`
- **请求头:** `Authorization: Bearer <token>`

#### 路径参数

| 参数 | 类型      | 说明     |
| :--- | :-------- | :------- |
| `id` | `Integer` | 模型ID。 |

#### 成功响应 (`ModelResponse`)

与 **2.1 创建模型** 的成功响应体中的`data`字段结构相同。

### 2.4 更新模型信息

- **功能描述:** 更新指定ID的模型信息。只会更新请求体中提供的字段。
- **请求方法:** `PUT`
- **请求路径:** `/v1/models/{id}`
- **请求头:**
    - `Content-Type: application/json`
    - `Authorization: Bearer <token>`

#### 路径参数

| 参数 | 类型      | 说明     |
| :--- | :-------- | :------- |
| `id` | `Integer` | 模型ID。 |

#### 请求体 (`ModelUpdateRequest`)

所有字段均为可选。

```json
{
  "displayName": "GPT-4 Turbo",
  "priority": 5
}
```

| 字段              | 类型              | 约束                               |
| :---------------- | :---------------- | :--------------------------------- |
| `displayName`     | `String`          | -                                  |
| `modelIdentifier` | `String`          | -                                  |
| `urlBase`         | `String`          | -                                  |
| `apiKey`          | `String`          | 留空则不更新。                     |
| `capabilities`    | `List<String>`    | -                                  |
| `priority`        | `Integer`         | 最小为1。                          |

#### 成功响应 (`ModelResponse`)

返回更新后的模型信息，结构与 **2.1 创建模型** 的成功响应体中的`data`字段结构相同。

### 2.5 更新模型状态

- **功能描述:** 单独更新模型的上线/下线状态。
- **请求方法:** `PATCH`
- **请求路径:** `/v1/models/{id}/status`
- **请求头:**
    - `Content-Type: application/json`
    - `Authorization: Bearer <token>`

#### 路径参数

| 参数 | 类型      | 说明     |
| :--- | :-------- | :------- |
| `id` | `Integer` | 模型ID。 |

#### 请求体 (`ModelStatusUpdateRequest`)

```json
{
  "status": 0
}
```

| 字段     | 类型      | 是否必填 | 约束                      |
| :------- | :-------- | :------- | :------------------------ |
| `status` | `Integer` | 是       | `0` (下线) 或 `1` (上线)。 |

#### 成功响应 (`ModelResponse`)

返回更新后的模型信息，结构与 **2.1 创建模型** 的成功响应体中的`data`字段结构相同。

### 2.6 删除模型

- **功能描述:** 根据ID删除一个模型。
- **请求方法:** `DELETE`
- **请求路径:** `/v1/models/{id}`
- **请求头:** `Authorization: Bearer <token>`

#### 路径参数

| 参数 | 类型      | 说明     |
| :--- | :-------- | :------- |
| `id` | `Integer` | 模型ID。 |

#### 成功响应

```json
{
  "code": 200,
  "message": "模型删除成功",
  "data": null
}
```


## 3. AI能力代理模块 (Proxy)

**根路径:** `/v1`
**注意:** 此模块所有接口均需要认证。

### 3.1 对话 (Chat)

- **功能描述:**
  此接口为统一的对话入口，智能地支持两种模式：
  - **文生文 (Text-to-Text):** 当请求体中的 `images` 字段为 `null` 或空数组时，接口将执行标准的文本对话。模型会根据 `userMessage` 和 `history`（如果提供）生成文本回答。
  - **图生文/多模态对话 (Vision / Image-to-Text):** 当请求体中的 `images` 字段包含一个或多个图片信息时，接口将执行多模态对话。模型会结合 `userMessage` 和图片内容进行理解，并生成相应的回答。

- **请求方法:** `POST`
- **请求路径:** `/v1/chat`
- **请求头:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`

#### 请求体 (`ChatRequest_dto`)

```json
{
  "userMessage": "这张图里的小狗是什么品种？",
  "modelIdentifier": "gpt-4-vision-preview",
  "history": [
    "user: 你好",
    "assistant: 你好，有什么可以帮你的吗？"
  ],
  "options": {
    "temperature": 0.7
  },
  "images": [
    {
      "url": "https://example.com/path/to/dog_image.jpg"
    }
  ]
}
```

| 字段              | 类型                  | 是否必填 | 说明                                                                                              |
| :---------------- | :-------------------- | :------- | :------------------------------------------------------------------------------------------------ |
| `userMessage`     | `String`              | 是       | 用户当前发送的消息文本。                                                                          |
| `modelInternalId` | `String`              | 否       | 指定使用的模型ID（数据库主键）。`modelInternalId` 和 `modelIdentifier` 二选一，优先使用此字段。 |
| `modelIdentifier` | `String`              | 否       | 指定使用的模型唯一标识。若两者都不传，系统将根据优先级和能力自动选择合适的模型。                  |
| `history`         | `List<String>`        | 否       | 对话历史记录，用于支持多轮对话。格式为 `["角色: 消息", ...]`。                                     |
| `options`         | `Map<String, Object>` | 否       | 传递给模型的额外参数，例如 `temperature`, `maxTokens` 等。                                        |
| `images`          | `List<ImageInput>`    | 否       | 输入的图片列表。**如果此字段非空，则激活图生文/多模态对话模式。**                                 |

#### `ImageInput` 结构

| 字段     | 类型     | 说明                                     |
| :------- | :------- | :--------------------------------------- |
| `base64` | `String` | 图片内容的Base64编码字符串。             |
| `url`    | `String` | 可公开访问的图片URL。`base64`和`url`二选一。 |

#### 成功响应 (`ChatResponse_dto`)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "assistantMessage": "这看起来像一只可爱的柯基犬。",
    "usedModelIdentifier": "gpt-4-vision-preview"
  }
}
```

| 字段                  | 类型     | 说明                       |
| :-------------------- | :------- | :------------------------- |
| `assistantMessage`    | `String` | AI助手生成的回复消息。     |
| `usedModelIdentifier` | `String` | 本次请求实际使用的模型标识。 |

---

### 3.2 生成或编辑图片 (Image Generation)

- **功能描述:**
  此接口为统一的图片生成入口，支持两种模式：
  - **文生图 (Text-to-Image):** 当请求体中的 `originImage` 字段为 `null` 时，接口将根据 `prompt` 文本描述直接生成一张全新的图片。
  - **图生图/图片编辑 (Image-to-Image / Editing):** 当请求体中提供了 `originImage` 时，接口将把这张图片作为基础，并结合 `prompt` 的描述对其进行修改、编辑或生成变体。

- **请求方法:** `POST`
- **请求路径:** `/v1/generate-image`
- **请求头:**
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`

#### 请求体 (`ImageGenerationRequest`)

```json
{
  "prompt": "一只戴着宇航员头盔的猫，赛博朋克风格",
  "modelIdentifier": "dall-e-3",
  "size": "1024x1024",
  "originImage": null
}
```

| 字段              | 类型                  | 是否必填 | 说明                                                                                                |
| :---------------- | :-------------------- | :------- | :-------------------------------------------------------------------------------------------------- |
| `prompt`          | `String`              | 是       | 描述想要生成的图片内容的文本。                                                                      |
| `modelInternalId` | `String`              | 否       | 指定使用的模型ID（数据库主键）。`modelInternalId` 和 `modelIdentifier` 二选一，优先使用此字段。   |
| `modelIdentifier` | `String`              | 否       | 指定使用的模型唯一标识。若两者都不传，系统将自动选择合适的文生图模型。                              |
| `size`            | `String`              | 否       | 生成图片的尺寸，格式为 "宽x高"，例如 "1024x1024"。                                                 |
| `originImage`     | `ImageInput`          | 否       | 原始图片输入，用于图片编辑或生成变体。**如果此字段非空，则激活图生图/图片编辑模式。**               |
| `options`         | `Map<String, Object>` | 否       | 传递给模型的额外参数，例如 `quality`, `style` 等。                                                  |

#### 成功响应 (`ImageGenerationResponse`)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "imageUrl": "https://cdn.example.com/generated-images/image-12345.png",
    "actualPrompt": "A cat wearing an astronaut helmet, in a cyberpunk art style.",
    "usedModelIdentifier": "dall-e-3"
  }
}
```

| 字段                  | 类型     | 说明                                             |
| :-------------------- | :------- | :----------------------------------------------- |
| `imageUrl`            | `String` | 生成图片的URL地址。                              |
| `actualPrompt`        | `String` | 实际发送给模型的prompt（可能经过了系统的优化或改写）。 |
| `usedModelIdentifier` | `String` | 本次请求实际使用的模型标识。                     |