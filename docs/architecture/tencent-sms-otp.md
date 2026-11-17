# 腾讯云短信 OTP：模板参数与国内/国际双通道分流

对应需求：[`../feature/tencent-sms-otp-template.md`](../feature/tencent-sms-otp-template.md)。

## 结论

本仓 `bgssai-short-user` 维护 `TencentSmsPhoneRoute`、`TencentSmsTemplateParams` 与 `TencentSmsSender`，按号码区号将短信路由至国内（`MAINLAND`）或国际/港澳台（`INTERNATIONAL`）配置组，禁止把境外及港澳台号码送入国内签名/模板通道。

## `sys_secret` 配置键（非破坏性扩展）

存量国内凭证键（`bgssai.sms.secret-id`、`bgssai.sms.secret-key`、`bgssai.sms.sdk-app-id`、`bgssai.sms.sign-name`、`bgssai.sms.template-id`）保持不覆盖；新增以下扩展键（通过 `ON DUPLICATE KEY UPDATE ... IF(secret_value IS NULL OR secret_value = '', ...)` 幂等补齐）：

| secret_key | 默认值 | 用途 |
| --- | --- | --- |
| `sms.endpoint` | `sms.tencentcloudapi.com` | 国内短信接入域名 |
| `sms.region` | `ap-guangzhou` | 国内腾讯云短信地域 |
| `sms.product_label` | `Short` | 国内短信正文产品名 `{1}` |
| `sms.template_param_count` | `2` | 国内模板参数数量 `2\|3\|4` |
| `sms.intl.enabled` | `0` | 国际/港澳台短信开关（`0` / `1`） |
| `sms.intl.secret_id` | 空 | 国际短信 SecretId（与 `secret_key` 全空时复用国内密钥） |
| `sms.intl.secret_key` | 空 | 国际短信 SecretKey（与 `secret_id` 全空时复用国内密钥） |
| `sms.intl.endpoint` | `sms.tencentcloudapi.com` | 国际短信 API 域名（国内站 `sms.tencentcloudapi.com` / 国际站 `sms.intl.tencentcloudapi.com`） |
| `sms.intl.region` | `ap-guangzhou` | 国际短信 API 地域（`ap-guangzhou`/`ap-beijing`/`ap-nanjing` 对应国内站；`ap-singapore`/`eu-frankfurt` 对应国际站） |
| `sms.intl.sdk_app_id` | 空 | 国际/港澳台短信 `SmsSdkAppId` |
| `sms.intl.sign_name` | 空 | 国际短信签名（可选；仅非空时传入 `SendSmsRequest.SignName`） |
| `sms.intl.sender_id` | 空 | 国际短信 SenderId（可选；仅非空时传入 `SendSmsRequest.SenderId`） |
| `sms.intl.template_id` | 空 | 国际/港澳台短信模板 ID |
| `sms.intl.template_param_count` | `2` | 国际模板参数数量 `2\|3\|4` |
| `sms.intl.product_label` | `Short` | 国际短信正文产品名 `{1}` |

用户端 `SmsSecrets` 读取上述 `sys_secret` 键（其中 `sms.product_label` 与 `sms.template_param_count` 在库值为空时回落 `application.properties` 的 `SmsOtpProperties`）；管理端 `SysSecretAdminService` 通过 `/bgssai/admin/secrets/**` 维护与非破坏性测试，并对敏感密钥键屏蔽明文回显。

## 路由与安全约定

1. 调用 `TencentSmsPhoneRoute.routeOf(rawPhone)` 识别 `MAINLAND` / `INTERNATIONAL`，非法返回 `null` 即拒收。
2. 存储与 OTP/冷却键统一调 `TencentSmsPhoneRoute.normalizeForStorage(rawPhone)`；发往腾讯云 `PhoneNumberSet` 调 `TencentSmsPhoneRoute.toTencentE164(rawPhone)`。
3. 国际通道关闭（`sms.intl.enabled != 1`）、仅填写单侧密钥（`secret_id` / `secret_key` 一空一非空）、缺失 `intl_sdk_app_id` / `intl_template_id`，或 `endpoint` 与 `region` 不匹配时 fail-closed，严禁回落国内签名与模板。
4. `OtpLoginSupport` 先通过真实 `TencentSmsSender.send` / `SmtpMailSender.sendLoginCode` 投递成功后再写入 `InMemoryOtpStore` 与冷却计时器；发送失败不留存有效 OTP 或冷却锁，响应 JSON 仅返回 `{ expires_in }`，不回显 `product`、`seq` 或验证码。
5. `AccountLoginMethodService` 与 `AccountDeletionService` 对手机绑定与注销验证均按 `normalizeForStorage` 归一。
