# CLAUDE.md

## 产品线规划（全线统一）

BGSSAI 产品线按下面八条划分职责，各仓实现与文档不得与此冲突。

1. **BGSSAI** 是给一人公司（OPC）创业者的全行业工具集合，让用户能找到 OPC 创业时所有需要的工具。
2. **bgssai-website** 是公司官网，对外介绍公司的产品和服务；`reference` 目录中的 `reference-website` 是原来的官网。官网无登录，不承载产品操作、在线对话或中心账号。
3. **bgssai-chat** 提供类似 ChatGPT、Gemini、Claude home、Grok 的 Web 在线对话 AI。境内仓为 `bgssai-chat-cn`，境外仓为 `bgssai-chat-global`。
4. **中心用户账号在 bgssai-chat**：用和 Google、GitHub 一样的**第三方登录**接入各 App，不是单点登录。Chat 也可以授权登录不是 BGSSAI 的应用。各 App 也可以有自己的用户账号体系。境内应用走 `bgssai-chat-cn`，境外应用走 `bgssai-chat-global`。
5. **bgssai-bot** 对标 Grok Bot；BGSSAI 的全部产品应用可以托管给 Bot 直接操作。参考 grok-bot、openclaw、hermes-agent、grok-build、mycontext、deepseek-harness。
6. **bgssai-tokenhub** 是模型中枢，对接主流模型原生 API，再提供给旗下产品。`bgssai-tokenhub-global` 对接国际主流模型，`bgssai-tokenhub-cn` 对接中国大陆模型。两仓都有用户端，对标腾讯云 TokenHub（`reference/tokenhub-prototypes`）；用户端走登录规范，也可用 Chat 登录。旗下产品调模型走中枢凭证。
7. **面向 OPC 的工具集合基本是 B2C**。
8. **境外应用**只有：`bgssai-geo-global`、`bgssai-saas-global`、`bgssai-tokenhub-global`、`bgssai-chat-global`。其余有用户端的应用按境内处理。

`bgssai-docmost` 与 `bgssai-build` 是研发解决方案，不是第 1 条工具，也不上官网。用户从 Docmost 站点下载 Build。Build 写代码时把软件工程文档（需求说明、概要设计、详细设计、类图、流程图、时序图、泳道图、状态图等）写入 Docmost。Docmost 要能承载 HTML：Build 可生成 HTML 格式设计并发布到 Docmost，版式与交互对照 `reference/docmost/htmldemo`。用户可自己编辑，也可和 Agent 对话修改，改完发送给 Build 继续编程。Docmost 参考开源 docmost；Build 参考 grok-build、Cursor、DeepSeek-Coder；Bot 参考 grok-bot、openclaw、hermes-agent、grok-build、mycontext、deepseek-harness。Tokenhub 管理端大模型 API Key 管理对照 dify。bgssai-short 对照 waoowaoo、Jellyfish、ArcReel、LocalMiniDrama、openframe、ZJT。所有参考项目在 `reference` 目录。

有用户端的应用必须支持账号密码、邮箱验证码、手机验证码登录。中国大陆境内应用另需微信、抖音、百度、支付宝登录。中国大陆境外应用另需 Google 账户与 GitHub 登录。官网无登录。管理端不开放注册。除官网用户端外，所有应用平台未登录时默认进入登录页。

愿景唯一权威：`bgssai-skeleton/docs/PRODUCT-LINE-VISION.md`。本段是各仓副本，变更以该文件为准。


**本仓位置**：本仓是第 1 条工具集合中的短剧制作产品。可自有账号，也可接受 bgssai-chat-cn 中心账号授权登录（第三方登录，不是单点登录）；应可被 bgssai-bot 托管操作。设计与交互对照 `reference/short/`：waoowaoo、Jellyfish、ArcReel、LocalMiniDrama、openframe、ZJT。

> 本文件与 `AGENTS.md` 内容保持一致（供不同 AI 工具各自读取），改一处必须同步改另一处。

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
| 可运行的双端工程 | `bgssai-short-admin/**`、`bgssai-short-user/**` | 管理端（8080 / 3001）与用户端（8080 / 3002），Java 21 + Spring Boot 4 + MyBatis + React 18 + Vite + Ant Design 5，共用同一 MySQL 库 |
| 基础链路（自骨架继承） | `common/**`、`configuration/**`、`utils/**` | 统一响应与分页、JWT 登录鉴权与 `@NeedAop` 切面、角色守卫、全局异常与错误码、全链路 traceId、Bucket4j 限流、CORS、SPA 回退。逐项清单见 `README.md`「内置的基础链路」 |
| 健康检查 | `health/**` + `configuration/AsyncConfig` + 对应四层产出 | 全产品线唯一口径（Standards §13）：`/bgssai/health/liveness` · `/readiness` · `/bgssai/health` 三端点、组件化探针、单检查硬超时、整批 TTL 缓存。只对齐、不发明 |
| 本产品 schema 权威 | `sql/DDL.sql`、`sql/DML.sql` | 收敛到仓库根、目录扁平，不用 `migration/` 与 `V*__xxx.sql`；当前仅 `sys_user`、`login_log` 两张基础表 |
| 交付与部署范式 | 前端 `npm run build:deploy` → 后端 `static/`、`deploy/**`、`Jenkinsfile` | 单体交付（前端产物打进后端 jar）；部署通道只有 Jenkins |
| 成本红线闸门 | `.claude/hooks/block-scheduled-automation.py` | 拦截会自唤起、持续消耗 token 的循环任务（Standards §12） |

## 职责边界（本仓不做什么）

* 不维护规范本体：编码与研发规范、目录拓扑的唯一权威在 `bgssai-skeleton`。本仓 `docs/BGSSAI-Standards.md`
  是骨架仓的**同步副本**，发现规范缺口应回骨架仓改本体再同步，**严禁**只在本仓改出第二套口径。
* 不维护 Jenkins 共享库与 Job 清单（`bgssai-workflows`）、不执行建库 / 清库（`bgssai-database`）、
  不采集服务器日志（`bgssai-logs`）。本仓只保留 20 余行的 `Jenkinsfile` / `jenkins/Jenkinsfile.stop` 作为单产品部署 / 关停壳子。
* 不承载其它产品的业务：本仓只放 AI 短剧制作平台自身的需求与实现。

## 产品线仓库拓扑（4 个非应用仓 + 产品仓）

| 仓库 | 类型 | 为了达到什么目的 | 核心功能一句话 |
| --- | --- | --- | --- |
| [bgssai-skeleton](https://github.com/liuliuzo/bgssai-skeleton) | 非应用（骨架与规则） | 让所有产品的代码结构、项目内容与编码规范有唯一维护地 | 可运行的「管理端 + 用户端」骨架工程 + 全局开发规范 |
| [bgssai-database](https://github.com/liuliuzo/bgssai-database) | 非应用（数据库） | 让全部应用的数据库脚本集中在一个仓库维护与执行 | DDL / DML 批次归档 + 建库 / 清库入口 |
| [bgssai-workflows](https://github.com/liuliuzo/bgssai-workflows) | 非应用（CI/CD 与运维） | 统一维护管理 Jenkins 部署通道，承载部署与运维任务 | Jenkins 共享库 + Job 清单唯一权威 + 中央全量启停 |
| [bgssai-logs](https://github.com/liuliuzo/bgssai-logs) | 非应用（日志） | 统一收集全部应用服务器上的日志 | 日志采集 / 清理 / 状态检查 + 批次归档 |
| **[bgssai-short](https://github.com/liuliuzo/bgssai-short)（本仓）** | 应用 | 承载 AI 短剧制作平台业务 | 业务代码 + 本产品 `sql/` 权威 + 单产品 deploy / stop |
| 其余产品仓（blog / builder / geo-cn / geo-global / publish / saas / vpn / voiceunion / marklens） | 应用 | 承载各产品自身业务 | 业务代码 + 本产品 `sql/` 权威 + 单产品 deploy / stop |

非应用仓之间**不重叠、不互相代管**：数据库脚本只在 database 仓执行，部署编排只在 workflows 仓维护，
日志与主机清单只在 logs 仓维护，规范与骨架只在 skeleton 仓维护。需要对方的能力时引用对方仓库的权威
文件，**不复制第二份实现**。

## 新功能必经链路

PRD（`docs/feature`）→ 原型（`docs/demo-static`）→ 架构与契约（`docs/architecture` / `docs/api` / `docs/security`）→ 代码（`src/**`、前端工程）与数据库（`sql/**`）。单向依赖，需求是版本基准与单一事实来源，一切下游产出对齐需求当前版本。

**功能研发必经顺序（强制规定，非功能需求除外）**：项目功能（功能需求）必须严格按 **需求文档（`docs/feature`）→ 原型设计 HTML（`docs/demo-static`）→ 详细设计文档（`docs/architecture` / `docs/api` / `docs/security`）→ 代码编写（`src/**`、前端工程与 `sql/**`）** 的先后顺序推进：上游产物未定稿并落盘前不得启动下游，顺序不可颠倒、不可跳跃。**非功能需求除外**——不改变对外功能契约（字段 / 枚举 / 状态 / 接口语义 / 页面跳转 / 业务规则）的变更（如性能调优、内部重构、依赖升级、配置 / 构建 / CI、日志与可观测性、技术债清理、文档订正等），不强制走原型设计 HTML 与详细设计文档两步。本条与「四层同步完成、缺一不可」互为表里——前者管**顺序**（上游先行），后者管**完整**（同一次变更四层齐备），详见 Standards §0「功能研发必经顺序：需求 → 原型 → 详设 → 代码」。

## 原型参照

原型（`docs/demo-static/**`）是接口 JSON 字段命名、响应结构、枚举 / 状态值与页面跳转关系的**权威参照**，前后端实现逐字对齐。原型按端分目录（**仅原型按端拆分**，其余文档目录统一维护）：管理端 `docs/demo-static/web/admin/`、用户端 `docs/demo-static/web/user/`，移动端 `docs/demo-static/ios/`、`docs/demo-static/android/`（仅用户端）；各端概览入口为该目录 `index.html`、跳转真值为该目录 `PAGE-FLOW.html`，web 端分流入口为 `docs/demo-static/web/index.html`。目录权威见 `docs/DIRECTORY-LAYOUT.md`。

## 仓库强制约定

* 全局开发规范（最高优先级约束，自动导入每次会话）：@docs/BGSSAI-Standards.md
* **配置分层（强制，违者代码不予合并）**：① 中间件参数（数据库、Redis、ES、OBS、MQTT、端口等）只写 `application-local.properties` / `application-dev.properties` / `application-test.properties` / `application-prod.properties`，禁止进 Admin / 库表；② 业务第三方凭证（短信、邮件、支付、OAuth、LLM API Key 等）由 Admin 维护并落库，禁止只写 properties，且必须在 `sql/DML.sql`（及 `sql/<env>/DML.sql`）维护 DML 种子（`ON DUPLICATE KEY UPDATE` 仅空值回填）。权威全文见 Standards §1「配置分层：中间件 vs 业务第三方凭证」。AI 助手不得反复提议违反本条，除非用户主动改决策。
* **持久层三件套手工维护（强制）**：Mapper XML / Example / Entity 是仓库内普通源码，由开发者手工维护。本产品线已下线 MyBatis Generator，不引入任何代码生成工具。改表先改 `sql/DDL.sql`，再手工同步三件套，一次提交。禁止只改 Java 不改 DDL，或只改 DDL 不同步三件套。权威见 Standards §2。
* 任何需求变更或 bug 修复，必须在同一次提交内同步完成**需求文档 / 原型 / 设计文档 / 代码**四层的对应修改，缺一不可，以防止代码与文档漂移（详见 Standards §0「需求与缺陷变更四层同步完成、缺一不可」）。
* 接口的 POST 业务参数一律走 JSON Body（POST→body，详见 Standards §3）。
* 目录权威：`docs/DIRECTORY-LAYOUT.md`。
* 提交信息遵循 Conventional Commits。
* **部署通道（强制）**：应用部署、建库/清库、运维派发只走 Jenkins（本仓 `Jenkinsfile` / `jenkins/Jenkinsfile.stop`，仅手动触发；见 `bgssai-workflows/jenkins/README.md`）。不得用 GitHub Actions 做部署或建库；本仓不保留部署类 `.github/workflows`。部署失败不得自动重部署。
* 一切产出物（代码 / 文档 / 原型 / 提交信息 / PR 描述 / 评审与回信）**全局禁用 emoji 及装饰性图形符号**；语义性符号（→、×、§ 等用于流程示意、表格与条款引用者）不属装饰性符号，允许使用。
* **AI 成本红线（Standards §12）**：不得创建 PR 后会持续唤醒 AI 的 heartbeat / automation / 后台轮询；PR 状态仅在当前会话单次查询或用户下次交互时再查。
* `docs/BGSSAI-Standards.md` 与 `docs/DIRECTORY-LAYOUT.md` 为骨架仓同步副本，**本仓不改本体**；需要改规范时先在骨架仓定稿，再整份同步回本仓。

## 工程命名与拓扑

* 后端包名 `com.bgssai.shortai`；聚合父 `com.bgssai.shortai:bgssai-short-parent`。
* 管理端 `bgssai-short-admin`（远程 HTTPS 443 + HTTP 80 跳转 / 本地前端 3001，`role_code=PLATFORM_ADMIN`，登录 `POST /bgssai/admin/login`）——运营与内容管理控制面。dev 管理端与 Jenkins 共机，避开其 8080。
* 用户端 `bgssai-short-user`（远程 HTTPS 443 + HTTP 80 跳转 / 本地前端 3002，`role_code=USER`，登录 `POST /bgssai/user/login`）——创作者制作短剧的业务面。
* 两后端共用同一 MySQL 库 `bgssai_short`；当前仅有 `sys_user`、`login_log` 两张基础表，业务表随需求逐个落地。
* 新增 / 变更表结构后，先改 `sql/DDL.sql`，再按 Standards §2 **手工**同步 Entity / Example / Mapper XML，一次提交。已下线 MyBatis Generator，禁止用代码生成工具生成或覆盖三件套。

## 登录与账号（产品线强制，禁止反复纠结）

* **用户端登录**：账号密码、手机验证码（登录即注册）、邮箱验证码登录。本仓为境内产品：授权登录必须支持微信、抖音、百度、支付宝，不引入 Google / GitHub。blog SPA 无忘记密码页，不主动补「忘记密码」。完整口径见产品线愿景「用户端登录」。不得另起一套登录语义，除非用户主动改决策。
* **未登录默认页**：除官网用户端外，应用平台（用户端与管理端）未登录时默认进入登录页。公开例外仅限登录、注册、找回/重置密码、OAuth 回调，以及产品明确标注的只读分享页。
* **管理端不开放注册**：管理端禁止自助注册 / 开放注册入口与注册接口；管理员账号只通过 `sql/DML.sql`（及 `sql/<env>/DML.sql`）种子写入。不得实现、不得提议管理端注册页或注册 API，除非用户主动改决策。
* **单会话（后登踢先登）**：登录签发 JWT 携带 `sid`，账户表写入 `current_session_id`；新登录使旧会话失效。被踢请求返回 HTTP 401、body `code=2003`、文案「账号已在其他设备登录」。用户端与管理端均适用。

## Git 分支（Git Flow，强制）

本产品线严格遵循 Git Flow。GitHub 默认分支一律是 **`develop`**。

| 分支 | 用途 | 发布环境 |
| --- | --- | --- |
| `feature/*` | Agent / 开发者的工作分支 | 不直接发布 |
| `develop` | 集成分支（默认分支） | 开发环境 |
| `release` | 测试冻结 | 测试环境 |
| `master` | 生产冻结 | 生产环境 |

- **AI Agent 必须先创建自己的 feature 分支再改文件**；禁止直接在 `develop` / `release` / `master` / `main` 上改。
- **多 Agent 并行：各自开独立工作目录（git worktree）**。grok-build、Claude Code、codex、cursor、gemini 会同时在同一个工作区（`Desktop/github`）上干活，而主工作区的工作树与 HEAD 是共用的——别人 `checkout` 一次就把你的 HEAD 带走，你的提交会落到别人的分支上。**接到任务先在仓库目录之外建一个独立 worktree**（形如 `github/.<工具名>-worktrees/<任务名>/<仓名>`），在里面开自己的 `feature/*` 分支，各管各的；**合并成功后把这个工作文件夹删掉**（`git worktree remove`）。实在要在主工作区改，每次写文件前先 `git branch --show-current` 确认自己还在自己的分支上。
- **别人的活不要碰**。grok-build、Claude Code、codex、cursor、gemini 各自的分支、worktree 和未合并的改动，不是自己的就不要去管——不要替别人提交、合并、改分支或删目录，**除非用户明确下达命令**。看到别人留下的半成品，报告即可，不要顺手处理。
- Feature 合入 **`develop`**（先开 PR）。`develop` → `release`、`release` → `master` 的晋升同样先开 PR。
- 开发环境发布 **`develop`**；测试环境发布 **`release`**；生产环境发布 **`master`**。
- 用户明确同意合并或直接要求合并时，可以执行指定 PR 的合并，无需再次询问。
- 用户未明确同意且未提出合并要求时，不得合并、开启 auto-merge，或直接推送到 `develop`、`release`、`master`、`main`、`Master` 等受保护分支。
- 合并前必须确认仓库、源分支、目标分支和待合并 commit；授权仅限用户指定的 PR 或分支，不得扩展到其他 PR 或分支。
- 本地 commit、远端工作分支 push、GitHub PR 创建和分支合并是四个不同状态，不得混淆或省略。
- 创建或更新 PR 后，对用户只说整体结果（已开 PR 等确认，或已合入 develop），不要列出 SHA、文件清单或检查详情。
- PR 状态查询遵守 **AI 成本红线**：不创建 PR 后持续唤醒的后台监控；在用户下一次交互开始时查询 PR 最新状态即可。

## 端口约定（强制）

所有 web 项目的 **dev / test / prod 三个环境，服务端口一律 8080**，不按环境换端口。

换端口的代价不在改配置本身：部署脚本、反向代理、健康检查、前端开发代理各要记一套，
而这几处记错都不会在开发时暴露，要等上线才发现。统一成一个值，这类错就不存在。

本机要同时跑两个服务而必须错开时，用启动参数临时改
（`--server.port=8081`），不要写进 `application-<profile>.properties`。

## 向用户汇报（强制）

用户不看、也看不懂修改细节。对用户**只汇报整体进度与结果**（做到哪一步、是否完成、要不要拍板）。执行过程中也不要在对话里复述改了什么，不要逐步播报文件或 diff。禁止输出文件清单、diff、命令日志、commit SHA、逐仓 PR 表、工具过程。需要时最多给一个链接。

