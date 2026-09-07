# 里程碑与进度

本文件记录 AI 短剧制作平台的里程碑与当前进度。需求与设计正文不在此处，见 `../feature/` 与
`../architecture/`；本文件只记「做到哪一步」。

## M0 仓库初始化（已完成）

按骨架仓 `bgssai-skeleton` 初始化本产品仓，产出可运行基线。

| 事项 | 状态 | 落点 |
| --- | --- | --- |
| 双端工程结构（管理端 8080 / 3001、用户端 8080 / 3002） | 已完成 | `bgssai-short-admin/**`、`bgssai-short-user/**` |
| 后端包名与聚合工程改为本产品命名（`com.bgssai.shortai`） | 已完成 | `pom.xml`、各模块 `pom.xml`、`src/main/java/**` |
| 数据库改为本产品库 `bgssai_short` | 已完成 | `sql/DDL.sql`、`sql/DML.sql`、各 `application-*.properties` |
| 基础链路（鉴权 / 统一响应 / 分页 / traceId / 限流 / CORS / SPA 回退） | 已完成 | `common/**`、`configuration/**`、`utils/**` |
| 健康检查三端点（Standards §13 口径） | 已完成 | `health/**` + `docs/feature/health-check.md` 等四层产出 |
| 文档与原型骨架（`docs/` 7 类目录 + 各端 `index.html` / `PAGE-FLOW.html`） | 已完成 | `docs/**` |
| 产品愿景与定位 | 已完成 | `../../PRODUCT_VISION.md` |
| 编译与单元测试校验（`mvn -DskipTests compile`、`mvn test`） | 已完成 | 两模块编译通过，单元测试全绿 |

M0 结束时仓库仅有骨架继承的登录鉴权、登录日志分页示例与健康检查，无任何短剧业务功能。

## M1 产品需求与原型定稿（已完成）

| 事项 | 状态 | 落点 |
| --- | --- | --- |
| 目标人群优先级与关键场景定稿 | 已完成 | `../../PRODUCT_VISION.md` |
| 制作全链路功能需求 | 已完成 | `../feature/*.md` |
| web 用户端 / 管理端原型与跳转真值 | 已完成 | `../demo-static/web/**` |
| 移动端（iOS / Android）原型 | 未开始 | 本轮不含，仍为占位页 |

## M2 详细设计与契约（本轮范围已完成）

| 事项 | 状态 | 说明 |
| --- | --- | --- |
| 制作闭环 + AI/OBS 横切 | 已完成 | `short-production-design.md`、`ai-provider-obs-design.md` |
| AI 模型与用量 | 已完成 | `ai-capability-usage-design.md`、`AI-CAPABILITY-USAGE.md` |
| 配音字幕 / 预演 / 图谱 / 质量 / 三问 | 已完成 | `voice-subtitle-design.md`、`previz-story-quality-ux-design.md` |
| 安全（AI/OBS 密钥） | 已完成 | `ai-provider-secrets.md` |

## M3 代码与数据库（本轮范围已完成）

| 事项 | 状态 | 说明 |
| --- | --- | --- |
| `sql/DDL.sql` + `UPGRADE-20260826-full-features.sql` | 已完成 | 模型/用量/配音/预演/图谱/质量/三问表 |
| 用户端制作全链路 + 用量/充值 | 已完成 | Provider SPI、OBS/本地存储、配音、预演、图谱、质量、样片/连贯性 |
| 管理端审核 / 创作者 / 模型 / 用量 / 质量模板 | 已完成 | 含加量包与支付商户 |
| AI 默认种子 | IMAGE/VIDEO 默认可灵，并启用 MiniMax / Seedance / Veo | 创作者提交时点选 `model_code`；MiniMax / Seedance / Veo 需写入对应 `sys_secret` 后才真正出片；OBS 未配时走本地目录 |

## M4 两端补齐（对标开源短剧平台后的增量）

M0-M3 把「想法到成片」的主干做完了，M4 补的是主干两头——进得来与出得去，
以及镜头之间接不接得上、整集看不看得见。参考项目见 `reference/short`
（ArcReel · Jellyfish · LocalMiniDrama · ZJT · openframe · waoowaoo）。

| 事项 | 状态 | 说明 |
| --- | --- | --- |
| 剪辑工程导出 | 已完成 | 剪映草稿 / FCPXML / EDL 三种工程格式，素材可打包可只出清单；前置刻意宽于合成（只要求至少一镜有视频），缺片镜头列名并在时间轴留空。需求 `feature/edit-project-export.md` |
| 原著导入与改编 | 已完成 | 小说全文 / 成品剧本 / 分集大纲三种源，自动分章可纠正，按章改编出带出处的大纲；权利确认为硬前置，改编稿照走节奏体检。需求 `feature/source-import.md` |
| 首尾帧衔接 | 已完成 | 尾帧由 ffmpeg 抽帧或单独出图；同场次相邻镜头可继承上一镜尾帧，跨场按剪辑语言不接；模型不支持首尾帧时直接拒绝而非静默降级。需求 `feature/frame-continuity.md` |
| 分镜画布编排 | 已完成 | 整集按场次分行一屏看完，框选批量走同一道预算闸门，场内拖拽即改镜号；不存自由坐标、不做跨场拖拽。需求 `feature/storyboard-canvas.md` |

## 已知待办（跨仓 / 运维）

| 事项 | 归属仓 | 说明 |
| --- | --- | --- |
| 在产品线拓扑中登记本仓 | `bgssai-skeleton` | 拓扑表与仓数尚未含本仓 |
| 数据库脚本归档 | `bgssai-database` | 本仓 `sql/` 权威，执行在 database 仓 |
| 部署与启停编排 | `bgssai-workflows` | Jenkins Job 登记 |
| 日志采集接入 | `bgssai-logs` | 主机清单与采集配置 |
| 生产密钥与 OBS 桶 | 运维 | 写入 `sys_secret` 并启用；安装 ffmpeg 以启用真实合成与镜头尾帧抽取 |
| 剪映草稿真机验证 | 待验证 | 剪映草稿是逆向格式、随客户端版本演进，单元测试只能验证生成结构符合设计，**证明不了剪映一定能打开**。上线前须在装有剪映桌面版的机器上，对 5.x 与 6.x 各实测一次：导出、解压进草稿目录、重启剪映、打开草稿，确认音画轨对齐、字幕可编辑、素材不离线。详见 `feature/edit-project-export.md` 第八节 |
