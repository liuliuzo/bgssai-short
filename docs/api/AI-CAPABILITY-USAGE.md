# AI 能力配置与用量账单 API

统一封装 `{ "code": "0", "message": "OK", "success": true, "result": {} }`。字段 snake_case。POST 走 JSON body。鉴权请求头 `Jwttoken`。金额单位分。

详设：`docs/architecture/ai-capability-usage-design.md`。密钥：`docs/security/ai-provider-secrets.md`。

`ability_type` 枚举：`TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC` / `COMPOSE`。

## 错误码（本域）

| code | 含义 |
| --- | --- |
| `4014` | 该能力当前无可用启用模型 |
| `4007` | 余额不足 |
| `4015` | 月度用量触顶 |
| `4006` | 业务拒绝 |
| `4003` | 无权限 |
| `2002` | token 失效 |

---

## 管理端 — 模型

### GET /bgssai/admin/ai-models/list

Query：`ability_type`（可选）。需 `PLATFORM_ADMIN`。

```json
{ "result": { "model_list": [
  { "model_id": "m-tokenhub-video-minimax", "ability_type": "VIDEO", "ability_type_name": "视频",
    "provider_code": "TENCENT_TOKENHUB", "model_code": "minimax-video-h3", "model_name": "MiniMax-Video-H3",
    "quality_level": "STANDARD", "price_unit": "PER_SECOND", "unit_price_cent": 50,
    "default_flag": 1, "enabled_flag": 1, "timeout_sec": 600, "max_retry": 2,
    "config_json": "{\"base_url\":\"https://tokenhub.tencentmaas.com\",\"secret_ref\":\"ai.tencent_tokenhub.api_key\",\"submit_path\":\"/v1/wand/minimax-video-v2/generation\"}",
    "updated_time": "2026-08-31 15:02:31" } ] } }
```

响应不含任何密钥明文。

### POST /bgssai/admin/ai-models/create

Body：`ability_type`、`provider_code`、`model_code`、`model_name`、`quality_level`、`price_unit`、`unit_price_cent`、`timeout_sec`、`max_retry`、`config_json`、可选 `default_flag` / `enabled_flag`。

### POST /bgssai/admin/ai-models/update

Body：`model_id` + 可改字段（同 create）。

### POST /bgssai/admin/ai-models/set-default

Body：`model_id`。同 `ability_type` 下其余默认清零。

### POST /bgssai/admin/ai-models/toggle-enabled

Body：`model_id`、`enabled_flag`（0/1）。

---

## 管理端 — 用量

### GET /bgssai/admin/usage/summary

Query：`start_date`、`end_date`、`ability_type`（可选）。

```json
{ "result": {
  "total_cost_cent": 128000, "task_count": 420, "success_rate_percent": 96.5,
  "avg_duration_sec": 12.3,
  "by_ability_list": [ { "ability_type": "VIDEO", "cost_cent": 90000, "task_count": 200 } ],
  "by_day_list": [ { "day": "2026-08-20", "cost_cent": 12000, "task_count": 40 } ]
} }
```

### GET /bgssai/admin/usage/by-creator

Query：`page_num`、`page_size`、`start_date`、`end_date`。分页字段同全局约定。

---

## 管理端 — 充值申请

### GET /bgssai/admin/recharge/list

Query：`page_num`、`page_size`、`apply_status`（可选）。

### POST /bgssai/admin/recharge/handle

```json
{ "apply_id": "ra-1", "handle_action": "COMPLETE", "remark": "已加额度" }
```

`handle_action`：`COMPLETE` / `REJECT`；`remark` 必填。`COMPLETE` 增加申请人 `extra_balance_cent` 并写 `quota_change_log`。

---

## 用户端 — 用量

### GET /bgssai/user/usage/summary

Query：`start_date`、`end_date`（可选）。

```json
{ "result": {
  "balance_cent": 80000, "extra_balance_cent": 5000, "available_cent": 85000,
  "monthly_limit_cent": 100000, "month_spent_cent": 20000,
  "recent_episode_avg_cost_cent": 10720,
  "estimated_remaining_episode_count": 7,
  "low_balance_flag": 0,
  "by_work_list": [], "by_ability_list": []
} }
```

### GET /bgssai/user/usage/records

Query：`page_num`、`page_size`、`work_id`、`episode_id`、`ability_type`、`start_date`、`end_date`。

传入 `episode_id` 时额外返回 `episode_total_cost_cent`。

### POST /bgssai/user/recharge/apply

```json
{ "apply_amount_cent": 5000, "remark": "活动加量" }
```

### GET /bgssai/user/recharge/list

本人申请列表。

---

## 与任务估价的交叉字段

`POST /bgssai/user/ai-tasks/estimate` 响应增加：

- `affordable_episode_count`（按当前余额与预估单集成本）
- 选用的 `model_code` / `unit_price_cent`（解析结果）

`POST /bgssai/user/ai-tasks/submit` Body 可选 `model_code`（本次任务）与 `next_model_code`（`auto_next_flag=1` 时自动接视频所用模型）。

### GET /bgssai/user/ai-models/list

Query：`ability_type`（可选）。需登录 `USER`。返回该能力下 **启用** 模型，供创作台点选；不含 `config_json` 与密钥。

```json
{ "result": { "model_list": [
  { "model_id": "m-tokenhub-video-minimax", "ability_type": "VIDEO", "provider_code": "TENCENT_TOKENHUB",
    "model_code": "minimax-video-h3", "model_name": "MiniMax-Video-H3", "quality_level": "STANDARD",
    "price_unit": "PER_SECOND", "unit_price_cent": 50, "default_flag": 1 }
] } }
```

首批生产目录：TEXT `hy4-preview` / `kimi-k3` / `deepseek-v4-pro`；VIDEO
`minimax-video-h3` / `pixverse-video-v6.0` / `kling-video-v3`；VOICE
`minimax-speech-2.8-hd`；ASR `hy-asr-3.0-preview`；MUSIC `minimax-music-v2.6`。
