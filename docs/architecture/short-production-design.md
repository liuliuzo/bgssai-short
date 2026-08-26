# 短剧制作闭环详设

对齐需求：`docs/feature/short-work-management.md`、`script-studio.md`、`character-bible.md`、
`storyboard-workbench.md`、`shot-generation.md`、`film-compose-export.md`、`content-compliance.md`
及后续扩展功能（配音、预演、剧情图谱、质量门、三问体验）。

创作者从建剧走到成片提审。AI 与素材存储横切见 **`ai-provider-obs-design.md`**
（`AiProvider` SPI + 华为云 OBS；`provider_code=MOCK` 仅作无密钥回退）。
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
| `short_character` / `short_scene_card` | 作品级资产卡 |
| `short_shot` | 镜头；`image_prompt` 为派生字段 |
| `short_ai_task` | 统一异步任务 |
| `short_asset` | 素材元数据；`storage_key` 为 OBS object key（本地回退可为文件路径） |
| `short_film` | 成片记录 |
| `short_review_record` | 审核留痕 |
| `short_compliance_hit` | 预检命中 |

状态枚举以 `short-work-management.md` 第八节为单一登记处。

## 3. 状态机

作品 `work_status` 由剧集阶段自动取「进度最靠前」一档；`ARCHIVED` 仅手工。

剧本：`NONE` → `GENERATING` → `DRAFT` → `READY`。定稿后再改（非错别字级）回落 `DRAFT`，
并令该集 `storyboard_status=DRAFT`。

分镜：`NONE` → `SPLITTING` → `DRAFT` → 全部镜头 `LOCKED` 则 `READY`。

镜头：`PENDING` → `IMAGE_GENERATING` → `IMAGE_READY` → `VIDEO_GENERATING` → `VIDEO_READY` → `LOCKED`；失败 `FAILED`。

任务：`QUEUED` → `RUNNING` → `SUCCEEDED` / `FAILED`；可 `CANCELED`。`PAUSED` 本切片不实现暂停入口。

成片：`NONE` → `COMPOSING` → `READY` / `FAILED`。

审核：`NOT_SUBMITTED` → `PENDING` → `APPROVED` / `REJECTED`。全部剧集 `APPROVED` 则作品 `COMPLETED`。

## 4. 任务时序

```
submit / outline-generate / scripts-generate / portrait-generate / dubbing / films-compose
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
