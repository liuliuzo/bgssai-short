# AI 能力适配与华为云 OBS 详设

对齐：`docs/feature/ai-capability-usage.md`、`shot-generation.md`、`film-compose-export.md`、
`PRODUCT_VISION.md` §五。TokenHub 的模型与端点映射见 `tokenhub-model-integration-design.md`。
本文件定稿「真实 AI + 对象存储」横切实现，供制作闭环与后续功能共用。

## 1. 目标

- 业务代码只认 `ability_type`：`TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC` / `COMPOSE`。
- 具体供应商、模型码、单价、开关、超时与重试由 `ai_model_config` 解析；换供应商不改业务编排。
- 创作者提交任务时可传 `model_code`；省略则用该能力下 `default_flag=1` 且 `enabled_flag=1` 的模型。
- 图 / 视频 / 音频 / 字幕 / 成片文件进 **华为云 OBS**；库表 `short_asset.storage_key` 只存 object key。
- 对外下载只发 **30 分钟** 预签名 URL，不暴露永久地址与 AK/SK。

## 2. 组件分解

```
业务 Service（大纲/分镜/配音/合成…）
  -> AiModelResolveService（解析 model + 单价；无启用模型 → 4014）
  -> AiTaskQueue（写 short_ai_task，快照 model_code / cost_cent）
  -> ShortAiTaskWorker
       -> AiProviderRegistry.get(provider_code)
       -> AiProvider.execute(ability, request)
       -> ObjectStorage.put(...) 写 OBS，落 short_asset
       -> 成功：ai_usage_record + QuotaConsumeService
```

| 组件 | 职责 |
| --- | --- |
| `AiProvider` | SPI：按能力执行一次生成，返回字节或结构化文本 |
| `OpenAiCompatibleAiProvider` | `provider_code=OPENAI_COMPAT`：HTTP 调 OpenAI 兼容网关（含 xAI） |
| `KlingAiProvider` | `provider_code=KLING`：可灵官方异步任务，覆盖 `IMAGE` / `VIDEO`（有首帧则图生视频，否则文生视频） |
| `MiniMaxAiProvider` | `provider_code=MINIMAX`：海螺 `IMAGE`（image-01）与 `VIDEO`（Hailuo 图生/文生） |
| `SeedanceAiProvider` | `provider_code=SEEDANCE`：火山方舟 Seedance `VIDEO` |
| `VeoAiProvider` | `provider_code=VEO`：Google Gemini API Veo `VIDEO` |
| `TencentTokenHubAiProvider` | `provider_code=TENCENT_TOKENHUB`：TEXT / VIDEO / VOICE / ASR / MUSIC 统一协议适配 |
| `FfmpegComposeProvider` | `provider_code=FFMPEG`：本机 FFmpeg 合成 9:16 成片；默认烧录 SRT 简单字幕（台词/`short_subtitle`），可关 `burn_subtitle_flag` |
| `AiProviderRegistry` | 按 `provider_code` 选实现 |
| `ObjectStorage` | put / get bytes / delete / presignGet |
| `HuaweiObsObjectStorage` | OBS Java SDK 实现 |
| `LocalFileObjectStorage` | 仅 `local` profile 或 OBS 未配置时的开发回退（目录可配） |

## 3. 模型配置与密钥

`ai_model_config` 字段见需求。`config_json` 约定（示例）：

```json
{
  "base_url": "https://api.x.ai/v1",
  "secret_ref": "ai.openai_compat.api_key",
  "chat_path": "/chat/completions",
  "image_path": "/images/generations",
  "audio_path": "/audio/speech",
  "extra_headers": {}
}
```

可灵（`provider_code=KLING`）`config_json` 示例：

```json
{
  "base_url": "https://api-beijing.klingai.com",
  "secret_ref": "ai.kling.api_key",
  "image_path": "/v1/images/generations",
  "text2video_path": "/v1/videos/text2video",
  "image2video_path": "/v1/videos/image2video",
  "aspect_ratio": "9:16",
  "mode": "std",
  "duration": "5",
  "poll_interval_ms": 3000
}
```

- `secret_ref` 对应 `sys_secret.secret_key`；**禁止**在 `config_json` 或 profile 明文存供应商 API Key（见 `docs/security/ai-provider-secrets.md`）。
- 可灵走 Bearer API Key（`Authorization: Bearer <key>`），任务提交后在 `timeout_sec` 内轮询；`IMAGE` 调 `/v1/images/generations`，`VIDEO` 有首帧图则 `/v1/videos/image2video` 否则 `/v1/videos/text2video`。画幅默认 `9:16`。
- OBS AK/SK 写在 `application-*.properties`（中间件，不入库）；供应商 API Key 仍走 `sys_secret`。

解析规则：

1. 请求带 `model_code` → 查启用行；禁用或不存在 → `4006`。
2. 未带 → 该 `ability_type` 下默认启用行；没有 → `4014`。
3. `estimate` / `submit` 均走同一解析；`cost_cent` 按快照 `unit_price_cent * usage_amount`，
   TEXT / VIDEO / VOICE / ASR / MUSIC 优先使用供应商返回用量，缺失时按模型计费配置的一次调用快照计算。

## 4. OBS 对象键规范

```
short/{env}/{user_id}/{work_id}/{asset_id}.{ext}
```

- `env`：`local` / `dev` / `test` / `prod`
- 内容类型：`image/png`、`image/jpeg`、`image/svg+xml`、`audio/mpeg`、`video/mp4`、`application/x-subrip`、`application/json`
- `short_asset`：`storage_key` = 上述 key；`mime_type` / `byte_size` / `width` / `height` / `duration_sec` 按可得填写
- 预签名 GET：有效期 **1800 秒**；`GET /bgssai/user/assets/{asset_id}` 对归属用户返回
  `{ "download_url": "...", "expire_at": "ISO-8601" }`（需登录）；不再直接流式吐 SVG 明文。种子演示数据的 `storage_key` 以 `<svg` 开头时直接返回 data URL，不走 OBS 预签名

## 5. 任务与计费时序

```
submit
  -> resolve model + cost
  -> 余额预检（不足 4007 / 月顶 4015）
  -> insert short_ai_task QUEUED
  -> Worker RUNNING
  -> Provider 调用（超时用 config.timeout_sec；失败按 max_retry）
  -> 成功：写 asset（如有）→ SUCCEEDED → ai_usage_record → quota consume
  -> 失败：FAILED + fail_reason_code；不计费
```

平台侧自动重试不重复计费；用户 `retry` 视为新任务重新估价。

## 6. COMPOSE（FFmpeg）

- 输入：镜头画面（图或视频）、配音音频、SRT 字幕、作品字幕样式
- 输出：单文件 `video/mp4`，竖屏 9:16，回写 OBS
- 部署依赖：运行环境需安装 `ffmpeg`；缺失时任务 `FAILED`，`fail_reason_code=COMPOSE_RUNTIME_MISSING`
- 最小成片策略（本轮）：静帧图按 `shot_duration_sec` 延时 + 音轨对齐 + 硬字幕烧录；有视频镜头则直接拼接

## 7. 与 MVP 文档关系

本文件生效后，`short-production-design.md` 中 Mock/SVG/固定单价描述废止，改为引用本文件。
业务表与用户可见 API 路径保持 `docs/api/page-api-mapping.md` 已登记口径。

## 8. 非目标

- 不引入独立消息队列
- 不实现 WebSocket 推送（继续轮询）
- 首批生产模型目录与默认值由 `tokenhub-model-integration-design.md` 绑定；既有直连 Provider 继续保留为备用
- **不提供 MOCK 兜底 provider**：COMPOSE 由 `FfmpegComposeProvider` 在本机完成；ffmpeg 缺失或镜头视频不全时合成任务直接失败并说明原因，不用假成片掩盖未接通；`films/detail.download_url` 返回可播放签名 URL（勿把 download-url JSON 路径当媒体地址）
- **目录 orphan provider**：用户端模型列表只暴露可解析的 provider；`HUNYUAN`/`WAND` 等无专用 bean 时，TEXT+`chat_path` 回落 `OPENAI_COMPAT`，TokenHub 域名回落 `TENCENT_TOKENHUB`，其余拒绝
