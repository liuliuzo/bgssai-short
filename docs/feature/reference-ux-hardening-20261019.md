# bgssai-short：参考项目对标 UX 与系统健壮性加固（2026-10-19）

版本：2026-10-19。关联基线：[PRODUCT_VISION.md](../../PRODUCT_VISION.md)、[opc-requirements-20261017.md](opc-requirements-20261017.md)。

## 一、背景与设计目标

`bgssai-short` 作为 BGSSAI 一人公司（OPC）工具集合中的境内 AI 短剧制作工作台，对标 `reference-github` 中的六个参考项目（`waoowaoo`、`Jellyfish`、`ArcReel`、`LocalMiniDrama`、`openframe`、`ZJT`），聚焦短剧从剧本拆解、资产定妆、分镜预演、多轨合成到 `bgssai-tube-*` 成片发布的全链路生产体验与系统防呆加固。

| 参考项目 | 核心借鉴能力 | 本仓硬化落点 |
| --- | --- | --- |
| `waoowaoo` | 右侧 AI 剧作协同抽屉、按模型能力自适应参数、首尾帧连续性模式 | `DramaAssistantDrawer.jsx`、`CinematographyHelper.js` 首尾帧衔接与镜头参数预检 |
| `Jellyfish` | 结构化五步工作流、生成前统一镜头就绪度预检、角色/场景/道具一致性锚定 | `ShotConsistency.java` 锚点就绪度核查、`WorkStudio.jsx` 镜头就绪度诊断提示 |
| `ArcReel` | 可审计可恢复管线、代价先于执行预估、剪映/FCPXML/EDL 工程导出与成片交接 | `modelPricing.js` 倍率漂移检测与批量 Token 估算、`FilmPublishToMediaService.java` 交付门禁 |
| `LocalMiniDrama` | 列表与故事板画布双视图、破坏性操作前确认、阶段化失败提示与重试 | `WorkStudio.jsx` 费用确认弹窗、任务失败原因透出与单镜恢复 |
| `openframe` | 人物关系图谱、剧本结构化抽取、关键帧时间轴编排与工程导出 | `StoryMapPage.jsx`、`EditExportPage.jsx`、`RhythmChecker.java` 黄金卡点与安全区检查 |
| `ZJT` | 多智能体分镜协同、用户级算力与 Token 额度透明计量 | `formatToken.js` 紧凑 `K/M/B` 格式化（如 `418.9M`）与精确整数核对 |

## 二、核心加固项

### 1. 同区 `bgssai-tokenhub-cn` 路由守卫与三档倍率校验（REQ-SHORT-COMMON-010 / 011 / 012 / 019）

- **严格同区路由**：`bgssai-short` 为中国大陆境内应用，所有文本、图像、视频、语音与音乐模型调用固定走 `bgssai-tokenhub-cn`，严禁在故障时静默回退至 `bgssai-tokenhub-global`，严禁接受 `api.openai.com`、`api.anthropic.com`、`api.deepseek.com` 等上游厂商直连地址或裸密钥。
- **三档倍率强校验与漂移检测**：前端模型选择器仅允许展示已确认的 `0.5x`、`1x`、`1.5x` 额度倍率。提交生成任务前，通过 `detectMultiplierDrift` 比对当前选中模型与最新目录快照的 `billing_multiplier` 及 `price_version`，若发生未确认漂移或倍率非法则显式拦截并提示用户重新确认。
- **Token 紧凑显示与边界进位**：`formatToken.js` 统一按 `0 ~ 999` 原数、`1K ~ 999.9K`、`1M ~ 999.9M`、`1B+` 规则格式化，最多保留一位小数（如 `418.9M`）；针对 `999,950` 等四舍五入进位边界自动晋升为 `1M`（避免出现 `1000K`），同时对空值、布尔值、带千分位字符串做安全归一化，并提供悬停精确整数展示。

### 2. 镜头就绪度预检与资产一致性锚定（REQ-SHORT-PRODUCT-002 / 006）

- **资产锚点完整性核查**：`ShotConsistency.checkAnchorReadiness` 与前端 `evaluateShotReadiness` 在出图/出视频前检查镜头绑定的角色卡（`ShortCharacter`）、场景卡与道具卡（`ShortSceneCard`）是否已完成定妆锁定（`LOCKED` 且具备 `ref_image_asset_id`），并识别定妆更新导致的 `stale_flag` 过期镜头。
- **首尾帧顺接校验**：当镜头配置为 `INHERIT_PREV`（接上一镜）或 `BOOKEND`（首尾帧）时，校验是否存在可用的首帧/前序参考帧，并在创作台与样片预览页展示清晰的就绪度状态。

### 3. 竖屏短剧画幅、字幕安全区与黄金节奏卡点（REQ-SHORT-PRODUCT-007 / 008）

- **画幅与安全区校验**：`RhythmChecker.validateAspectRatioAndSafeZone` 支持 `9:16` 竖屏与 `16:9` 横屏标准画幅校验，拒绝非法拉伸画幅，并检查单行字幕字数与时间码是否超出移动端播放 UI 底部/右侧安全区。
- **黄金节奏卡点提取**：`RhythmChecker.extractRhythmMarkers` 自动标注开篇黄金 3 秒钩子（`HOOK_3S`）、中段高潮反转（`MID_REVERSAL`）与集尾悬念卡点（`CLIFFHANGER_END`），辅助创作者在低成本文本阶段修正节奏塌陷。

### 4. Tube 成片发布前置校验与幂等交接（REQ-SHORT-PRODUCT-005）

- **严格前置门禁**：`FilmPublishToMediaService.assertPublishable` 与 `MediaIngestContract` 在调用 `POST /bgssai/user/tube/ingest/short` 前强制校验：
  1. 成片状态必须为 `READY` 且未过期（`expired_flag == 0`）；
  2. 剧集审核状态必须为 `APPROVED`；
  3. 成片时长必须为正整数（`film_duration_sec > 0`），拒绝零时长或负时长成片；
  4. 成片 `video_url` 必须为真实可拉取的 `http(s)` 地址，由 `CommercialOutcomeGuard` 拦截 `stub://`、`placeholder://`、`example.com` 等伪造地址；
  5. 携带稳定幂等键 `short:{workId}:{episodeId}:{filmId}`，已处于 `READY` 状态的同一成片重复点击发布时直接幂等回放，不重复创建 Tube 目录条目。
