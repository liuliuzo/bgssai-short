# AGENTS.md

> 本文件与 `CLAUDE.md` 内容保持一致（供不同 AI 工具各自读取），改一处必须同步改另一处。

## 仓库定位

本仓库是 BGSSAI 产品线的**应用仓（产品仓）**，承载产品「**AI 短剧制作平台**」——面向短剧创作者的
AI 短剧制作应用，让用户以更低门槛把一部短剧从想法做到成片。

本仓自骨架仓 [`bgssai-skeleton`](https://github.com/liuliuzo/bgssai-skeleton) 初始化而来：鉴权、统一
响应、分页、日志链路、CORS、限流、健康检查等基础链路与「管理端 + 用户端」双端工程结构均继承自骨架，
本仓在其上落本产品自己的业务。规范与目录口径的**唯一权威仍在骨架仓**，本仓只持同步副本、不自行发明。

## 仓库目的与核心功能

**目的：承载 AI 短剧制作平台的全部业务产出**——需求、原型、设计与契约、应用代码、数据库脚本，四层
在本仓同版本维护。

| 核心功能 | 落点 | 说明 |
| --- | --- | --- |
| 产品愿景与定位 | `PRODUCT_VISION.md` | 产品为谁、解决什么问题、范围边界与角色矩阵；是需求（`docs/feature/**`）的上位背景 |
| 功能需求（版本基准） | `docs/feature/**` | 每个功能的需求澄清 + 详细设计；一切下游产出对齐需求当前版本 |
| 原型与跳转真值 | `docs/demo-static/**` | 字段命名 / 响应结构 / 枚举 / 页面跳转的权威参照；按端分目录 |
| 设计与接口契约 | `docs/architecture`、`docs/api`、`docs/security` | 数据模型、状态机、REST 契约、页面 → API 映射、安全设计 |
| 可运行的双端工程 | `bgssai-short-drama-admin/**`、`bgssai-short-drama-user/**` | 管理端（8080 / 3001）与用户端（8081 / 3002），Java 21 + Spring Boot 4 + MyBatis + React 18 + Vite + Ant Design 5，共用同一 MySQL 库 |
| 基础链路（自骨架继承） | `common/**`、`configuration/**`、`utils/**` | 统一响应与分页、JWT 登录鉴权与 `@NeedAop` 切面、角色守卫、全局异常与错误码、全链路 traceId、Bucket4j 限流、CORS、SPA 回退。逐项清单见 `README.md`「内置的基础链路」 |
| 健康检查 | `health/**` + `configuration/AsyncConfig` + 对应四层产出 | 全产品线唯一口径（Standards §13）：`/bgssai/health/liveness` · `/readiness` · `/bgssai/health` 三端点、组件化探针、单检查硬超时、整批 TTL 缓存。只对齐、不发明 |
| 本产品 schema 权威 | `sql/DDL.sql`、`sql/DML.sql` | 收敛到仓库根、目录扁平，不用 `migration/` 与 `V*__xxx.sql`；当前仅 `sys_user`、`login_log` 两张基础表 |
| 交付与部署范式 | 前端 `npm run build:deploy` → 后端 `static/`、`deploy/**`、`.github/workflows/ci.yml` | 单体交付（前端产物打进后端 jar）、密钥模板、CI 构建校验 |
| 成本红线闸门 | `.claude/hooks/block-scheduled-automation.py` | 拦截会自唤起、持续消耗 token 的循环任务（Standards §12） |

## 职责边界（本仓不做什么）

* 不维护规范本体：编码与研发规范、目录拓扑的唯一权威在 `bgssai-skeleton`。本仓 `docs/BGSSAI-Standards.md`
  是骨架仓的**同步副本**，发现规范缺口应回骨架仓改本体再同步，**严禁**只在本仓改出第二套口径。
* 不做应用部署与 Jenkins / Actions 编排（`bgssai-workflows`）、不执行建库 / 清库（`bgssai-database`）、
  不采集服务器日志（`bgssai-logs`）。
* 不承载其它产品的业务：本仓只放 AI 短剧制作平台自身的需求与实现。

## 产品线仓库拓扑（4 个非应用仓 + 产品仓）

| 仓库 | 类型 | 为了达到什么目的 | 核心功能一句话 |
| --- | --- | --- | --- |
| [bgssai-skeleton](https://github.com/liuliuzo/bgssai-skeleton) | 非应用（骨架与规则） | 让所有产品的代码结构、项目内容与编码规范有唯一维护地 | 可运行的「管理端 + 用户端」骨架工程 + 全局开发规范 |
| [bgssai-database](https://github.com/liuliuzo/bgssai-database) | 非应用（数据库） | 让全部应用的数据库脚本集中在一个仓库维护与执行 | DDL / DML 批次归档 + 建库 / 清库入口 |
| [bgssai-workflows](https://github.com/liuliuzo/bgssai-workflows) | 非应用（CI/CD 与运维） | 统一维护管理 Jenkins 与 GitHub 两条通道，承载部署与运维任务 | Jenkins 共享库 + Job 清单唯一权威 + 中央全量启停 |
| [bgssai-logs](https://github.com/liuliuzo/bgssai-logs) | 非应用（日志） | 统一收集全部应用服务器上的日志 | 日志采集 / 清理 / 状态检查 + 批次归档 |
| **[bgssai-short-drama](https://github.com/liuliuzo/bgssai-short-drama)（本仓）** | 应用 | 承载 AI 短剧制作平台业务 | 业务代码 + 本产品 `sql/` 权威 + 单产品 deploy / stop |
| 其余产品仓（blog / builder / geo-cn / geo-global / publish / saas / vpn / voiceunion / marklens） | 应用 | 承载各产品自身业务 | 业务代码 + 本产品 `sql/` 权威 + 单产品 deploy / stop |

非应用仓之间**不重叠、不互相代管**：数据库脚本只在 database 仓执行，部署编排只在 workflows 仓维护，
日志与主机清单只在 logs 仓维护，规范与骨架只在 skeleton 仓维护。需要对方的能力时引用对方仓库的权威
文件，**不复制第二份实现**。

> 待办：本仓为新增产品仓，骨架仓 `CLAUDE.md` / `AGENTS.md` 的拓扑表与「9 个产品仓」计数尚未登记本仓，
> 需在骨架仓侧同步（含 Standards §13 与健康检查各文档中的仓数 / 后端数），本仓不代改。

## 新功能必经链路

PRD（`docs/feature`）→ 原型（`docs/demo-static`）→ 架构与契约（`docs/architecture` / `docs/api` / `docs/security`）→ 代码（`src/**`、前端工程）与数据库（`sql/**`）。单向依赖，需求是版本基准与单一事实来源，一切下游产出对齐需求当前版本。

**功能研发必经顺序（强制规定，非功能需求除外）**：项目功能（功能需求）必须严格按 **需求文档（`docs/feature`）→ 原型设计 HTML（`docs/demo-static`）→ 详细设计文档（`docs/architecture` / `docs/api` / `docs/security`）→ 代码编写（`src/**`、前端工程与 `sql/**`）** 的先后顺序推进：上游产物未定稿并落盘前不得启动下游，顺序不可颠倒、不可跳跃。**非功能需求除外**——不改变对外功能契约（字段 / 枚举 / 状态 / 接口语义 / 页面跳转 / 业务规则）的变更（如性能调优、内部重构、依赖升级、配置 / 构建 / CI、日志与可观测性、技术债清理、文档订正等），不强制走原型设计 HTML 与详细设计文档两步。本条与「四层同步完成、缺一不可」互为表里——前者管**顺序**（上游先行），后者管**完整**（同一次变更四层齐备），详见 Standards §0「功能研发必经顺序：需求 → 原型 → 详设 → 代码」。

## 原型参照

原型（`docs/demo-static/**`）是接口 JSON 字段命名、响应结构、枚举 / 状态值与页面跳转关系的**权威参照**，前后端实现逐字对齐。原型按端分目录（**仅原型按端拆分**，其余文档目录统一维护）：管理端 `docs/demo-static/web/admin/`、用户端 `docs/demo-static/web/user/`，移动端 `docs/demo-static/ios/`、`docs/demo-static/android/`（仅用户端）；各端概览入口为该目录 `index.html`、跳转真值为该目录 `PAGE-FLOW.html`，web 端分流入口为 `docs/demo-static/web/index.html`。目录权威见 `docs/DIRECTORY-LAYOUT.md`。

## 仓库强制约定

* 全局开发规范（最高优先级约束，自动导入每次会话）：@docs/BGSSAI-Standards.md
* 任何需求变更或 bug 修复，必须在同一次提交内同步完成**需求文档 / 原型 / 设计文档 / 代码**四层的对应修改，缺一不可，以防止代码与文档漂移（详见 Standards §0「需求与缺陷变更四层同步完成、缺一不可」）。
* 接口的 POST 业务参数一律走 JSON Body（POST→body，详见 Standards §3）。
* 目录权威：`docs/DIRECTORY-LAYOUT.md`。
* 提交信息遵循 Conventional Commits。
* 一切产出物（代码 / 文档 / 原型 / 提交信息 / PR 描述 / 评审与回信）**全局禁用 emoji 及装饰性图形符号**；语义性符号（→、×、§ 等用于流程示意、表格与条款引用者）不属装饰性符号，允许使用。
* `docs/BGSSAI-Standards.md` 与 `docs/DIRECTORY-LAYOUT.md` 为骨架仓同步副本，**本仓不改本体**；需要改规范时先在骨架仓定稿，再整份同步回本仓。

## 工程命名与拓扑

* 后端包名 `com.bgssai.shortdrama`；聚合父 `com.bgssai.shortdrama:bgssai-short-drama-parent`。
* 管理端 `bgssai-short-drama-admin`（后端 8080 / 前端 3001，`role_code=PLATFORM_ADMIN`，登录 `POST /bgssai/admin/login`）——运营与内容管理控制面。
* 用户端 `bgssai-short-drama-user`（后端 8081 / 前端 3002，`role_code=USER`，登录 `POST /bgssai/user/login`）——创作者制作短剧的业务面。
* 两后端共用同一 MySQL 库 `bgssai_short_drama`；当前仅有 `sys_user`、`login_log` 两张基础表，业务表随需求逐个落地。
* 新增 / 变更表结构后，用 MyBatis Generator 按新库表重新生成 `model/**`、`mapper/**` 与 Mapper XML（Standards §2「Generator 同步更新契约」），**禁止**手改这三件套。
