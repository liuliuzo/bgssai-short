# liuliuzo/bgssai-short 存量需求覆盖索引

基线：`aa74224179ae7b43fcafa99565b9271e37fdabc6`；日期：2026-10-17。列出38个存量需求/索引文件。功能清单中的有效条款按专项用例保留；索引、模板、旧方案与审计材料标注用途，不当作独立产品能力。

历史套件匹配依据为需求路径/文件名或同名套件；这是按文件的测试设计导航，不等于每个旧条款已经运行通过。执行前须同时核对源文中的字段与所有参数组合。本轮逐条验收见[追溯矩阵](TRACEABILITY-20261017.md)。

| 来源 | 关联方式/范围 | 历史用例（结果未改写） | 本轮用例 |
| --- | --- | --- | --- |
| [功能名称（feature-id）](../../docs/feature/_TEMPLATE-feature.md) | 索引或模板，不作为独立业务功能 | — | — |
| [AI 能力配置与用量成本（ai-capability-usage）](../../docs/feature/ai-capability-usage.md) | 历史套件保留 | [TC-ai-capability-usage.md](../../docs/review/test-cases/TC-ai-capability-usage.md) | — |
| [TokenHub 多模型接入（ai-model-integration）](../../docs/feature/ai-model-integration.md) | 历史套件保留 | [TC-ai-model-integration.md](../../docs/review/test-cases/TC-ai-model-integration.md) | — |
| [资产一致性面板（asset-consistency-panel）](../../docs/feature/asset-consistency-panel.md) | 历史套件保留 | [TC-asset-consistency-panel.md](../../docs/review/test-cases/TC-asset-consistency-panel.md) | — |
| [Bot 下载入口体验](../../docs/feature/bot-download-entry.md) | 专项或共同契约用例 | — | [REQ-SHORT-COMMON-014](TC-opc-common-20261017.md)：TC-SHORT-COMMON-014-P, TC-SHORT-COMMON-014-N |
| [Bot 全平台安装包识别修复](../../docs/feature/bot-mobile-downloads.md) | 专项或共同契约用例 | [TC-bot-mobile-downloads.md](../../docs/review/test-cases/TC-bot-mobile-downloads.md) | [REQ-SHORT-COMMON-014](TC-opc-common-20261017.md)：TC-SHORT-COMMON-014-P, TC-SHORT-COMMON-014-N |
| [角色卡与场景卡（character-bible）](../../docs/feature/character-bible.md) | 历史套件保留 | [TC-character-bible.md](../../docs/review/test-cases/TC-character-bible.md) | — |
| [合规预检与内容审核（content-compliance）](../../docs/feature/content-compliance.md) | 历史套件保留 | [TC-content-compliance.md](../../docs/review/test-cases/TC-content-compliance.md) | — |
| [剪辑工程导出（edit-project-export）](../../docs/feature/edit-project-export.md) | 历史套件保留 | [TC-edit-project-export.md](../../docs/review/test-cases/TC-edit-project-export.md) | — |
| [成片合成与导出（film-compose-export）](../../docs/feature/film-compose-export.md) | 历史套件保留 | [TC-film-compose-export.md](../../docs/review/test-cases/TC-film-compose-export.md) | — |
| [成片发布到 bgssai-media（film-publish-media）](../../docs/feature/film-publish-media.md) | 历史套件保留 | [TC-film-publish-media.md](../../docs/review/test-cases/TC-film-publish-media.md) | — |
| [首尾帧衔接（frame-continuity）](../../docs/feature/frame-continuity.md) | 历史套件保留 | [TC-frame-continuity.md](../../docs/review/test-cases/TC-frame-continuity.md) | — |
| [需求：健康检查（health-check）](../../docs/feature/health-check.md) | 历史套件保留 | [TC-health-check.md](../../docs/review/test-cases/TC-health-check.md) | — |
| [站内在线客服（留言 / 会话）](../../docs/feature/in-app-support.md) | 专项或共同契约用例 | [TC-in-app-support.md](../../docs/review/test-cases/TC-in-app-support.md) | [REQ-SHORT-REGRESSION-006](TC-feature-gaps-20261017.md)：TC-SHORT-REGRESSION-006-P, TC-SHORT-REGRESSION-006-N |
| [视频生成结果下载修复](../../docs/feature/launch-audit-20260907.md) | 历史套件保留 | [TC-launch-audit-20260907.md](../../docs/review/test-cases/TC-launch-audit-20260907.md) | — |
| [协议入口去重（2026-09-29）](../../docs/feature/legal-entry-dedup.md) | 专项或共同契约用例 | — | [REQ-SHORT-REGRESSION-003](TC-feature-gaps-20261017.md)：TC-SHORT-REGRESSION-003-P, TC-SHORT-REGRESSION-003-N |
| [官网法律页分发（legal-fan-out）](../../docs/feature/legal-fan-out.md) | 历史套件保留 | [TC-legal-fan-out.md](../../docs/review/test-cases/TC-legal-fan-out.md) | — |
| [用户登录页整理（2026-09-26）](../../docs/feature/login-page-cleanup-20260926.md) | 专项或共同契约用例 | — | [REQ-SHORT-REGRESSION-002](TC-feature-gaps-20261017.md)：TC-SHORT-REGRESSION-002-P, TC-SHORT-REGRESSION-002-N |
| [MCP 服务（用户端）](../../docs/feature/mcp-server.md) | 历史套件保留 | [TC-mcp-server.md](../../docs/review/test-cases/TC-mcp-server.md) | — |
| [用户端壳内窄栏布局（mobile-in-bot-shell）](../../docs/feature/mobile-in-bot-shell.md) | 历史套件保留 | [TC-mobile-in-bot-shell.md](../../docs/review/test-cases/TC-mobile-in-bot-shell.md) | — |
| [模型定价与真实用量对账（2026-10-15）](../../docs/feature/model-pricing-receipts.md) | 专项或共同契约用例 | — | [REQ-SHORT-REGRESSION-005](TC-feature-gaps-20261017.md)：TC-SHORT-REGRESSION-005-P, TC-SHORT-REGRESSION-005-N |
| [验证码登录即注册](../../docs/feature/otp-entry-20260924.md) | 专项或共同契约用例 | — | [REQ-SHORT-REGRESSION-002](TC-feature-gaps-20261017.md)：TC-SHORT-REGRESSION-002-P, TC-SHORT-REGRESSION-002-N |
| [文字预演与提示词优化（previz-text-storyboard）](../../docs/feature/previz-text-storyboard.md) | 历史套件保留 | [TC-previz-text-storyboard.md](../../docs/review/test-cases/TC-previz-text-storyboard.md) | — |
| [用户端动态视觉（2026-09-25）](../../docs/feature/product-motion.md) | 专项或共同契约用例 | — | [REQ-SHORT-REGRESSION-004](TC-feature-gaps-20261017.md)：TC-SHORT-REGRESSION-004-P, TC-SHORT-REGRESSION-004-N |
| [优秀短剧质量流水线（quality-pipeline）](../../docs/feature/quality-pipeline.md) | 历史套件保留 | [TC-quality-pipeline.md](../../docs/review/test-cases/TC-quality-pipeline.md) | — |
| [docs/feature（功能需求 + 详细设计）](../../docs/feature/README.md) | 索引或模板，不作为独立业务功能 | — | — |
| [剧本工作台与节奏体检（script-studio）](../../docs/feature/script-studio.md) | 历史套件保留 | [TC-script-studio.md](../../docs/review/test-cases/TC-script-studio.md) | — |
| [SHORT-01 商业门禁（成片生产 → Media）](../../docs/feature/short-01-commercial-gate.md) | 历史套件保留 | [TC-short-01-commercial-gate.md](../../docs/review/test-cases/TC-short-01-commercial-gate.md) | — |
| [作品与剧集管理（short-work-management）](../../docs/feature/short-work-management.md) | 历史套件保留 | [TC-short-work-management.md](../../docs/review/test-cases/TC-short-work-management.md) | — |
| [画面生成与任务中心（shot-generation）](../../docs/feature/shot-generation.md) | 历史套件保留 | [TC-shot-generation.md](../../docs/review/test-cases/TC-shot-generation.md) | — |
| [补齐单会话登录](../../docs/feature/single-session-completion.md) | 专项或共同契约用例 | — | [REQ-SHORT-REGRESSION-001](TC-feature-gaps-20261017.md)：TC-SHORT-REGRESSION-001-P, TC-SHORT-REGRESSION-001-N |
| [原著导入与改编（source-import）](../../docs/feature/source-import.md) | 历史套件保留 | [TC-source-import.md](../../docs/review/test-cases/TC-source-import.md) | — |
| [剧情图谱与走位示意（story-map）](../../docs/feature/story-map.md) | 历史套件保留 | [TC-story-map.md](../../docs/review/test-cases/TC-story-map.md) | — |
| [分镜画布编排（storyboard-canvas）](../../docs/feature/storyboard-canvas.md) | 历史套件保留 | [TC-storyboard-canvas.md](../../docs/review/test-cases/TC-storyboard-canvas.md) | — |
| [分镜工作台（storyboard-workbench）](../../docs/feature/storyboard-workbench.md) | 历史套件保留 | [TC-storyboard-workbench.md](../../docs/review/test-cases/TC-storyboard-workbench.md) | — |
| [订阅套餐与加量 credit（subscription-credit）](../../docs/feature/subscription-credit.md) | 历史套件保留 | [TC-subscription-credit.md](../../docs/review/test-cases/TC-subscription-credit.md) | — |
| [三个问题体验主线（ux-three-questions）](../../docs/feature/ux-three-questions.md) | 历史套件保留 | [TC-ux-three-questions.md](../../docs/review/test-cases/TC-ux-three-questions.md) | — |
| [配音与字幕（voice-subtitle）](../../docs/feature/voice-subtitle.md) | 历史套件保留 | [TC-voice-subtitle.md](../../docs/review/test-cases/TC-voice-subtitle.md) | — |
