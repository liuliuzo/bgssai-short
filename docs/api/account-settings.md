# 账号设置 API（注销 / 绑定解绑）

统一封装 `{ "code": "0", "message": "OK", "success": true, "result": {} }`。字段 snake_case。POST 走 JSON Body。鉴权请求头 `Jwttoken`。

## GET /bgssai/user/account/login-methods

```json
{ "result": {
  "method_count": 2,
  "method_list": [
    { "method_type": "password", "platform": "password", "label": "账号密码", "bound": true },
    { "method_type": "oauth", "platform": "wechat", "label": "微信用户", "bound": true }
  ]
} }
```

## POST /bgssai/user/account/login-methods/bind-oauth

```json
{ "platform": "wechat", "code": "...", "state": "..." }
```

冲突（已绑其他账号）→ `code=4020`。

## POST /bgssai/user/account/login-methods/bind-contact

```json
{ "method_type": "phone", "phone": "13800138000", "code": "123456" }
```

或 `method_type=email` + `email`。

## POST /bgssai/user/account/login-methods/unbind

```json
{ "method_type": "oauth", "platform": "wechat",
  "verify_method": "password", "password": "..." }
```

最后一种方式 → `code=4019`。未重新验证 → 参数错误，绑定关系不变。

## GET /bgssai/user/account/deletion/preview

返回后果说明、未结订单数、进行中任务数。

## POST /bgssai/user/account/deletion

```json
{ "verify_method": "password", "password": "...",
  "acknowledge_consequences": true,
  "force_despite_open_items": false,
  "reason": "可选" }
```

未确认后果 / 未验证 → 不注销。未结事项且未 force → `code=4021`。成功后原凭证失效；再登录 → `code=4018`。
