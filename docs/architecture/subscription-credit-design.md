# 订阅套餐与加量 credit 详设

> 对齐需求 `docs/feature/subscription-credit.md`。金额单位：分。

## 1. 组件

- 用户端：套餐 / 加量包查询、下单、公开回调。
- 管理端：商户配置、加量包保存、手工调额。
- 渠道：`WechatPaymentChannel`（Native）、`AlipayPaymentChannel`（当面付 precreate）。`PaymentChannelRouter` 按 `WECHAT` / `ALIPAY` 路由，业务层不做渠道 if-else。

## 2. 下单

1. 校验 JWT `user_id`。
2. 按 `plan_code` / `pack_code` 读目录行（`is_delete=0` 且 `is_active=1`）。
3. `amount_cent` = 目录 `price_cent`；`grant_cent` = 套餐 `monthly_balance_cent` 或加量包 `credit_cent`。
4. FREE 或 `price_cent=0`：订单直接 `paid` 并发放，不调渠道。
5. 否则插入 `payment_order`（`created`，`expires_at` = now+30min），调用渠道 `createPayment`，回写 `code_url` / `pay_form` / `pay_merchant_no`。
6. 生产模式无收款商户或凭据不全：`60013`。

## 3. 查单补偿

`POST /bgssai/user/subscription/order/status`：仅订单本人。`created` 且非沙箱、创建超过 5 秒时
回查渠道；已支付则 `grantIfCreated` 后返回 `paid`。沙箱不查渠道。渠道异常只记 WARN，接口仍返回库状态。
前端订阅页在展示 `code_url` 后每 2.5 秒轮询直至 `paid`。

## 3. 回调（验签永不删除）

```
渠道 POST notify
  -> PaymentChannel.verifyAndParseNotify
  -> verified=false -> 60005 / 渠道 FAIL 应答
  -> 非成功态 -> 仍返回成功应答（避免无意义重试）
  -> 按 order_no 查单
  -> amount_cent 与通知金额（分）不一致 -> 60005
  -> status=paid -> 幂等返回成功应答
  -> status=created -> 置 paid，按 order_type 发放
```

微信生产：APIv3 平台证书 / 公钥验签 + AEAD 解密。沙箱：HMAC-SHA256（`sign` 字段，canonical 排序排除 sign）。

支付宝生产：`AlipaySignature.rsaCheckV1` RSA2，按 `app_id` 路由商户。沙箱：HMAC-SHA256。

## 4. 发放

- `order_type=subscription`：upsert `user_subscription`（`active`，周期一个月），`user_quota.balance_cent` = 套餐 `monthly_balance_cent`，`monthly_limit_cent` 同步，`extra_balance_cent` 不动。
- `order_type=credit_pack`：`extra_balance_cent += grant_cent`。

同一事务内改订单状态与钱包。

## 5. 扣减（任务成功）

`QuotaConsumeService.consume(userId, costCent, monthLimitCheck=true)`：

1. 确保 `user_quota` 行存在；跨月则重置 `month_spent_cent` 与 `month_reset_date`（周期额度不在此自动补发，补发只发生在套餐续期）。
2. `available = balance_cent + extra_balance_cent`；不足 -> `4007`。
3. `month_spent_cent + cost > monthly_limit_cent` 且上限 > 0 -> `4015`。
4. 先减 `balance_cent`，再减 `extra_balance_cent`；`month_spent_cent += cost`。
5. CAS：`updateByExampleSelective` 带当前 `balance_cent` / `extra_balance_cent` 条件，冲突重试。

## 6. 表与索引

见 `sql/DDL.sql`。`payment_order.uk_order_no`；`user_quota.uk_user_id`；`user_subscription.uk_user_id`。

## 7. 配置

用户端 profile：

- `bgssai.payment.sandbox-enabled`
- `bgssai.payment.wechat.sign-key`（仅沙箱 HMAC）
- `bgssai.payment.alipay.sign-key`（仅沙箱 HMAC）

商户凭据只在 `wechat_pay_config` / `alipay_config`。
