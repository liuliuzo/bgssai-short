# bgssai-short（AI 短剧制作平台）

昆山兵贵神速智能科技有限公司 BGSSAI 产品线的产品仓之一，承载 **AI 短剧制作平台**：面向短剧创作者的
AI 短剧制作应用，把一部短剧从想法做到成片的全流程搬到线上，用 AI 承担其中最费人力的环节。产品定位与
范围边界见 [`PRODUCT_VISION.md`](./PRODUCT_VISION.md)。

本仓自骨架仓 [`bgssai-skeleton`](https://github.com/liuliuzo/bgssai-skeleton) 初始化而来，以单仓
（monorepo）方式统一管理一套「管理端 + 用户端」的前后端工程，落地 BGSSAI 全局开发规范
（`docs/BGSSAI-Standards.md`）中的强制约定：单接口单 Controller、扁平代码、JSON 强制 snake_case、
MyBatis Example 模式、PageHelper 物理分页、禁用 Lombok、禁用 emoji、properties 禁用 `${}` 占位符等。

## 当前状态

**M1 产品需求与原型定稿完成**：AI 短剧制作全链路已拆为 13 份功能需求（`docs/feature/**`，含 12 张
自包含 HTML 配图）与双端 27 个原型页面（`docs/demo-static/web/**`），页面 → API 映射主表已同步。

**代码侧尚未落任何短剧业务功能**：仓库现有的仍是骨架继承来的可运行基线（登录鉴权、登录日志分页示例、
健康检查三端点）。下一步是 M2 详细设计与契约（`docs/architecture` / `docs/api` / `docs/security`），
之后才进入 M3 代码与数据库。

业务功能一律按 **需求 → 原型 → 详设 → 代码** 顺序推进（Standards §0），不得跳过上游直接编码。

### 产品主线一句话

把一部短剧从想法做到成片的全流程搬到线上，用 AI 承担最费人力的环节；并且不止于「能做出来」——
四道质量门（剧本 / 分镜 / 画面 / 成片）配合结构模板对标、多方案择优与三视角评审团，
把问题定位到具体一场戏、一个镜头、一句台词，让创作者定点返工而不是整集重抽
（主线见 [`docs/feature/quality-pipeline.md`](./docs/feature/quality-pipeline.md)）。

## 目录结构

```text
bgssai-short-parent/         # 本仓库根（Maven 聚合父工程，packaging=pom）
├── README.md                      # 本文件（总览）
├── PRODUCT_VISION.md              # 产品愿景与定位
├── CLAUDE.md / AGENTS.md          # AI 助手协作约定
├── pom.xml                        # Maven 聚合父工程（聚合两个后端模块）
├── sql/                           # DDL.sql / DML.sql（本产品共享库权威 schema）
├── docs/                          # 所有文档（单一事实来源）
├── deploy/                        # 部署说明与 Jenkins 流水线脚本（与中央仓逐字节一致）
├── .github/workflows/             # CI（构建校验）
│
├── bgssai-short-admin/      # 【管理端】端口 8080，role_code=PLATFORM_ADMIN
│   ├── bgssai-short-admin/       # 后端：Java 21 + Spring Boot 4 + MyBatis
│   └── bgssai-short-admin-react/ # 前端：React 18 + Vite + Ant Design 5（端口 3001）
│
└── bgssai-short-user/       # 【用户端】端口 8081，role_code=USER
    ├── bgssai-short-user/        # 后端：Java 21 + Spring Boot 4 + MyBatis
    └── bgssai-short-user-react/  # 前端：React 18 + Vite + Ant Design 5（端口 3002）
```

两个后端共用同一个 MySQL 库（`bgssai_short`）：管理端承载运营方（`PLATFORM_ADMIN`）的控制面
API（账号、内容审核、AI 能力与用量管理），用户端承载创作者（`USER`）的制作业务 API。每个前端工程与其
对应后端工程是同一父目录下的兄弟目录，因此各前端的 `npm run build:deploy`（`scripts/sync-static.mjs`，
向对应后端 `src/main/resources/static/` 同步产物）开箱即用。

## 内置的基础链路

| 能力 | 后端落点 | 说明 |
| --- | --- | --- |
| 统一响应封装 | `common/ApiResponse`、`common/PageResponse` | `{ code, message, success, result }`，分页附 `page_num/page_size/total_size` |
| 登录鉴权 | `login/**`、`utils/JwtUtil`、`common/aop/JwtTokenAspect` | 账号密码登录签发 JWT；`@NeedAop` 切面校验请求头 `Jwttoken` 并写入 `UserContext` |
| 角色守卫 | `common/AdminAuthGuard`（仅管理端） | 零信任：角色取自 JWT，不取自请求参数 |
| 全局异常 | `common/exception/GlobalExceptionHandler` | 统一错误码与 HTTP 状态映射 |
| 全链路 traceId | `common/filter/RequestTraceFilter`、`common/logging/MdcSupport` | logback pattern 自动输出 traceId/userId |
| 限流 | `common/ratelimit/**`（Bucket4j 令牌桶） | `@RateLimit` 注解按 IP/USER 维度限流 |
| 持久层范式 | `mapper/**`、`model/**`（MyBatis Generator 产物） | 仅用 Example 白名单方法；分页走 PageHelper |
| snake_case | `configuration/BgssConfig` | Jackson 2 全局 snake_case 序列化 |
| 健康检查 | `health/**`、`configuration/AsyncConfig` | 全产品线统一口径：`GET /bgssai/health/liveness` · `/readiness` · `/bgssai/health`，组件化探针 + 硬超时 + TTL 缓存。口径见 Standards §13 |
| 示例功能 | `loginlog/**` | 登录日志分页列表，演示 `@NeedAop` + `PageResponse` + PageHelper 全链路 |

## 文档单一事实来源（SoT）

全项目文档（需求 / 原型 / 架构 / API 契约 / 安全）统一收敛在仓库根 **`docs/`** 目录：

- 目录结构规约：`docs/DIRECTORY-LAYOUT.md`
- 全局开发规范（最高优先级约束）：`docs/BGSSAI-Standards.md`
- 产品愿景：`PRODUCT_VISION.md`

前端工程不承载需求 / 设计 / 接口文档，一律依据 `docs/` 开发。

> `docs/BGSSAI-Standards.md` 与 `docs/DIRECTORY-LAYOUT.md` 是骨架仓 `bgssai-skeleton` 的**同步副本**，
> 本仓不改本体：需要改规范时先在骨架仓定稿，再整份同步回本仓。

## 数据库脚本

本产品的库 schema 与种子数据统一在 **`sql/`** 目录维护（`DDL.sql` / `DML.sql`），不在各后端模块的
`src/` 下重复存放。当前仅含两张基础表 `sys_user`、`login_log`，业务表随需求逐个落地。

## 构建与运行

前置：JDK 21、Maven 3.9+、Node 18+、MySQL 8。

成片合成走本机 FFmpeg（`provider_code=FFMPEG`），**运行 / 部署机需预装 ffmpeg 并在 PATH 中**，否则合成任务会明确失败并提示「本机未安装 ffmpeg」。文本 / 配音走 OpenAI 兼容网关，API Key 由管理端写入 `sys_secret.ai.openai_compat.api_key`（业务第三方凭证不入 properties）。

```bash
# 1. 建库并导入 schema 与种子数据
mysql < sql/DDL.sql
mysql bgssai_short < sql/DML.sql

# 2. 编译后端（根聚合工程一次编译两个模块）
mvn -q -DskipTests compile

# 3. 启动后端（dev 档连境外开发库；远程 HTTPS 443。本机明文调试可加 --server.port=8080 --server.ssl.enabled=false）
mvn -pl bgssai-short-admin/bgssai-short-admin spring-boot:run   # 管理端
mvn -pl bgssai-short-user/bgssai-short-user   spring-boot:run   # 用户端

# 4. 启动前端（另开终端）
cd bgssai-short-admin/bgssai-short-admin-react && npm install && npm run dev   # 3001
cd bgssai-short-user/bgssai-short-user-react   && npm install && npm run dev   # 3002
```

两个后端均为 Java 21 + Spring Boot 4，不使用 Docker，以可执行 jar 运行（`java -jar`）。部署细节见
`deploy/README.md`。

## 新增功能的必经链路

1. 需求：`docs/feature/**`（照 `_TEMPLATE-feature.md` 落盘，含详细设计与图示）。
2. 原型：`docs/demo-static/**`（涉及的每个端目录，并更新该端 `PAGE-FLOW.html`）。
3. 详设与契约：`docs/architecture` / `docs/api`（含 `docs/api/page-api-mapping.md`）/ `docs/security`。
4. 代码与数据库：`src/**`、前端工程与 `sql/**`；改表后重跑 MyBatis Generator。

顺序不可颠倒、不可跳跃；同一次变更四层同步完成、当次交清（详见 `docs/BGSSAI-Standards.md` §0）。
