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

**尚未开始**：任何短剧业务功能。仓库现有功能仅为骨架继承的登录鉴权、登录日志分页示例与健康检查。

## M1 产品需求定稿（未开始）

| 事项 | 状态 | 说明 |
| --- | --- | --- |
| 目标人群优先级与角色权限矩阵 | 未开始 | 回填 `../../PRODUCT_VISION.md` 第二节「待确认」项 |
| 核心场景拆解为功能需求 | 未开始 | 逐个落 `../feature/*.md`（照 `_TEMPLATE-feature.md`） |
| AI 能力接入方式、素材存储、长耗时任务机制 | 未开始 | 三项影响数据模型与接口契约，需在需求 / 详设阶段定稿 |

进入 M1 后一律按「需求 → 原型 → 详设 → 代码」顺序推进，上游未定稿不得启动下游（Standards §0）。

## 已知待办（跨仓）

| 事项 | 归属仓 | 说明 |
| --- | --- | --- |
| 在产品线拓扑中登记本仓 | `bgssai-skeleton` | 骨架仓 `CLAUDE.md` / `AGENTS.md` 拓扑表与「9 个产品仓」计数、Standards §13 及健康检查各文档的仓数 / 后端数尚未含本仓，需在骨架仓侧同步 |
| 数据库脚本归档 | `bgssai-database` | 本产品 schema 权威在本仓 `sql/`，批次归档与建库 / 清库执行在 database 仓 |
| 部署与启停编排 | `bgssai-workflows` | Jenkins / Actions Job 登记 |
| 日志采集接入 | `bgssai-logs` | 主机清单与采集配置 |
