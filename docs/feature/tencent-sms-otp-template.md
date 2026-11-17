# 腾讯云登录验证码短信模板参数与国内/国际通道分流

## 适用范围

本仓自维护腾讯云短信 `2021-01-11` `SendSms` 发码客户端（`TencentSmsSender`）。全线统一模板参数顺序、区号路由与国内/国际通道严格隔离规则。

## 推荐正文模板

```
您正在登录{1}，序号{2}，验证码{3}，{4}分钟内有效
```

| 槽位 | 含义 | 来源 |
| --- | --- | --- |
| `{1}` | 产品标签 `product_label` | `sys_secret`（`sms.product_label` / `sms.intl.product_label`；回落 `bgssai.sms.product-label`，默认 `Short`） |
| `{2}` | 发码序号 `seq` | 每次发码现场 `SecureRandom` 生成四位数字（`0000`–`9999`），仅入模板与服务端日志 |
| `{3}` | 验证码 | 本次生成的 6 位数字 OTP |
| `{4}` | 有效分钟数 | `5` 分钟（与 `expires_in=300` 对齐） |

## `template_param_count`（`2` / `3` / `4`）

| count | `TemplateParamSet` 顺序 | 典型模板 |
| --- | --- | --- |
| `2` | `[验证码, 分钟]` | 存量两参模板（`2677885`） |
| `3` | `[产品标签, 验证码, 分钟]` | 三参过渡模板 |
| `4` | `[产品标签, 序号, 验证码, 分钟]` | 推荐；四参模板 |

配置非 `2|3|4` 时按 `2` 回落并记录 `WARN` 日志；管理端保存时校验为 `2|3|4`。

## 国内与国际/港澳台通道分流

1. **区号路由（`TencentSmsPhoneRoute`）**：
   - 先清理首尾空白、内部空白及连字符 `[\s-]`。
   - **国内通道（`MAINLAND`）**：匹配 `^1[3-9]\d{9}$` 或 `^(?:\+86|0086)1[3-9]\d{9}$`。库表与 OTP 键统一归一为 11 位数字；发往腾讯云 `PhoneNumberSet` 固定为 `"+86" + 11位号码`。
   - **国际/港澳台通道（`INTERNATIONAL`）**：匹配 `^\+[1-9]\d{6,14}$` 且不以 `+86` 开头（支持前导 `00` 转 `+`）。库表、OTP 键与 `PhoneNumberSet` 统一为标准 E.164 `+<国家码><号码>`。
   - 其它格式直接拒绝（`4301`）。
2. **配置隔离与 Fail-Closed**：
   - 国内通道读 `bgssai.sms.secret-id`、`bgssai.sms.secret-key`、`bgssai.sms.sdk-app-id`、`bgssai.sms.sign-name`（必填）、`bgssai.sms.template-id`，以及 `sms.endpoint`（默认 `sms.tencentcloudapi.com`）、`sms.region`（默认 `ap-guangzhou`）、`sms.product_label`、`sms.template_param_count`。
   - 国际/港澳台通道读 `sms.intl.enabled`（默认 `0`）、`sms.intl.secret_id` / `sms.intl.secret_key`（全空时复用国内密钥，仅配一侧则失败）、`sms.intl.endpoint`（默认 `sms.tencentcloudapi.com`）、`sms.intl.region`（默认 `ap-guangzhou`）、`sms.intl.sdk_app_id`、`sms.intl.sign_name`（可选）、`sms.intl.sender_id`（可选）、`sms.intl.template_id`、`sms.intl.template_param_count`、`sms.intl.product_label`。
   - `sms.intl.enabled != 1`、国际必填项缺失、或 `endpoint` 与 `region` 不兼容时直接失败，严禁回落国内签名或国内模板。
3. **发码安全约定**：
   - 发码响应 JSON 仅返回 `{ expires_in }`，不回显验证码、内部 `product` 或 `seq`。
   - 短信或邮件发送失败时不得写入有效 OTP，也不得保留冷却限流锁。

## 验收

1. 用户端登录、绑定手机与注销验证手机输入框提供区号选择器（默认 `+86`）。
2. 国内与国际/港澳台分别按对应 `endpoint`、`region`、`TemplateParamSet`（`2`/`3`/`4`）调用 `SendSms`；`intl_sign_name` 留空可正常发送。
3. 国际通道关闭或缺失必填配置时明确报错，不串用国内配置。
4. 管理端支持非破坏性查看状态并维护 `bgssai.sms.*`、`sms.*`、`sms.intl.*`。
