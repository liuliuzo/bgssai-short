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
| `ai.tokenhub_cn.api_key` | BGSSAI TokenHub-CN Bearer Key（境内中枢，短剧唯一调用键） |
| `ai.tokenhub_global.api_key` | 停用。短剧不读取、不作为境内失败时的回退 |
| `ai.tencent_tokenhub.api_key` | 旧 CN 键（仅空值回填兼容，新配置请用 `ai.tokenhub_cn.api_key`） |
| `ai.openai_compat.api_key` | 公司自托管网关密钥。仅 `https://chat.bgssai.com/v1`，文本 `qwen`、画面 `sd-turbo`、视频 `cogvideox`、配音 `piper`。中枢密钥非空时不走这条 |
| `ai.kling.api_key` | 可灵官方 API Key（Bearer；`provider_code=KLING` 的 IMAGE / VIDEO 共用） |
| `ai.minimax.api_key` | MiniMax / 海螺 API Key（Bearer） |
| `ai.seedance.api_key` | 火山方舟 Seedance API Key（Bearer） |
| `ai.veo.api_key` | Google Gemini / Veo API Key（`x-goog-api-key`） |
| `ai.{provider_code}.{model_code}.api_key` | 单模型覆盖（可选） |

本仓 DML 对所有 AI 供应商只放空值占位行；真实值由 **Admin「AI 密钥」页**
（`GET/POST /bgssai/admin/secrets/**`）或受控 SQL 写入。TokenHub 行使用
`ON DUPLICATE KEY UPDATE` 的仅空值回填规则，禁止提交真实 Bearer Key。九个生产模型可以保持启用，
但密钥为空或停用时任务必须明确失败。Admin「测试」只检查密钥非空已启用，**不向供应商发扣费请求**。
properties 只允许写中枢 base URL，禁止写 TokenHub API Key。

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

## 6. DEV 怎么配

主机是中间件，密钥是业务凭证，两边分开。

1. 走境内中枢：`application-dev.properties` 已写 `bgssai.llm.tokenhub-cn-base-url=https://dev.user.bgssai-tokenhub.cn`。管理端把真实密钥写入 `ai.tokenhub_cn.api_key`。开发库 init 会把该键和旧键 `ai.tencent_tokenhub.api_key` 盖回占位符，init 之后要再填一次。不要把真实密钥提交进 git。
2. 中枢密钥留空、改走公司自托管：管理端填写 `ai.openai_compat.api_key`。直连主机固定 `https://chat.bgssai.com/v1`，模型为 `qwen`、`sd-turbo`、`cogvideox`、`piper`。种子里的音色 `zh`、画面 `512x768` 只是占位，按网关实际值在模型配置里改。中枢密钥非空时这条直连不会生效。
3. 成片合成仍要部署机安装 ffmpeg。对象存储按 `deploy/README.md` 注入，不在本文件。
