# 测试用例：MCP 服务（用户端）

- **功能文档**: `docs/feature/mcp-server.md`
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 5
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-MCP-SERVER-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：无效令牌 `POST /api/mcp` 返回 HTTP 401 + JSON-RPC error -32001。 | 无效令牌 `POST /api/mcp` 返回 HTTP 401 + JSON-RPC error -32001。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：`tools/list` 返回 3 个工具：`list_login_logs` / `get_liveness` / `get_readiness`。 | `tools/list` 返回 3 个工具：`list_login_logs` / `get_liveness` / `get_readiness`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：用户端 MCP Connectors 页可创建 / 列表 / 吊销令牌，并展示五类 Agent 配置指引与 tagline。 | 用户端 MCP Connectors 页可创建 / 列表 / 吊销令牌，并展示五类 Agent 配置指引与 tagline。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：GET /api/mcp 返回 405。 | GET /api/mcp 返回 405。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：JSON-RPC 批量数组返回 -32600。 | JSON-RPC 批量数组返回 -32600。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
