# MCP 服务（用户端）

## 一、背景与目标

- 背景：Claude / Codex / Cursor / Grok Bot / Bgssai Bot 等 MCP 客户端需要以工具调用查询本站登录日志与健康探针，而不是再走浏览器会话 JWT。
- 目标：用户端提供 Streamable HTTP MCP 服务与个人访问令牌；网页「MCP Connectors」页以 tagline「Connect your AI tools to bgssai」引导五类 Agent 接入；工具包装既有登录日志、健康探针及正式短剧制作服务。
- 关联角色：`USER`

## 二、需求澄清（WHAT）

- 用户故事：作为短剧平台用户，我希望在「MCP Connectors」页创建 PAT，并按 Claude / Codex / Cursor / Grok Bot / Bgssai Bot 的指引把本站接到 AI 工具，以便查询本人记录、保存原创剧本和分镜，并经正式制作流程获得本人可播放成片。
- 业务规则：
  1. 端点 `POST /api/mcp` 不走 JWT / RBAC，由 `Authorization: Bearer bgs_mcp_...` 自校验。
  2. 身份从令牌行 `user_id` 注入 `UserContext`，工具入参禁止 `user_id`。
  3. 每人最多 5 枚令牌；吊销立即失效；明文仅创建时回显一次。
  4. 工具覆盖诊断探针与短剧业务读写：`list_login_logs` / `get_liveness` / `get_readiness` / `list_works` / `get_work` / `create_work` / `list_episodes` / `save_episode_script` / `list_shots` / `submit_render_task`（需 `confirm_cost=true`） / `publish_episode_to_media`（需 `confirm_publish=true`）。
  5. 每用户 60 次/分钟（initialize/ping 不计）。
  6. 连接器页须展示 tagline「Connect your AI tools to bgssai」，并为上述五类 Agent 提供可复制配置片段（后端契约不变）。

## 三、验收标准

1. 无效或已吊销令牌 `POST /api/mcp` 返回 HTTP 401 + JSON-RPC error -32001。
2. `tools/list` 返回登录日志、健康探针及作品/分集/剧本/分镜/渲染任务/Tube 发布等业务工具。
3. 业务读写工具严格校验 PAT 归属，越权拒绝；渲染任务需 `confirm_cost=true` 且沿用正式队列与结算（每次调用创建新任务，不自动重试）；发布需 `confirm_publish=true` 且成片已过审。
4. 用户端 MCP Connectors 页可创建 / 列表 / 吊销令牌，并展示五类 Agent 配置指引与 tagline。
5. GET /api/mcp 返回 405；JSON-RPC 批量数组返回 -32600。

## 四、接口契约（snake_case）

详见 [`../api/MCP.md`](../api/MCP.md)。

- `POST /api/mcp`（MCP JSON-RPC，Bearer PAT）
- `POST /api/mcp/tokens/create` body `{ token_name }`
- `POST /api/mcp/tokens/list`
- `POST /api/mcp/tokens/revoke` body `{ id }`

## 五、流程 / 状态机

```
登录用户 → MCP Connectors 页创建 PAT（快照 user_id）
按 Agent 指引配置 Claude / Codex / Cursor / Grok Bot / Bgssai Bot
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
- [x] 代码与数据库（本轮补齐 MCP 制作适配与公开查询；SQL、供应商配置和发布授权不变）

## 制作闭环验收补充（2026-11-11）

- `tools/list` 提供 28 个工具；包括模型与声音目录、角色声音设置、剧本定稿/拆分/结构化保存、参考图生成与定妆、任务查询、资产下载验收、镜头确认锁定、成片查询、字幕/BGM 设置及配音对齐。字段详见 MCP 契约。
- 同一分集保存的所有分镜先逐项验证归属，任一失败不部分写入；不接收伪造用户或直接资产覆盖。
- IMAGE/VIDEO 必须通过正式分镜检查；VOICE 必须创建正式配音记录；MUSIC 使用正式 BGM 服务；COMPOSE 必须通过正式预检。不能通过 MCP 旁路插入缺少 prompt/配音上下文的任务。
- 所有收费工具都要求布尔 `confirm_cost=true`；生成过程使用正式队列、工作线程余额检查及结算。参考图锁定只能选择该卡已生成候选。
- 查询仅本人任务/分集，输出真实错误与结果；成片未成功、已过期或占位 URL 不得冒充可播放 MP4。Tube 对外发布保持单独确认。
- MCP 页面与原型展示上述真实能力；所有 array schema 提供完整 items。测试通过不等于真实模型/成片商业验收，真实制作由本次授权操作另行记录。

- 多角色场次逐行以 character_name 确认说话者，拆分优先绑定该角色；角色声音必须选自正式目录，配音生成保留各角色声音差异。

## 资产验收与镜头确认（2026-11-11）

- `get_asset_download(asset_id)` 仅查询本人资产，直接复用正式 AssetService.getDownload；返回真实
  `download_url` 与 `expire_at`，用于查看首帧、视频和配音。到期后重新查询，不伪造链接、延长原签名或公开资产。
- `lock_shot(episode_id, shot_id, confirm_review=true)` 要求确认参数为 JSON boolean true。
  调用者应先取得并检查该镜视频；缺少确认、false、字符串或数字均拒绝，不自动验收或批量锁定。
- 分集与镜头必须属于当前 PAT 用户且为同一集；镜头实际 video_asset_id 非空，状态只能为
  VIDEO_READY 或 LOCKED。锁定只委托正式 ShotService.lock(req,true)，不接收资产覆盖或直接写数据库。
- 锁定不是成片合成通过；FilmService 的完整预检、费用确认、发布确认保持原样。查询和锁定不触发生成、结算或公开发布。
- 新增验证须覆盖真实资产归属、他人/跨集镜头拒绝、严格确认、未就绪视频拒绝及正式服务委托。
  无数据库结构或新 HTTP 路由；仍通过现有 MCP tools/call。

## 字幕重建（2026-11-11）

- `regenerate_subtitles(episode_id, confirm_replace=true)` 根据已有配音和当前分镜重建本人分集字幕，替换已有字幕（包括人工编辑）。调用前应保存需要保留的编辑，并显式确认替换。
- 只接受 JSON boolean true，拒绝其他字段和非本人分集；委托正式 DubbingService.subtitleRegenerate，不直接写入字幕或修改配音、分镜时长。
- 返回正式 subtitle_list；不创建模型任务、不产生生成费用，也不自动重新合成已有成片。重建后通过 get_episode_film 检查字幕，再按正式合成流程制作。
- SQL、HTTP 路由和模型配置无变更。
