# 契约：登录与登录日志

字段一律 snake_case；统一响应封装 `{ code, message, success, result }`；鉴权走请求头 `Jwttoken`。

本篇一份契约覆盖两端（管理端 / 用户端），端差异逐节并列说明。对应原型：管理端
[`../demo-static/web/admin/index.html`](../demo-static/web/admin/index.html)、用户端
[`../demo-static/web/user/index.html`](../demo-static/web/user/index.html)；页面 → API 映射见
[`page-api-mapping.md`](./page-api-mapping.md)。

## 1. 登录

管理端与用户端登录形状一致，仅路径与返回 `role_code` 不同：

- 管理端：`POST /bgssai/admin/login`，成功 `role_code=PLATFORM_ADMIN`（非该角色返回 4003）。
- 用户端：`POST /bgssai/user/login`，成功 `role_code=USER`。

公开接口（无需 token），按 IP 限流（capacity 5 / 每分钟补 5）。

登录页与登录后页脚展示官网五条法律外链（中文标签开中文页，英文标签开英文页，新标签打开）。正文只在
`https://www.bgssai.com` 权威路径（`/terms-of-service/`、`/privacy-policy/`、
`/personal-information-inventory/`、`/third-party-sharing/`、`/app-permissions/` 及其 `/en/` 对照页）。
管理端只做外链，不托管条款。版权行不写证照编号。状态：需法务审阅。
见 [`../feature/legal-fan-out.md`](../feature/legal-fan-out.md)。

### 请求体

```json
{
  "login_type": "password",
  "identifier": "admin",
  "password": "your-password"
}
```

- `login_type`：密码登录为 `password`。短信 / 邮箱验证码走独立端点（见 §1.1）。
- `identifier`：登录账号（`sys_user.user_name`）。
- `password`：密码（后端 哈希校验，兼容历史明文并首登升级）。

## 1.1 验证码登录即注册（仅用户端）

公开接口（无需 token，无 `@NeedAop`）。未注册标识符现场建号。

- `POST /bgssai/user/sms/send` `{ "phone": "13800138000" }` → `{ expires_in: 300, product: "Short", seq: 42 }`；凭据缺失 `5203`。
  - `product` / `seq`：本次写入短信模板的产品名与递增序号（客服对照）；邮箱发码不含这两项。
  - 模板参数个数由 `bgssai.sms.template-param-count`（`2|3|4`，**默认 2**）决定：
    - `2` → `[code, minutes]`（生产已审批模板 `2677885` 双参；待审 `2734311` 通过前勿切 3/4）
    - `3` → `[product#seq, code, minutes]`
    - `4` → `[product, seq, code, minutes]`
  - 产品名配置：`bgssai.sms.product-label`（默认 `Short`），写在 `application.properties`，非 `sys_secret`。
- `POST /bgssai/user/login/sms` `{ "phone": "13800138000", "code": "123456" }`，成功形状与密码登录一致，可带 `is_new_user`。
- `POST /bgssai/user/email/send` `{ "email": "buyer@example.com" }` → `{ expires_in: 300 }`；凭据缺失 `5204`。
- `POST /bgssai/user/login/email` `{ "email": "buyer@example.com", "email_code": "123456" }`。

短信 / SMTP 凭据读 `sys_secret`（DML STEP SMS-1 / SMTP-1），不新增 Admin 页。

腾讯云短信对齐 BGSSAI 共享控制台应用（`TencentSmsSender` / `SmsSecrets`）：

| 项 | 开发 | 生产 |
| --- | --- | --- |
| `bgssai.sms.sdk-app-id` | `1401156379`（`sql/dev/DML.sql`） | `1401191076`（`sql/prod/DML.sql`） |
| `bgssai.sms.sign-name` | `昆山兵贵神速智能科技`（共享 `sql/DML.sql`） | 同左 |
| `bgssai.sms.template-id` | `2677885`（登录 OTP，已审批双参；勿用待审 `2734311`） | 同左 |
| `bgssai.sms.product-label` | `Short`（`application.properties`） | 同左 |
| `bgssai.sms.template-param-count` | `2`（默认；与 `2677885` 对齐） | 同左 |

`SecretId` / `SecretKey` 仍走 `sys_secret`；示例占位见 `sms/credentials.example.env`。

### 成功响应

```json
{
  "code": "0",
  "message": null,
  "success": true,
  "result": {
    "jwt_token": "eyJhbGciOiJIUzI1NiJ9...",
    "role_code": "PLATFORM_ADMIN",
    "role_name": "管理员",
    "user_id": "u-admin-0001",
    "user_name": "admin",
    "real_name": "系统管理员"
  }
}
```

前端将 `result` 存入本地并在后续请求注入请求头 `Jwttoken: <jwt_token>`。

### 失败响应

```json
{ "code": "4001", "message": "账号或密码不正确", "success": false }
```

```json
{ "code": "4003", "message": "access denied: only admin may use this console", "success": false }
```

## 2. 登录日志（示例分页接口）

- 管理端：`GET /bgssai/admin/login-logs`（返回全部登录记录）。
- 用户端：`GET /bgssai/user/login-logs`（仅返回当前用户本人记录，`user_id` 取自 JWT，不取自参数）。

需鉴权（`@NeedAop`，请求头 `Jwttoken`）。物理分页走 PageHelper。

### 查询参数

| 参数 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `page_num` | int | 1 | 1 起页码 |
| `page_size` | int | 20 | 每页条数（>100 回落 20） |

### 成功响应（PageResponse）

```json
{
  "code": "0",
  "success": true,
  "result": [
    {
      "id": 101,
      "user_id": "u-admin-0001",
      "action": "LOGIN_SUCCESS",
      "ip_address": "203.0.113.7",
      "user_agent": "Mozilla/5.0 ...",
      "create_time": "2026-07-19 10:00:00"
    }
  ],
  "page_num": 1,
  "page_size": 20,
  "total_size": 1
}
```

### 未授权 / 无权限响应

token 缺失或失效时切面直接返回（HTTP 200）：

```json
{ "code": "2002", "message": "token validation failed", "success": false }
```

token 有效但身份不符时同样是 HTTP 200 + 封装内 code：

- 管理端 `GET /bgssai/admin/login-logs`，角色不是 `PLATFORM_ADMIN`：

  ```json
  { "code": "4003", "message": "only admin can view login logs", "success": false }
  ```

- 用户端 `GET /bgssai/user/login-logs`，上下文取不到 `user_id`（异常态）：

  ```json
  { "code": "2002", "message": "no user in context", "success": false }
  ```

## 登录页第三方入口：只画已开通的渠道（2026-09-20）

- `GET /bgssai/user/oauth/channels` 返回凭证已配齐（且未停用）的第三方登录渠道；与授权地址接口同一把尺子。
- **登录页与注册页只渲染返回里的渠道**，一个都没开通则第三方登录整段（含分隔线）不出现。
  此前入口无条件常驻，点下去才报「未开放 / 暂不可用」——界面承诺了实际用不了的登录方式。
- 品牌标统一引自 `public/brand/oauth/*.svg`（全线同一套方形 viewBox 矢量标），按钮只写品牌名，分隔线固定「第三方账号登录」。口径见骨架仓 [全局开发规范](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/BGSSAI-Standards.md) 第 14 节。
