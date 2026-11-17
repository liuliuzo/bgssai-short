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
        "secret_key_masked": "",
        "enabled": 1,
        "default_model": 1,
        "last_check_status": "OK",
        "last_check_time": "2026-11-17 12:00:00"
      }
    ],
    "provider_preset_list": [
      {
        "provider_code": "MINIMAX",
        "provider_name": "MiniMax (海螺)",
        "auth_type": "BEARER",
        "default_base_url": "https://api.minimaxi.com",
        "abilities": [
          {
            "modality_type": "TEXT",
            "ability_type": "TEXT",
            "ability_label": "文本模型",
            "models": [
              { "model_code": "MiniMax-M2.5", "model_name": "MiniMax M2.5", "recommended": true }
            ]
          }
        ]
      }
    ]
  }
}
```

`auth_type` 取值：`BEARER`（单 API Key）、`AK_SK`（可灵双密钥 `api_key` + `secret_key`）、`API_KEY_HEADER`（PixVerse `API-KEY`）。

## POST /bgssai/user/ai-model-keys/create

支持单条创建或通过 `items` 一键批量创建多模态配置：

```json
{
  "provider_code": "KLING",
  "base_url": "https://api-beijing.klingai.com",
  "api_key": "ak_...",
  "secret_key": "sk_...",
  "modality_type": "VIDEO",
  "ability_type": "VIDEO",
  "model_code": "kling-v3",
  "model_name": "Kling V3",
  "only_user_key": 1,
  "items": [
    { "modality_type": "VIDEO", "ability_type": "VIDEO", "model_code": "kling-v3", "model_name": "Kling V3" },
    { "modality_type": "IMAGE", "ability_type": "IMAGE", "model_code": "kling-v3", "model_name": "Kling Image V3" }
  ]
}
```

返回 `{ "result": { "only_user_key": 1, "created_count": 2, "key_list": [ ... ] } }`。

## POST /bgssai/user/ai-model-keys/update

`api_key` 与 `secret_key` 可选，留空时保留已存密钥：

```json
{
  "key_id": "umk-1001",
  "modality_type": "TEXT",
  "ability_type": "TEXT",
  "provider_code": "DEEPSEEK",
  "model_code": "deepseek-chat",
  "model_name": "DeepSeek Chat",
  "base_url": "https://api.deepseek.com",
  "api_key": "",
  "secret_key": ""
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

支持传已保存的 `key_id`，或新建前直传 `provider_code`、`base_url`、`api_key`、`secret_key`、`model_code`：

```json
{
  "provider_code": "MINIMAX",
  "base_url": "https://api.minimaxi.com",
  "api_key": "eyJ...",
  "secret_key": "",
  "model_code": "MiniMax-M2.5"
}
```

按供应商执行零计费探活（`MINIMAX` 调 `POST /v1/get_voice`；`KLING` 签发短期 JWT 调 `GET /v1/videos/text2video?pageNum=1&pageSize=1`；`PIXVERSE` 调 `GET /openapi/v2/account/balance`；`DEEPSEEK` / `XAI` 调 `GET /models`），返回：

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

## GET /bgssai/user/ai-keys（兼容 GET /api/user/ai-keys）

返回当前用户对 11 个受支持厂商（`DEEPSEEK`、`MINIMAX`、`KLING`、`DASHSCOPE`、`ZHIPU`、`MOONSHOT`、`VOLCENGINE`、`HUNYUAN`、`OPENAI`、`ANTHROPIC`、`GOOGLE`）的脱敏密钥状态，不返回明文密钥：

```json
{
  "result": {
    "items": [
      {
        "provider_code": "MINIMAX",
        "provider_name": "MiniMax",
        "default_base_url": "https://api.minimaxi.com",
        "supports_ak_sk": false,
        "auth_mode": "API_KEY",
        "configured": true,
        "enabled": true,
        "masked_key": "mm-k****7890",
        "has_secret_key": false,
        "base_url": "https://api.minimax.io",
        "updated_at": "2026-11-17T10:00:00Z"
      }
    ]
  }
}
```

## POST /bgssai/user/ai-keys（兼容 POST /api/user/ai-keys）

保存或更新指定厂商的 BYOK 凭据。`provider_code` 支持 `KLINGAI` 别名自动归一化为 `KLING`，可灵支持单 `api_key`（Bearer）及 `access_key` + `secret_key`（HS256 JWT）两种模式；会校验空白裁剪、拒绝脱敏占位符（`****`）并校验 `base_url`（公网 HTTPS）。启用后运行时直连对应厂商并跳过 BGSSAI 平台额度检查与扣费。

```json
{
  "provider_code": "KLINGAI",
  "access_key": "ak-xxx",
  "secret_key": "sk-xxx",
  "base_url": "https://api.klingai.com",
  "enabled": true
}
```

## DELETE /bgssai/user/ai-keys/{providerCode}（兼容 DELETE /api/user/ai-keys/{providerCode}）

删除当前用户的指定厂商密钥配置（`providerCode` 自动归一化 `KLINGAI` -> `KLING`），返回 `{ "result": { "provider_code": "KLING", "deleted": true, "configured": false } }`。

