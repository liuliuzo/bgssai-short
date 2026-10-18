# 短剧制作闭环详设

2026-09-11 字幕烧录修复：FFmpeg 子进程的工作目录已经固定为本次任务临时目录，字幕文件固定为该目录的 `subs.srt`。subtitles filter 使用该相对文件名，避免 Windows 盘符冒号被滤镜语法误解成下一选项。流程为“写入任务目录 → 以该目录启动 FFmpeg → 烧录字幕 → 检查进程结果 → 返回 MP4”；失败维持现有错误语义。未修改 API、任务状态、DDL/DML 或持久层三件套。现有真实视频合成测试验证该路径。

对齐需求：`docs/feature/short-work-management.md`、`script-studio.md`、`character-bible.md`、
`storyboard-workbench.md`、`shot-generation.md`、`film-compose-export.md`、`content-compliance.md`
及后续扩展功能（配音、预演、剧情图谱、质量门、三问体验）。

创作者从建剧走到成片提审。AI 与素材存储横切见 **`ai-provider-obs-design.md`**
（`AiProvider` SPI + 华为云 OBS；无 MOCK 兜底，能力未接通即明确报错）。
首批 TEXT / VIDEO / VOICE / ASR / MUSIC 模型映射见 **`tokenhub-model-integration-design.md`**。
用量与模型目录见 **`ai-capability-usage-design.md`**。

## 1. 范围

用户端：作品 / 剧集 / 大纲与剧本 / 角色与场景卡 / 拆镜 / 生成 / 配音字幕 / 成片 / 提审 / 预检 /
预演 / 剧情图谱 / 质量报告 / 样片与连贯性。

管理端：审核队列、创作者、加量包与支付商户、AI 模型、用量、质量模板、充值申请。

## 2. 数据模型

共享库 `bgssai_short`。新表审计列：`deleted_flag` / `created_time` / `updated_time`（与需求一致）。
业务主键为字符串 id（`work_id` 等），另有自增 `id`。

| 表 | 作用 |
| --- | --- |
| `short_work` | 作品容器，含 `outline_json` |
| `short_episode` | 剧集占位与六阶段状态 |
| `short_script` / `short_script_version` | 单集剧本与最近 20 版 |
| `short_character` / `short_scene_card` | 作品级资产卡；`card_type` 取 `SCENE` / `PROP` |
| `short_shot` | 镜头；`image_prompt` 为派生字段；`prop_ref_json` 引用道具卡 |
| `short_ai_task` | 统一异步任务：TEXT / IMAGE / VIDEO / VOICE / COMPOSE，状态 QUEUED → RUNNING → SUCCEEDED / FAILED / CANCELED |
| `short_asset` | 素材元数据；`storage_key` 为 OBS object key（本地回退可为文件路径） |
| `short_film` | 成片记录 |
| `short_review_record` | 审核留痕 |
| `short_compliance_hit` | 预检命中 |
| `short_edit_export` | 剪辑工程导出记录（剪映草稿 / FCPXML / EDL），产物落 `short_asset`（`asset_type=EDIT_PROJECT`） |
| `short_source_doc` / `short_source_chapter` | 原著源文档与章节；一份源可被多部作品分别改编，软删源不级联毁作品 |

`short_episode` 增列 `source_ref_json`（该集取自的源与章号）；`short_shot` 增列 `last_frame_asset_id` / `frame_link_mode` / `last_frame_prompt`（首尾帧衔接）。
分镜画布**不新增任何表与列**：节点位置由 `scene_no` 与 `shot_no` 推导，存坐标会造出与镜号并行的第二份顺序事实。

状态枚举以 `short-work-management.md` 第八节为单一登记处。

## 3. 状态机

作品 `work_status` 由剧集阶段自动取「进度最靠前」一档；`ARCHIVED` 仅手工。

剧本：`NONE` → `GENERATING` → `DRAFT` → `READY`。定稿后再改（非错别字级）回落 `DRAFT`，
并令该集 `storyboard_status=DRAFT`。

分镜：`NONE` → `SPLITTING` → `DRAFT` → 全部镜头 `LOCKED` 则 `READY`。

镜头：`PENDING` → `IMAGE_GENERATING` → `IMAGE_READY` → `VIDEO_GENERATING` → `VIDEO_READY` → `LOCKED`；失败 `FAILED`。

任务：`QUEUED` → `RUNNING` → `SUCCEEDED` / `FAILED`；`QUEUED` / `RUNNING` 可 `CANCELED`；
`FAILED` / `CANCELED` 经 `retry` / `recover` 回 `QUEUED`。`PAUSED` 本切片不实现暂停入口。
资产一致性：创作台面板绑定角色 / 场景 / 道具后，生成闸门校验卡已 `LOCKED`；见 `asset-consistency-panel.md`。

成片：`NONE` → `COMPOSING` → `READY` / `FAILED`。

审核：`NOT_SUBMITTED` → `PENDING` → `APPROVED` / `REJECTED`。全部剧集 `APPROVED` 则作品 `COMPLETED`。

剪辑工程导出：`QUEUED` → `RUNNING` → `READY` / `FAILED`。**与合成、审核完全解耦**——导出不置成片过期、不回落审核结论、不改镜头状态；被保留上限（每集每格式 10 条）挤出的记录置 `deleted_flag=1` 并软删其产物。

原著源：`IMPORTED` → `ADAPTED`。软删源不影响已改编出的作品与剧集。

首尾帧衔接：`frame_link_mode` 取 `NONE` / `INHERIT_PREV` / `BOOKEND`，镜头级、可随时改且改动不触发重抽；仅同 `scene_no` 的相邻镜头之间生效，跨场自动降级为不衔接。上游尾帧变化（重抽视频、重出尾帧、画布重排改变前驱）把下一镜置 `stale_flag=1`，只标记、不自动重抽。

## 4. 任务时序

```
submit / outline-generate / scripts-generate / portrait-generate / dubbing / subtitles-transcribe /
films-bgm-generate / films-compose
  -> AiModelResolveService 解析 model_code 与单价（无启用模型 → 4014）
  -> 插入 short_ai_task (QUEUED)，快照 model_code / cost_cent
  -> 返回 task_id / batch_id
  -> ShortAiTaskWorker 拉取 QUEUED
  -> RUNNING，AiProviderRegistry 执行，产物写入 OBS
  -> 成功：ai_usage_record + QuotaConsumeService.consume；失败写 fail_reason_code
  -> 前端轮询 list / batch-progress
```

单价来自 `ai_model_config.unit_price_cent`（按次 `PER_CALL` 时 `usage_amount=1`）。
余额不足 `4007`，月度触顶 `4015`。提交前若无订阅则按 FREE 惰性建档（与套餐 `me` 一致）。

`GET /bgssai/user/assets/{asset_id}`：需登录且校验归属，返回 30 分钟预签名 `download_url`（见 OBS 详设）。

## 5. 所有权与错误码

用户端一律按 JWT `user_id` 过滤。越权 `4003`，不泄漏资源是否存在。
业务拒绝 `4006`，版本冲突 `4009`，重复提审 `4013`。

关键词预检：命中 `BLOCK` 词拦截定稿 / 提交生成 / 提审；`WARN` 可继续。

## 6. 管理端

审核只读创作内容。通过 / 驳回写入 `short_review_record`。驳回必填 `reject_item_list`。
创作者列表读 `sys_user`（`org_code=USER`），冻结写 `status=2`。

## 7. Short → Tube 幂等交接

成片 `compose_status=READY` 且本集 `APPROVED` 后，用户端
`POST /bgssai/user/films/publish-to-media` 调 Tube
`POST /bgssai/user/tube/ingest/short`。剪辑工程导出不是闸门。

幂等键 `short:{work_id}:{episode_id}:{film_id}` 写入 Body 与 `Idempotency-Key` 头。
已 READY 且已有 `media_id` / `play_url` 时 Short 回放，不再 ingest。
重新合成后取消这次回放，下次用同一键再 POST。
Tube 对端按该键 UNIQUE upsert，禁止双目录。Body 带 `status=READY`、`approved=true`。
PENDING 可以没有 `play_url`，不得记成失败或成功。契约见
[`../api/short-to-media-publish.md`](../api/short-to-media-publish.md)，
配图见 [`../feature/flow-film-publish-media.html`](../feature/flow-film-publish-media.html)。

`short_film` 记录 `media_id` / `media_play_url` / `media_publish_status` /
`media_published_at` / `media_last_error` / `media_publish_log`。
PENDING 不是商业成功；stub / 未配置不得写成 READY。
