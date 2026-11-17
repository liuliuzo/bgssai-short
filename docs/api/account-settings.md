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

或 `method_type=email` + `email`。手机号支持大陆 11 位 / `+86` / `0086`（存库归一为 11 位）及非 `+86` 标准 `+E.164`（存库保留 `+E.164`）。

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

## GET /bgssai/user/ai-model-keys/list

可选参数：`modality_type`（`TEXT`/`IMAGE`/`VIDEO`/`AUDIO`）、`ability_type`（`TEXT`/`IMAGE`/`VIDEO`/`VOICE`/`ASR`/`MUSIC`）。

```json
{
  "result": {
    "only_user_key": 1,
    "key_list": [
      {
        "key_id": "umk-1001",
        "modality_type": "TEXT",
        "ability_type": "TEXT",
        "provider_code": "XAI",
        "provider_name": "Grok (xAI)",
        "model_code": "grok-4.7",
        "model_name": "Grok 4.7",
        "base_url": "https://api.x.ai/v1",
        "api_key_masked": "xai-****9k2a",
        "enabled": 1,
        "default_model": 1,
        "last_check_status": "OK",
        "last_check_time": "2026-11-17 12:00:00"
      }
    ],
    "provider_preset_list": [
      {
        "provider_code": "XAI",
        "provider_name": "Grok (xAI)",
        "default_base_url": "https://api.x.ai/v1",
        "abilities": [
          {
            "modality_type": "TEXT",
            "ability_type": "TEXT",
            "ability_label": "文本模型",
            "models": [
              { "model_code": "grok-4.7", "model_name": "Grok 4.7", "recommended": true }
            ]
          }
        ]
      }
    ]
  }
}
```

## POST /bgssai/user/ai-model-keys/create

支持单条创建或通过 `items` 一键批量创建多模态配置：

```json
{
  "provider_code": "XAI",
  "base_url": "https://api.x.ai/v1",
  "api_key": "xai-...",
  "modality_type": "TEXT",
  "ability_type": "TEXT",
  "model_code": "grok-4.7",
  "model_name": "Grok 4.7",
  "only_user_key": 1,
  "items": [
    { "modality_type": "TEXT", "ability_type": "TEXT", "model_code": "grok-4.7", "model_name": "Grok 4.7" },
    { "modality_type": "IMAGE", "ability_type": "IMAGE", "model_code": "grok-imagine-image-2.0", "model_name": "Grok Imagine Image 2.0" },
    { "modality_type": "VIDEO", "ability_type": "VIDEO", "model_code": "grok-imagine-video-1.5", "model_name": "Grok Imagine Video 1.5" }
  ]
}
```

返回 `{ "result": { "only_user_key": 1, "created_count": 3, "key_list": [ ... ] } }`。

## POST /bgssai/user/ai-model-keys/update

`api_key` 可选，留空时保留已存密钥：

```json
{
  "key_id": "umk-1001",
  "modality_type": "TEXT",
  "ability_type": "TEXT",
  "provider_code": "XAI",
  "model_code": "grok-4.7",
  "model_name": "Grok 4.7",
  "base_url": "https://api.x.ai/v1",
  "api_key": ""
}
```

返回 `{ "result": { "item": { ... } } }`。

## POST /bgssai/user/ai-model-keys/toggle

```json
{ "key_id": "umk-1001", "enabled": 1 }
```

返回 `{ "result": { "item": { ... } } }`。

## POST /bgssai/user/ai-model-keys/set-default

```json
{ "key_id": "umk-1001" }
```

返回 `{ "result": { "item": { ... } } }`。

## POST /bgssai/user/ai-model-keys/delete

```json
{ "key_id": "umk-1001" }
```

逻辑删除并按需自动顺延同能力剩余启用项为默认，返回 `{ "result": { "deleted": 1, "key_id": "umk-1001" } }`。

## POST /bgssai/user/ai-model-keys/test

支持传已保存的 `key_id`，或新建前直传 `provider_code`、`base_url`、`api_key`、`model_code`：

```json
{ "key_id": "umk-1001" }
```

向 `{base_url}/models` 发起零计费连通检测，返回：

```json
{
  "result": {
    "ok": true,
    "status": "OK",
    "message": "连接成功",
    "model_matched": true,
    "discovered_models": ["grok-4.7", "grok-imagine-image-2.0"]
  }
}
```

## POST /bgssai/user/ai-model-keys/mode

```json
{ "only_user_key": 1 }
```

返回 `{ "result": { "only_user_key": 1 } }`。

