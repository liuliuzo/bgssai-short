# SHORT-01 一集真实短剧生产验收骨架（2026-09-16）

- 日期：2026-09-16（Asia/Shanghai）
- 仓库：`bgssai-short`
- 任务：**SHORT-01（P0 联调验收）**
- 分支：`feature/short-01-episode-production-acceptance`（自 `origin/develop`）
- 依据：`ShortAiTaskWorker` 成片合成路径（约 `finishShot` / `finishCompose`）、`docs/feature/short-01-commercial-gate.md`、`docs/api/SHORT-PRODUCTION.md`、skeleton `product-line-acceptance.md` SHORT-01

## 结论

**PARTIAL / 契约 + 剧本已锁**。真实付费模型出片、华为 OBS 成片、有声成片浏览器可播、Short→Media 直播 E2E 仍为 **NOT_VERIFIED**。  
**禁止**把单测绿、mock、或占位 URL 记为 SHORT-01 全量 PASS。

| 项 | 结论 | 证据 |
| --- | --- | --- |
| 字幕时长与镜头序列一致（≥3 镜） | 契约 PASS | `Short01EpisodeProductionAcceptanceTest#subtitleAndTimelineAlignToThreeShotSequence` |
| 角色/场景一致性注入分镜提示 | 契约 PASS | 同测 `characterAndSceneConsistencyInjectedForShots`；既有 `ShotConsistencyTest` |
| 缺镜头视频诚实失败（点名镜号） | 契约 PASS | `missingClipFailsWithNamedShots`；`ShortAiTaskWorker#loadClips` |
| 空 clip / 空 ffmpeg 入参失败 | 契约 PASS | `emptyClipsRejectedByFfmpegProvider` |
| 占位 URL 不得标商业成功 | 契约 PASS | `CommercialOutcomeGuard` + `placeholderUrlNeverCommercialPass`；`films/detail` 过滤 stub |
| 成片证据链（任务/模型/耗用/存储） | 契约 PASS | `films/detail` → `production_evidence`；`filmDetailExposesTaskModelUsageStorageEvidence` |
| 预检暴露验收镜数地板（≥3，仅提示） | 契约 PASS | `precheckReportsShotCountForAcceptanceFloor` |
| 模型超时收割 FAILED | 既有契约 PASS | `ShortAiTaskWorkerTest#reapsStaleRunningTask`（`MODEL_TIMEOUT`） |
| FFmpeg 缺失诚实失败 | 既有契约 PASS | `FfmpegComposeProvider#requireFfmpeg` → `ABILITY_UNAVAILABLE` |
| 本机 FFmpeg 三镜合成有声可探测 | 环境条件 PASS / 否则 skip | `threeClipComposeWithSubtitleAndDubbingProducesPlayableMp4`（无 ffmpeg 则 assume skip，记 NOT_VERIFIED） |
| 真实 Tokenhub IMAGE/VIDEO/VOICE | **NOT_VERIFIED** | 本环境无生产/联调密钥 |
| 华为 OBS 成片上传与签名播放 | **NOT_VERIFIED** | 非 local 已 fail-fast；未跑真实 OBS |
| Short→Media 可播 E2E | **NOT_VERIFIED** | 见商业门禁；本 PR 不伪造 |
| 首尾帧衔接真人出片 | **NOT_VERIFIED** | 状态机有单测；真人联调缺凭证 |

## 本轮代码改动

1. `CommercialOutcomeGuard`：`isFakeUrl` / `safeCommercialUrl`，读路径不得把占位地址当可播 URL。
2. `FilmService.detail`：返回 `compose_task_id`、`production_evidence`（`source_task_id` / `model_code` / `cost_cent` / `storage_key` / `file_size_byte`）、`commercial_ready`；过滤 stub `media_play_url` / `download_url`。
3. `FilmService.precheck`：`shot_count` / `acceptance_min_shot_count=3` / `acceptance_shot_count_ok`（不足 3 镜**不**阻断合成，仅验收提示）。
4. `ShortAiTaskWorker`：IMAGE/VIDEO/FILM 空字节拒绝落 READY。
5. 验收契约测：`production/acceptance/Short01EpisodeProductionAcceptanceTest`。

## 验收剧本（人工 / 有凭据环境）

> 任一步缺真实模型回执、OBS 对象、可播 URL 或 ffmpeg 成片 → 该项保持 NOT_VERIFIED，不得改 PASS。

### E1 来源 → 剧本

1. 导入或新建作品与第 1 集；生成/定稿剧本（`script_status=READY`）。
2. 记录 `task_id`、`model_code`、费用快照。

### E2 角色 / 场景

1. 至少 1 角色 + 1 场景卡生成定妆并 `LOCKED`，绑定参考图资产（非 SVG 种子）。
2. 分镜提示须含一致性片段（角色外观 + 场景）。

### E3 分镜 ≥ 3 镜 + 首尾衔接

1. 拆镜至少 3 镜；同场相邻镜可设 `INHERIT_PREV`。
2. `films/precheck` 中 `acceptance_shot_count_ok=true`。

### E4 视频素材（真模型）

1. 逐镜 IMAGE→VIDEO；失败/超时任务为 `FAILED`（含 `MODEL_TIMEOUT`），不得 SUCCEEDED。
2. 空内容不得 `VIDEO_READY`。

### E5 配音 / 字幕 / BGM

1. 配音入队；字幕轨起止与镜头序列一致（或 SRT 与 `cuesFromShots` 对齐）。
2. 可选 BGM；选用后写入 `compose_setting_json.bgm_asset_id`。

### E6 FFmpeg 合成

1. 缺任镜视频 → 明确失败文案含镜号。
2. 本机无 ffmpeg → `ABILITY_UNAVAILABLE`，不得假成片。
3. 成功：`compose_status=READY`，`production_evidence.commercial_ready=true`，`download_url` 非 stub。

### E7 对象存储与 Media

1. `storage_key` 指向真实 OBS/本地对象，`file_size_byte>0`。
2. 发布 Media：`play_url` 经 `CommercialOutcomeGuard`；PENDING ≠ FAIL ≠ READY。

## 自动化怎么跑

```bash
mvn -B -ntp -pl bgssai-short-user/bgssai-short-user -am test \
  -Dtest=Short01EpisodeProductionAcceptanceTest,CommercialOutcomeGuardTest,FfmpegComposeProviderTest,ShortAiTaskWorkerTest
```

## 明确不宣称

- 不宣称真实付费模型已出片或 OBS/Media 联调通过。
- 不写入伪造 API Key / OBS 密钥。
- 不把占位 URL、SVG 种子、空字节资产标为 PASS。

## 关联

- 商业门禁：`docs/feature/short-01-commercial-gate.md`
- 产品线基线：skeleton `docs/feature/product-line-acceptance.md` SHORT-01
- 进度台账：skeleton `docs/review/2026-09-16-plan01-feature-progress-ledger.md`
