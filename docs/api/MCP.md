# BGSSAI Short Drama MCP 接入契约

平台用户端提供 MCP（Model Context Protocol）Server，供 Cursor / Claude 等客户端以工具查询本站能力。

## 端点与鉴权

- **MCP 端点**：`POST https://<用户端域名>/api/mcp`（Streamable HTTP，无状态；GET/DELETE 返回 405）
- **协议版本**：协商 `2025-06-18` / `2025-03-26` / `2024-11-05`
- **鉴权**：`Authorization: Bearer bgs_mcp_xxx`
  - 令牌在网页「MCP 接入」页创建，明文仅创建时显示一次
  - 每人最多 5 枚；吊销立即生效
- 令牌管理（需登录 JWT，`@NeedAop`）：
  - `POST /bgssai/mcp-tokens/create` body `{ "token_name": "Cursor" }` → `result.token` 仅一次
  - `POST /bgssai/mcp-tokens/list` → 掩码列表
  - `POST /bgssai/mcp-tokens/revoke` body `{ "id": 1 }`

限流：每用户 60 次/分钟；超限 JSON-RPC `-32000`（不回 HTTP 429）。不支持 JSON-RPC 批量数组（`-32600`）。

## 工具

| 工具 | 说明 |
|---|---|
| `list_login_logs` | 当前用户本人登录日志分页 |
| `get_liveness` | 进程存活探针（不触碰数据库） |
| `get_readiness` | 就绪探针（critical 组件） |

不提供：短剧剧集 / 播放 / 支付 / 管理端操作。本产品 MCP 不发明领域工具。

## 客户端配置

```json
{
  "mcpServers": {
    "bgssai-short-drama": {
      "url": "https://<域名>/api/mcp",
      "headers": { "Authorization": "Bearer bgs_mcp_你的令牌" }
    }
  }
}
```

## 安全边界

- 身份只从 PAT 的 `user_id` 进入 `UserContext`，工具入参不得带 `user_id`
- 临时开发期 `token_hash` 列存明文，上生产前按 interim-plaintext 政策改回 SHA-256
