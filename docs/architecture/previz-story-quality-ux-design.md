# 预演 / 剧情图谱 / 质量流水线 / 三问体验详设

对齐：`previz-text-storyboard.md`、`story-map.md`、`quality-pipeline.md`、`ux-three-questions.md`。
横切：`ai-provider-obs-design.md`。页面 API：`docs/api/page-api-mapping.md`。

## 1. 文字预演

表：`short_shot_previz`、`short_prompt_version`、`short_continuity_issue`；shot 列 `previz_level` / `stale_flag` / `prompt_version`；asset `draft_flag`。

层级：`NONE` → `TEXT` → `DRAFT_IMAGE` → 正式 IMAGE/VIDEO。跳级需 `skip_level_check_flag` 或显式确认。
镜头列表与预演列表回传 `look_plain`（观众会看到什么）和 `prompt_score`；创作台默认先出图确认构图，不自动接视频。
`previz_text` 的「在哪」先取镜头描述里的地点短语，没有时才用场景卡名；列表展示时只替换仍写着「未写清场景」的那一槽。
API：`/previz/**`、prompt check/diff；生成提交校验层级。

## 2. 剧情图谱

表：`short_story_map`、`short_character_relation`；shot `blocking_json` / `axis_flag`。
生成张力 / 关系 / 走位 JSON；越轴写入 `short_continuity_issue`（`EYELINE`）。
API：`/story-map/**`。

## 3. 质量流水线

表：`short_reference_template`、`short_quality_gate`、`short_quality_issue`、`short_quality_threshold_log`、`short_variant`；
work `template_id` / `template_override_json` / `overall_quality_score`。

四道门 `gate_code` 按需求；评审优先规则引擎 + TEXT 模型结构化 JSON。
管理端模板 CRUD；用户端 gate-run / force-pass / issue-handle / variants。

## 4. 三问体验

表：`short_foreshadow`、`short_episode_gap`。
`/preview/summary`、`/continuity/**` 聚合只读；`episodes/batch` 实现批量状态操作。

## 5. 成片加深

在 MVP `FilmService` 上：四轨时间轴、`DUBBING_MISALIGNED` 门禁、`rebake-subtitle` / `export` / `settings-save`；
`COMPOSE` 走 FFmpeg（缺运行时 → `COMPOSE_RUNTIME_MISSING`），产物进 OBS。
