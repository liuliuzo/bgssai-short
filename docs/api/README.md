# docs/api（REST / API 契约）

本目录承载项目所有接口说明与 API 契约，离线 Markdown 维护，**禁用 Swagger 等运行时注解工具**。字段命名
以原型（`docs/demo-static/<端>/`）为权威参照，前后端逐字对齐。

管理端与用户端共用同一套数据结构与契约，因此本目录**不按端拆分**：一份契约同时覆盖两端，端差异（路径
前缀 `/bgssai/admin` 与 `/bgssai/user`、角色门禁、数据可见范围）在同一篇契约内并列说明。「页面 → 后端 API」
映射主表见 [`page-api-mapping.md`](./page-api-mapping.md)。

## 通用约定

- 字段命名一律 **snake_case**（如 `jwt_token`、`page_num`、`ip_address`）；Java 内部属性仍为驼峰，由
  `BgssConfig` 全局 Jackson 2 snake_case 策略转换。
- POST 业务参数一律走 **JSON Body**（POST→body），不拼在 URL 查询串。
- 鉴权走请求头 **`Jwttoken`**（HTTP 头名大小写不敏感，前端注入 `Jwttoken`，后端 `@NeedAop` 切面读取）。
- 统一响应封装：
  ```json
  { "code": "0", "message": "OK", "success": true, "result": {} }
  ```
  分页接口额外含 `page_num` / `page_size` / `total_size`。成功 `code` 为 `"0"` 或 `"200"`。

## 通用错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| `0` / `200` | 200 | 成功 |
| `2002` | 200 | token 校验失败 / 失效（前端据此跳登录） |
| `4001` | 200 | 凭证错误（账号或密码不正确） |
| `4003` | 200 | 无权限（角色不满足） |
| `50000001` | 200 | 请求参数非法 |
| `429000` | 429 | 触发限流（响应头含 `Retry-After`） |
| `999999` | 500 | 系统错误 |

**未授权 / 无权限一律 HTTP 200 + 封装内 code**：`2002` 与 `4003` 不论由鉴权切面
（`JwtTokenAspect`）、登录角色门禁（`AdminLoginService`）还是受保护接口的角色 / 归属校验
（`ForbiddenException` / `UnauthorizedException`）产出，**都返回 HTTP 200，失败信息只在响应封装的
`code` 里**——前端据 `code` 统一处理（`2002` 清身份跳登录、`4003` 提示后跳登录），不依赖 HTTP 状态码。

> 完整错误码枚举见后端 `common/exception/CommonErrorCode`，该枚举与本表**逐条对应**、是代码侧单一来源：
> 各层一律引用枚举常量，不写字面量。
>
> 墓碑标注（2026-07-30 废弃）：历史代码中 `UnauthorizedException` / `ForbiddenException` 曾携带
> `601` / `603` 并配 HTTP 401 / 403，与本表及前端不一致（同一语义两个码），已统一为 `2002` / `4003`
> + HTTP 200。`601` / `603` **不再使用**，客户端无需识别。

## 接口清单

| 接口 | 方法 | 路径 | 契约 |
| --- | --- | --- | --- |
| 管理端登录 | POST | `/bgssai/admin/login` | `auth-login.md` |
| 用户端登录 | POST | `/bgssai/user/login` | `auth-login.md` |
| 登录日志（管理端，全量） | GET | `/bgssai/admin/login-logs` | `auth-login.md` |
| 登录日志（用户端，仅本人） | GET | `/bgssai/user/login-logs` | `auth-login.md` |
| 存活探针（两端同构，公开） | GET | `/bgssai/health/liveness` | `health-check.md` |
| 就绪探针（两端同构，公开） | GET | `/bgssai/health/readiness` | `health-check.md` |
| 全量健康报告（两端同构，公开） | GET | `/bgssai/health` | `health-check.md` |

> 健康检查三端点是**全产品线共用契约**（Standards §13），不带 `/admin` · `/user` 端前缀、不需要
> `Jwttoken`，且 `code` / `success` 恒表达「接口调用成功」——健康结论只看 `result.status` 与 HTTP 状态码。
