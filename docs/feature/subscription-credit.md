# 订阅套餐与加量 credit（subscription-credit）

> feature-id: SUB-CREDIT｜版本 v1.0（定稿）｜2026-08-14
>
> 商业模式对齐 `PRODUCT_VISION.md`：订阅套餐发放周期余额 + 加量 credit 包。金额一律为分（`*_cent`）。

## 一、背景与目标

- 背景：短剧生成按模型单价真实烧钱。创作者需要可预期的月度额度，用尽后能即时加量，而不是只靠运营手工调余额。
- 目标：用户端可购买套餐与加量包（微信 Native / 支付宝当面付），支付成功后发放额度；任务成功时扣减额度。运营仍可手工调余额并留痕。
- 关联角色：`USER`（购买与消费）、`PLATFORM_ADMIN`（商户配置、加量包目录、手工调额）。

## 二、需求澄清（WHAT）

### 用户故事

1. 作为 `USER`，我希望看到 FREE / PRO / MAX5 / MAX10 / MAX20 套餐与月度发放额度，以便按产能选档。
2. 作为 `USER`，我希望用微信扫码或支付宝当面付购买套餐或加量包，支付成功后余额立即到账。
3. 作为 `USER`，我希望任务成功才扣费，失败不扣，以便成本可控。
4. 作为 `PLATFORM_ADMIN`，我希望配置微信 / 支付宝收款商户与加量包目录，并在必要时手工调整创作者额度且必须填备注。

### 业务规则

1. **套餐档位**：`FREE` / `PRO` / `MAX5` / `MAX10` / `MAX20`。每档 `monthly_balance_cent` 为开通或续期时写入周期钱包的额度；`monthly_limit_cent` 为本期用量上限；`price_cent` 为服务端售价（分）。FREE 售价 0，开通不走渠道。
2. **加量包**：`credit_pack` 目录，`credit_cent` 为入账加量（分），`price_cent` 为服务端售价。加量进入 `user_quota.extra_balance_cent`，**不随套餐到期清零**。
3. **钱包口径**：
   - `balance_cent`：本期套餐剩余额度（续期时按套餐发放额重置）。
   - `extra_balance_cent`：加量钱包。
   - 展示可用合计 = `balance_cent + extra_balance_cent`。
   - `month_spent_cent` 为本期已消耗；触顶 `monthly_limit_cent` 时拒绝新任务（`4015`）。
4. **计价**：下单金额只取服务端套餐 / 加量包目录价，忽略客户端传入的金额。
5. **支付渠道**：微信 Native（`code_url` 二维码）、支付宝当面付预下单（`code_url` 二维码）。渠道代码 `WECHAT` / `ALIPAY`。
6. **沙箱 / 生产**：`bgssai.payment.sandbox-enabled=true` 时走 HMAC-SHA256 占位下单与回调验签，不读商户行。生产模式必须有启用且标记收款的商户，凭据不全返回 `60013`，不得静默回落沙箱。
7. **回调**：公开路径验签 + 金额核对（分）+ 幂等。已 `paid` 的订单重复通知不重复发放。验签失败返回渠道要求的失败应答（微信 HTTP 401 + FAIL JSON；支付宝 `failure`）。
8. **扣减时机**：任务 `SUCCEEDED` 时调用额度扣减：先扣 `balance_cent`，不足再扣 `extra_balance_cent`；合计不足返回 `4007`。`FAILED` / `CANCELED` 不计费。
9. **手工调额**：`POST /bgssai/admin/creators/adjust-quota`，`remark` 必填，写 `quota_change_log`。
10. **本轮不做**：自动续费代扣、退款自动化、JSAPI / H5。

### 套餐种子价（示例，运营可改目录行）

| plan_code | 名称 | price_cent | monthly_balance_cent | monthly_limit_cent |
| --- | --- | --- | --- | --- |
| FREE | 免费 | 0 | 10000 | 10000 |
| PRO | 专业 | 9900 | 100000 | 100000 |
| MAX5 | MAX 5 | 29900 | 500000 | 500000 |
| MAX10 | MAX 10 | 49900 | 1000000 | 1000000 |
| MAX20 | MAX 20 | 89900 | 2000000 | 2000000 |

### 加量包种子价

| pack_code | 名称 | credit_cent | price_cent |
| --- | --- | --- | --- |
| pack_50 | 加量 50 元 | 5000 | 4900 |
| pack_200 | 加量 200 元 | 20000 | 18900 |
| pack_1000 | 加量 1000 元 | 100000 | 89900 |

## 三、验收标准

1. 用户端套餐列表返回五档，金额与发放额与目录一致，不以客户端报价为准。
2. 购买 PRO 微信支付：创建订单金额等于目录 `price_cent`，返回 `code_url`；用户端弹出扫码弹窗（二维码由 `code_url` 渲染，可复制链接），轮询 `order/status` 至 `paid` 后刷新余额；沙箱回调 HMAC 验签通过后订阅 `active`，`balance_cent` 重置为该档 `monthly_balance_cent`。支付宝当面付同 UX。
3. 购买加量包后仅增加 `extra_balance_cent`，套餐到期不扣减该字段。
4. 同一 `order_no` 重复通知只发放一次。
5. 回调金额与订单 `amount_cent` 不一致则拒绝，不改订单状态。
6. 任务成功扣费：先周期后加量；余额不足 `4007`；月度上限触顶 `4015`。
7. 管理端保存微信 / 支付宝商户（路径对齐 VPN `/bgssai/admin/pay/alipay-configs/save` 与 wechat）；保存加量包；`adjust-quota` 无备注被拒。
8. 生产模式无收款商户时下单失败 `60013`。

## 四、接口契约（snake_case）

用户端（需 `Jwttoken`，`user_id` 取自 JWT）：

- `GET /bgssai/user/subscription/plans`
- `GET /bgssai/user/subscription/me`
- `POST /bgssai/user/subscription/order/create` body：`plan_code`、`payment_channel`
- `POST /bgssai/user/subscription/order/status` body：`order_no`（套餐与加量包共用；含查单补偿）
- `GET /bgssai/user/credit-packs`
- `POST /bgssai/user/credit-packs/orders` body：`pack_code`、`payment_channel`

渠道回调（公开，验签）：

- `POST /bgssai/user/payment/wechat/notify`
- `POST /bgssai/user/payment/alipay/notify`

管理端（需 `Jwttoken` + `PLATFORM_ADMIN`）：

- `POST /bgssai/admin/pay/wechat-configs/save` · `GET /bgssai/admin/pay/wechat-configs`
- `POST /bgssai/admin/pay/alipay-configs/save` · `GET /bgssai/admin/pay/alipay-configs`
- `GET /bgssai/admin/credit-packs/list` · `POST /bgssai/admin/credit-packs/save`
- `POST /bgssai/admin/creators/adjust-quota`（`user_id` + `balance_delta_cent` + `extra_delta_cent` + `monthly_limit_cent` + `remark`）

错误码：`4007` 余额不足 / `4015` 月度触顶 / `60005` 回调验签或金额不符 / `60013` 生产支付未配置。

详细字段见 `docs/api/SUBSCRIPTION-CREDIT.md`。

## 五、流程 / 状态机

订单：`created` -> `paid` | `closed`。`paid` 仅由验签通过且金额一致的回调（或 FREE 零元当场发放）写入。

扣费：任务成功 -> 校验合计余额与月度上限 -> 先 `balance_cent` 后 `extra_balance_cent` -> 累加 `month_spent_cent`。

配图：原型页内嵌购买步骤；详设时序见 `docs/architecture/subscription-credit-design.md`。

## 六、数据模型影响

新增表（canonical 审计列 `id` / `del_flag` / `creator` / `modifier` / `gmt_create` / `gmt_modified`）：

- `subscription_plan`
- `user_subscription`
- `user_quota`（`balance_cent`、`monthly_limit_cent`、`extra_balance_cent`、`month_spent_cent`、`month_reset_date`）
- `credit_pack`
- `payment_order`
- `quota_change_log`
- `wechat_pay_config` / `alipay_config`（列集对齐 VPN）

## 七、原型

- 用户端：[`../demo-static/web/user/subscription.html`](../demo-static/web/user/subscription.html)
- 管理端：[`../demo-static/web/admin/credit-packs.html`](../demo-static/web/admin/credit-packs.html)、[`../demo-static/web/admin/pay-merchants.html`](../demo-static/web/admin/pay-merchants.html)
- 跳转：两端 `PAGE-FLOW.html`
- 映射：`../api/page-api-mapping.md`

## 八、四层同步检查清单

- [x] 需求（本文件 / `PRODUCT_VISION.md`）
- [x] 原型（用户订阅页 + 管理端加量包 / 商户配置）
- [x] 设计与契约（`docs/architecture/subscription-credit-design.md`、`docs/api/SUBSCRIPTION-CREDIT.md`、`docs/security/payment-notify-verify.md`）
- [x] 代码与数据库（双端 Java / React、`sql/DDL.sql`、`sql/DML.sql`）
