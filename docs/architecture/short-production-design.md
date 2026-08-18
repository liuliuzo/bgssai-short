# 短剧制作闭环详设（MVP 切片）

对齐需求：`docs/feature/short-work-management.md`、`script-studio.md`、`character-bible.md`、
`storyboard-workbench.md`、`shot-generation.md`、`film-compose-export.md`、`content-compliance.md`。

本切片把创作者从建剧走到成片提审跑通。AI 接入走 **MockAiProvider**（规则模板 + 进程内任务），
接口与表结构按需求字段落库，后续替换为真实模型时不改契约。

## 1. 范围

用户端：作品 / 剧集 / 大纲与剧本 / 角色与场景卡 / 拆镜 / 模拟生成 / 成片 / 提审 / 关键词预检。

管理端：审核队列、创作者列表与冻结、已有加量包与支付商户页。

本切片不实现：剧情图谱、连贯性中心、质量门与模板、配音字幕、用量账单、真实对象存储与视频编码。
对应接口在契约中标「本切片未实现」。

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
| `short_asset` | 占位图 / 成片元数据，`storage_key` 存 SVG 或时间轴 JSON |
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

## 4. 模拟任务时序

```
submit / outline-generate / scripts-generate / portrait-generate / films-compose
  -> 插入 short_ai_task (QUEUED)
  -> 返回 task_id / batch_id
  -> ShortAiTaskWorker 每秒拉取一条 QUEUED
  -> RUNNING，MockAiProvider 写结果
  -> 成功则 QuotaConsumeService.consume；失败写 fail_reason_code
  -> 前端轮询 list / batch-progress
```

扣费单价（分）：TEXT 15、IMAGE 50、VOICE 20、VIDEO 150、COMPOSE 80；定妆候选每张按 IMAGE。
余额不足 `4007`，月度触顶 `4015`。提交前若无订阅则按 FREE 惰性建档（与套餐 `me` 一致）。

素材不进对象存储：`GET /bgssai/user/assets/{asset_id}` 输出 `image/svg+xml`（公开，id 不可猜测）。

## 5. 所有权与错误码

用户端一律按 JWT `user_id` 过滤。越权 `4003`，不泄漏资源是否存在。
业务拒绝 `4006`，版本冲突 `4009`，重复提审 `4013`。

关键词预检：命中 `BLOCK` 词拦截定稿 / 提交生成 / 提审；`WARN` 可继续。

## 6. 管理端

审核只读创作内容。通过 / 驳回写入 `short_review_record`。驳回必填 `reject_item_list`。
创作者列表读 `sys_user`（`org_code=USER`），冻结写 `status=2`。
