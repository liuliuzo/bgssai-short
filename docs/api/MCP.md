# BGSSAI Short MCP 接入契约

平台用户端提供 MCP（Model Context Protocol）Server，供 AI 工具以工具查询本站能力。

**Tagline**：Connect your AI tools to bgssai

**MCP Connectors（支持的客户端）**：Claude · Codex · Cursor · Grok Bot · Bgssai Bot

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
| `list_login_logs` | 当前用户本人登录日志分页，包含原 IP 与本地数据解析的 `ip_location`（仅地区参考） |
| `get_liveness` | 进程存活探针（不触碰数据库） |
| `get_readiness` | 就绪探针（critical 组件） |
| `list_works` | 分页查询当前用户本人的短剧作品列表 |
| `get_work` | 获取当前用户本人的短剧作品详情及分集概要 |
| `create_work` | 为当前用户创建新的短剧作品草稿 |
| `list_episodes` | 查询当前用户指定短剧作品下的分集列表 |
| `save_episode_script` | 保存或更新当前用户本人分集的剧本正文与版本 |
| `list_shots` | 查询当前用户本人分集下的分镜镜头列表 |
| `submit_render_task` | 提交分集或分镜 AI 渲染/合成任务（需显式 `confirm_cost=true`；每次调用创建新任务，不支持自动重试去重） |
| `publish_episode_to_media` | 将已过审成片发布到 Tube 平台（需显式 `confirm_publish=true`） |

### 制作闭环工具

| 工具 | 输入与正式行为 |
|---|---|
| `list_voices` | 无参数；复用正式 DubbingService 声音目录，返回产品 voice_id；不自造供应商声音。 |
| `set_character_voice` | `character_id, voice_id`；voice_id 必须存在正式目录，角色必须属于本人；仅保存配置，不自动重生成配音。 |
| `list_models` | 可选 `ability_type`；直接使用用户模型目录，返回 `model_list` 的公开编码、能力、价格。只列已启用记录，不能据此保证实时供应商可用。目录未提供时长限制时不推断。 |
| `ready_episode_script` | `episode_id`，可选 `force_ready=false`；正式剧本定稿、节奏与合规检查、角色/场景提取。显式 force 只接受低节奏评分，不放行合规 BLOCK。 |
| `split_storyboard` | `episode_id, confirm_replace=true`；READY 剧本每行拆一个镜头，替换未锁定镜头。须先保存未完成工作并明确确认。 |
| `save_storyboard` | `episode_id, shots:[{shot_id, shot_desc?, dialogue_text?, shot_size?, camera_move?, shot_duration_sec?, extra_prompt?}]`；1-100 镜，已存在且属于该分集，锁定镜头拒绝。整个调用为事务，正式 ShotService 重算提示词。 |
| `list_reference_cards` | `work_id`；正式一致性面板返回角色/场景/道具卡及 `candidate_list`。 |
| `generate_reference_image` | `work_id, card_type:CHARACTER/SCENE/PROP, card_id, confirm_cost=true`，可选 `candidate_count:2/4/8`（默认 2）、`model_code`、非空 `appearance_prompt`；先经正式角色或场景/道具编辑保存描述，再提交正式参考图任务。保留作品归属与价格确认校验，不替用户锁定候选。 |
| `lock_reference_card` | `work_id, card_type, card_id, asset_id`；只能选择该卡真实 `candidate_list` 中的资产，正式锁定和镜头过期规则保持。 |
| `get_render_task` | `task_id`；复用任务中心所有权检查与安全输出，返回状态、进度、公开错误、`result_json`、费用；不暴露 request_json。 |
| `get_asset_download` | `asset_id`；直接委托正式 AssetService.getDownload，仅本人资产返回 `download_url, expire_at`，支持首帧、视频和配音验收；链接到期后重新查询。 |
| `lock_shot` | `episode_id, shot_id, confirm_review:true`；只接受 JSON boolean true，验证本人同集镜头、实际 video_asset_id 非空及 VIDEO_READY/LOCKED 状态，再委托正式 ShotService.lock(req,true)。不接受 asset_id/video_asset_id 等覆盖字段；不自动生成、合成或发布。 |
| `get_episode_film` | `episode_id`；返回 `film/precheck/dubbing/subtitles`。`film.download_url` 为正式服务解析的可播放链接，过期或未生成时为空，不返回伪造成片。重新查询可刷新临时链接。 |
| `save_film_settings` | `episode_id`，可选 `bgm_asset_id`（空串取消）、`burn_subtitle`、`aspect_ratio:9:16/16:9/1:1`；从本人分集推导作品，正式服务校验本集 BGM。设置由同作品各集共享，不自动重合成。 |
| `regenerate_subtitles` | `episode_id, confirm_replace:true`；从已有配音和当前分镜重建本人分集字幕，覆盖已有字幕（含人工编辑），须先保存需保留的编辑。仅接受 boolean true 和这两个字段；返回正式 subtitle_list，不创建模型任务、不收费、不自动重合成。 |
| `align_dubbing` | `episode_id, shot_id, align_action:PAD_SILENCE/ADJUST_SPEED/EXTEND_SHOT`；正式对齐，无新增收费任务。EXTEND_SHOT 改变镜头时长，仍需核对视频与成片。 |

`submit_render_task` 保留 `episode_id, confirm_cost=true`，类型扩展为 `IMAGE/VIDEO/VOICE/MUSIC/COMPOSE`（默认 COMPOSE）：

- IMAGE/VIDEO → `AiTaskService.submit`：可选 `shot_id` 与公开 `model_code`；省略镜头时按整集处理。画面来自已保存分镜，VIDEO 要求已有首帧；引用素材必须定妆。返回 `task_id_list/batch_id`。
- VOICE → `DubbingService.submit`：从镜头台词、已配置角色声音或默认旁白生成正式配音，保留配音记录与字幕链路；可选单镜头和模型。返回 `task_id_list/batch_id`。
- MUSIC → `FilmService.generateBgm`：`prompt` 必填；可选 `lyrics/is_instrumental`（默认 true）/模型，不接受 `shot_id`。生成后通过 `get_episode_film` 的 `bgm_candidate_list` 选择资产，再调用 `save_film_settings`；不自动覆盖选曲。
- COMPOSE → `FilmService.compose`：不接受 `shot_id/model_code`；沿用正式合成器、素材与配音预检、费用估算及余额检查，使用已有配音/字幕/BGM。返回 `task_id`。
- 非 MUSIC 输入 `prompt/lyrics/is_instrumental` 会明确拒绝，避免静默丢弃创作要求；用 `save_storyboard` 保存画面和台词。
- 任务采用正式队列与工作线程的余额校验和结算。MCP 不直接插入任务、不伪造成功，不改变供应商或存储配置。所有生成调用都可能产生新费用，不自动重试或宣称幂等。

建议顺序：保存 `scene_list` → READY → 列出参考卡 → 生成/查询/选择锁定参考图 → 拆分 → 列出与编辑分镜 → IMAGE → 查询成功 → VIDEO → 查询成功 → VOICE/MUSIC → 查询成功并对齐、选曲、字幕设置 → get_asset_download 验收媒体 → 显式 lock_shot → 合成预检 → COMPOSE → 查询任务与成片 MP4。

`tools/list` 共 27 个工具。`get_asset_download` 不返回存储配置或凭据，只保留正式下载响应。
`lock_shot` 不接收 `user_id` 或任意资产覆盖，缺省/false/字符串/数字确认均拒绝；外人资源和跨集镜头拒绝。
重复确认已有 LOCKED 镜头仍走正式服务，不会重生成。锁定不代表已通过 FilmService 的完整合成预检。

`scene_list` 的 items 为场次对象（`scene_no, scene_location, scene_time?, scene_summary, line_list`）；`line_list` 的 items 为台词对象（`line_no, character_name, line_text, emotion?, estimated_sec`）。每行按正式拆分逻辑产生一个镜头，所有数组 schema 均声明 items。`tools/list` 为实际可调用工具的权威契约。

此流程不自动公开发布；`publish_episode_to_media` 仍单独要求 `confirm_publish=true` 与审核通过。SQL 无变更：仅包装现有生产服务和实体。

所有业务读写工具均按 PAT 绑定的 `user_id` 严格校验资源归属（跨用户访问拒绝），计费任务与对外发布须显式传入确认参数。

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

### Bgssai Bot

- **PAT**：与 Grok Bot 相同，在 Bgssai Bot 中配置 MCP URL + Bearer 令牌。
- **用 Chat 登录**：可由平台下发 MCP（不经过本页手工粘贴令牌）；本页只管理 PAT。

## 安全边界

- 身份只从 PAT 的 `user_id` 进入 `UserContext`，工具入参不得带 `user_id`
- 临时开发期 `token_hash` 列存明文，上生产前按 interim-plaintext 政策改回 SHA-256

### 多角色对白

正式拆分将每行 `character_name` 对应角色放在该镜角色引用的第一位，其余画面人物保持原顺序。配音服务支持引用对象与 ID 数组两种既有格式，按首位说话者选用已配置声音；不会因同场提到其他人而任意用第一个建档角色。先调用 `list_voices`，为各角色 `set_character_voice`，再提交 VOICE。声音变更只影响后续生成，不自动重录旧音频。

当前公司 bgssai-tts/edge-tts 由正式适配层将产品声音编号转换为已核实的中文 Edge 音色。客户端应使用 list_voices 的产品编号；其他模型不套用 Edge/MiniMax 互换映射，真实音频仍须验收。
