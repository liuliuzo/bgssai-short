# bgssai-short：2026-10-19 功能目录与现行范围

版本：2026-10-19（Asia/Shanghai）。权威愿景：用户 2026-10-17 规划。现行范围对照：[2026-10-19 OPC 静态差距审计](opc-gap-audit-20261019.md)。

本文件只增加 Short 的功能目录验收项。[2026-10-17 需求](opc-requirements-20261017.md) 的需求表与用例结果保持原样。本次没有实现产品功能，也没有执行用例。

## 产品职责

境内短剧制作平台，模型固定 Tokenhub CN。成片发布交给 Tube 播放，本仓不另建播放平台。

## 功能与验收条件

| 需求ID | 能力 | 功能要求 | 通过/失败判定 | 用例 |
| --- | --- | --- | --- | --- |
| REQ-SHORT-PRODUCT-101 | 成片到 Tube 的未知结果恢复 | 导出成片并交给 Tube 后，若发布结果未知，恢复时不重复创建作品。来源见差距审计 AUD-SHORT-20261019-01，对照 [成片发布](film-publish-media.md) 与既有 REQ-SHORT-PRODUCT-004、REQ-SHORT-PRODUCT-005。 | 正向判定：Short 任务与 Tube 作品号一一对应，未知状态可见。异常判定：重试后 Tube 出现两条相同作品，不能通过。 | [TC-SHORT-PRODUCT-101-P / TC-SHORT-PRODUCT-101-N](../test/TC-opc-product-20261019.md) |
| REQ-SHORT-PRODUCT-102 | 一致性资产与成片基准 | 角色和分镜时间轴与导出成片使用同一版本；成片画面能对上该时间轴。剪辑工程上，已锁定角色卡显示「角色版本」，已锁定时间轴卡显示「时间轴版本」（镜号、时长、字幕）。已作废角色版本不得标成与现行时间轴一致。来源见差距审计 AUD-SHORT-20261019-02，对照 [资产一致性](asset-consistency-panel.md)、[分镜工作台](storyboard-workbench.md)、[剪辑工程导出](edit-project-export.md) 与既有 REQ-SHORT-PRODUCT-006、REQ-SHORT-PRODUCT-007、REQ-SHORT-PRODUCT-008。 | 正向判定：抽检镜头的角色、字幕和时长与时间轴一致，且剪辑工程能读到「角色版本」「时间轴版本」。异常判定：成片使用了已作废的角色版本却标一致，不能通过。 | [TC-SHORT-PRODUCT-102-P / TC-SHORT-PRODUCT-102-N](../test/TC-opc-product-20261019.md) |

## 规则边界与依赖

- 2026-10-17 的 REQ-SHORT-PRODUCT-001～008 与共同契约、平台、回归项继续有效，编号不顺延改写。
- develop 上 PRODUCT-101、PRODUCT-102 未被占用，本轮从 101 起编号。
- 短剧继续只走 Tokenhub CN，不因发布或成片对照改走境外中枢。
- 新增用例统一待测。文件存在和结构校验不等于产品验收通过。

## 追溯

[覆盖索引](../test/REQUIREMENT-COVERAGE.md) 的 2026-10-19 节列出差距审计与参考需求；[本轮矩阵](../test/TRACEABILITY-20261019.md) 列出本文件全部验收项与正反用例。2026-10-17 矩阵仍见 [TRACEABILITY-20261017.md](../test/TRACEABILITY-20261017.md)。
