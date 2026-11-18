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
    "only_user_key": 0,
    "key_list": [
      {
        "key_id": "umk-1001",
        "key_name": "工作室",
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
        "last_check_time": "2026-11-17 12:00:00",
        "balance_status": "OK",
        "balance_list": [
          { "label": "", "amount": "110.00", "unit": "元" }
        ]
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

`balance_status`：`OK` / `UNAVAILABLE` / `FAILED`。`balance_list` 每项为 `label`、`amount`、`unit`。创建、更新、启停、设默认返回的 `item` 使用同一组字段。数值来自该密钥供应商的只读余额接口：PixVerse 积分、DeepSeek `total_balance`（CNY 为元）、可灵未过期资源包剩余次数、MiniMax 套餐剩余百分比或 `available_amount`。BGSSAI TokenHub 取网关 `GET /v1/account/balance` 的 `balance_tokens` 与 `balance_fen`（需 TokenHub 侧同步上线）。其余供应商（Grok、OpenAI、Claude、腾讯云 TokenHub）无密钥级余额只读接口，为 `UNAVAILABLE`。失败不返回 0。

## 供应商凭据

`GET /bgssai/user/ai-model-keys/list` 在原有模型列表之外返回 `credential_list`。凭据使用 `credential_id` 标识；密钥管理显示名称、供应商、脱敏密钥和当前使用状态。模型行保留原 `key_id`。

下表路径以 `/bgssai/user/ai-model-keys/credentials/` 为前缀，各操作均为 POST，限定当前用户。

| 路径后缀 | 参数与作用 |
| --- | --- |
| `create` | 提交供应商、名称、地址及密钥，可先于模型独立保存。 |
| `update` | 按 `credential_id` 修改名称、地址或密钥，密码字段留空时沿用旧值。 |
| `activate` | 按 `credential_id` 切换该供应商当前使用的 Key。 |
| `test` | 按 `credential_id` 对该凭据执行只读连通检测。 |
| `delete` | 按 `credential_id` 删除，服务端检查是否在用。 |

## POST /bgssai/user/ai-model-keys/create

账号页发送供应商、能力、模型编码和展示名，服务端使用供应商当前凭据。供应商尚未选定当前凭据时，先在「密钥管理」选用。支持单条或通过 `items` 批量创建。旧客户端仍可随请求传入密钥，兼容示例如下。

```json
{
  "provider_code": "KLING",
  "key_name": "备用账户",
  "activate_on_create": false,
  "base_url": "https://api-beijing.klingai.com",
  "api_key": "ak_...",
  "secret_key": "sk_...",
  "modality_type": "VIDEO",
  "ability_type": "VIDEO",
  "model_code": "kling-v3",
  "model_name": "Kling V3",
  "items": [
    { "modality_type": "VIDEO", "ability_type": "VIDEO", "model_code": "kling-v3", "model_name": "Kling V3" },
    { "modality_type": "IMAGE", "ability_type": "IMAGE", "model_code": "kling-v3", "model_name": "Kling Image V3" }
  ]
}
```

返回 `{ "result": { "only_user_key": 0, "created_count": 2, "key_list": [ ... ] } }`。顶层 `key_name` 可选，最多 64 个字符，批量新增时复制到每条新记录。`activate_on_create` 省略时为 true；传 false 时新增行均为停用，当前启用项不变。请求中的 `only_user_key` 为兼容字段，忽略不读。

## POST /bgssai/user/ai-model-keys/update

「模型选择」对自有模型的编辑、启停、设为默认和移除均发送 `provider_model_selection: true`。服务端按供应商、能力和模型编码识别同一模型的历史配置；与旧客户端的单条密钥配置操作并行兼容。模型编辑仅提交模型字段。

`key_id` 定位当前用户的单条配置。同供应商同模型可有多个 ID。`key_name` 省略保留原名称，空字符串清除名称。`api_key` 与 `secret_key` 留空时保留已存密钥：

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

按供应商执行零计费探活（`MINIMAX` 调 `POST /v1/get_voice`；`KLING` 签发短期 JWT 调 `GET /v1/videos/text2video?pageNum=1&pageSize=1`；`PIXVERSE` 调 `GET /openapi/v2/account/balance`；`DEEPSEEK` / `XAI` 调 `GET /models`；`OPENAI` 调 `GET /v1/models`；`ANTHROPIC` 调 `GET /v1/models`；`BGSSAI_TOKENHUB` 与 `TENCENT_TOKENHUB` 调 `GET /v1/models`），返回：

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

## POST /bgssai/user/ai-model-keys/mode（兼容接口）

无全局模式开关；该接口仅为旧客户端保留，请求值忽略、不落库。

```json
{ "only_user_key": 1 }
```

返回 `{ "result": { "only_user_key": 0 } }`。

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

