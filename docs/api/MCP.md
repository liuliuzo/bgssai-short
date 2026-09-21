# BGSSAI Short MCP 接入契约

平台用户端提供 MCP（Model Context Protocol）Server，供 AI 工具以工具查询本站能力。

**Tagline**：Connect your AI tools to bgssai

**MCP Connectors（支持的客户端）**：Claude · Codex · Cursor · Grok Bot · bgssai-bot

## 端点与鉴权

- **MCP 端点**：`POST https://<用户端域名>/api/mcp`（Streamable HTTP，无状态；GET/DELETE 返回 405）
- **协议版本**：协商 `2025-06-18` / `2025-03-26` / `2024-11-05`
- **鉴权**：`Authorization: Bearer bgs_mcp_xxx`
  - 令牌在网页「MCP Connectors」页创建，明文仅创建时显示一次
  - 每人最多 5 枚；吊销立即生效
- 令牌管理（需登录 JWT，`@NeedAop`）：
  - `POST /api/mcp/tokens/create` body `{ "token_name": "Cursor" }` → `result.token` 仅一次
  - `POST /api/mcp/tokens/list` → 掩码列表
  - `POST /api/mcp/tokens/revoke` body `{ "id": 1 }`
  - 过渡别名：`POST /bgssai/mcp-tokens/create|list|revoke`

限流：每用户 60 次/分钟；超限 JSON-RPC `-32000`（不回 HTTP 429）。不支持 JSON-RPC 批量数组（`-32600`）。

## 工具

| 工具 | 说明 |
|---|---|
| `list_login_logs` | 当前用户本人登录日志分页 |
| `get_liveness` | 进程存活探针（不触碰数据库） |
| `get_readiness` | 就绪探针（critical 组件） |

不提供：短剧剧集 / 播放 / 支付 / 管理端操作。本产品 MCP 不发明领域工具。

## 客户端配置（Connectors）

端点记为 `ENDPOINT=https://<域名>/api/mcp`，令牌记为 `TOKEN=bgs_mcp_你的令牌`。

### Claude

```bash
claude mcp add --transport http bgssai-short ENDPOINT \
  --header "Authorization: Bearer TOKEN"
```

### Codex

```bash
codex mcp add bgssai-short --url ENDPOINT \
  --header "Authorization: Bearer TOKEN"
```

### Cursor

`.cursor/mcp.json`（或用户级 `~/.cursor/mcp.json`）：

```json
{
  "mcpServers": {
    "bgssai-short": {
      "url": "ENDPOINT",
      "headers": { "Authorization": "Bearer TOKEN" }
    }
  }
}
```

### Grok Bot

在 Grok Bot 的 MCP / 远程工具配置中填写：

- MCP URL：`ENDPOINT`
- Auth：`Authorization: Bearer TOKEN`
- Transport：Streamable HTTP（POST）

### bgssai-bot

- **PAT**：与 Grok Bot 相同，在 bgssai-bot 中配置 MCP URL + Bearer 令牌。
- **用 Chat 登录**：可由平台下发 MCP（不经过本页手工粘贴令牌）；本页只管理 PAT。

## 安全边界

- 身份只从 PAT 的 `user_id` 进入 `UserContext`，工具入参不得带 `user_id`
- 临时开发期 `token_hash` 列存明文，上生产前按 interim-plaintext 政策改回 SHA-256
