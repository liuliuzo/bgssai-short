# AI 供应商与 OBS 密钥保管

对齐：`docs/feature/ai-capability-usage.md`、`docs/architecture/ai-provider-obs-design.md`、
`docs/security/secrets-checklist.md`。

## 1. 原则

- AI 供应商 API Key **禁止**写入 `ai_model_config.config_json`、properties、前端包或仓库明文。
- AI 供应商密钥落点：表 `sys_secret`（`secret_key` / `secret_value` / `enabled`），与现有 OAuth
  读库凭据同一通道；OBS 属中间件，AK/SK 只进 `application-*.properties`，不进本表。
- `ai_model_config.config_json` 只存非密钥参数，并通过 `secret_ref` 引用 `sys_secret.secret_key`。
- 管理端模型配置接口的请求/响应 **不得回显** 密钥明文；编辑模型时只读写 `secret_ref` 字符串。

## 2. 约定 secret_key

| secret_key | 用途 |
| --- | --- |
| `ai.tencent_tokenhub.api_key` | TokenHub 共用 Bearer Key（TEXT / VIDEO / VOICE / ASR / MUSIC） |
| `ai.openai_compat.api_key` | OpenAI 兼容网关默认密钥（可多模型共用同一 ref） |
| `ai.kling.api_key` | 可灵官方 API Key（Bearer；`provider_code=KLING` 的 IMAGE / VIDEO 共用） |
| `ai.minimax.api_key` | MiniMax / 海螺 API Key（Bearer） |
| `ai.seedance.api_key` | 火山方舟 Seedance API Key（Bearer） |
| `ai.veo.api_key` | Google Gemini / Veo API Key（`x-goog-api-key`） |
| `ai.{provider_code}.{model_code}.api_key` | 单模型覆盖（可选） |

本仓 DML 对所有 AI 供应商只放空值占位行；真实值由 **Admin「AI 密钥」页**
（`GET/POST /bgssai/admin/secrets/**`）或受控 SQL 写入。TokenHub 行使用
`ON DUPLICATE KEY UPDATE` 的仅空值回填规则，禁止提交真实 Bearer Key。九个生产模型可以保持启用，
但密钥为空或停用时任务必须明确失败。Admin「测试」只检查密钥非空已启用，**不向供应商发扣费请求**。

## 3. 运行时读取

1. `AiModelResolveService` / Provider 读取 `config_json.secret_ref`。
2. 查 `sys_secret`：`enabled=1` 且非空；否则任务失败。任何 AI Provider 均无 MOCK 密钥回退。
3. OBS：启动或首次 put/presign 时解析 properties 中的 `obs.*`；缺失时 `local` profile 可回退
   `LocalFileObjectStorage`，非 local 档 fail-fast 或任务级失败（见详设）。

## 4. Admin 接口

- `GET /bgssai/admin/secrets/list?key_prefix=ai.` → `secret_list`（掩码，不回显明文）
- `POST /bgssai/admin/secrets/update` → `{ secret_key, secret_value?, enabled?, description? }`
- `POST /bgssai/admin/secrets/test` → `{ secret_key }` → `{ ok, configured_flag, message }`

## 4. 日志与审计

- 禁止把 Authorization 头、AK/SK、预签名 URL 完整串打进 INFO 日志；DEBUG 亦需脱敏。
- 预签名 URL 仅返回给资源归属用户；asset_id 保持不可枚举。
- 模型启停、默认切换写管理操作日志（若项目已有通用操作日志则复用；否则至少应用日志 INFO 记 `model_id` + 操作者）。

## 5. 与 secrets-checklist 的关系

`secrets-checklist.md` 覆盖数据源 / JWT / TLS 等随 profile 打包项。
本文件覆盖第三方 AI 的库表密钥通道；profile 不承担供应商 API Key，OBS 中间件参数仍遵循
Standards §1 写入分环境 properties。
