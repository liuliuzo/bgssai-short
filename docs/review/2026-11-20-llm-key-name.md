# 2026-11-20 LLM 密钥命名与平台名称探测检查记录

## 检查范围

- Request 1：`sys_secret` 与 `user_ai_provider_key` 扩展可空列 `key_name`（`VARCHAR(64)`）与 `platform_key_name`（`VARCHAR(128)`）；管理端与用户端 BYOK 服务、MyBatis 显式列映射、SSRF 安全只读探测及 React 页面与打包产物。
- Request 3：`sql/DML.sql`、`sql/dev/DML.sql` 与 `sql/prod/DML.sql` 为 `ai.tencent_tokenhub.api_key` 与 `ai.tokenhub_cn.api_key` 按左到右安全空值回填规则初始化 `'sk-bgssai-short-cn-temp'` 与 `'bgssai-short temporary gateway key (tokenhub-cn)'`，保持 `ai.tokenhub_global.api_key` 为空且 `enabled = 0`。
- 四层同步：`docs/feature/**`、`docs/architecture/**`、`docs/api/account-settings.md`、`docs/security/ai-provider-secrets.md`、`docs/demo-static/**` 及 `docs/review/**`。

## 依据条款

- 配置分层与明文存储规范（`.cursor/rules/config-layering.mdc`、`.cursor/rules/plaintext-policy.mdc`）
- 无冗余解释文案规范（`.cursor/rules/no-explanatory-copy.mdc`）
- 四层同步、前端 `static/` 构建同步与 SSRF 安全边界

## 逐条结论与证据

1. 表结构与增量脚本：通过
   - `sql/DDL.sql` 已为 `sys_secret` 与 `user_ai_provider_key` 增加 `key_name VARCHAR(64)` 与 `platform_key_name VARCHAR(128)`。
   - 新增幂等脚本 `sql/upgrade/20261120_secret_provider_key_name.sql` 并在 `sql/README.md` 登记。
   - `UserAiProviderKeyMapper.xml` 全量列出显式字段，无 `SELECT *`，无 Lombok。
2. TokenHub-CN 种子与回填安全：通过
   - `sql/DML.sql`、`sql/dev/DML.sql` 与 `sql/prod/DML.sql` 均将 `platform_key_name` 置于 `secret_value` 之前执行条件回填，仅在 `secret_value` 为空（或已为占位值且 `platform_key_name` 为空）时写入；`ai.tokenhub_global.api_key` 保持空且 `enabled = 0`。
3. 平台名称非阻塞 SSRF 安全探测：通过
   - 管理端 `PlatformKeyNameProber` 与用户端 `PlatformKeyNameProber` 仅对 BGSSAI TokenHub、xAI 与 OpenRouter 发起公网 `https://` GET 只读探测，禁用重定向并拒绝本地/私网/链路本地/CGNAT/ULA 地址，超时 3 秒且失败静默返回 `null`。
   - 其余受支持或非 AI 密钥（`ai.deepseek.*`、`ai.hunyuan.*`、`bgssai.sms.*` 等）发起 0 次 HTTP 请求。
4. 生命周期与接口验证：通过
   - Admin `PlatformKeyNameProberTest`（3/3）与 `SysSecretAdminServiceTest`（4/4）通过，总计 7/7。
   - User `UserAiProviderKeyServiceTest`（5/5）、`PlatformKeyNameProberTest`（2/2）、`TokenHubClientSupportTest`（5/5）、`UserByokCatalogAndModelResolveTest`（2/2）与 `UserOwnedProviderKeyTest`（7/7）通过，总计 21/21。
5. 前端与静态资源同步：通过
   - `AiSecretsPage.jsx`、`UserAiProviderKeysCard.jsx` 与 `userAiProviderKeysHelper.js` 提供可选 `密钥名称` 与只读 `平台名称` 展示，无多余说明文字与表情符号。
   - `bgssai-short-admin-react` 14/14 测试通过、`bgssai-short-user-react` 199/199 测试通过，均已构建同步至 `src/main/resources/static`。

## 处置

- 相关后端、前端、SQL 与四层文档原型同步已完成并在工作区验证。
