# MCP 服务安全设计

- 威胁：令牌泄露导致本人作品/日志读取或制作请求；工具入参伪造身份；越权资源绑定、重复收费或伪造成片。
- 控制：
  1. `/api/mcp` 在 `/bgssai/**` 之外，自校验 Bearer，不走 JWT 单会话
  2. 调用期 `UserContext` 只注入令牌行 `user_id`，finally 清理
  3. 工具不接受 `user_id`
  4. 生产提交只包装正式 AiTask/Dubbing/Film/Character 服务，保留归属、素材、余额、队列、结算和合成预检；不直接插入任务
  5. 60 次/分钟限流；拒绝 JSON-RPC 批量数组
  6. 明文令牌仅创建响应回显；列表掩码

  7. 收费生成须布尔 confirm_cost=true，分镜替换须 confirm_replace=true，Tube 发布须 confirm_publish=true 且审核通过；不会自动重试重复收费。
  8. 批量分镜验证同分集归属并使用事务；参考图只能锁定该卡真实候选；BGM 沿用正式作品/分集资产检查。
  9. 任务 detail 复用正式所有权及公开 view，不返回 request_json；模型/声音目录不返回密钥或原始供应商配置；角色声音须来自正式产品目录。
  10. 成片真实 URL 与过期/占位保护由 FilmService 保留；MCP 不把未完成或失败任务伪装为可下载成品。
