# MCP 服务安全设计

- 威胁：令牌泄露导致登录日志读取；工具入参伪造身份；把未实现的短剧业务或写操作暴露给模型。
- 控制：
  1. `/api/mcp` 在 `/bgssai/**` 之外，自校验 Bearer，不走 JWT 单会话
  2. 调用期 `UserContext` 只注入令牌行 `user_id`，finally 清理
  3. 工具不接受 `user_id`
  4. 仅包装 ListLoginLogs / QueryLiveness / QueryReadiness
  5. 60 次/分钟限流；拒绝 JSON-RPC 批量数组
  6. 明文令牌仅创建响应回显；列表掩码
