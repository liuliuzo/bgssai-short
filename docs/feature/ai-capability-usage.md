# AI 能力配置与用量成本（ai-capability-usage）

## 一、背景与目标

- 背景：AI 短剧的成本结构和传统 SaaS 完全不同——每一次点击都在真实烧钱，且单价随模型、
  分辨率、时长浮动。创作者侧的痛点是「不知道钱花在哪、也不知道还能做几集」；
  平台侧的痛点是「模型换一家就要改代码、单价调整无处统一维护、用量与成本无法核账」。
- 目标：AI 能力（供应商 / 模型 / 单价 / 开关）在管理端集中配置，业务代码只认 `ability_type`
  不认具体供应商；创作者侧每一分钱都能追溯到具体镜头与任务。
- 关联角色：`PLATFORM_ADMIN`（配置与运营）、`USER`（查看自己的用量与账单）。

## 二、需求澄清（WHAT）

### 管理端（`PLATFORM_ADMIN`）

- 用户故事一：作为 `PLATFORM_ADMIN`，我希望按能力类型配置可用模型与单价、设默认模型、临时停用某模型，
  而不需要改代码发版。
- 用户故事二：作为 `PLATFORM_ADMIN`，我希望看到平台整体与分创作者的用量、成本、成功率、
  平均耗时，以便定价与容量规划。
- 用户故事三：作为 `PLATFORM_ADMIN`，我希望给创作者账号设置余额与月度用量上限。

- 业务规则：
  1. `ai_model_config` 按 `ability_type`（`TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `COMPOSE`）分组，
     每组可配多个模型，其一为 `default_flag=1`；业务侧提交任务时只传 `task_type`，
     由平台解析为当前默认模型（用户可在允许范围内改选，见规则 4）。
  2. 单价按计费单位配置：`price_unit` 取 `PER_CALL`（次）/ `PER_SECOND`（视频秒）/
     `PER_KILO_TOKEN`（千 token）/ `PER_MINUTE`（音频分钟）；`unit_price_cent` 为分。
     单价变更只对**变更后**创建的任务生效，历史任务按 `ai_usage_record` 快照的单价核算，不追溯。
  3. 停用模型（`enabled_flag=0`）后：在途任务照常跑完；新任务不再落到该模型；
     若被停用的是默认模型且组内无其它可用模型，则该能力对用户端整体不可用，
     提交时返回 `4014` 并给出可读提示。
  4. 模型可标注 `quality_level`（`FAST` 快速 / `STANDARD` 标准 / `HIGH` 高质），
     用户端在提交生成时可在启用的模型中选择档位，价格随之变化（预估接口返回对应单价）。
  5. 创作者配额：`user_quota` 维护 `balance_cent`（余额）与 `monthly_limit_cent`（月度上限，0 为不限）；
     余额不足拒绝新任务（`4007`），月度上限触顶同样拒绝并提示。
  6. 平台侧不做支付：充值由运营在管理端手工调整余额并留痕（`quota_change_log`），
     线上支付渠道不在本轮范围（见 `PRODUCT_VISION.md` 范围边界）。

### 用户端（`USER`）

- 用户故事四：作为 `USER`，我希望看到本月花了多少、花在哪部剧哪一集、还剩多少余额。

- 业务规则：
  7. 用量流水按任务粒度记录，可下钻到 `work_id` / `episode_id` / `shot_id`；
     支持按作品、按能力类型、按时间区间聚合。
  8. 每条流水固定字段：能力类型、模型、计费单位、用量、单价快照、金额、关联对象、时间。
  9. 只读：创作者不能修改流水，只能查看与导出。

## 三、验收标准

1. 管理端新增一个 `VIDEO` 模型并设为默认后，用户端新提交的视频任务落到该模型，无需发版。
2. 调整单价后，历史任务的账单金额不变，新任务按新价计费。
3. 停用当前默认模型且组内无其它启用模型时，用户端提交该类任务返回 `4014` 且提示可读原因。
4. 用户端账单页金额合计与管理端该用户用量合计一致（同一时间区间口径下逐分相等）。
5. 余额不足时新任务被拒（`4007`），在途任务不受影响。
6. 每条用量流水可点击下钻到对应作品 / 剧集 / 镜头，`FAILED` 任务不产生流水。
7. 管理端调整创作者余额时必须填写备注，调整记录可查。

## 四、接口契约（snake_case）

### 管理端

- `GET /bgssai/admin/ai-models/list?ability_type=VIDEO`
  ```json
  { "result": { "model_list": [
      { "model_id": "m-90003", "ability_type": "VIDEO", "ability_type_name": "视频",
        "provider_code": "PROVIDER_A", "model_code": "vid-fast-v3", "model_name": "视频快速版 v3",
        "quality_level": "FAST", "price_unit": "PER_SECOND", "unit_price_cent": 40,
        "default_flag": 1, "enabled_flag": 1, "timeout_sec": 600, "max_retry": 2,
        "updated_time": "2026-08-10 15:02:31" } ] } }
  ```
- `POST /bgssai/admin/ai-models/create` · `/update` · `/set-default` · `/toggle-enabled`
- `GET /bgssai/admin/usage/summary?start_date&end_date&ability_type`
  （`total_cost_cent`、`task_count`、`success_rate_percent`、`avg_duration_sec`、按能力与按日分组）
- `GET /bgssai/admin/usage/by-creator?page_num&page_size&start_date&end_date`
- `GET /bgssai/admin/creators/list?page_num&page_size&keyword&enabled_flag`
  （创作者账号、作品数、本月消耗、余额、月度上限、状态）
- `POST /bgssai/admin/creators/adjust-quota`（`user_id` + `balance_delta_cent` + `monthly_limit_cent` + `remark`，`remark` 必填）
- `POST /bgssai/admin/creators/toggle-enabled`（停用账号）
- 鉴权：需 `Jwttoken` + `PLATFORM_ADMIN`（角色取自 JWT）。

### 用户端

- `GET /bgssai/user/usage/summary?start_date&end_date`
  （`balance_cent`、`monthly_limit_cent`、`month_spent_cent`、按作品与按能力聚合）
- `GET /bgssai/user/usage/records?page_num&page_size&work_id&ability_type&start_date&end_date`
  ```json
  { "result": { "record_list": [
      { "record_id": "ur-a1001", "ability_type": "VIDEO", "model_code": "vid-fast-v3",
        "price_unit": "PER_SECOND", "usage_amount": 4, "unit_price_cent": 40, "cost_cent": 160,
        "work_id": "w-10001", "work_title": "退婚三年后她成了首富",
        "episode_no": 1, "shot_no": 1, "task_id": "t-60001",
        "created_time": "2026-08-13 10:22:48" } ],
      "page_num": 1, "page_size": 20, "total_size": 863 } }
  ```
- 鉴权：需 `Jwttoken`；仅本人流水（`user_id` 取自 JWT）。
- 错误码：`4003` 无权限 / `4007` 余额不足 / `4014` 该能力当前不可用 / `4015` 月度用量触顶。

## 五、流程 / 状态机（须配图）

能力解析、计费与账单链路见 [`flow-usage-billing.html`](./flow-usage-billing.html)。

计费时机：任务 `SUCCEEDED` 时按快照单价写一条 `ai_usage_record`，同时扣减 `user_quota.balance_cent`
并累加 `drama_work.cost_spent_cent`；`FAILED` / `CANCELED` 与平台侧自动重试不计费。

## 六、数据模型影响

- 新增表 `ai_model_config`：`model_id`(PK)、`ability_type`、`provider_code`、`model_code`、`model_name`、
  `quality_level`、`price_unit`、`unit_price_cent`、`default_flag`、`enabled_flag`、`timeout_sec`、
  `max_retry`、`config_json`（端点与参数，密钥不入库明文，见 `docs/security`）、`deleted_flag`、时间戳。
- 新增表 `ai_usage_record`：`record_id`(PK)、`user_id`、`work_id`、`episode_id`、`shot_id`、`task_id`、
  `ability_type`、`model_code`、`price_unit`、`usage_amount`、`unit_price_cent`、`cost_cent`、`created_time`。
- 新增表 `user_quota`：`user_id`(PK)、`balance_cent`、`monthly_limit_cent`、`month_spent_cent`、
  `month_reset_date`、`updated_time`。
- 新增表 `quota_change_log`：`log_id`(PK)、`user_id`、`operator_user_id`、`balance_delta_cent`、
  `balance_after_cent`、`monthly_limit_cent`、`remark`、`created_time`。
- 索引：`ai_usage_record(user_id, created_time)`、`ai_usage_record(work_id)`、
  `ai_model_config(ability_type, enabled_flag, default_flag)`。

## 七、原型

- 涉及端：web 管理端 + web 用户端。
- 页面：[`../demo-static/web/admin/ai-models.html`](../demo-static/web/admin/ai-models.html)（能力与模型配置）、
  [`../demo-static/web/admin/usage-cost.html`](../demo-static/web/admin/usage-cost.html)（平台用量与成本）、
  [`../demo-static/web/admin/creators.html`](../demo-static/web/admin/creators.html)（创作者账号与配额）、
  [`../demo-static/web/user/usage-billing.html`](../demo-static/web/user/usage-billing.html)（我的用量与账单）。
- 跳转关系：分别登记在两端各自的 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（管理端三页 + 用户端一页，及两端 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`；密钥保管待 `docs/security` 补）
- [ ] 代码与数据库
