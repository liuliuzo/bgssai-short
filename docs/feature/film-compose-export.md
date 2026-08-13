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
     字幕样式（取自 `voice-subtitle.md` 的 `subtitle_style_json`）。
  4. **BGM 闪避**：有台词处 BGM 自动降低音量（`duck_flag=1`，默认开），避免压住人声。
  5. **导出规格**：分辨率固定 1080×1920（`VERTICAL_9_16`），帧率 30；导出版本可多选：
     `WITH_SUBTITLE`（烧录字幕）、`NO_SUBTITLE`（纯净版，便于二次发行）、`SRT_ONLY`（仅字幕文件）。
     整剧打包导出按集编号命名（`{work_title}_E{episode_no}_{version}.mp4`）。
  6. **合成也是任务**：`task_type=COMPOSE`，走统一任务体系（进度、失败重试、断点续跑、计费口径
     与 `shot-generation.md` 一致）；合成失败给出可读原因（缺素材 / 素材损坏 / 时长越界）。
  7. **重合成**：任何上游素材变更（镜头重抽、配音重配、字幕修改）会把该集 `compose_status`
     置回 `NONE` 并在页面标注「成片已过期」，避免用户下载到旧版本；已通过审核的成片被置过期时，
     审核状态同步回落 `NOT_SUBMITTED`（见 `content-compliance.md`）。
  8. **成片时长校验**：合成结果与 `episode_duration_sec` 偏差超过 ±20% 时给出警告（不阻塞下载）。
  9. 下载走临时签名 URL，有效期 30 分钟，不暴露对象存储永久地址。

## 三、验收标准

1. 前置条件任一不满足时，「合成本集」按钮禁用并逐条列出缺口（含直达修复页的链接）。
2. 时间轴预览四条轨道对齐显示，点击任一镜头可跳转到分镜工作台对应镜头。
3. 一键合成产生 `COMPOSE` 任务，进度可见；成功后页面出现成片播放器与导出面板。
4. 导出勾选 `WITH_SUBTITLE` + `NO_SUBTITLE` 两个版本，产出两个文件，命名符合规则。
5. 重抽任一镜头后，该集成片标注「已过期」，`compose_status=NONE`，且原审核结论回落 `NOT_SUBMITTED`。
6. 整剧打包导出仅包含已合成且未过期的剧集，过期集在打包前明确列出并要求确认。
7. 下载链接 30 分钟后失效，重新获取需再次调用接口。

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
- `POST /bgssai/user/films/compose`（`episode_id`；返回 `task_id`）
- `GET /bgssai/user/films/detail?episode_id=e-20001`
  （`film_asset_id`、`film_duration_sec`、`compose_status`、`expired_flag`、`composed_time`）
- `POST /bgssai/user/films/settings-save`（作品级合成设置，`work_id` + 上述设置字段）
- `POST /bgssai/user/films/export`（`episode_id_list` + `version_list`；返回 `task_id` 或直接给下载项）
- `GET /bgssai/user/films/download-url?asset_id=a-53001`（返回 30 分钟有效签名 URL）
- 鉴权：需 `Jwttoken`；下载 URL 生成前校验素材归属。
- 错误码：`4003` 越权 / `4006` 前置条件不满足即合成 / `4007` 余额不足 / `4012` 成片已过期不可下载 / `429` 限流。

## 五、流程 / 状态机（须配图）

合成管线与过期回落规则见 [`flow-post-production.html`](./flow-post-production.html)。

`compose_status`：`NONE` → `COMPOSING` → `READY` / `FAILED`；上游素材变更时由 `READY` 回落 `NONE`
并置 `expired_flag=1`（成片文件保留，仅标注过期，供对比参考）。

## 六、数据模型影响

- 新增表 `drama_film`：`film_id`(PK)、`episode_id`、`work_id`、`user_id`、`film_asset_id`、
  `film_duration_sec`、`compose_status`、`expired_flag`、`compose_task_id`、`composed_time`、
  `export_json`（各版本导出产物）、`deleted_flag`、时间戳。
- 作品级新增列：`drama_work.compose_setting_json`（转场 / BGM / 片头片尾 / 闪避开关）。
- 成片与导出文件复用 `drama_asset`（`asset_type=FILM`）。

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
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
