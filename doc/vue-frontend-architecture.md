# LLM Proxy 管理与调用前端（Vue）设计方案

## 1. 总体目标
基于 `doc/大模型代理后端API-251117.md` 所描述的能力，构建一套统一的运营/用户前端：
- **运营门户**：模型、供应商、订单、统计等管理场景，依赖 JWT + 角色控制。
- **开发者门户**：Access Key 申请、余额与调用日志、异步任务查询等，需配合 ACCESS-KEY 头访问推理 API。
- **监控大屏（可选）**：对接统计与日志接口，实时展示模型使用情况。

系统采用 Vue 3 + TypeScript + Vite 构建，配合 Pinia 状态管理、Vue Router 拆分模块，并通过 Axios + 拦截器调用后端 REST API。UI 层推荐 Element Plus 或 Arco Design 等成熟组件库。

## 2. 项目结构
```
frontend/
├── src
│   ├── api          # axios 封装、接口定义（按 controller 分模块）
│   ├── assets       # 静态资源
│   ├── components   # 通用组件（表格、搜索表单、SSE 控件等）
│   ├── layouts      # 基础布局：AuthLayout、AppLayout、BlankLayout
│   ├── router       # 路由定义（含动态权限守卫）
│   ├── stores       # Pinia 仓库
│   ├── utils        # 工具（jwt 解析、权限、日期、SSE、下载）
│   ├── views        # 功能页面（按控制器拆分类目：auth, access-keys, providers, models...）
│   ├── plugins      # 组件库、pinia、i18n 注册
│   └── main.ts      # 入口
├── env/.env.*       # API 基地址、SSE 地址、默认模型
├── vite.config.ts   # 构建配置
└── package.json
```

### 2.1 模块划分
| 模块 | 主要页面 | 说明 |
| ---- | -------- | ---- |
| `auth` | 登录、注册、修改密码、个人资料 | 与 `/v1/auth/*` 交互；登录页放在单独路由，登录后持久化 JWT、角色、用户名等信息 |
| `access-keys` | Key 列表/创建/删除 | 使用 JWT 调 `/v1/access-keys`；创建成功后弹窗展示 `keyValue`，提醒用户妥善保存 |
| `providers` | 供应商、Key 管理 | 支持 CRUD、分页（`pageNum/pageSize`），UI 包含表格、筛选抽屉、Key 管理对话框 |
| `models` | 模型 CRUD、状态开关、统计 | 模型表格 + 可视化统计（对接 `/v1/models/usage`）；创建/编辑表单联动 Provider 信息 |
| `orders` | 我的订单、运营订单、支付/取消 | 普通用户仅访问 `/my` 与 `/get`；ROOT 管理员可查看全部 |
| `proxy` | 调用体验、SSE 调试、图像生成 | 前端封装 chat 请求、OpenAI 格式、图片上传；SSE 需单独组件展示 stream chunk |
| `async` | 异步任务提交/查询 | 页面提供 `POST /v1/async/*` 示例表单和 `jobId` 轮询查询 |
| `statistics` | 模型/用户日志、组合查询 | 表格 + 图表；支持查询条件缓存与导出 CSV |

## 3. 技术选型与关键实现
### 3.1 状态管理（Pinia）
- `useAuthStore`: 持有 JWT、用户信息、角色、菜单；支持 `login/register/logout/fetchProfile` 调用。
- `useAppStore`: 保存全局 loading、主题、布局。
- `useModelStore`: 缓存模型列表供下拉使用，避免多次接口请求。
- 状态持久化通过 `pinia-plugin-persistedstate` 写入 `localStorage`，但敏感信息如 `ACCESS-KEY` 仅在 session 内持有。

### 3.2 HTTP 客户端
- 封装 `axiosInstance`：
  - Base URL 来自 `.env`（例如 `VITE_API_BASE_URL=https://api.example.com`）。
  - Request 拦截器：附加 `Authorization: Bearer <token>`，如请求头标记 `x-access-key` 则附加 ACCESS-KEY。
  - Response 拦截器：解析 `Result<T>`，统一处理 `code != 200`、401/403 异常，重定向到登录或弹出提示。
- 提供 typed API 函数，每个 controller 对应文件，如 `api/auth.ts`, `api/providers.ts`，便于调用方按接口文档传参。

### 3.3 路由与权限
- Vue Router 采用 `createWebHistory`，划分公开路由（登录/注册/推理体验页）与受限路由。
- 全局 `beforeEach` 守卫：
  1. 未登录访问受限路由 → 跳转登录页。
  2. 已登录但无权限（角色与页面 meta `roles` 不匹配） → 跳转 403 页面。
- 根据角色加载菜单（ROOT / MODEL_ADMIN / USER）。

### 3.4 表单与校验
- 所有表单对应后端 DTO 字段，使用 `yup`/`vee-validate` 或组件库内建校验规则；例如模型创建表单校验 `modelIdentifier` 唯一性（前端可在 blur 时触发后端校验或在提交时提示错误）。
- 表格、搜索表单抽象为组件：`CrudTable`（分页、操作列、条件查询），`SearchPanel`（统一折叠/展开行为）。

### 3.5 SSE 流式处理
- 单独封装 `useSseChat` hook：
  - 接收 `OpenAiChatRequest`，使用 `EventSource` 或 `fetch + ReadableStream` 连接 `/v1/v2/chat/stream`。
  - 解析 chunk JSON，维护当前消息与工具调用列表。
  - 支持中断、超时、错误提示；同时记录 `Result` 错误 chunk。
- UI 上提供“流式输出”视图，展示 tokens、finish_reason 等状态。

### 3.6 文件与图片上传
- 图像生成需支持 URL/Base64。建议封装上传组件，将图片转换为 Base64 后传入 `ImageInput.base64`；若集成 OSS，可返回 URL。
- 注意：前端只存储短期 Base64，避免大文件常驻内存。

### 3.7 异步任务查询
- 提供任务创建页：提交 `POST /v1/async/*` 后，回显 `jobId`。
- `AsyncJobDetail` 页面定时轮询 `GET /v1/async/jobs/{jobId}`，或提供“立即刷新”按钮；状态完成后展示 `resultPayload`。
- 轮询间隔建议 3~5s，超时检测/取消请求需覆盖。

## 4. API 接入策略
- 根据文档中每个 controller 的参数表，在 `api/` 目录建立类型定义：
  - 使用 TypeScript interface 与 `axios` 响应泛型，确保字段可选性与后端一致。
  - 统一封装分页响应 `PageResult<T>` (`records`, `total`, `size`, `current`, `pages`)。
- 对 Result 包装的接口，封装 `request<Result<T>>` 自动解包；OpenAI 兼容接口直接返回 `OpenAiChatResponse` 或 SSE 流。
- 所需权限：
  - `ROLE_ROOT_ADMIN` 才能访问用户角色分配和所有订单列表。
  - `ROLE_MODEL_ADMIN` 才能访问模型与统计模块。
  - 普通用户只能访问 Access Key、自有订单、MyLog 等。

## 5. 需要注意的关键点
1. **令牌与 Key 存储**：JWT 存 `localStorage`/`sessionStorage`，ACCESS-KEY 不持久化，只在推理调试页中输入并由用户管理，防止泄露。
2. **错误码映射**：结合 `ResultCode` 定义，在前端设置错误提示字典（如 `401xxx` → “请重新登录”，`600xxx` → “模型配置错误”）。
3. **日期时间处理**：使用 `dayjs`，确保与后端 `ISO-8601` / `yyyy-MM-dd HH:mm:ss` 格式兼容，尤其是统计查询。
4. **表单参数与 DTO 对齐**：严格按照文档中的字段类型和可选项构造请求，避免空字符串触发 `@NotBlank` 约束。
5. **SSE/流式兼容**：Safari 等浏览器对 `EventSource` 的 header 支持有限，必要时 fallback 到 `fetch` + `ReadableStream`。
6. **国际化与可维护性**：配置 i18n，便于未来扩展英文界面；模块化 API 和 store，避免文件膨胀。
7. **安全与权限**：
   - 所有后台接口都需校验 JWT 角色，前端仅做提示与导航控制，避免依赖前端控制权限。
   - 对于 `assign-role` 等敏感操作，增加二次确认（Modal + 口令校验）。
8. **部署交付**：通过 `vite build` 输出静态资源，部署到 Nginx；反向代理 `/v1` 到 Spring Boot，启用 HTTPS，必要时配置 CORS。

## 6. 实现路线图
1. **初始化工程**：`npm create vite@latest frontend -- --template vue-ts`，安装 Element Plus、Pinia、Vue Router、Axios、Day.js、Pinia 持久化插件。
2. **搭建基础设施**：实现路由守卫、axios 拦截器、Pinia store、布局组件。
3. **认证模块**：完成登录/注册/个人中心、密码修改，与后端 `/v1/auth` 对接。
4. **Access Key & 订单**：实现 CRUD/分页功能，打通余额与订单操作链路。
5. **Provider/Model 管理**：完成表格 + 表单；模型统计页对接图表（ECharts）。
6. **推理体验**：开发聊天、OpenAI 兼容、流式、图像 UI；封装 SSE hook、图片上传组件。
7. **异步任务与统计**：实现任务轮询、日志查询、导出。
8. **测试与优化**：使用 Cypress/Playwright 做关键流程回归；集成 eslint/prettier；设置 `.env.production` 指定后端域名。


