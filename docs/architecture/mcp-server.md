# MCP 服务架构

用户端新增 `com.bgssai.shortai.mcp`：

- `McpEndpointController`：`POST /api/mcp`
- `McpProtocolService`：JSON-RPC 分发 + Bucket4j（60/分钟，拒绝批量数组）
- `McpToolService`：包装登录日志、健康探针与短剧业务读写工具（作品、分集、剧本、分镜、渲染任务提交与 Tube 过审发布，强制校验用户归属与计费/发布显式确认）
- `McpTokenService` + 三个令牌 Controller：创建 / 列表 / 吊销

不引入 MCP SDK；协议帧手工 ObjectNode（camelCase，Jackson 2）。业务 JSON 仍走全局 snake_case。

用户端「MCP Connectors」页（路由 `/mcp-tokens`）对齐 blog 范式：tagline「Connect your AI tools to bgssai」，为 Claude / Codex / Cursor / Grok Bot / Bgssai Bot 提供配置指引；令牌 API 不变。

流程见 [`mcp-server.html`](./mcp-server.html)。

## 正式制作适配（2026-11-11）

`McpToolService` 保留原工具与确认入口，新增 `McpProductionService`（事务边界）与 `McpProductionSchemas`。适配器不持有任务 Mapper、不直接插入生成任务：IMAGE/VIDEO 委托 AiTaskService，VOICE 委托 DubbingService，MUSIC/COMPOSE 委托 FilmService。既有队列在入队时检查模型流水线，工作线程在执行前检查余额并结算；MCP 不改费用规则。

结构化分镜先通过 ScriptService.ready/ShotService.split 产生正式镜头，再由 save_storyboard 批量委托 ShotService.update。批内先校验所有镜头属于同一已授权分集，事务保证失败回滚。正式提示词由 PromptComposer 重算，沿用锁定与素材门禁。

角色、场景、道具通过 CharacterService 的一致性面板、参考图任务和锁定方法；生成角色参考图时可先传 appearance_prompt 委托 updateCharacter 保存准确外观；保留角色变更导致镜头过期的规则。锁定额外验证 asset_id 属于目标卡的 candidate_list，不允许任意跨卡/跨用户资产。作品共享的合成设置从已授权分集推导 work_id，BGM 资产归属由原服务校验。

`AiTaskService.detail` 复用现有 requireTask 与任务中心 view，返回公开错误和结果，不暴露 request_json。成片查询聚合 FilmService.detail/precheck 与 DubbingService.list/subtitleDetail；真实下载 URL、过期保护、占位 URL 拦截仍由原服务负责。list_models 原样复用公开模型目录，不读取或返回原始配置/密钥，不猜测未提供的时长限制。

无新增 HTTP 路由、数据表、字段或 SQL；全部通过既有 `/api/mcp`。新增工具非幂等，客户端持有返回 task_id 后应查询，不应重复提交。外发 Tube 必须单独明确授权。

`list_voices`/`set_character_voice` 包装正式 DubbingService；配置前验证产品声音目录，正式方法继续校验角色归属。ShotConsistency 新增 speakerName 排序，ShotService.split 从行 character_name 传入；DubbingService 用公共 characterIds 解析器兼容对象和 ID 两种引用格式。声音映射由正式供应商适配器负责，不让客户端填写任意供应商参数。
