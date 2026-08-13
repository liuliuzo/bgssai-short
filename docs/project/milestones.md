# 里程碑与进度

本文件记录 AI 短剧制作平台的里程碑与当前进度。需求与设计正文不在此处，见 `../feature/` 与
`../architecture/`；本文件只记「做到哪一步」。

## M0 仓库初始化（已完成）

按骨架仓 `bgssai-skeleton` 初始化本产品仓，产出可运行基线。

| 事项 | 状态 | 落点 |
| --- | --- | --- |
| 双端工程结构（管理端 8080 / 3001、用户端 8081 / 3002） | 已完成 | `bgssai-short-drama-admin/**`、`bgssai-short-drama-user/**` |
| 后端包名与聚合工程改为本产品命名（`com.bgssai.shortdrama`） | 已完成 | `pom.xml`、各模块 `pom.xml`、`src/main/java/**` |
| 数据库改为本产品库 `bgssai_short_drama` | 已完成 | `sql/DDL.sql`、`sql/DML.sql`、各 `application-*.properties` |
| 基础链路（鉴权 / 统一响应 / 分页 / traceId / 限流 / CORS / SPA 回退） | 已完成 | `common/**`、`configuration/**`、`utils/**` |
| 健康检查三端点（Standards §13 口径） | 已完成 | `health/**` + `docs/feature/health-check.md` 等四层产出 |
| 文档与原型骨架（`docs/` 7 类目录 + 各端 `index.html` / `PAGE-FLOW.html`） | 已完成 | `docs/**` |
| 产品愿景与定位 | 已完成 | `../../PRODUCT_VISION.md` |
| 编译与单元测试校验（`mvn -DskipTests compile`、`mvn test`） | 已完成 | 两模块编译通过，单元测试全绿 |

M0 结束时仓库仅有骨架继承的登录鉴权、登录日志分页示例与健康检查，无任何短剧业务功能。

## M1 产品需求与原型定稿（进行中）

按「需求 → 原型 → 详设 → 代码」顺序推进，上游未定稿不得启动下游（Standards §0）。
本里程碑覆盖前两步。

| 事项 | 状态 | 落点 |
| --- | --- | --- |
| 目标人群优先级与关键场景定稿 | 已完成 | `../../PRODUCT_VISION.md` 第二、三节 |
| 制作全链路拆解为功能需求（13 份） | 已完成 | `../feature/*.md`（作品 / 剧本 / 剧情图谱 / 三问体验 / 角色卡 / 分镜 / 文字预演 / 生成 / 配音 / 合成 / 质量 / 合规 / 用量） |
| 各功能详细设计配图（12 张自包含 HTML + 内联 SVG） | 已完成 | `../feature/flow-*.html`、`../feature/state-*.html` |
| 统一枚举字典（单一登记处） | 已完成 | `../feature/drama-work-management.md` 第八节 |
| web 用户端原型（18 页，含骨架继承 3 页；分镜与文字预演已合并为创作台一页）与跳转真值 | 已完成 | `../demo-static/web/user/**`（含 `index.html`、`PAGE-FLOW.html`） |
| web 管理端原型（9 页）与跳转真值 | 已完成 | `../demo-static/web/admin/**`（含 `index.html`、`PAGE-FLOW.html`） |
| 原型界面系统（应用外壳 + 业务组件） | 已完成 | `../demo-static/web/assets/shortdrama.css`（theme.css 为骨架同步副本，不改本体） |
| 页面 → API 映射主表同步 | 已完成 | `../api/page-api-mapping.md`（Standards §9 要求） |
| AI 能力接入方式、素材存储、长耗时任务机制 | 需求层已定口径，待详设定稿 | 能力经 `ai_model_config` 抽象、素材存对象存储只落 `storage_key`、长耗时统一走 `drama_ai_task` 任务体系 |
| 移动端（iOS / Android）原型 | 未开始 | 本轮不含移动端工程，`../demo-static/ios|android/` 仍为占位页 |

## M2 详细设计与契约（未开始）

| 事项 | 状态 | 说明 |
| --- | --- | --- |
| 数据模型与状态机 | 未开始 | 落 `../architecture/`：14 张业务表、四条状态机、任务与计费时序 |
| REST 契约正文 | 未开始 | 落 `../api/*.md`，并把 `page-api-mapping.md` 的「契约」列由需求文档改指契约正文 |
| 安全设计 | 未开始 | 落 `../security/`：AI 供应商密钥保管、素材签名 URL、越权与数据范围、审核留痕 |

## M3 代码与数据库（未开始）

| 事项 | 状态 | 说明 |
| --- | --- | --- |
| `sql/DDL.sql` 业务表 | 未开始 | 详设定稿后一次性落表，随后重跑 MyBatis Generator |
| 双端后端与前端工程 | 未开始 | 按 §0「每次 PR 必须可运行」拆为多个各自可运行的里程碑 PR |

## 已知待办（跨仓）

| 事项 | 归属仓 | 说明 |
| --- | --- | --- |
| 在产品线拓扑中登记本仓 | `bgssai-skeleton` | 骨架仓 `CLAUDE.md` / `AGENTS.md` 拓扑表与「9 个产品仓」计数、Standards §13 及健康检查各文档的仓数 / 后端数尚未含本仓，需在骨架仓侧同步 |
| 数据库脚本归档 | `bgssai-database` | 本产品 schema 权威在本仓 `sql/`，批次归档与建库 / 清库执行在 database 仓 |
| 部署与启停编排 | `bgssai-workflows` | Jenkins / Actions Job 登记 |
| 日志采集接入 | `bgssai-logs` | 主机清单与采集配置 |
