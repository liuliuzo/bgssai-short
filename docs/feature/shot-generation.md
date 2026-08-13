# 画面生成与任务中心（shot-generation）

## 一、背景与目标

- 背景：一集 40 多个镜头意味着一次要发起几十个长耗时生成任务，创作者的真实处境是：
  提交完就失去掌控——不知道排到第几个、哪几个失败了、失败的要不要重试、重试花不花钱、
  合计花了多少钱。加上文生视频「抽卡」属性（一次不满意就重来），成本极易失控，
  很多人是做到一半发现余额没了才停手。这是 AI 短剧从「能做」到「敢做」之间的门槛。
- 目标：让批量生成变成可预估、可暂停、可续跑、可核账的工程动作。
  具体口径：提交前给出费用预估（误差不超过 10%）；任务失败自动重试且失败不计费；
  中断后可断点续跑，不重复消耗已成功镜头的费用。
- 关联角色：`USER`（发起与管理任务）、`PLATFORM_ADMIN`（配置能力与单价，见 `ai-capability-usage.md`）。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望勾选一批镜头一次性提交生成，并在提交前看到「预计花费 X 元」。
- 用户故事二：作为 `USER`，我希望有一个任务中心，能看到每个镜头排队 / 生成中 / 成功 / 失败，
  失败的能一键重试，全部失败的能一键重试全部。
- 用户故事三：作为 `USER`，我希望对不满意的镜头单独重抽，并保留历史候选，方便选回上一版。
- 用户故事四：作为 `USER`，我希望先出首帧图确认构图，再由首帧图生成视频，以免直接生视频反复烧钱。

- 业务规则：
  1. **两段式生成（默认，且是省钱主路径）**：镜头先生成首帧图（`IMAGE`，单价低）→ 用户确认构图 →
     再由首帧图生成视频（`VIDEO`，单价高，图生视频）。允许在作品设置里切换为「直接生成视频」，
     但界面须提示成本差异。
     本条与 `previz-text-storyboard.md` 的四级阶梯（`TEXT` → `DRAFT_IMAGE` → `IMAGE` → `VIDEO`）
     是同一条路径的两段表述：文字稿与草图是本节「首帧图」之前的两级更廉价预演；
     未出高清首帧的镜头提交 `VIDEO` 任务返回 `4016`（跳级约束，作品设置可关闭）。
  2. **费用预估与预算闸门**：提交前按「镜头数 × 对应能力单价 × 候选数」估算 `estimated_cost_cent`，
     与账户可用余额、作品预算 `cost_budget_cent` 比对：
     - 预估超过账户余额：拒绝提交，返回 `4007` 并引导充值；
     - 预估导致作品累计超预算：拦截并要求二次确认（`confirm_over_budget_flag=1`）后放行；
     - 生成过程中作品累计消耗触及预算 100%：自动暂停该作品的排队任务（`task_status=QUEUED` 转
       `PAUSED`），不静默继续烧钱；提示条直接提供「提高预算并恢复排队」按钮，恢复
       （`PAUSED` 转回 `QUEUED`）原位续跑，不重走提交流程、不重复计费。
  3. **计费口径**：`SUCCEEDED` 计费，`FAILED` / `CANCELED` 不计费；因平台侧原因（模型超时、供应商 5xx）
     的自动重试不计费，用户手工发起的重抽计费。每次计费写 `ai_usage_record`（见 `ai-capability-usage.md`）。
  4. **重试与续跑**：任务失败按指数退避自动重试至多 2 次；仍失败置 `FAILED` 并记录 `fail_reason`
     与分类 `fail_reason_code`（`MODEL_TIMEOUT` 模型超时 / `CONTENT_BLOCKED` 内容拦截 /
     `ASSET_MISSING` 素材缺失），任务中心按分类给差异化动作：超时给「换备用模型重试」
     （`retry` 带 `fallback_model_flag=1`）、内容拦截给「去改提示词」、素材缺失给「去补首帧」。
     批次支持「重试全部失败项」与**暂停 / 恢复**：暂停把批内 `QUEUED` 任务转 `PAUSED`
     （`RUNNING` 任务跑完当前一条、不强行中断），恢复原位转回 `QUEUED`，不重走提交流程；
     页面刷新 / 关闭浏览器不影响任务执行，重新进入按 `batch_id` 恢复视图。
  5. **候选与回滚**：每次生成产出的素材进 `drama_asset` 并挂到镜头的候选列表（`candidate_list`），
     用户可在候选间切换选定（`selected_flag`），切换不产生新费用；候选保留最近 5 个，超出按时间淘汰
     （已被选定的不淘汰）。
  6. **镜头定稿**：视频候选选定后可 `LOCKED`，锁定的镜头不参与批量重生成（除非显式解锁）。
  7. **并发与限流**：单用户在途任务（`QUEUED` + `RUNNING`）上限 20，超出的排队等待；
     提交接口按 `USER` 维度限流（`@RateLimit`），防止误点重复提交。
  8. 任务与镜头解耦：`drama_ai_task` 是通用任务表，`task_type` 覆盖 `TEXT` / `IMAGE` / `VIDEO` /
     `VOICE` / `COMPOSE`，剧本生成、定妆、配音、合成等全部走同一张任务表与同一套任务中心视图。
  9. **站内通知**：批次完成、批次出现失败任务时各发一条站内通知（`notify_type=BATCH_DONE` /
     `BATCH_FAILED`），通知中心入口常驻任务中心页头，用户不必守在页面等待。
  10. **自动接续**：提交批次可勾选 `auto_next_flag`（完成后自动做下一步）：首帧图批次全部成功后
      自动提交对应镜头的视频批次，视频批次完成后自动提交该集配音批次；自动提交同样先出费用预估
      并过预算闸门，触发拦截时转为站内通知等用户确认，不静默烧钱。
  11. 任务中心列表支持按剧集与批次筛选（list 接口的 `episode_id` / `batch_id` 参数）；
      类型下拉展示中文（剧本 / 图片 / 视频 / 配音 / 合成），枚举值收在选项 value 中。

## 三、验收标准

1. 勾选 42 个镜头点「批量生成首帧」，弹窗给出预估费用与预算占用条，确认后返回 `batch_id`，
   任务中心可见 42 条任务并实时推进状态。
2. 预估费用超过余额时提交被拒并返回 `4007`；超过作品预算但余额充足时，须勾选确认方可提交。
3. 人为制造 3 个任务失败，任务中心可筛出失败项，「重试全部失败项」只重跑这 3 个，
   已成功的 39 个不重复计费。
4. 关闭浏览器 5 分钟后重新进入任务中心，批次进度与状态延续，未丢失、未重复执行。
5. 单镜重抽 3 次后候选列表存在 4 个版本（含初版），切换选定不产生新的用量记录。
6. 作品累计消耗达到预算 100% 时，剩余排队任务自动转 `PAUSED`，页面顶部出现预算触顶提示并提供
   「提高预算并恢复排队」按钮；恢复后原位续跑，不重复计费。
7. 失败任务在 `ai_usage_record` 中无计费记录；平台侧自动重试成功的任务只计费一次。
8. 暂停批次后批内排队任务转 `PAUSED`、执行中任务不被强行中断；恢复后不产生新的提交记录。
9. 批次完成与出现失败任务时，通知中心各收到一条站内通知，点击直达对应批次视图。
10. 三类 `fail_reason_code` 在任务中心分别展示「换备用模型重试 / 去改提示词 / 去补首帧」动作。
11. 勾选 `auto_next_flag` 的首帧批次全部成功后，自动提交对应视频批次并先出费用预估过预算闸门。

## 四、接口契约（snake_case）

- `POST /bgssai/user/ai-tasks/estimate`
  ```json
  { "task_type": "IMAGE", "shot_id_list": ["s-40001", "s-40002"], "candidate_count": 1 }
  ```
  ```json
  { "result": { "shot_count": 2, "unit_price_cent": 80, "candidate_count": 1,
      "estimated_cost_cent": 160, "balance_cent": 250000,
      "work_budget_cent": 300000, "work_spent_cent": 128650,
      "over_budget_flag": 0, "model_code": "img-std-v2" } }
  ```
- `POST /bgssai/user/ai-tasks/submit`
  ```json
  { "task_type": "IMAGE", "episode_id": "e-20001",
    "shot_id_list": ["s-40001", "s-40002"], "candidate_count": 1,
    "confirm_over_budget_flag": 0, "auto_next_flag": 0 }
  ```
  返回 `{ "batch_id": "b-70001", "task_count": 2, "estimated_cost_cent": 160 }`
- `GET /bgssai/user/ai-tasks/list?page_num&page_size&task_type&task_status&batch_id&episode_id`
  ```json
  { "result": { "task_list": [
      { "task_id": "t-60001", "batch_id": "b-70001", "task_type": "VIDEO",
        "task_status": "RUNNING", "task_status_name": "执行中",
        "target_type": "SHOT", "target_id": "s-40001", "target_label": "第 1 集 · 镜 1",
        "model_code": "vid-fast-v3", "progress_percent": 62,
        "retry_count": 0, "fail_reason": null, "fail_reason_code": null,
        "cost_cent": 0, "queued_time": "2026-08-13 10:21:03",
        "started_time": "2026-08-13 10:21:20", "finished_time": null } ],
      "page_num": 1, "page_size": 20, "total_size": 42 } }
  ```
- `GET /bgssai/user/ai-tasks/batch-progress?batch_id=b-70001`
  （`total_count` / `succeeded_count` / `failed_count` / `running_count` / `queued_count` / `cost_spent_cent`）
- `POST /bgssai/user/ai-tasks/retry`（`task_id_list` 或 `batch_id` + `only_failed_flag`，
  可选 `fallback_model_flag`：超时类失败换备用模型重试）
- `POST /bgssai/user/ai-tasks/cancel`（`task_id_list` 或 `batch_id`）
- `POST /bgssai/user/ai-tasks/pause` · `POST /bgssai/user/ai-tasks/resume`（`batch_id`；
  暂停把批内 `QUEUED` 转 `PAUSED`，恢复转回 `QUEUED`，不重走提交流程）
- `GET /bgssai/user/notifications/list?page_num&page_size`（站内通知：`notify_id`、
  `notify_type`（`BATCH_DONE` / `BATCH_FAILED`）、`title`、`link_page`、`read_flag`、`created_time`）
- `POST /bgssai/user/notifications/read`（`notify_id_list`）
- `GET /bgssai/user/shots/candidates?shot_id=s-40001&asset_type=VIDEO`
- `POST /bgssai/user/shots/select-candidate`（`shot_id` + `asset_id`）
- `POST /bgssai/user/shots/lock` · `POST /bgssai/user/shots/unlock`
- 鉴权：全部需 `Jwttoken`；`submit` 与 `retry` 加 `@RateLimit`（USER 维度）。
- 错误码：`4003` 越权 / `4006` 镜头被合规拦截或引用未定妆卡 / `4007` 余额不足 /
  `4008` 超作品预算未确认 / `4011` 在途任务超上限 / `4016` 未出高清首帧即提交视频 / `429` 限流。

## 五、流程 / 状态机（须配图）

任务提交、执行、重试与计费链路见 [`flow-generation-task.html`](./flow-generation-task.html)；
镜头状态机见 [`state-shot-lifecycle.html`](./state-shot-lifecycle.html)。

`task_status` 流转：`QUEUED` → `RUNNING` → `SUCCEEDED` / `FAILED`（自动重试至多 2 次后仍失败）；
`QUEUED` 可转 `PAUSED`（手工暂停批次或预算触顶），`PAUSED` 恢复后转回 `QUEUED`；
`QUEUED` / `PAUSED` / `RUNNING` 可 `CANCELED`。镜头状态随任务推进：
`PENDING` → `IMAGE_GENERATING` → `IMAGE_READY` → `VIDEO_GENERATING` → `VIDEO_READY` → `LOCKED`；
任一阶段失败落 `FAILED`，重试从失败阶段续跑，不回退已完成阶段。

## 六、数据模型影响

- 新增表 `drama_ai_task`：`task_id`(PK)、`batch_id`、`user_id`、`work_id`、`episode_id`、`task_type`、
  `task_status`、`target_type`、`target_id`、`model_code`、`request_json`、`result_json`、
  `progress_percent`、`retry_count`、`fail_reason`、`fail_reason_code`、`auto_next_flag`、`cost_cent`、
  `queued_time`、`started_time`、`finished_time`、`created_time`、`updated_time`。
- 新增表 `drama_notification`：`notify_id`(PK)、`user_id`、`notify_type`、`title`、`content`、
  `link_page`、`read_flag`、`deleted_flag`、时间戳。
- 新增表 `drama_asset`：`asset_id`(PK)、`user_id`、`work_id`、`episode_id`、`shot_id`、`asset_type`
  （`IMAGE` / `VIDEO` / `AUDIO` / `SUBTITLE` / `FILM`）、`storage_key`、`file_url`、`file_size_byte`、
  `duration_sec`、`width`、`height`、`source_task_id`、`selected_flag`、`deleted_flag`、时间戳。
- 索引：`drama_ai_task(user_id, task_status)`、`drama_ai_task(batch_id)`、`drama_asset(shot_id, asset_type)`。
- 素材文件存对象存储，库内只存 `storage_key` 与访问用的签名 URL 生成参数（详设阶段定稿存储方案）。

## 七、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/task-center.html`](../demo-static/web/user/task-center.html)
  （批次进度 + 任务列表 + 失败筛选与重试 + 费用累计）；镜头候选切换在
  [`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html) 详情面板内。
- 跳转关系：`storyboard.html` ←→ `task-center.html`，登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`docs/demo-static/web/user/task-center.html` 及 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
