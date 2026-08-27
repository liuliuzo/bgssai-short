# docs/security（安全设计 / 威胁建模 / 安全规范）

本目录承载项目安全相关设计，**必须与常规功能分开存放**，不混入 `feature` / `architecture`。

## 安全基线

| 项 | 基线 |
| --- | --- |
| 密码存储 | 哈希（`login/service/PasswordService`）；兼容历史明文，首次校验通过即在库内升级为 哈希 |
| 身份令牌 | JWT（jjwt 0.12.x，HMAC-SHA256）；密钥 `jwt.secret` 须 >= 32 字符 |
| 密钥管理 | dev 用占位密钥写在 `application-dev.properties`；test/prod 一律留空，由部署期 `application-secrets.properties` 经 `--spring.config.import` 注入；未注入时 `JwtUtil` 启动 fail-fast（详见 `secrets-checklist.md`） |
| 零信任角色 | 角色取自 JWT 声明（`UserContext` / `AdminAuthGuard`），绝不取自请求参数 |
| 传输鉴权 | 受保护接口需 `@NeedAop`（请求头 `Jwttoken`）；管理端另做 `PLATFORM_ADMIN` 角色校验 |
| CORS | `configuration/CorsConfig` 显式来源白名单，**禁用 `*`**（配置为 `*` 直接启动报错） |
| 限流 | Bucket4j 令牌桶（`@RateLimit`），登录接口按 IP 限流，防暴力破解 |
| 配置安全 | properties **禁用 `${}` 占位符**（Standards §1），环境差异分 profile 硬编码；生产凭证绝不入 git |
| 数据越权 | 用户端分页接口以 JWT 中的 `user_id` 过滤，用户只能读本人数据 |
| MCP PAT | Bearer `bgs_mcp_` 自校验；工具不接受 `user_id`；仅登录日志与健康探针。见 `mcp-server.md` |
| 支付回调验签 | 微信 / 支付宝 notify 必须验签，金额按分核对，已支付订单幂等。见 `payment-notify-verify.md` |
| AI / OBS 密钥 | 供应商 API Key 与 OBS AK/SK 进 `sys_secret`，`ai_model_config.config_json` 只存 `secret_ref`。见 `ai-provider-secrets.md` |

## 变更约束

涉及鉴权、密钥、权限、越权边界、外部回调签名等安全语义的改动，必须在本目录同步登记设计 / 威胁分析，
并与需求 / 原型 / 代码四层同步（Standards §0）。
