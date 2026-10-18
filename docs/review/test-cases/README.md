# bgssai-short 功能测试用例索引

| 项 | 值 |
| --- | --- |
| 仓库 | liuliuzo/bgssai-short |
| 分支 | develop |
| 用户端环境 | `https://dev.user.bgssai-short.com` |
| 管理端环境 | `https://dev.admin.bgssai-short.com` |
| feature 目录文件盘点 | 31（含 meta / OOS） |
| 范围内功能（in-scope） | 29 |
| OUT-OF-SCOPE | 0 |
| 元文档（README/模板/愿景/总览） | 2 |
| 验收标准合计（in-scope） | 229 |
| 生成用例合计（in-scope） | 230 |
| 用例目录 | `TC-<slug>.md` |
| 模板 | [功能测试模板](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/test/TEMPLATE.md) |
| 产品缩写（用例ID） | `SHORT` |
| 编写日期 | 2026-09-24 |

## 功能清单

| 功能 slug | 标题 | 范围 | AC数 | TC数 | 用例文件 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| `README` | docs/feature（功能需求 + 详细设计） | meta | 0 | 0 | — | 元文档，不生成 TC |
| `_TEMPLATE-feature` | 功能名称（feature-id） | meta | 0 | 0 | — | 元文档，不生成 TC |
| `ai-capability-usage` | AI 能力配置与用量成本（ai-capability-usage） | in-scope | 11 | 11 | TC-ai-capability-usage.md |  |
| `ai-model-integration` | TokenHub 多模型接入（ai-model-integration） | in-scope | 10 | 10 | TC-ai-model-integration.md |  |
| `asset-consistency-panel` | 资产一致性面板（asset-consistency-panel） | in-scope | 4 | 4 | TC-asset-consistency-panel.md |  |
| `bot-mobile-downloads` | Bot 全平台安装包识别修复 | in-scope | 3 | 3 | TC-bot-mobile-downloads.md | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `character-bible` | 角色卡与场景卡（character-bible） | in-scope | 7 | 7 | TC-character-bible.md |  |
| `content-compliance` | 合规预检与内容审核（content-compliance） | in-scope | 9 | 9 | TC-content-compliance.md |  |
| `edit-project-export` | 剪辑工程导出（edit-project-export） | in-scope | 12 | 12 | TC-edit-project-export.md |  |
| `film-compose-export` | 成片合成与导出（film-compose-export） | in-scope | 9 | 9 | TC-film-compose-export.md |  |
| `film-publish-media` | 成片发布到 bgssai-tube（film-publish-media） | in-scope | 8 | 8 | TC-film-publish-media.md |  |
| `frame-continuity` | 首尾帧衔接（frame-continuity） | in-scope | 8 | 8 | TC-frame-continuity.md |  |
| `health-check` | 需求：健康检查（health-check） | in-scope | 10 | 10 | TC-health-check.md |  |
| `in-app-support` | 站内在线客服（留言 / 会话） | in-scope | 5 | 5 | TC-in-app-support.md |  |
| `launch-audit-20260907` | 视频生成结果下载修复 | in-scope | 10 | 10 | TC-launch-audit-20260907.md | 原文无编号验收标准；按可验证需求句拆为 AC。 |
| `legal-fan-out` | 官网法律页分发（legal-fan-out） | in-scope | 5 | 5 | TC-legal-fan-out.md |  |
| `mcp-server` | MCP 服务（用户端） | in-scope | 5 | 5 | TC-mcp-server.md |  |
| `mobile-in-bot-shell` | 用户端壳内窄栏布局（mobile-in-bot-shell） | in-scope | 5 | 5 | TC-mobile-in-bot-shell.md |  |
| `previz-text-storyboard` | 文字预演与提示词优化（previz-text-storyboard） | in-scope | 9 | 9 | TC-previz-text-storyboard.md |  |
| `quality-pipeline` | 优秀短剧质量流水线（quality-pipeline） | in-scope | 11 | 11 | TC-quality-pipeline.md |  |
| `script-studio` | 剧本工作台与节奏体检（script-studio） | in-scope | 8 | 8 | TC-script-studio.md |  |
| `short-01-commercial-gate` | SHORT-01 商业门禁（成片生产 → Tube） | in-scope | 3 | 3 | TC-short-01-commercial-gate.md |  |
| `short-work-management` | 作品与剧集管理（short-work-management） | in-scope | 5 | 6 | TC-short-work-management.md |  |
| `shot-generation` | 画面生成与任务中心（shot-generation） | in-scope | 12 | 12 | TC-shot-generation.md |  |
| `source-import` | 原著导入与改编（source-import） | in-scope | 9 | 9 | TC-source-import.md |  |
| `story-map` | 剧情图谱与走位示意（story-map） | in-scope | 11 | 11 | TC-story-map.md |  |
| `storyboard-canvas` | 分镜画布编排（storyboard-canvas） | in-scope | 8 | 8 | TC-storyboard-canvas.md |  |
| `storyboard-workbench` | 分镜工作台（storyboard-workbench） | in-scope | 6 | 6 | TC-storyboard-workbench.md |  |
| `subscription-credit` | 订阅套餐与加量 credit（subscription-credit） | in-scope | 8 | 8 | TC-subscription-credit.md |  |
| `ux-three-questions` | 三个问题体验主线（ux-three-questions） | in-scope | 9 | 9 | TC-ux-three-questions.md |  |
| `voice-subtitle` | 配音与字幕（voice-subtitle） | in-scope | 9 | 9 | TC-voice-subtitle.md |  |

## 验收标准不够清晰 / 需推导的功能

- `bot-mobile-downloads`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。
- `launch-audit-20260907`：原文无编号「验收标准」或需从正文推导；已按可验证语句拆 AC。

## OUT-OF-SCOPE

- （无）

## 说明

1. 用例由 `docs/feature/*.md` 验收标准 1:1 展开，并补充明显负向/边界。
2. HTML 流程图不单独建用例，归属对应 `.md` 功能。
3. 文件中不含口令、JWT、PAT、短信密钥等密钥。
4. 真机 / 真实支付 / 真实三方 OAuth 未就绪项在备注标 **PENDING**。
5. 本目录为功能测试用例正式落点：`docs/review/test-cases/`。
