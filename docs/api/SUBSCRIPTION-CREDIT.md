# 订阅套餐与加量 credit API

统一封装 `{ "code": "0", "message": "OK", "success": true, "result": {} }`。字段 snake_case。POST 走 JSON body。鉴权请求头 `Jwttoken`。金额单位分。

## 用户端

### GET /bgssai/user/subscription/plans

鉴权：需 JWT。

```json
{ "code": "0", "success": true, "result": { "plan_list": [
  { "plan_code": "PRO", "plan_name": "专业", "price_cent": 9900,
    "monthly_balance_cent": 100000, "monthly_limit_cent": 100000 }
] } }
```

### GET /bgssai/user/subscription/me

```json
{ "result": {
  "plan_code": "PRO", "status": "active",
  "current_period_start": "2026-08-14 12:00:00",
  "current_period_end": "2026-09-14 12:00:00",
  "balance_cent": 80000, "extra_balance_cent": 5000,
  "monthly_limit_cent": 100000, "month_spent_cent": 20000,
  "available_cent": 85000
} }
```

无行时按 FREE 惰性建档。

### POST /bgssai/user/subscription/order/create

```json
{ "plan_code": "PRO", "payment_channel": "WECHAT" }
```

`payment_channel`：`WECHAT` / `ALIPAY`（大小写不敏感）。

```json
{ "result": {
  "order_no": "SUB...", "order_type": "subscription", "status": "created",
  "plan_code": "PRO", "amount_cent": 9900, "grant_cent": 100000,
  "payment_channel": "WECHAT", "code_url": "weixin://wxpay/bizpayurl?pr=...",
  "pay_form": null, "expires_at": "2026-08-14 12:30:00"
} }
```

### GET /bgssai/user/credit-packs

```json
{ "result": { "pack_list": [
  { "pack_code": "pack_50", "name": "加量 50 元", "credit_cent": 5000, "price_cent": 4900 }
] } }
```

### POST /bgssai/user/credit-packs/orders

```json
{ "pack_code": "pack_50", "payment_channel": "ALIPAY" }
```

响应同下单，`order_type=credit_pack`。

### POST /bgssai/user/subscription/order/status

鉴权：需 JWT。仅订单本人可查。Body：`{ "order_no": "SUB..." }`。

套餐与加量包共用此接口。`created` 且非沙箱、创建超过 5 秒时回查渠道（微信商户单号 /
`alipay.trade.query`）；渠道已支付则发放后返回 `paid`。渠道查询异常不影响本接口（只读库）。

```json
{ "result": {
  "order_no": "SUB...", "order_type": "subscription", "status": "paid",
  "amount_cent": 9900, "payment_channel": "WECHAT"
} }
```

## 回调（公开）

### POST /bgssai/user/payment/wechat/notify

成功：HTTP 200，`{"code":"SUCCESS","message":"OK"}`。验签失败：HTTP 401，`{"code":"FAIL","message":"SIGN_ERROR"}`。

沙箱 body 为 form：`out_trade_no`、`transaction_id`、`trade_state=SUCCESS`、`amount`（分）、`sign`（HMAC-SHA256）。

### POST /bgssai/user/payment/alipay/notify

`application/x-www-form-urlencoded`。成功：`success`。失败：`failure`。

沙箱字段：`out_trade_no`、`trade_no`、`trade_status=TRADE_SUCCESS`、`total_amount`（元）、`sign`。

## 管理端

### POST /bgssai/admin/pay/wechat-configs/save

对齐 VPN。空白凭据列保留原值。`mch_id` 全表唯一；逻辑删除后同号复活。

### GET /bgssai/admin/pay/wechat-configs

明文回显凭据。

### POST /bgssai/admin/pay/alipay-configs/save

对齐 VPN `/bgssai/admin/pay/alipay-configs/save`。

### GET /bgssai/admin/pay/alipay-configs

### GET /bgssai/admin/credit-packs/list

含未上架。`result.pack_list`。

### POST /bgssai/admin/credit-packs/save

```json
{ "id": null, "pack_code": "pack_50", "name": "加量 50 元",
  "credit_cent": 5000, "price_cent": 4900, "is_active": 1, "sort_order": 1 }
```

### POST /bgssai/admin/creators/adjust-quota

```json
{ "user_id": "u-demo-0001", "balance_delta_cent": 1000,
  "extra_delta_cent": 0, "monthly_limit_cent": 100000, "remark": "运营补偿" }
```

`remark` 必填。写 `quota_change_log`。

## 错误码

| code | 含义 |
| --- | --- |
| 4007 | 余额不足 |
| 4015 | 月度用量触顶 |
| 60004 | 不支持的支付渠道 |
| 60005 | 回调验签失败或金额不符 |
| 60013 | 生产支付未配置 |
| 50000001 | 参数非法 |
| 4003 | 无权限 |
| 2002 | token 无效 |
