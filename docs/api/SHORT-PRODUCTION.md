# 短剧制作闭环 API（MVP 切片）

统一封装 `{ "code": "0", "message": "OK", "success": true, "result": {} }`。字段 snake_case。
POST 走 JSON body。鉴权请求头 `Jwttoken`。金额单位分。详设见
[`../architecture/short-production-design.md`](../architecture/short-production-design.md)。

分页：`page_num` / `page_size` / `total_size`（`PageResponse`）。

AI 按 `ai_model_config.provider_code` 路由：首批 TEXT / VIDEO / VOICE / ASR / MUSIC 生产模型统一走
`TENCENT_TOKENHUB`，IMAGE 保留既有直连 Provider，COMPOSE 走本机 `FFMPEG`（部署机需预装
ffmpeg，缺失时返回 `ABILITY_UNAVAILABLE`）。无 MOCK 兜底。端点映射见
[`../architecture/tokenhub-model-integration-design.md`](../architecture/tokenhub-model-integration-design.md)。

## 错误码

| code | 含义 |
| --- | --- |
| `2002` | token 失效 |
| `4003` | 无权限或越权 |
| `4006` | 业务规则拒绝 |
| `4007` | 余额不足 |
| `4009` | 版本冲突 |
| `4013` | 重复提交审核 |
| `4015` | 月度用量触顶 |
| `50000001` | 参数非法 |

## 本切片未实现（映射表仍指向需求，调用返回 `4006`）

`POST /bgssai/user/episodes/batch`、质量门 / 剧情图谱 / 用量 / AI 模型配置 /
充值申请 / `ai-tasks/pause` / `ai-tasks/resume` / 提示词 check-diff / 场景卡批量定妆以外的扩展接口。

`GET /bgssai/user/quality/templates` 返回空 `template_list`，创建作品不依赖模板。

---

## 用户端作品

### GET /bgssai/user/short-works/list

query：`page_num` `page_size` `work_status` `genre` `keyword`

`result` 为作品数组：`work_id` `work_title` `one_line_pitch` `genre` `genre_name` `work_status`
`work_status_name` `episode_count` `updated_time` `cost_spent_cent`。

### POST /bgssai/user/short-works/create

```json
{ "work_title": "退婚三年后她成了首富",
  "one_line_pitch": "被退婚的落魄千金三年后带着资本回归" }
```

可选：`genre` `episode_count` `episode_duration_sec` `visual_style`。默认 `REVENGE` / 24 / 90 /
`MODERN_URBAN`，画幅固定 `VERTICAL_9_16`。创建后插入等量剧集占位，并入队 `TEXT` 大纲任务。

响应：`work_id` `task_id`。

### GET /bgssai/user/short-works/detail?work_id=

含 `episode_list`（六阶段状态）。

### POST /bgssai/user/short-works/update

`work_id` + 可改字段（`work_title` `one_line_pitch` `genre` `visual_style` `cost_budget_cent`
`quality_threshold_score` `skip_level_check_flag`）。

### POST /bgssai/user/short-works/archive

`{ "work_id", "archived_flag": 1 }`

### POST /bgssai/user/episodes/create|update|delete

create：`work_id` + `episode_title`（追加末尾）。
delete：已合成（`compose_status=READY`）返回 `4006`。

---

## 剧本

### POST /bgssai/user/scripts/outline-generate

`{ "work_id", "model_code": "hy4-preview" }` → `{ "task_id" }`。`model_code` 可选，省略时使用
TEXT 默认模型。完成后写 `short_work.outline_json` 并回写各集标题/梗概/钩子。

### GET /bgssai/user/scripts/outline?work_id=

### POST /bgssai/user/scripts/outline-save

`{ "work_id", "episode_list": [ { "episode_no", "episode_title", "episode_synopsis", "ending_hook" } ] }`

### POST /bgssai/user/scripts/generate

`{ "episode_id", "extra_prompt", "model_code": "kimi-k3" }` → `{ "task_id" }`。
`model_code` 可选；大纲未出返回 `4006`。

### GET /bgssai/user/scripts/detail?episode_id=

含 `scene_list` `rhythm_check` `script_version` `script_status`。

### POST /bgssai/user/scripts/save

`{ "episode_id", "script_version", "scene_list" }`。版本不匹配 `4009`。

### POST /bgssai/user/scripts/rewrite

`{ "episode_id", "scene_no", "line_no", "instruction", "model_code": "deepseek-v4-pro" }`。
`line_no`、`model_code` 可空，分别表示整场和使用 TEXT 默认模型。入队 TEXT 任务。

### POST /bgssai/user/scripts/ready

`{ "episode_id", "force_ready_flag" }`。总分 &lt; 60 且未强制则 `4006`。BLOCK 预检拦截。
定稿时抽出角色/场景为 `CANDIDATE`。

### GET /bgssai/user/scripts/versions?episode_id=

### POST /bgssai/user/scripts/rollback

`{ "episode_id", "script_version" }`

---

## 角色与场景卡

### GET /bgssai/user/characters/list?work_id=

### POST /bgssai/user/characters/create|update|delete|lock

lock：`{ "character_id", "asset_id" }`

### POST /bgssai/user/characters/portrait-generate

`{ "character_id", "candidate_count" }`（2/4/8，默认 4）→ `{ "task_id" }`

### GET /bgssai/user/scene-cards/list?work_id=

### POST /bgssai/user/scene-cards/create|lock|portrait-generate

---

## 分镜

### POST /bgssai/user/shots/split-from-script

`{ "episode_id" }`。剧本须 `READY`。已 `LOCKED` 镜头保留。

### GET /bgssai/user/shots/list?episode_id=&page_num=&page_size=&shot_status=

`GET /bgssai/user/shots/list` 每镜另回 `look_plain`（这一镜观众会看到什么）、`prompt_score`、`previz_level`。

### POST /bgssai/user/shots/update|lock|unlock

update 可改 `shot_desc` `dialogue_text` `shot_size` `camera_move` `shot_duration_sec` `extra_prompt`，
并重算 `image_prompt`。

---

## 任务

### POST /bgssai/user/ai-tasks/estimate

`{ "episode_id", "shot_ids", "task_type", "auto_next_flag", "model_code?", "next_model_code?" }` → `{ "task_count", "estimated_cost_cent", "model_code", "unit_price_cent" }`

### POST /bgssai/user/ai-tasks/submit

同上 + 余额校验。返回 `{ "batch_id", "task_id_list" }`。USER 限流。

### GET /bgssai/user/ai-tasks/list

query：`page_num` `page_size` `task_type` `task_status` `batch_id` `episode_id` `work_id`

任务完成后可返回脱敏的 `result_json`；MUSIC 任务仅含 `bgm_asset_id`，不含供应商临时 URL。

### GET /bgssai/user/ai-tasks/batch-progress?batch_id=

`{ "total", "queued", "running", "succeeded", "failed", "canceled", "progress_percent" }`

### POST /bgssai/user/characters/portrait-generate | /bgssai/user/scene-cards/portrait-generate

`{ "character_id" | "scene_card_id", "candidate_count", "model_code" }`。
`model_code` 可选，缺省用该能力的默认模型；取值来自 `GET /bgssai/user/ai-models/list?ability_type=IMAGE`。

### POST /bgssai/user/ai-tasks/retry|cancel

`{ "task_id" }`。retry 可带 `fallback_model_flag`。

---

## 配音与字幕

### POST /bgssai/user/dubbings/submit

```json
{ "episode_id": "e-20001", "model_code": "minimax-speech-2.8-hd" }
```

按有台词且存在音色绑定的镜头入队 VOICE，`model_code` 可选。返回
`{ "batch_id", "task_id_list", "task_count" }`。

### POST /bgssai/user/dubbings/regenerate

`{ "dubbing_id", "model_code" }` → `{ "task_id" }`。重新生成后以供应商返回音频时长重新计算对齐。

### GET /bgssai/user/dubbings/list?episode_id=

### POST /bgssai/user/subtitles/transcribe

```json
{ "episode_id": "e-20001", "model_code": "hy-asr-3.0-preview", "confirm_overwrite_flag": 1 }
```

仅为已有配音素材创建 ASR 任务。检测到人工修改字幕且未确认覆盖时返回 `4006`；无配音素材时同样拒绝且
不扣费。返回 `{ "batch_id", "task_id_list", "task_count" }`。

### GET /bgssai/user/subtitles/detail?episode_id=

### POST /bgssai/user/subtitles/update|regenerate|style-save

沿用现有字幕编辑、按台词重建与作品级样式契约。

---

## 成片与审核

### GET /bgssai/user/films/precheck?episode_id=

未锁定镜头列表；BLOCK 命中。

### GET /bgssai/user/films/timeline?episode_id=

### POST /bgssai/user/films/compose

`{ "episode_id" }` → `{ "task_id" }`

### POST /bgssai/user/films/bgm-generate

```json
{
  "work_id": "w-10001",
  "episode_id": "e-20001",
  "model_code": "minimax-music-v2.6",
  "prompt": "悬疑、克制、低频弦乐，适合都市复仇短剧",
  "is_instrumental": 1
}
```

返回 `{ "task_id" }`。任务成功后的 `result_json` 含 `bgm_asset_id`；生成只增加候选，不自动删除或
覆盖已选 BGM。

### POST /bgssai/user/films/settings-save

`{ "work_id", "episode_id", "compose_setting": { "bgm_asset_id", "bgm_volume", "duck_flag" } }`。
选用的 BGM 必须是当前用户、当前作品、当前剧集下的 AUDIO 候选。

### GET /bgssai/user/films/detail?episode_id=

增加 `bgm_asset_id`、`bgm_url` 与
`bgm_candidate_list[{ "asset_id", "file_url", "duration_sec", "created_time", "selected_flag" }]`。

### GET /bgssai/user/films/download-url?asset_id=

`{ "download_url", "expires_at" }`。download_url 为同源资产地址。

### POST /bgssai/user/reviews/submit|withdraw

submit：`{ "episode_id" }`。成片须 READY。

### GET /bgssai/user/reviews/list?work_id=

### POST /bgssai/user/compliance/precheck

`{ "check_scope": "SCRIPT"|"SHOT"|"FILM", "episode_id" }`

```json
{ "risk_level": "PASS", "hit_list": [
  { "risk_level": "WARN", "risk_category": "VIOLENCE", "scene_no": 1, "line_no": 2,
    "hit_text": "...", "suggestion": "..." }
] }
```

### GET /bgssai/user/assets/{asset_id}

需鉴权（`Jwttoken`），且只返回归属本人的资产。返回 `{ "download_url", "expire_at" }`：
内联 SVG 种子返回 `data:` URL，其余返回对象存储预签名 URL（1800 秒）。

注意本接口**返回 JSON、不返回图片字节**，因此不能直接塞进 `<img src>`。
列表类接口（`shots/list`、`preview/summary`、`characters/list` 的候选图）已直接返回
可渲染的 `first_frame_url` / `video_url` / `file_url`，前端渲染图片一律用这些字段。

---

## 管理端

### GET /bgssai/admin/reviews/list

query：`page_num` `page_size` `review_status` `keyword`

### GET /bgssai/admin/reviews/detail?review_id=

### POST /bgssai/admin/reviews/approve

`{ "review_id", "review_comment" }`

### POST /bgssai/admin/reviews/reject

```json
{ "review_id", "review_comment": "",
  "reject_item_list": [
    { "risk_category": "VIOLENCE", "episode_no": 1, "shot_no": 3, "line_no": null,
      "reject_desc": "血腥过线", "suggestion": "改为推搡" }
  ] }
```

### GET /bgssai/admin/creators/list

query：`page_num` `page_size` `keyword` `enabled_flag`（1 正常 / 0 冻结）

### POST /bgssai/admin/creators/toggle-enabled

`{ "user_id", "enabled_flag": 0 }`
