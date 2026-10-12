# SHORT-01 商业门禁（成片生产 → Media）

## 背景

SHORT-01 目标是短剧制作链路达到可商用：真实模型生成、OBS 成片落盘、Short→Media 发布端到端可用。
当前验收为 **PARTIAL**：合成与发布代码已在，但**不得**用 stub/占位 URL 或本地假落盘冒充商业成功（对照 long 的 `stub://` 假成功路径）。

## 门禁口径（本轮落地）

1. **未配置 ≠ 成功**
   - AI Provider 缺密钥：任务失败（既有 `RULE_REJECT`），不写 SUCCEEDED。
   - Media 缺 `base-url` / ingest-token：发布失败，文案明确「未配置」。
   - OBS：仅 `local` profile 允许 `LocalFileObjectStorage`；`dev`/`test`/`prod` 未配 OBS 或 OBS 初始化失败时 **启动 fail-fast**，禁止静默本地回落看起来像已接通 OBS。

2. **禁止假成功 URL**
   - `stub://`、`mock://`、`placeholder://`、`about:` 等，以及明显占位地址，在 Provider 下载、成片发布、Media `play_url` 解析处一律拒绝。
   - 不得把此类 URL 写成任务 SUCCEEDED 或 Media READY。

3. **Short→Media 交接：PENDING vs FAIL**
   - **READY**（含 SUCCEEDED 归一）：`commercial_ready=true`，可视为发布成功。
   - **PENDING**（PROCESSING/QUEUED 等）：交接已受理但未就绪；落库 status=PENDING，`commercial_ready=false`；**不是 FAIL**。
   - **FAILED**：明确失败；抛错并落库 FAILED；与 PENDING 文案区分。
   - 批量接口单独统计 `pending`，不得把 PENDING 计入 `succeeded`。

## 仍为 PENDING 的商业证明

以下需真实凭证与联调环境，**不伪造**：

- 付费/生产模型真实出图出片
- 华为 OBS 成片上传与签名播放
- Short → Media 全链路 E2E（含 Media 侧可播）
- prod 是否已经部署 develop 上的登录修复（本仓不能代替运维确认）
- 多端播放器冒烟（Windows / Web / Android / iOS）

## 2026-10-12 发布与成片状态

- 发布 Body 与 Media 现行契约对齐：`status=READY`、`approved=true`、幂等键不变。
- Media PENDING（无 `media_id` / `play_url`）落库 PENDING，不记失败、不记可播放成功。
- Media `code=422` / `playable=false` 记失败，文案用上游 message。
- `bgssai.media.base-url` 为空时 `4006`，不返回假 `media_id`。dev/prod 仍是约定域名，**没有**写入虚构 MEDIA IP。主机是否已部署：未在本仓确认，不可达即失败。
- 镜头视频或配音变更会使已有成片过期，并取消 Media 回放；合成成功才清过期。详见 [film-compose-export.md](film-compose-export.md)。
- 直播 E2E 仍是 **NOT_VERIFIED**。记录见 [../review/p0-5-short-publish-20261012.md](../review/p0-5-short-publish-20261012.md)。

## 相关代码

- `CommercialOutcomeGuard`
- `ObjectStorageConfiguration`
- `RestMediaIngestClient` / `FilmPublishToMediaService`
- `ProviderHttp` 下载入口

## 验收对齐

见 `docs/review/acceptance-20260911.md`：假成功已阻断；直播模型+OBS+Media E2E 仍为 PENDING。

## 一集生产联调骨架（2026-09-16）

联调验收剧本、契约测与 NOT_VERIFIED 清单见
[`docs/review/short-01-episode-production-20260916.md`](../review/short-01-episode-production-20260916.md)。
成片详情须带 `production_evidence`；占位 URL 不得标 `commercial_ready`。

