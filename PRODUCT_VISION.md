# 产品愿景（PRODUCT_VISION）

## 产品线规划（全线统一）

BGSSAI 产品线按下面八条划分职责，各仓实现与文档不得与此冲突。

1. **BGSSAI** 是给一人公司（OPC）创业者的全行业工具集合，让用户能找到 OPC 创业时所有需要的工具。
2. **bgssai-website** 是公司官网，对外介绍公司的产品和服务；`reference` 目录中的 `reference-website` 是原来的官网。官网无登录，不承载产品操作、在线对话或中心账号。
3. **bgssai-chat** 提供类似 ChatGPT、Gemini、Claude home、Grok 的 Web 在线对话 AI。境内仓为 `bgssai-chat-cn`，境外仓为 `bgssai-chat-global`。
4. **中心用户账号在 bgssai-chat**：用和 Google、GitHub 一样的**第三方登录**接入各 App，不是单点登录。Chat 也可以授权登录不是 BGSSAI 的应用。各 App 也可以有自己的用户账号体系。境内应用走 `bgssai-chat-cn`，境外应用走 `bgssai-chat-global`。
5. **bgssai-bot** 对标 Grok Bot；BGSSAI 的全部产品应用可以托管给 Bot 直接操作。参考 grok-bot、openclaw、hermes-agent、grok-build。
6. **bgssai-tokenhub** 是模型中枢，对接主流模型原生 API，再提供给旗下产品。`bgssai-tokenhub-global` 对接国际主流模型，`bgssai-tokenhub-cn` 对接中国大陆模型。两仓都有用户端，对标腾讯云 TokenHub（`reference/tokenhub-prototypes`）；用户端走登录规范，也可用 Chat 登录。旗下产品调模型走中枢凭证。
7. **面向 OPC 的工具集合基本是 B2C**。
8. **境外应用**只有：`bgssai-geo-global`、`bgssai-saas-global`、`bgssai-tokenhub-global`、`bgssai-chat-global`。其余有用户端的应用按境内处理。

`bgssai-docmost` 与 `bgssai-build` 是研发解决方案，不是第 1 条工具，也不上官网。用户从 Docmost 站点下载 Build。Build 写代码时把软件工程文档（需求说明、概要设计、详细设计、类图、流程图、时序图、泳道图、状态图等）写入 Docmost；简单图用 Mermaid，拓扑 / 动画 / 状态机用自包含 HTML（参考 `reference/docmost/htmldemo`）。用户可自己编辑，也可和 Agent 对话修改，改完发送给 Build 继续编程。Docmost 参考开源 docmost；Build 参考 grok-build 与 Cursor；Bot 参考 grok-bot、openclaw、hermes-agent、grok-build。所有参考项目在 `reference` 目录。

有用户端的应用必须支持账号密码、邮箱验证码、手机验证码登录。中国大陆境内应用另需微信、抖音、百度、支付宝登录。中国大陆境外应用另需 Google 账户与 GitHub 登录。官网无登录。管理端不开放注册。

愿景唯一权威：`bgssai-skeleton/docs/PRODUCT-LINE-VISION.md`。本段是各仓副本，变更以该文件为准。

**本仓位置**：本仓是第 1 条工具集合中的业务产品。可自有账号，也可接受 bgssai-chat-cn 中心账号授权登录（第三方登录，不是单点登录）；应可被 bgssai-bot 托管操作。

> 本文件是产品「AI 短剧制作平台」的愿景与定位，是需求（`docs/feature/**`）的上位背景；需求版本变更时
> 同步回顾本文件受影响处（Standards §0）。
>
> **当前状态**：M0–M3 基础范围已完成（见 `docs/project/milestones.md`）。生产模型统一通过腾讯云
> TokenHub 接入：文本为 Hy4 / Kimi K3 / DeepSeek-V4-Pro，视频为 MiniMax / PixVerse / Kling，
> 声音为 Hy-ASR / MiniMax-Speech / MiniMax-Music；既有直连 Provider 保留为可选通道。素材默认本地回退或华为云 OBS。

## 一、产品定位

一句话定位：**面向短剧创作者的 AI 短剧制作应用，把一部短剧从想法做到成片的全流程搬到线上，用 AI 承担
其中最费人力的环节，让不具备专业影视制作能力的人也能独立产出短剧。**

产品要解决的核心矛盾：短剧的需求量大、更新快，而传统制作链路（编剧、分镜、拍摄、剪辑、配音、后期）
人力密集、周期长、门槛高。本产品用 AI 把「文字 → 画面 → 声音 → 成片」的环节工具化，使单人或小团队
也能完成过去需要一支剧组的工作。

## 二、目标用户与角色

| 角色 | role_code | 说明 |
| --- | --- | --- |
| 平台管理员 | `PLATFORM_ADMIN` | 使用管理端（8080 / 3001）：账号与权限、内容合规审核、AI 能力与用量管理、运营数据 |
| 创作者用户 | `USER` | 使用用户端（8081 / 3002）：创建作品、编写剧本、生成画面与配音、合成并导出成片 |

细分人群优先级（M1 定稿）：

| 优先级 | 人群 | 差异化诉求 | 本轮如何满足 |
| --- | --- | --- | --- |
| P0 | 个人短剧创作者 | 不懂分镜与剪辑、预算敏感、一个人干全流程 | 全流程线上化 + 一键拆镜 + 费用预估与预算闸门 + 一键合成 |
| P1 | MCN 与内容工作室 | 批量产出、质量下限要稳、成本要能核算 | 结构模板库 + 四道质量门 + 用量流水下钻到镜头 |
| P2 | 品牌短剧投放方 | 强调品牌调性与审核安全 | 角色 / 场景卡锁定视觉调性 + 合规预检与平台审核 |

P2 人群本轮只借助通用能力满足，不做品牌定制化功能。

## 三、核心价值与关键场景

- 核心价值一：**降门槛**——不会分镜、不会剪辑、没有演员与场地的人也能做出可发布的短剧。
- 核心价值二：**提效率**——把重复劳动（分镜拆解、素材生成、配音、字幕、成片合成）交给 AI 与流程编排。
- 核心价值三：**出好片**——不止「能做出来」，而是让 AI 承担专业剧组里编审 / 导演 / 剪辑师的挑毛病工作，
  用「结构对标 → 多方案择优 → 三视角评审 → 定点返工」的闭环把成片推到水准线以上
  （主线功能，见 `docs/feature/quality-pipeline.md`）。
- 核心价值四：**可管控**——创作过程留痕、内容可审核、AI 用量与成本可计量且不失控，支撑规模化产出。

关键场景（M1 定稿，逐个落 `docs/feature/**`）：

| 场景 | 说明 | 需求文档 |
| --- | --- | --- |
| 场景一 | 输入一句话梗概，产出符合短剧节奏的分集大纲与可拍剧本 | `script-studio.md` |
| 场景二 | 为角色一次定妆，全剧锁定形象，杜绝跨镜头换脸 | `character-bible.md` |
| 场景三 | 剧本一键拆成镜头表，参数化镜头语言并自动合成提示词 | `storyboard-workbench.md` |
| 场景四 | 生成之前先出文字分镜稿与低清草图，在最便宜的一级把画面构想改顺，再优化提示词 | `previz-text-storyboard.md` |
| 场景五 | 用张力曲线、人物关系图与走位示意，一眼看出节奏塌在哪、关系揭早了没、两人有没有站反 | `story-map.md` |
| 场景六 | 批量生成画面，费用可预估、进度可掌控、失败可续跑 | `shot-generation.md` |
| 场景七 | 配音自动对齐镜头时长，字幕自动生成，一键合成竖屏成片 | `voice-subtitle.md` · `film-compose-export.md` |
| 场景八 | 每个阶段过关前先过质量门，问题定位到具体一场戏 / 一个镜头 / 一句台词 | `quality-pipeline.md` |
| 场景九 | 风险在花钱之前被拦截，成片由平台审核并给出可执行的驳回原因 | `content-compliance.md` |
| 场景十 | 管理员配置 AI 能力与单价、维护结构模板与阈值、管理创作者配额 | `ai-capability-usage.md` · `quality-pipeline.md` |

## 四、范围边界（做什么 / 不做什么）

- 做：短剧制作全流程的线上化与 AI 化（剧本 → 分镜 → 画面 / 配音 → 成片），以及支撑它的账号、作品、
  素材、任务与审核能力。
- 暂不做：短剧的分发与投放渠道运营、社区与互动、版权交易、直播——本轮不进范围。
- 不做：模型自研与训练。AI 能力通过外部服务接入，本产品负责流程编排、素材管理与结果可控。

商业模式（2026-08-14 起）：**订阅套餐 + 加量 credit**。FREE / PRO / MAX 档发放周期余额（分），
生成按模型单价从周期额度扣，用尽后扣加量钱包；微信 Native / 支付宝当面付。运营仍可在管理端手工调余额并留痕。

本轮明确**不做**的具体项：横屏与非 9:16 画幅、移动端 App（原型仅占位）、模型自研与微调、
内容分发与数据回流、多人协作与权限细分（一个作品当前只归属一个创作者）、自动续费代扣与退款自动化。

## 五、技术基线

- 后端：Java 21 + Spring Boot 4 + MyBatis（Example 模式）+ PageHelper + Druid + JWT + Bucket4j 限流。
- 前端：React 18 + Vite + Ant Design 5 + Zustand + Axios。
- 数据库：MySQL 8（`bgssai_short`），管理端与用户端共享一个库。
- 工程基线：自骨架仓 `bgssai-skeleton` 初始化，鉴权 / 统一响应 / 分页 / traceId / 限流 / CORS /
  健康检查等基础链路开箱可用（清单见 `README.md`）。
- 约束：遵循 `docs/BGSSAI-Standards.md` 全部强制条款。
- AI 能力接入（M1 定口径）：业务侧只认能力类型（`TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` /
  `MUSIC` / `COMPOSE`），
  具体供应商与模型、单价、开关由管理端 `ai_model_config` 配置解析，换供应商不改业务代码
  （见 `docs/feature/ai-capability-usage.md`、`docs/feature/ai-model-integration.md`）。
- 素材存储（M1 定口径）：图 / 视频 / 音频 / 字幕 / 成片统一进 `short_asset`，文件存对象存储，
  库内只存 `storage_key`；对外下载走 30 分钟有效的签名 URL，不暴露永久地址。
- 长耗时任务（M1 定口径）：全部生成类动作统一走 `short_ai_task` 任务体系（排队 / 执行 / 重试 /
  断点续跑 / 计费），前端按 `task_id` 与 `batch_id` 恢复视图（见 `docs/feature/shot-generation.md`）。
- 供应商选型：首批生产模型统一走 `TENCENT_TOKENHUB`，端点参数与异步轮询由模型 `config_json` 驱动；
  对象存储继续使用华为云 OBS（本地 profile 可回退），任务调度继续复用当前线程池 Worker。

## 六、里程碑

| 里程碑 | 目标 | 状态 |
| --- | --- | --- |
| M0 | 仓库初始化：按骨架落双端可运行工程、文档骨架与基础链路 | 已完成 |
| M1 | 产品需求与原型定稿：功能需求进 `docs/feature/**`，双端原型进 `docs/demo-static/**` | 已完成 |
| M2 | 详细设计与契约：数据模型 / 状态机进 `docs/architecture`，REST 契约进 `docs/api`，安全设计进 `docs/security` | 已完成 |
| M3 | 代码与数据库：`sql/DDL.sql` 业务表与双端前后端实现 | 已完成 |
