# 页面 → 后端 API 映射主表

本表是「原型页面 → 后端接口」映射的**主表**（Standards §9 / §10）。后端实现端点时按本表回溯原型字段；
新增 / 调整原型页后必须同步更新本表，**禁止**只改原型不刷映射表。

- 原型权威：`docs/demo-static/<端>/`（web 端再分 `admin/` 与 `user/`）。
- 契约正文：本目录下对应的接口契约 Markdown。
- 字段命名一律 snake_case；鉴权走请求头 `Jwttoken`；金额字段一律以分为单位（`*_cent`）。

> **当前阶段说明**：短剧业务功能已完成「需求（`docs/feature/**`）→ 原型（`docs/demo-static/**`）」两步，
> 详细设计（本目录契约正文 / `docs/architecture`）尚未启动。因此下表业务行的「契约」列暂指向**需求文档**，
> 待详设阶段落 `docs/api/*.md` 后改指契约正文。接口路径与字段以需求文档与原型为准，两者已逐字对齐。

## web 管理端（`docs/demo-static/web/admin/`，前端 3001 / 后端 8080，`role_code=PLATFORM_ADMIN`）

| 原型页 | 前端路由 | 后端接口 | 鉴权 | 契约 |
| --- | --- | --- | --- | --- |
| `login.html` | `/login` | `POST /bgssai/admin/login` | 公开（IP 限流） | [`auth-login.md`](./auth-login.md) |
| `home.html` | `/home` | -（消费登录响应缓存的身份字段） | 需 `Jwttoken` | - |
| `login-logs.html` | `/login-logs` | `GET /bgssai/admin/login-logs?page_num&page_size` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`auth-login.md`](./auth-login.md) |
| `content-review.html` | `/reviews` | `GET /bgssai/admin/reviews/list?page_num&page_size&review_status&risk_level&keyword` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`../feature/content-compliance.md`](../feature/content-compliance.md) |
| `review-detail.html` | `/reviews/:review_id` | `GET /bgssai/admin/reviews/detail?review_id` · `POST /bgssai/admin/reviews/approve` · `POST /bgssai/admin/reviews/reject` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`../feature/content-compliance.md`](../feature/content-compliance.md) |
| `ai-models.html` | `/ai-models` | `GET /bgssai/admin/ai-models/list?ability_type` · `POST /bgssai/admin/ai-models/create` · `/update` · `/set-default` · `/toggle-enabled` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`../feature/ai-capability-usage.md`](../feature/ai-capability-usage.md) |
| `quality-templates.html` | `/quality-templates` | `GET /bgssai/admin/quality/templates/list` · `POST /bgssai/admin/quality/templates/create` · `/update` · `/toggle-enabled` · `POST /bgssai/admin/quality/threshold-save` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`../feature/quality-pipeline.md`](../feature/quality-pipeline.md) |
| `usage-cost.html` | `/usage-cost` | `GET /bgssai/admin/usage/summary?start_date&end_date&ability_type` · `GET /bgssai/admin/usage/by-creator?page_num&page_size&start_date&end_date` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`../feature/ai-capability-usage.md`](../feature/ai-capability-usage.md) |
| `creators.html` | `/creators` | `GET /bgssai/admin/creators/list?page_num&page_size&keyword&enabled_flag` · `POST /bgssai/admin/creators/adjust-quota` · `/toggle-enabled` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`../feature/ai-capability-usage.md`](../feature/ai-capability-usage.md) |

## web 用户端（`docs/demo-static/web/user/`，前端 3002 / 后端 8081，`role_code=USER`）

用户端接口一律只返回当前登录用户本人数据，`user_id` 取自 JWT，不从请求参数取。

| 原型页 | 前端路由 | 后端接口 | 鉴权 | 契约 |
| --- | --- | --- | --- | --- |
| `login.html` | `/login` | `POST /bgssai/user/login` | 公开（IP 限流） | [`auth-login.md`](./auth-login.md) |
| `home.html` | `/home` | -（消费登录响应缓存的身份字段 + 各模块概览接口） | 需 `Jwttoken` | - |
| `login-logs.html` | `/login-logs` | `GET /bgssai/user/login-logs?page_num&page_size`（仅本人记录） | 需 `Jwttoken` | [`auth-login.md`](./auth-login.md) |
| `mcp-tokens.html` | `/mcp-tokens` | `POST /bgssai/mcp-tokens/create` · `/list` · `/revoke`；MCP 调用走 `POST /api/mcp`（Bearer PAT，无 JWT） | 令牌 CRUD 需 `Jwttoken`；`/api/mcp` 自校验 PAT | [`MCP.md`](./MCP.md) |
| `works.html` | `/works` | `GET /bgssai/user/drama-works/list?page_num&page_size&work_status&genre&keyword` | 需 `Jwttoken` | [`../feature/drama-work-management.md`](../feature/drama-work-management.md) |
| `work-create.html` | `/works/create` | `POST /bgssai/user/drama-works/create` · `GET /bgssai/user/quality/templates?genre` | 需 `Jwttoken` | [`../feature/drama-work-management.md`](../feature/drama-work-management.md) · [`../feature/quality-pipeline.md`](../feature/quality-pipeline.md) |
| `work-detail.html` | `/works/:work_id` | `GET /bgssai/user/drama-works/detail?work_id` · `POST /bgssai/user/drama-works/update` · `/archive` · `POST /bgssai/user/episodes/create` · `/update` · `/delete` | 需 `Jwttoken` | [`../feature/drama-work-management.md`](../feature/drama-work-management.md) |
| `script-studio.html` | `/works/:work_id/script` | `POST /bgssai/user/scripts/outline-generate` · `/outline-save` · `/generate` · `/save` · `/rewrite` · `/ready` · `/rollback`；`GET /bgssai/user/scripts/outline` · `/detail` · `/versions`；`GET /bgssai/user/quality/gate-result?gate_code=G1_SCRIPT` | 需 `Jwttoken` | [`../feature/script-studio.md`](../feature/script-studio.md) · [`../feature/quality-pipeline.md`](../feature/quality-pipeline.md) |
| `story-map.html` | `/works/:work_id/story-map` | `POST /bgssai/user/story-map/generate` · `/relation-save` · `/blocking-save`；`GET /bgssai/user/story-map/tension?episode_id` · `/tension-band?work_id` · `/relations?work_id&as_of_episode_no` · `/blocking?shot_id` · `/scene-band?episode_id` · `/appearance-heat?work_id` | 需 `Jwttoken` | [`../feature/story-map.md`](../feature/story-map.md) |
| `character-bible.html` | `/works/:work_id/characters` | `GET /bgssai/user/characters/list?work_id` · `POST /bgssai/user/characters/create` · `/portrait-generate` · `/lock` · `/update` · `/delete` · `/bind-voice`；`GET /bgssai/user/characters/affected-shots`；`GET /bgssai/user/scene-cards/list` 及同名写接口 | 需 `Jwttoken` | [`../feature/character-bible.md`](../feature/character-bible.md) |
| `storyboard.html` | `/works/:work_id/storyboard` | `POST /bgssai/user/shots/split-from-script` · `/update` · `/split` · `/merge` · `/move` · `/insert` · `/delete` · `/lock` · `/unlock` · `/select-candidate`；`GET /bgssai/user/shots/list?episode_id&page_num&page_size&shot_status` · `/candidates` · `/prompt-preview`；`POST /bgssai/user/compliance/precheck`；`GET /bgssai/user/quality/gate-result?gate_code=G2_STORYBOARD` | 需 `Jwttoken` | [`../feature/storyboard-workbench.md`](../feature/storyboard-workbench.md) · [`../feature/content-compliance.md`](../feature/content-compliance.md) |
| `previz.html` | `/works/:work_id/previz` | `POST /bgssai/user/previz/generate` · `/update` · `/rewrite` · `/draft-generate` · `/promote`；`GET /bgssai/user/previz/list?episode_id` · `/continuity-check?episode_id`；`GET /bgssai/user/prompts/check?shot_id` · `/diff`；`POST /bgssai/user/prompts/apply-to-shots` | 需 `Jwttoken` | [`../feature/previz-text-storyboard.md`](../feature/previz-text-storyboard.md) |
| `task-center.html` | `/works/:work_id/tasks` | `POST /bgssai/user/ai-tasks/estimate` · `/submit` · `/retry` · `/cancel`；`GET /bgssai/user/ai-tasks/list?page_num&page_size&task_type&task_status&batch_id&episode_id` · `/batch-progress?batch_id` | 需 `Jwttoken`；`submit` / `retry` 加 `@RateLimit`（USER 维度） | [`../feature/shot-generation.md`](../feature/shot-generation.md) |
| `voice-studio.html` | `/works/:work_id/voice` | `POST /bgssai/user/dubbings/submit` · `/regenerate` · `/align` · `/apply-voice-to-character`；`GET /bgssai/user/dubbings/list?episode_id` · `GET /bgssai/user/voices/list`；`GET /bgssai/user/subtitles/detail?episode_id` · `POST /bgssai/user/subtitles/regenerate` · `/update` · `/style-save` | 需 `Jwttoken` | [`../feature/voice-subtitle.md`](../feature/voice-subtitle.md) |
| `compose-export.html` | `/works/:work_id/compose` | `GET /bgssai/user/films/precheck?episode_id` · `/timeline?episode_id` · `/detail?episode_id` · `/download-url?asset_id`；`POST /bgssai/user/films/compose` · `/settings-save` · `/export`；`POST /bgssai/user/reviews/submit` · `/withdraw`；`GET /bgssai/user/reviews/list?work_id` | 需 `Jwttoken` | [`../feature/film-compose-export.md`](../feature/film-compose-export.md) · [`../feature/content-compliance.md`](../feature/content-compliance.md) |
| `quality-report.html` | `/works/:work_id/quality` | `POST /bgssai/user/quality/gate-run` · `/force-pass` · `/variants-generate` · `/variant-select`；`GET /bgssai/user/quality/gate-result?gate_code&episode_id` · `/variants?variant_node&target_id` · `/profile?work_id` · `/templates?genre` | 需 `Jwttoken` | [`../feature/quality-pipeline.md`](../feature/quality-pipeline.md) |
| `usage-billing.html` | `/usage` | `GET /bgssai/user/usage/summary?start_date&end_date` · `/records?page_num&page_size&work_id&ability_type&start_date&end_date` | 需 `Jwttoken` | [`../feature/ai-capability-usage.md`](../feature/ai-capability-usage.md) |

## iOS 端（`docs/demo-static/ios/`）

本产品暂不含 iOS 工程，原型为占位页，暂无映射条目；支持 iOS 端后在此补齐。

## Android 端（`docs/demo-static/android/`）

本产品暂不含 Android 工程，原型为占位页，暂无映射条目；支持 Android 端后在此补齐。

## 业务错误码约定（跨页面统一）

| 错误码 | 含义 | 主要出现处 |
| --- | --- | --- |
| `2002` | token 失效 / 上下文无身份 | 全部受保护接口 |
| `4003` | 无权限或越权访问他人数据 | 全部业务接口 |
| `4006` | 业务规则拒绝（前置未就绪、必填未填、被拦截对象提交等） | 剧本定稿 / 拆镜 / 生成 / 合成 / 审核处置 |
| `4007` | 余额不足 | 任务提交 |
| `4008` | 超作品预算未确认 | 任务提交 |
| `4009` | 版本冲突（`script_version` / `shot_version` 不匹配） | 剧本 / 镜头保存 |
| `4010` | 一致性指纹缺失 | 角色定妆 |
| `4011` | 在途任务超上限 | 任务提交 |
| `4012` | 成片已过期不可下载 | 成片下载 |
| `4013` | 重复提交（已在审核中） | 提交审核 |
| `4014` | 该 AI 能力当前不可用（组内无启用模型） | 任务提交 |
| `4015` | 月度用量触顶 | 任务提交 |
| `4016` | 未出高清首帧即提交视频（阶梯跳级约束） | 视频任务提交 |
| `429` | 限流 | 登录 / 任务提交 / 重试 |

## 维护规约

1. **新增页面**：先在对应端 `PAGE-FLOW.html` 登记入口 / 出口，再在本表补行，最后写实现代码。
2. **改接口路径 / 字段**：同步改契约 Markdown、原型内联 mock 与本表，三处口径一致。
3. **删页面**：删除本表对应行，并核对对应端 `PAGE-FLOW.html` 无悬空节点。
4. **CR 校验**：涉及原型或前端路由的 PR，CR 阶段核对本表是否已同步（Standards §9 / §10）。
