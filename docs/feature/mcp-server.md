# MCP 服务（用户端）

## 一、背景与目标

- 背景：Claude / Codex / Cursor / Grok Bot / bgssai-bot 等 MCP 客户端需要以工具调用查询本站登录日志与健康探针，而不是再走浏览器会话 JWT。
- 目标：用户端提供 Streamable HTTP MCP 服务与个人访问令牌；网页「MCP Connectors」页以 tagline「Connect your AI tools to bgssai」引导五类 Agent 接入；工具仅包装既有登录日志与存活/就绪探针。
- 关联角色：`USER`

## 二、需求澄清（WHAT）

- 用户故事：作为短剧平台用户，我希望在「MCP Connectors」页创建 PAT，并按 Claude / Codex / Cursor / Grok Bot / bgssai-bot 的指引把本站接到 AI 工具，以便查询本人登录日志与服务健康状态。
- 业务规则：
  1. 端点 `POST /api/mcp` 不走 JWT / RBAC，由 `Authorization: Bearer bgs_mcp_...` 自校验。
  2. 身份从令牌行 `user_id` 注入 `UserContext`，工具入参禁止 `user_id`。
  3. 每人最多 5 枚令牌；吊销立即失效；明文仅创建时回显一次。
  4. 工具只读：`list_login_logs` / `get_liveness` / `get_readiness`。不发明短剧领域工具。
  5. 每用户 60 次/分钟（initialize/ping 不计）。
  6. 连接器页须展示 tagline「Connect your AI tools to bgssai」，并为上述五类 Agent 提供可复制配置片段（后端契约不变）。

## 三、验收标准

1. 无效令牌 `POST /api/mcp` 返回 HTTP 401 + JSON-RPC error -32001。
2. `tools/list` 返回 3 个工具：`list_login_logs` / `get_liveness` / `get_readiness`。
3. 用户端 MCP Connectors 页可创建 / 列表 / 吊销令牌，并展示五类 Agent 配置指引与 tagline。
4. GET /api/mcp 返回 405。
5. JSON-RPC 批量数组返回 -32600。

## 四、接口契约（snake_case）

详见 [`../api/MCP.md`](../api/MCP.md)。

- `POST /api/mcp`（MCP JSON-RPC，Bearer PAT）
- `POST /bgssai/mcp-tokens/create` body `{ token_name }`
- `POST /bgssai/mcp-tokens/list`
- `POST /bgssai/mcp-tokens/revoke` body `{ id }`

## 五、流程 / 状态机

```
登录用户 → MCP Connectors 页创建 PAT（快照 user_id）
按 Agent 指引配置 Claude / Codex / Cursor / Grok Bot / bgssai-bot
AI 客户端 Bearer PAT → /api/mcp → 注入 UserContext → tools/call → 既有 Service
吊销 = del_flag=1，立即 401
```

图示见 [`../architecture/mcp-server.html`](../architecture/mcp-server.html)。

## 六、数据模型影响

- 表：`mcp_access_token`（新增，`user_id` VARCHAR，见 `sql/DDL.sql`）。本轮不改表结构。

## 七、原型

- 涉及端：web 用户端
- 页面：`../demo-static/web/user/mcp-tokens.html`
- 跳转：`../demo-static/web/user/PAGE-FLOW.html`

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型
- [x] 设计与契约（`docs/api/MCP.md`、`docs/security/mcp-server.md`）
- [x] 代码与数据库（本轮仅前端 UI + 文档；后端保持不变）
