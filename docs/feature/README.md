# docs/feature（功能需求 + 详细设计）

本目录承载每个功能的**需求澄清（WHAT）+ 详细设计**。需求是版本基准与单一事实来源，一切下游产出
（原型 / 架构 / 契约 / 代码 / 数据库）对齐到需求当前版本（Standards §0）。

## 约定

- 每个功能一份 Markdown（可拆多份），命名用小写连字符，如 `account-registration.md`。
- 详细设计**必须配图**（流程图 / 时序图 / 状态泳道图），优先用自包含 HTML 绘制，随功能就近存放本目录。
- 新增 / 修改功能时，同步更新原型（`docs/demo-static` 下**涉及的每个端目录**）、架构与契约
  （`docs/architecture` / `docs/api`，含 `docs/api/page-api-mapping.md`）、代码与数据库（`src/**`、`sql/**`），
  四层缺一不可。
- 本目录**不按端拆分**：管理端与用户端共用同一套数据结构与契约，同一功能一份需求文档，端差异在文内分
  小节说明；严禁建 `admin/` · `user/` 子目录或双份副本。
- 新建功能文档可复制 [功能需求模板](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/_TEMPLATE-feature.md)。

## 本产品业务功能（AI 短剧制作全链路）

制作主线按流程顺序排列，质量流水线横跨全流程。全部功能的枚举值统一登记在
[`short-work-management.md`](./short-work-management.md) 第八节（单一登记处），其它文档只引用不复制。

| 功能 | 解决的痛点 | 需求文档 | 配图 | 原型 |
| --- | --- | --- | --- | --- |
| **三个问题体验主线** | 功能按管线组织，但用户关心的是：视频会是什么样 / 剧情连不连贯 / 管起来方不方便 | [`ux-three-questions.md`](./ux-three-questions.md) | [`flow-ux-three-questions.html`](./flow-ux-three-questions.html) | 用户端 `episode-preview` / `continuity-center` + `work-detail` 三问入口 |
| 作品与剧集管理 | 产物散落、进度靠记，没有承载整部剧的容器 | [`short-work-management.md`](./short-work-management.md) | [`flow-production-pipeline.html`](./flow-production-pipeline.html) | 用户端 `works` / `work-create` / `work-detail` |
| 剧本工作台与节奏体检 | AI 写成小说腔，没钩子没反转，节奏对不上时长 | [`script-studio.md`](./script-studio.md) | [`flow-script-generation.html`](./flow-script-generation.html) | 用户端 `script-studio` |
| **剧情图谱与走位示意** | 节奏塌在哪、关系揭早了没、两人有没有站反——用文字读半天，看图一眼就知道 | [`story-map.md`](./story-map.md) | [`flow-story-map.html`](./flow-story-map.html) | 用户端 `story-map`（走位内嵌 `previz` / `storyboard`） |
| 角色卡与场景卡 | 同一角色每镜换脸、场景每镜换装修，只能反复抽卡 | [`character-bible.md`](./character-bible.md) | [`flow-character-consistency.html`](./flow-character-consistency.html) | 用户端 `character-bible` |
| **资产一致性面板** | 资产库和分镜拆开，镜头不能显式点名角色 / 场景 / 道具 | [`asset-consistency-panel.md`](./asset-consistency-panel.md) | [`flow-asset-consistency-panel.html`](./flow-asset-consistency-panel.html) | 用户端 `character-bible` · `storyboard` |
| 分镜工作台 | 不懂镜头语言，一集 40 多镜要手写提示词，耗掉大半天 | [`storyboard-workbench.md`](./storyboard-workbench.md) | [`state-shot-lifecycle.html`](./state-shot-lifecycle.html) | 用户端 `storyboard` |
| **文字预演与提示词优化** | 视频生成一次要等几分钟，不满意再改提示词，时间与钱全耗在等待上 | [`previz-text-storyboard.md`](./previz-text-storyboard.md) | [`flow-previz-prompt.html`](./flow-previz-prompt.html) | 用户端 `previz`（+ `storyboard` 内嵌） |
| 画面生成与任务中心 | 批量任务失控、失败无从续跑、花了多少钱不知道 | [`shot-generation.md`](./shot-generation.md) | [`flow-generation-task.html`](./flow-generation-task.html) · [`state-shot-lifecycle.html`](./state-shot-lifecycle.html) | 用户端 `task-center` |
| 配音与字幕 | 音色跨集不一致、音画对不齐、字幕逐句手打 | [`voice-subtitle.md`](./voice-subtitle.md) | [`flow-post-production.html`](./flow-post-production.html) | 用户端 `voice-studio` |
| 成片合成与导出 | 要学剪辑软件，每集重复体力活，导出规格易出错 | [`film-compose-export.md`](./film-compose-export.md) | [`flow-post-production.html`](./flow-post-production.html) | 用户端 `compose-export` |
| 成片发布到 Tube | 成片就绪后需推到 bgssai-tube 才能播放分发 | [`film-publish-media.md`](./film-publish-media.md) | [`flow-film-publish-media.html`](./flow-film-publish-media.html) | 用户端 `compose-export` |
| 分镜画布编排 | 一集四五十镜只有纵向列表，看不出全貌，批量操作只能一个个勾 | [`storyboard-canvas.md`](./storyboard-canvas.md) | [`flow-storyboard-canvas.html`](./flow-storyboard-canvas.html) | 用户端 `storyboard-canvas` |
| 首尾帧衔接 | 每镜只有首帧，镜头之间各拍各的：人物位置、姿态、光线镜镜突变 | [`frame-continuity.md`](./frame-continuity.md) | [`flow-frame-continuity.html`](./flow-frame-continuity.html) | 用户端 `storyboard` |
| 原著导入与改编 | 只能从一句话梗概起步，手里握着网文 IP、成品剧本、已有大纲的人进不来 | [`source-import.md`](./source-import.md) | [`flow-source-import.html`](./flow-source-import.html) | 用户端 `source-import` |
| 剪辑工程导出 | 只给 mp4 等于最后一公里断在平台边界：想改剪辑点、语速、转场只能把素材逐个下载、在剪映里从零重排一遍时间轴 | [`edit-project-export.md`](./edit-project-export.md) | [`flow-edit-project-export.html`](./flow-edit-project-export.html) | 用户端 `edit-export` |
| **优秀短剧质量流水线（主线）** | 做得出但做得平庸，不知道差在哪、该改哪一句 | [`quality-pipeline.md`](./quality-pipeline.md) | [`flow-quality-pipeline.html`](./flow-quality-pipeline.html) | 用户端 `quality-report` · 管理端 `quality-templates` |
| 合规预检与内容审核 | 花完生成费才发现过不了审 | [`content-compliance.md`](./content-compliance.md) | [`state-review-compliance.html`](./state-review-compliance.html) | 管理端 `content-review` / `review-detail`（用户端内嵌预检） |
| AI 能力配置与用量成本 | 换模型要改代码、单价无处统一维护、用量无法核账 | [`ai-capability-usage.md`](./ai-capability-usage.md) | [`flow-usage-billing.html`](./flow-usage-billing.html) | 管理端 `ai-models` / `usage-cost` / `creators` · 用户端 `usage-billing` |
| TokenHub 多模型接入 | 文本、视频、配音、ASR、音乐模型未形成统一生产目录 | [`ai-model-integration.md`](./ai-model-integration.md) | [`flow-generation-task.html`](./flow-generation-task.html) · [`flow-post-production.html`](./flow-post-production.html) | 管理端 `ai-models` · 用户端 `script-studio` / `storyboard` / `voice-studio` / `compose-export` |
| 订阅套餐与加量 credit | 无支付、无套餐发放、无法加量 | [`subscription-credit.md`](./subscription-credit.md) | 用户端 `subscription` · 管理端 `credit-packs` / `pay-merchants` | 用户端 `subscription` · 管理端 `credit-packs` / `pay-merchants` |

> **当前进度**：本轮功能（作品 / 剧本 / 角色 / 分镜 / 预演 / 图谱 / 生成 / 配音字幕 / 成片 / 质量 /
> 合规 / 用量与模型配置 / 三问体验 / 订阅加量）已完成需求 → 原型 → 详设与契约 → 代码与数据库。
> 首批生产模型统一走 TokenHub：TEXT 为 Hy4 / Kimi K3 / DeepSeek-V4-Pro，VIDEO 为 MiniMax / PixVerse /
> Kling，声音为 Hy-ASR / MiniMax-Speech / MiniMax-Music；密钥进 `sys_secret`，素材走 OBS 或本地回退。

## 仓库内置的基础功能（自骨架继承）

| 功能 | 说明 | 契约 | 原型 |
| --- | --- | --- | --- |
| 登录鉴权 | 账号密码 + 短信/邮箱验证码登录即注册签发 JWT；短信发码回显 `product`/`seq`（默认双参模板）。管理端密码框可点击显示或隐藏，默认掩码，不改登录接口 | `../api/auth-login.md` · [`admin-login-password-reveal.md`](./admin-login-password-reveal.md) | `../demo-static/web/admin/login.html`、`../demo-static/web/user/login.html` |
| 中文 /en 入口 | `/en` 与 `/en/*` 进入已有中文 `/login`，不另做英文站；未知路径仍为「页面不存在」 | [`chinese-only-en-alias.md`](./chinese-only-en-alias.md) | 两端 `PAGE-FLOW.html` |
| 官网法律页外链 | 用户端登录 + 页脚指向 `www.bgssai.com` 五页中英路径；管理端仅外链；不编造证照号；需法务审阅 | [`legal-fan-out.md`](./legal-fan-out.md) | 用户端 / 管理端登录与页脚 |
| 登录日志（示例分页） | 分页查询登录记录，演示 `@NeedAop` + `PageResponse` + PageHelper；管理端全量、用户端仅本人 | `../api/auth-login.md` | `../demo-static/web/admin/login-logs.html`、`../demo-static/web/user/login-logs.html` |
| 健康检查 | 全产品线统一方案（Standards §13）的落地实现：存活 / 就绪 / 全量三端点 + 组件化探针；供部署脚本与集中巡检平台 `bgssai-healthcheck` 调用 | `../api/health-check.md`（详设 `../architecture/health-check-design.md`） | 无（非功能需求，无用户界面，按 Standards §0 不走原型一步） |
| MCP 服务 | 用户端 Streamable HTTP MCP + PAT；MCP Connectors（Claude / Codex / Cursor / Grok Bot / bgssai-bot）；tagline「Connect your AI tools to bgssai」；只读包装登录日志与健康探针 | `../api/MCP.md` | `../demo-static/web/user/mcp-tokens.html` |
| 站内在线客服 | 对话气泡留言 / 会话 + 聊内提工单；管理端处理台含工单状态；会话 OPEN/PENDING/CLOSED | [`in-app-support.md`](./in-app-support.md) · [`../api/in-app-support.md`](../api/in-app-support.md) | 用户端 `support-chat` · 管理端 `support-inbox` |
| 壳内窄栏布局 | Bot / Chat 旁嵌用户端时切触控窄栏；独立桌面不改；管理端跳过 | [`mobile-in-bot-shell.md`](./mobile-in-bot-shell.md) | 用户端 `shell-pane.html` |

<!-- OPC-20261017 START -->
## 2026-10-17 用户规划补充

[opc-requirements-20261017.md](opc-requirements-20261017.md)：本产品职责、共同契约、五端边界、验收条件及逐条功能用例。仅需求与测试设计，不代表实施完成。
<!-- OPC-20261017 END -->

- [2026-10-19 OPC 静态差距审计与验收补充](opc-gap-audit-20261019.md)

<!-- OPC-20261019 START -->
## 2026-10-19 功能目录增量

[opc-requirements-20261019.md](opc-requirements-20261019.md)：新增 REQ-SHORT-PRODUCT-101、REQ-SHORT-PRODUCT-102。仅需求与测试设计，不代表实施完成。2026-10-17 需求与用例结果保持原样。
<!-- OPC-20261019 END -->

- [2026-10-30 功能测试补遗](functional-test-supplement-20261030.md)
