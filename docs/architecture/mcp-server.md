# MCP 服务架构

用户端新增 `com.bgssai.shortai.mcp`：

- `McpEndpointController`：`POST /api/mcp`
- `McpProtocolService`：JSON-RPC 分发 + Bucket4j（60/分钟，拒绝批量数组）
- `McpToolService`：包装登录日志、健康探针与短剧业务读写工具（作品、分集、剧本、分镜、渲染任务提交与 Tube 过审发布，强制校验用户归属与计费/发布显式确认）
- `McpTokenService` + 三个令牌 Controller：创建 / 列表 / 吊销

不引入 MCP SDK；协议帧手工 ObjectNode（camelCase，Jackson 2）。业务 JSON 仍走全局 snake_case。

用户端「MCP Connectors」页（路由 `/mcp-tokens`）对齐 blog 范式：tagline「Connect your AI tools to bgssai」，为 Claude / Codex / Cursor / Grok Bot / bgssai-bot 提供配置指引；令牌 API 不变。

流程见 [`mcp-server.html`](./mcp-server.html)。
