# bgssai-short：OPC 规划静态差距审计（2026-10-19）

源码基线：`origin/develop`，`517257e874455c4de86d73160871e910c5359647`。本次已 fetch；原工作副本未用于修改。审计时开放 PR 数：0。

本文件是用户要求的需求完善与验收补充，关联现有 [2026-10-17 需求](opc-requirements-20261017.md)，不替代既有 PRD，不宣称新增功能已经实现。本次不改原型、接口、代码、数据库或部署；后续实施须按需求、原型、详设、实现顺序完成一致性验证。

结论：已有需求覆盖及部分真实实现，但不能判为全部满足。下表将明确的实现差异与尚未运行验证分开；没有运行证据不等同于功能不存在。源码方法、适配器、测试文件或安装脚本存在均不作为端到端通过证明。

## 现有需求与核心能力

| 现有需求 ID | 能力 | 本次核查结果 |
|---|---|---|
| REQ-SHORT-PRODUCT-001 | 剧本与分镜版本 | 已抽查相关实现，见 E3；边界与差异见下文，运行待验收 |
| REQ-SHORT-PRODUCT-002 | 角色场景与素材生成 | 已抽查相关实现，见 E1、E3；边界与差异见下文，运行待验收 |
| REQ-SHORT-PRODUCT-003 | 配音字幕与合成 | 已抽查相关实现，见 E3；边界与差异见下文，运行待验收 |
| REQ-SHORT-PRODUCT-004 | 长任务恢复与幂等 | 已抽查相关实现，见 E2、E3；边界与差异见下文，运行待验收 |
| REQ-SHORT-PRODUCT-005 | Tube成片发布 | 已抽查相关实现，见 E2；边界与差异见下文，运行待验收 |
| REQ-SHORT-PRODUCT-006 | 角色场景资产库与跨镜头一致性锚定 | 需求已覆盖；本次未逐条追通实现，保持未证实 |
| REQ-SHORT-PRODUCT-007 | 可视多轨时间轴精编与音画时序对齐 | 需求已覆盖；本次未逐条追通实现，保持未证实 |
| REQ-SHORT-PRODUCT-008 | 竖屏短剧画幅与黄金卡点节奏模板 | 需求已覆盖；本次未逐条追通实现，保持未证实 |

## 源码证据

| 编号 | 路径与符号 | 静态发现及限制 |
|---|---|---|
| E1 | [bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/aiprovider/DirectVendorGuard.java](../../bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/aiprovider/DirectVendorGuard.java)：`rejected`（基线第 15 行附近） | 直接厂商调用拒绝路径明确指向 CN 中枢；真实供应商及成本需凭配置与调用回执验收。 |
| E2 | [bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/production/service/FilmPublishToMediaService.java](../../bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/production/service/FilmPublishToMediaService.java)：`publish`（基线第 46 行附近） | 校验 READY 成片及所属作品，生成来源幂等键，区分 PENDING/FAILED/READY 并持久化回执；已有成功可重放。 |
| E3 | [bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/production/service/ShortAiTaskWorker.java](../../bgssai-short-user/bgssai-short-user/src/main/java/com/bgssai/shortai/production/service/ShortAiTaskWorker.java)：`reapStaleTasks`（基线第 204 行附近） | 存在任务执行、超时回收和结算服务调用；本次没有执行模型、FFmpeg 或故障恢复。 |

## 增量需求与验收任务

以下 AUD 编号是现有需求的差距/验收补充任务编号，不是另建一套同名功能。P0 在宣称对应能力可用前关闭；P1 随完整产品体验交付关闭。所有条目目前均待实施或待验证，本文不将其标为通过。

### AUD-SHORT-20261019-01 · P0 · 成片到 Tube 的未知结果恢复

- 关联需求：REQ-SHORT-PRODUCT-004、REQ-SHORT-PRODUCT-005。
- 当前状态：部分实现，联调未证实。
- 需求补充：细化交付幂等验收：来源作品/集数/成片版本与 Tube 媒体 ID 成对记录；超时或 PENDING 后须以接收端状态恢复，区分重试请求与新版本发布。
- 正常验收：一次合成产物经 Tube 处理后在四端播放，重试同一来源版本仍关联同一 media_id，最终状态与真实可播结果一致。
- 异常验收：接收成功但响应丢失、PENDING 后服务重启、批量部分失败不得重复建作品或把待处理写成功；未审核/无权成片拒绝交付。
- 依赖：Tube ingest 幂等及查询契约、对象存储有效期、FFmpeg；按已有字段推进详设。

### AUD-SHORT-20261019-02 · P1 · 一致性资产与时间轴的真实成片基准

- 关联需求：REQ-SHORT-PRODUCT-006、REQ-SHORT-PRODUCT-007、REQ-SHORT-PRODUCT-008。
- 当前状态：运行未证实。
- 需求补充：将跨镜头角色锚点、多轨时序、画幅和安全区需求收敛为可复现工程样本，并关联生成版本及最终文件。
- 正常验收：同一角色两镜头、配音字幕和转场，在 9:16 与 16:9 样本中按时间轴导出，预览与最终音画顺序一致。
- 异常验收：失败镜头重试不覆盖已确认版本；缺素材/过期资源有明确失败；模型不能保持一致性时标限制而非虚构成功。
- 依赖：六个短剧参考项目、素材版本、模型能力目录、渲染引擎。

## 全线共同要求的关闭证据

共同要求沿用原需求，不新增重复 ID。下列范围本次没有真实环境/制品运行证据，因此保持未证实；不能从页面入口或文案检索推定已实现。

| 范围 | 原需求索引 | 关闭条件 |
|---|---|---|
| B2C、密码/验证码/区域授权与 admin 隔离 | REQ-SHORT-COMMON-001、REQ-SHORT-COMMON-002、REQ-SHORT-COMMON-003、REQ-SHORT-COMMON-004、REQ-SHORT-COMMON-006、REQ-SHORT-COMMON-007、REQ-SHORT-COMMON-008、REQ-SHORT-COMMON-009 | 真实正向回调/投递、取消重放、跨账号拒绝；应用独立 user 会话，admin 不开放注册。 |
| 模型与额度 | REQ-SHORT-COMMON-010、REQ-SHORT-COMMON-011、REQ-SHORT-COMMON-012、REQ-SHORT-COMMON-018、REQ-SHORT-COMMON-019 | 有该能力的业务真实路由固定 CN；提交前展示已有倍率规则；请求、用量与账本可对账。Blog 保留既有不提供站内 AI 写文章及相关已确认边界。 |
| Bot 下载与授权 | REQ-SHORT-COMMON-014、REQ-SHORT-COMMON-015 | 实际包版本/平台与安装启动；真实业务调用、撤销与越权拒绝，用户授权不能提升为平台 admin。 |
| 五端交付 | REQ-SHORT-PLATFORM-001、REQ-SHORT-PLATFORM-002、REQ-SHORT-PLATFORM-003、REQ-SHORT-PLATFORM-004、REQ-SHORT-PLATFORM-005 | Windows/macOS/Linux/iOS/Android 分别有真实可安装制品、设备和核心流程证据。Tube 按系列分工：PC 桌面三端，Android/iOS 各自原生端，Web 浏览器端；不要求四仓重复造五端。 |

部署验收沿用用户规则：dev 与 prod 均部署 develop，分别加载 application-dev.properties 与 application-prod.properties。需记录同一源码版本与配置差异；本次没有触发部署。

每项关闭记录至少包含源码 SHA、构建/制品版本、设备/系统、环境、脱敏输入输出、预期与实际、时间、失败原因。缺渠道配置、模型额度、设备、样本或外部回执时标记阻塞/未验证，不能用模拟结果替代真实运行。

## 参考版本与 PR 去重

参考仓已按远端默认分支同步并固定下列快照；参考版本仅作对照，未复制第三方实现，也不表示本产品获得参考项目全部功能。

| 参考仓 | 同步后 SHA | 同步状态 |
|---|---|---|
| [ArcReel/ArcReel](https://github.com/ArcReel/ArcReel) | `8d9d3a00d9b7cd6c1d119ad303206dc2d57ddf7f` | current |
| [Forget-C/Jellyfish](https://github.com/Forget-C/Jellyfish) | `a9678194ddf2d9be3ccbe78d4287d87d5089e123` | current |
| [jeffstric/ZJT](https://github.com/jeffstric/ZJT) | `168e63ea585fa52395dd4f0ce5f80182552d726e` | current |
| [murongg/openframe](https://github.com/murongg/openframe) | `9f978a5d906388593f54ccdfa06b400d14430d49` | current |
| [waooAI/waoowaoo](https://github.com/waooAI/waoowaoo) | `dfec20e32298ed675050329e4a41477024148399` | current |
| [xuanyustudio/LocalMiniDrama](https://github.com/xuanyustudio/LocalMiniDrama) | `abcec932e2395bad2fdb298dac95d59c7c6a83a3` | current |

审计前未发现开放 PR；以下是查询到的最近已合并 PR，仅用于识别当前来源，不能据此推定部署完成：

- [#117：chore: point bgssai-media references at bgssai-tube](https://github.com/liuliuzo/bgssai-short/pull/117)（合并时间 2026-10-18T11:37:28Z）。
- [#116：docs: remove redundant document copies](https://github.com/liuliuzo/bgssai-short/pull/116)（合并时间 2026-10-17T14:33:24Z）。
- [#115：docs: supplement OPC product functional requirements and test cases](https://github.com/liuliuzo/bgssai-short/pull/115)（合并时间 2026-10-17T14:35:37Z）。

验证范围：本次只验证新增文档的本地链接、已有需求 ID、证据路径及 git diff --check；未执行产品测试、模型调用、真实投递、投稿、支付或部署。
