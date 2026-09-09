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

返回 `ready`、`unlocked_shots`、`missing_video_shots`、`blocker_list`、`risk_level`。
`ready=true` 要求全部镜头为 `VIDEO_READY`/`LOCKED` 且 `video_asset_id` 非空，且无配音
`MISALIGNED`、合规无 `BLOCK`。仅有 `IMAGE_READY` 草图不得通过预检。

### GET /bgssai/user/films/timeline?episode_id=

返回 `clip_list`、`audio_track`、`subtitle_track`（优先 `short_subtitle`，否则按镜
`dialogue_text` 推算）、`bgm_asset_id` / `bgm_url`、`total_sec`。

### POST /bgssai/user/films/compose

`{ "episode_id" }` → `{ "task_id" }`

合成走 `FFMPEG`：拼接镜头视频，默认烧录简单字幕（`burn_subtitle_flag` 缺省开），可选混入已选 BGM。

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

`{ "work_id", "episode_id", "compose_setting": { "bgm_asset_id", "bgm_volume", "duck_flag", "burn_subtitle_flag" } }`。
选用的 BGM 必须是当前用户、当前作品、当前剧集下的 AUDIO 候选。

### GET /bgssai/user/films/detail?episode_id=

返回 `compose_status`、`film_asset_id`、`film_duration_sec`、`download_url`（可播放的签名/本地 URL，
不是 download-url JSON 接口路径）、以及 `bgm_asset_id`、`bgm_url`、
`bgm_candidate_list[{ "asset_id", "file_url", "duration_sec", "created_time", "selected_flag" }]`。

### GET /bgssai/user/films/download-url?asset_id=

`{ "download_url", "expires_at" }`。download_url 为同源资产地址。

## 分镜画布重排

需求见 [`../feature/storyboard-canvas.md`](../feature/storyboard-canvas.md)。
画布视图**不新增数据模型**，除本接口外全部复用既有接口。

### POST /bgssai/user/shots/reorder

```json
{ "episode_id": "e-20001", "scene_no": 3, "shot_id_list": ["s-40012", "s-40009", "s-40010"] }
```

按数组顺序重排该场次镜号，从该场次原有最小镜号起连续编号；其余场次不受影响。
重排只改顺序：不删素材、不改状态、不触发任何重抽。

响应：

```json
{ "result": { "reordered_count": 3, "stale_shot_no_list": [13] } }
```

`stale_shot_no_list` 为因上一镜变化而被置 `stale_flag=1` 的镜号
（仅 `frame_link_mode=INHERIT_PREV` 且同场次者），系统不自动为它们提交新任务。

错误码：`50000001` 参数非法（列表为空 / 镜头不属于该场次）/ `4003` 越权 /
`4006` 含锁定镜头不可重排 / `429000` 限流。

## 首尾帧衔接

需求见 [`../feature/frame-continuity.md`](../feature/frame-continuity.md)。全部需 `Jwttoken`。

### POST /bgssai/user/shots/frame-link-save

```json
{ "shot_id": "s-40005", "frame_link_mode": "INHERIT_PREV" }
```

`frame_link_mode`：`NONE`（各拍各的）/ `INHERIT_PREV`（上一镜尾帧作本镜首帧参考）/
`BOOKEND`（首尾帧同时送模型）。设 `BOOKEND` 时可带 `model_code`，服务端校验该视频模型的
`config_json.support_last_frame`，不支持返回 `4006`。

响应：

```json
{ "result": { "shot_id": "s-40005", "frame_link_mode": "INHERIT_PREV",
    "link_effective_flag": 1, "downgrade_reason": null, "link_hint": "接镜 4 尾帧" } }
```

`downgrade_reason` 取值：`CROSS_SCENE`（跨场次，按设计不衔接）/ `PREV_NO_LAST_FRAME`
（上一镜尚无尾帧）/ `NO_LAST_FRAME`（BOOKEND 但本镜无尾帧）。降级不阻断生成。

### POST /bgssai/user/shots/frame-link-batch

```json
{ "episode_id": "e-20001", "frame_link_mode": "INHERIT_PREV" }
```

整集一次生效：同场次相邻镜头置为该模式，跨场处一律留 `NONE`。响应
`{ "result": { "linked_count": 36, "cross_scene_count": 6 } }`。

### POST /bgssai/user/shots/last-frame-generate

```json
{ "shot_id": "s-40005", "last_frame_prompt": "她转身推门，背影留在画面右侧" }
```

单独出一张尾帧图（`task_type=IMAGE`、`target_type=LAST_FRAME`，按 IMAGE 单价计费）。
已出视频的镜头默认由本机 ffmpeg 抽帧生成尾帧，不必走这个接口。

### GET /bgssai/user/shots/list?episode_id=

元素新增 `last_frame_asset_id`、`last_frame_url`、`last_frame_prompt`、`frame_link_mode`、
`link_effective_flag`、`downgrade_reason`、`link_hint`。

错误码：`50000001` 参数非法 / `4003` 越权 / `4006` 所选模型不支持首尾帧 /
`4007` 余额不足 / `429000` 限流。

## 原著导入与改编

需求见 [`../feature/source-import.md`](../feature/source-import.md)。全部需 `Jwttoken`，源归属校验到 `user_id`。

### POST /bgssai/user/sources/import

```json
{ "work_id": "w-10001", "source_type": "NOVEL", "source_title": "退婚三年后她成了首富",
  "source_content": "第一章 退婚
三年前那纸退婚书...", "rights_confirm_flag": 1 }
```

- `source_type`：`NOVEL`（小说全文）/ `SCRIPT`（成品剧本）/ `OUTLINE`（分集大纲）。
- `source_content`：纯文本，上限 100 万字符；前端读取 `.txt` / `.md` 后以文本提交，不走 multipart。
- `rights_confirm_flag`：必须为 1，否则拒绝导入（`50000001`）；确认时间落 `rights_confirm_time`。

返回 `{ "result": { "source_id": "src-70001", "chapter_count": 42, "char_count": 121840 } }`。

### GET /bgssai/user/sources/list?work_id=&page_num=&page_size=

分页返回 `source_id`、`source_type`、`source_title`、`char_count`、`chapter_count`、
`import_status`、`adapted_flag`、`created_time`。

### GET /bgssai/user/sources/detail?source_id=&page_num=&page_size=

分页返回章节：`chapter_id`、`chapter_no`、`chapter_title`、`char_count`、`chapter_text`。

### POST /bgssai/user/sources/chapters-save

```json
{ "source_id": "src-70001", "chapter_list": [
    { "chapter_no": 1, "chapter_title": "退婚", "chapter_text": "..." } ] }
```

整份替换该源的章节（前端提交调整后的完整分章结果），旧章节软删。

### POST /bgssai/user/sources/adapt-estimate

```json
{ "source_id": "src-70001", "chapter_no_from": 1, "chapter_no_to": 5, "target_episode_count": 6 }
```

返回 `{ "result": { "char_count": 18420, "estimated_cost_cent": 12, "model_code": "hy4" } }`。

### POST /bgssai/user/sources/adapt

参数同 `adapt-estimate`，另可带 `model_code`。返回 `{ "result": { "task_id": "t-..." } }`，
任务 `task_type=TEXT`、`target_type=ADAPT_OUTLINE`。完成后写入作品 `outline_json` 与各集
`source_ref_json`（`{"source_id","chapter_no_list"}`），已定稿集正文逐字保留。

### POST /bgssai/user/sources/delete

`{ "source_id": "src-70001" }`，软删。已改编出的作品与剧集不受影响，出处位置显示「原文已删除」。

错误码：`50000001` 请求参数非法（未勾选权利确认 / 超字数 / 章节范围非法）/ `4003` 越权 /
`4006` 源已删除或无可用章节 / `4007` 余额不足 / `429000` 限流。

## 剪辑工程导出

需求见 [`../feature/edit-project-export.md`](../feature/edit-project-export.md)。四个端点全部需 `Jwttoken`，
资产归属校验到 `user_id`。本组接口**不调模型、不写用量、不扣配额**，也不进 `short_ai_task`。

### GET /bgssai/user/edit-projects/precheck?episode_id=

```json
{ "result": { "episode_id": "e-20001", "exportable_flag": 1,
    "ready_shot_count": 18, "total_shot_count": 20,
    "missing_shot_list": [ { "shot_no": 7, "shot_desc": "女主推门" } ],
    "estimated_media_byte": 486539264, "media_limit_byte": 2147483648,
    "last_draft_dir": "C:\Users\demo\AppData\Local\JianyingPro\User Data\Projects\com.lveditor.draft" } }
```

`exportable_flag=0` 表示该集没有任何镜头具备可用视频，此时提交导出返回 `4006`。
`last_draft_dir` 取该用户最近一条导出记录的 `draft_dir`，无历史时为 `null`。

### POST /bgssai/user/edit-projects/export

```json
{ "episode_id": "e-20001", "project_format": "JIANYING", "jianying_version": "V6",
  "media_mode": "WITH_MEDIA", "draft_dir": "C:\Users\demo\...\com.lveditor.draft" }
```

- `project_format`：`JIANYING` / `FCPXML` / `EDL`，必填。
- `jianying_version`：`V6`（默认，主文件 `draft_info.json`）/ `V5`（主文件 `draft_content.json`），
  仅 `JIANYING` 有效。
- `media_mode`：`WITH_MEDIA`（默认，媒体一并打包）/ `LINK_ONLY`（仅工程文件加 `manifest.csv`）。
- `draft_dir`：`project_format=JIANYING` 时必填（剪映按绝对路径找素材），其余格式忽略。

返回 `{ "result": { "export_id": "x-60001", "export_status": "QUEUED" } }`，打包异步执行。

### GET /bgssai/user/edit-projects/detail?export_id=

```json
{ "result": { "export_id": "x-60001", "episode_id": "e-20001", "work_id": "w-10001",
    "project_format": "JIANYING", "jianying_version": "V6", "media_mode": "WITH_MEDIA",
    "export_status": "READY", "clip_count": 18,
    "missing_shot_list": [ { "shot_no": 7, "shot_desc": "女主推门" } ],
    "package_byte": 486539264, "asset_id": "a-54001",
    "package_name": "退婚三年后她成了首富_E14_JIANYING.zip",
    "download_url": "https://obs.example.com/...", "expire_at": "2026-09-07T09:30:00Z",
    "fail_reason": null, "created_time": "2026-09-07T09:00:00",
    "finished_time": "2026-09-07T09:00:21" } }
```

`export_status`：`QUEUED` / `RUNNING` / `READY` / `FAILED`。`download_url` 仅 `READY` 时非空，
30 分钟有效；过期后重新调用本接口取新链接，无需重新导出。

### GET /bgssai/user/edit-projects/list?episode_id=&page_num=&page_size=

分页返回该集导出记录（`created_time` 倒序），元素字段同 `detail` 但不含 `download_url`。
每集每格式仅保留最近 10 条 `READY` 记录，更旧的记录与其产物一并软删。

错误码：`50000001` 请求参数非法（剪映格式未填 `draft_dir` 等）/ `4003` 越权 /
`4006` 本集尚无已生成镜头视频 / `4017` 素材体积超限须改用 `LINK_ONLY` / `429000` 限流。

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
