# AI 供应商与 OBS 密钥保管

对齐：`docs/feature/ai-capability-usage.md`、`docs/architecture/ai-provider-obs-design.md`、
`docs/security/secrets-checklist.md`。

## 1. 原则

- 供应商 API Key、OBS AK/SK **禁止**写入 `ai_model_config.config_json`、前端包、仓库明文示例以外的真实值。
- 密钥落点：表 `sys_secret`（`secret_key` / `secret_value` / `enabled`），与现有 OAuth 读库凭据同一通道。
- `ai_model_config.config_json` 只存非密钥参数，并通过 `secret_ref` 引用 `sys_secret.secret_key`。
- 管理端模型配置接口的请求/响应 **不得回显** 密钥明文；编辑模型时只读写 `secret_ref` 字符串。

## 2. 约定 secret_key

| secret_key | 用途 |
| --- | --- |
| `ai.openai_compat.api_key` | OpenAI 兼容网关默认密钥（可多模型共用同一 ref） |
| `ai.{provider_code}.{model_code}.api_key` | 单模型覆盖（可选） |
| `obs.access_key` | 华为云 OBS Access Key |
| `obs.secret_key` | 华为云 OBS Secret Key |

运营通过已有密钥运维手段或 SQL 写入 `sys_secret`；本仓 DML 只放 **空值或占位说明行**（`enabled=0`），避免真实密钥进 git。

## 3. 运行时读取

1. `AiModelResolveService` / Provider 读取 `config_json.secret_ref`。
2. 查 `sys_secret`：`enabled=1` 且非空；否则任务失败或回退 MOCK（仅 `provider_code=MOCK` 允许无密钥）。
3. OBS：启动或首次 put/presign 时解析 `obs.*`；缺失时 `local` profile 可回退 `LocalFileObjectStorage`，非 local 档 fail-fast 或任务级失败（见详设）。

## 4. 日志与审计

- 禁止把 Authorization 头、AK/SK、预签名 URL 完整串打进 INFO 日志；DEBUG 亦需脱敏。
- 预签名 URL 仅返回给资源归属用户；asset_id 保持不可枚举。
- 模型启停、默认切换写管理操作日志（若项目已有通用操作日志则复用；否则至少应用日志 INFO 记 `model_id` + 操作者）。

## 5. 与 secrets-checklist 的关系

`secrets-checklist.md` 覆盖数据源 / JWT / TLS 等随 profile 打包项。
本文件覆盖 **第三方 AI 与 OBS** 的库表密钥通道，二者并存：profile 不承担供应商 API Key。
