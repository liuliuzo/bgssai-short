# AI 能力配置与用量账单详设

对齐需求：`docs/feature/ai-capability-usage.md`。横切存储与 Provider 见 `ai-provider-obs-design.md`。

## 1. 范围

- 管理端：模型 CRUD / 设默认 / 启停；平台用量汇总与按创作者下钻；充值申请处置；创作者配额（已有 adjust-quota 复用）。
- 用户端：我的用量汇总与流水；低余额提示字段；充值申请提交与列表。
- 计费：任务 `SUCCEEDED` 写 `ai_usage_record` 并扣 `user_quota`（已有钱包，不新建第二套余额）。

本片不改支付下单主路径（订阅 / 加量包仍见 `subscription-credit-design.md`）。

## 2. 数据模型

| 表 | 说明 |
| --- | --- |
| `ai_model_config` | 能力模型目录；`config_json` 无密钥明文 |
| `ai_usage_record` | 成功任务用量流水（单价快照） |
| `recharge_apply` | 用户充值申请（运营手工加余额的工单） |
| `user_quota` / `quota_change_log` | 已存在，复用 |

索引按需求第六节。`recharge_apply_status`：`SUBMITTED` / `PROCESSING` / `COMPLETED` / `REJECTED`。

## 3. 关键流程

### 3.1 模型解析

见 `ai-provider-obs-design.md` §3。管理端设默认时：同 `ability_type` 下将其余 `default_flag` 置 0，保证至多一个默认。

### 3.2 成功计费

```
task SUCCEEDED
  -> insert ai_usage_record (user/work/episode/shot/task/ability/model/price/usage/cost)
  -> QuotaConsumeService.consume(cost_cent)
  -> short_work.cost_spent_cent += cost_cent
```

失败 / 取消 / 平台自动重试中间态不计费。

### 3.3 充值申请

用户提交 → `SUBMITTED` → 管理端 handle：`COMPLETED` 时调已有 `AdjustQuotaService` 增加 `extra_balance_cent`（或 period 余额，按备注约定默认加 extra）并写 `quota_change_log`；`REJECTED` 必填运营备注。

## 4. API 一览

契约正文：`docs/api/AI-CAPABILITY-USAGE.md`。页面映射：`docs/api/page-api-mapping.md`。

## 5. 错误码

| code | 含义 |
| --- | --- |
| `4014` | 无可用启用模型 |
| `4007` | 余额不足 |
| `4015` | 月度消费触顶 |
| `4006` | 业务拒绝（模型禁用、申请状态非法等） |

## 6. 安全

密钥：`docs/security/ai-provider-secrets.md`。用量接口仅本人或 `PLATFORM_ADMIN`。
