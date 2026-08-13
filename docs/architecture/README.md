# docs/architecture（通用技术架构设计）

本目录承载与具体功能无关的**通用技术设计（HOW）**：系统分解、ADR、数据模型、状态机、时序图、架构图等。
功能级详细设计归 `docs/feature`，对外接口契约归 `docs/api`，安全设计归 `docs/security`。

## 技术架构概览

### 拓扑

单仓 monorepo，含两套独立部署、共享一个 MySQL 库（`bgssai_short_drama`）的系统：

- 管理端 `bgssai-short-drama-admin`：后端 8080（`PLATFORM_ADMIN` 控制面）+ React 前端 3001。
- 用户端 `bgssai-short-drama-user`：后端 8081（`USER` 业务面）+ React 前端 3002。

每个后端为 Java 21 + Spring Boot 4，以可执行 jar 运行；前端 `build:deploy` 将产物同步到对应后端
`src/main/resources/static/`，由 Spring Boot 作为 SPA 提供，深链经 `SpaFallbackConfig` 白名单前缀
forward 到 `/index.html`。

### 横切基础设施

| 关注点 | 实现 |
| --- | --- |
| 统一响应 | `common/ApiResponse` / `common/PageResponse`（`{ code, message, success, result }` + 分页字段） |
| 鉴权 | `login/**` 签发 JWT（`utils/JwtUtil`，jjwt 0.12.x）；`@NeedAop` + `common/aop/JwtTokenAspect` 校验请求头 `Jwttoken`，把身份写入 `common/aop/UserContext`（ThreadLocal，finally 清理） |
| 角色守卫 | `common/AdminAuthGuard`（仅管理端，零信任：角色取自 JWT） |
| 全局异常 | `common/exception/GlobalExceptionHandler`（`@RestControllerAdvice`，错误码 → HTTP 状态映射） |
| 全链路日志 | `common/filter/RequestTraceFilter` 注入 traceId 到 MDC，`common/logging/MdcSupport` 管理键，logback pattern 自动输出 traceId/userId/companyCode |
| 限流 | `common/ratelimit/**`（Bucket4j 令牌桶，`@RateLimit` 按 IP/USER 维度） |
| 持久层 | MyBatis（仅 Generator Example 白名单方法）+ PageHelper 物理分页 + Druid 连接池 |
| 序列化 | `configuration/BgssConfig`：Jackson 2 全局 snake_case；Spring Boot 4 默认 Jackson 3，故显式插入 Jackson 2 转换器保住契约 |
| CORS | `configuration/CorsConfig`：显式白名单（禁 `*`），按 profile 配置 allowed-origins |
| 健康检查 | `health/**` + `configuration/AsyncConfig` 的 `healthProbeExecutor`：三个公开端点（存活 / 就绪 / 全量）+ 组件化探针 + 单检查硬超时 + 整批 TTL 缓存。全产品线统一口径见 Standards §13，详设见 `health-check-design.md` |

### 请求处理链（示例：GET /bgssai/admin/login-logs）

```
Client
  → RequestTraceFilter（注入 traceId）
  → DispatcherServlet → ListLoginLogsController(@NeedAop)
  → JwtTokenAspect（校验 Jwttoken，填充 UserContext）
  → ListLoginLogsService（角色守卫 + PageHelper.startPage + Example 查询 → PageResponse）
  → BgssConfig Jackson2 snake_case 序列化
  → Client
```

## 图示模板

`_template.html` 是自包含 HTML 图示模板（内联 SVG / CSS）。新增技术设计文档时复制它绘制流程图 /
时序图 / 状态泳道图，与本仓库原型统一为 HTML 形态。
