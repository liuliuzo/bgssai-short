# 成片合成与导出（film-compose-export）

## 一、背景与目标

- 背景：镜头、配音、字幕都齐了，最后一步却要创作者装一套剪辑软件，把几十段素材拖进时间轴、
  对齐音画、加转场、烧字幕、配 BGM、导出竖屏规格。这一步既是学习门槛（剪辑软件），
  也是体力活（每集重复一遍），还是出错点（漏字幕、比例错、时长超）。做到这里放弃的人不在少数。
- 目标：合成不需要离开平台、不需要剪辑技能——素材齐备即可一键出片，
  单集从「素材齐」到「拿到成片」不超过 1 次点击加一次等待；导出规格符合主流竖屏短剧平台要求。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望素材齐了点一下「合成本集」就能拿到带配音带字幕的成片。
- 用户故事二：作为 `USER`，我希望在合成前用一条时间轴看清每个镜头的画面、配音、字幕是否对得上。
- 用户故事三：作为 `USER`，我希望能给全剧统一加片头片尾、BGM 和转场，不必逐集设置。
- 用户故事四：作为 `USER`，我希望导出时能选「带字幕 / 不带字幕」等版本，并支持整剧打包下载。

- 业务规则：
  1. **合成前置条件**（不满足则合成入口禁用并列出缺口）：该集全部镜头 `shot_status=LOCKED`；
     `dubbing_status=READY`（无 `MISALIGNED` 镜头）；字幕已生成；合规预检无 `BLOCK` 项。
  2. **时间轴**：按 `shot_no` 顺序排布，轨道为「视频轨 + 配音轨 + 字幕轨 + BGM 轨」；
     时间轴为**只读预览**，镜头时长与顺序的修改仍回分镜工作台进行——避免同一事实两处可改而漂移。
  3. **作品级合成设置**（全剧沿用，可逐集覆盖）：`transition_type`（`NONE` / `FADE` / `DISSOLVE`）、
     `bgm_asset_id` 与 `bgm_volume`、`opening_asset_id`（片头）、`ending_asset_id`（片尾）、
     字幕样式（与 `voice-subtitle.md` 共用同一份 `subtitle_style_json`，合成设置卡内可直接修改，
     两处编辑的是同一事实，不构成漂移）。
  4. **BGM 闪避**：有台词处 BGM 自动降低音量（`duck_flag=1`，默认开），避免压住人声。
     BGM 可上传已有素材，也可输入音乐描述选择 `MUSIC` 模型生成；首批默认
     `minimax-music-v2.6` 且默认 `is_instrumental=1`。生成成功后只更新候选素材，用户选用后才写入
     `compose_setting_json.bgm_asset_id` 并参与合成。
  5. **导出规格**：分辨率固定 1080×1920（`VERTICAL_9_16`），帧率 30；导出版本可多选：
     `WITH_SUBTITLE`（烧录字幕）、`NO_SUBTITLE`（纯净版，便于二次发行）、`SRT_ONLY`（仅字幕文件）。
     整剧打包导出按集编号命名（`{work_title}_E{episode_no}_{version}.mp4`）。
  6. **合成也是任务**：`task_type=COMPOSE`，走统一任务体系（进度、失败重试、断点续跑、计费口径
     与 `shot-generation.md` 一致）；合成失败给出可读原因（缺素材 / 素材损坏 / 时长越界）。
  7. **重合成与失效分级**：上游变更按影响面分两级处理，避免「改一个错别字也要整片重编码重审核」：
     - **轻量级（仅字幕变更）**：字幕文本修改、字幕样式调整只影响烧录层，走「重烧字幕」
       （`compose_mode=SUBTITLE_ONLY`，低价如 0.20 元，不重编码画面与音频）；成片不置过期，
       页面置 `subtitle_stale_flag=1` 并提示「字幕已变更，可仅重烧字幕」；已通过的审核结论
       保留并标注「仅字幕变更」，不回落。
     - **全量级（画面 / 声音变更）**：镜头重抽、配音重配把该集 `compose_status` 置回 `NONE`、
       `expired_flag=1`，页面标注「成片已过期」，过期成片禁止下载（`4012`）；已通过审核的成片
       被置过期时，审核状态同步回落 `NOT_SUBMITTED`（见 `content-compliance.md`）。
  8. **成片时长校验**：合成结果与 `episode_duration_sec` 偏差超过 ±20% 时给出警告（不阻塞下载）。
  9. **合成在本机完成，不调外部模型**：`provider_code=FFMPEG`，把各镜已生成的视频按镜号拼成
     9:16 成片。因此**运行 / 部署机必须预装 ffmpeg 并在 PATH 中**，缺失时合成任务明确失败
     并提示「本机未安装 ffmpeg」；任一镜头缺可用视频时失败信息点名缺哪几镜。不提供
     「合不成就出一张占位图」的兜底——假成片会让创作者以为已经出片。
  10. **MVP 字幕 + 配音混流**：合成默认烧录字幕（`compose_setting_json.burn_subtitle_flag` 缺省为开）。
     优先使用 `short_subtitle` 行；若尚无字幕表数据，则按镜号用 `dialogue_text` 生成 SRT。
     若镜头已有 `short_dubbing.audio_asset_id`，FFmpeg 按镜序混入配音轨（缺配音镜用静音占位，
     `export_json.dubbing_missing_shot_nos` / `dubbing_warn` 记录缺口）；无任何配音时仍走画面-only 路径。
     有配音后再选 BGM 时对 BGM 降音量 `amix`，不再整轨替换人声。样式化字幕工作台仍按本节其余规则增量。
  11. 下载走临时签名 URL，有效期 30 分钟，不暴露对象存储永久地址；`films/detail` 的
     `download_url` 直接返回可播放地址（不是 download-url JSON 接口路径）。下载区提供
     「重新获取链接」按钮，链接过期后重取新签名 URL 即可，无需重新导出或重走合成流程。
  12. **合成预检**：全部镜头须 `VIDEO_READY` / `LOCKED` 且 `video_asset_id` 非空；
     仅有草图（`IMAGE_READY`）不得显示「可合成」。

## 三、验收标准

1. 前置条件任一不满足时，「合成本集」按钮禁用并逐条列出缺口（含直达修复页的链接）。
2. 时间轴预览四条轨道对齐显示，点击任一镜头可跳转到分镜工作台对应镜头。
3. 一键合成产生 `COMPOSE` 任务，进度可见；成功后页面出现成片播放器与导出面板。
4. 导出勾选 `WITH_SUBTITLE` + `NO_SUBTITLE` 两个版本，产出两个文件，命名符合规则。
5. 重抽任一镜头或重配任一镜头后，该集成片标注「已过期」，`compose_status=NONE`、`expired_flag=1`，
   且原审核结论回落 `NOT_SUBMITTED`；过期成片下载返回 `4012`。
6. 仅修改字幕文本或字幕样式后，成片不置过期，页面提示「字幕已变更」并提供「仅重烧字幕」入口
   （低价、不重编码画面与音频）；重烧完成后已通过的审核结论保留并标注「仅字幕变更」。
7. 整剧打包导出仅包含已合成且未过期的剧集，过期集在打包前明确列出并要求确认。
8. 下载链接 30 分钟后失效，点「重新获取链接」重取新签名 URL 即可下载，无需重新导出。
9. 输入 BGM 描述生成音乐后出现可试听候选；选用候选后时间轴 BGM 轨展示对应素材，重新生成不删除旧候选。

## 四、接口契约（snake_case）

- `GET /bgssai/user/films/precheck?episode_id=e-20001`
  ```json
  { "result": { "ready_flag": 0,
      "blocker_list": [
        { "blocker_code": "SHOT_NOT_LOCKED", "blocker_desc": "3 个镜头未定稿", "target_count": 3,
          "fix_page": "storyboard" },
        { "blocker_code": "DUBBING_MISALIGNED", "blocker_desc": "2 个镜头音画未对齐", "target_count": 2,
          "fix_page": "voice-studio" } ] } }
  ```
- `GET /bgssai/user/films/timeline?episode_id=e-20001`
  ```json
  { "result": { "episode_id": "e-20001", "total_duration_sec": 88,
      "track_list": [
        { "track_type": "VIDEO", "clip_list": [
            { "shot_id": "s-40001", "shot_no": 1, "start_ms": 0, "end_ms": 4000,
              "asset_id": "a-50102", "thumb_url": "/mock/shot-1.png" } ] },
        { "track_type": "AUDIO", "clip_list": [
            { "shot_id": "s-40001", "start_ms": 300, "end_ms": 3500, "asset_id": "a-51001" } ] },
        { "track_type": "SUBTITLE", "clip_list": [
            { "subtitle_no": 1, "start_ms": 300, "end_ms": 3500, "subtitle_text": "退婚书我签，但有个条件。" } ] },
        { "track_type": "BGM", "clip_list": [
            { "asset_id": "a-52001", "start_ms": 0, "end_ms": 88000, "duck_flag": 1 } ] } ] } }
  ```
- `POST /bgssai/user/films/compose-estimate`（`episode_id`；返回与 `/ai-tasks/estimate` 同构的费用字段，
  `task_type=COMPOSE`；余额不足 `4007` / 月额度 `4015`）
- `POST /bgssai/user/films/compose`（`episode_id`；先过预检与费用闸门；返回 `task_id` + `estimated_cost_cent`）
- `GET /bgssai/user/films/detail?episode_id=e-20001`
  （`film_asset_id`、`film_duration_sec`、`compose_status`、`expired_flag`、`subtitle_stale_flag`、
  `composed_time`）
- `POST /bgssai/user/films/rebake-subtitle`（`episode_id`；返回 `task_id`，`compose_mode=SUBTITLE_ONLY`，
  仅重烧字幕、不重编码画面与音频，审核结论保留并标注「仅字幕变更」）
- `POST /bgssai/user/films/settings-save`（作品级合成设置，`work_id` + 上述设置字段）
- `POST /bgssai/user/films/bgm-generate`（`work_id` + `episode_id` + `prompt` + 可选 `model_code` /
  `is_instrumental`；返回 `task_id`，成功后 `result_json` 含 `bgm_asset_id`）
- `POST /bgssai/user/films/export`（`episode_id_list` + `version_list`；返回 `task_id` 或直接给下载项）
- `GET /bgssai/user/films/download-url?asset_id=a-53001`（返回 30 分钟有效签名 URL）
- 鉴权：需 `Jwttoken`；下载 URL 生成前校验素材归属。
- 错误码：`4003` 越权 / `4006` 前置条件不满足即合成 / `4007` 余额不足 / `4012` 成片已过期不可下载 / `429` 限流。

## 五、流程 / 状态机（须配图）

合成管线与过期回落规则见 [`flow-post-production.html`](./flow-post-production.html)。

`compose_status`：`NONE` → `COMPOSING` → `READY` / `FAILED`；镜头重抽、配音重配等画面 / 声音级
变更由 `READY` 回落 `NONE` 并置 `expired_flag=1`（成片文件保留，仅标注过期，供对比参考）。
仅字幕文本 / 样式变更不回落，置 `subtitle_stale_flag=1`，「重烧字幕」（`compose_mode=SUBTITLE_ONLY`）
完成后清零。

## 六、数据模型影响

- 新增表 `short_film`：`film_id`(PK)、`episode_id`、`work_id`、`user_id`、`film_asset_id`、
  `film_duration_sec`、`compose_status`、`expired_flag`、`subtitle_stale_flag`、`compose_task_id`、
  `composed_time`、
  `export_json`（各版本导出产物）、`deleted_flag`、时间戳。
- 作品级新增列：`short_work.compose_setting_json`（转场 / BGM / 片头片尾 / 闪避开关）。
- 成片与导出文件复用 `short_asset`（`asset_type=FILM`）；AI BGM 复用 `short_asset(asset_type=AUDIO)`。

## 七、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/compose-export.html`](../demo-static/web/user/compose-export.html)
  （合成前置检查 + 四轨时间轴 + 合成设置 + 成片与导出面板）。
- 跳转关系：`voice-studio.html` → `compose-export.html` → 提交审核（`content-compliance.md`），
  登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`docs/demo-static/web/user/compose-export.html` 及 `PAGE-FLOW.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`；AI 为 MockAiProvider）
- [x] 代码与数据库
