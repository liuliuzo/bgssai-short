# 用户自定义模型 API Key 多供应商接入详设

对齐：`docs/feature/user-custom-ai-keys.md`、`docs/api/account-settings.md`、`docs/security/ai-provider-secrets.md`。

## 1. 数据模型

### 1.1 `user_ai_setting`（历史兼容表，不再参与解析）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | `NOT NULL AUTO_INCREMENT PK` | 主键 |
| `user_id` | `VARCHAR(64)` | `NOT NULL UNIQUE` | 用户标识 |
| `is_only_user_key` | `TINYINT(1)` | `NOT NULL DEFAULT 0` | 已废弃，恒按 `0` 处理；存量 `1` 不再停用 BGSSAI 自建模型 |
| `del_flag` / `creator` / `modifier` / `gmt_create` / `gmt_modified` | 标准六列 | 见 `sql/DDL.sql` | 审计与逻辑删除 |

### 1.2 `user_ai_model_key`（用户自定义模型密钥）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | `NOT NULL AUTO_INCREMENT PK` | 主键 |
| `key_id` | `VARCHAR(64)` | `NOT NULL UNIQUE` | 业务标识（`umk-...`） |
| `user_id` | `VARCHAR(64)` | `NOT NULL` | 用户标识 |
| `modality_type` | `VARCHAR(16)` | `NOT NULL` | `TEXT` / `IMAGE` / `VIDEO` / `AUDIO` |
| `ability_type` | `VARCHAR(16)` | `NOT NULL` | `TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC` |
| `provider_code` | `VARCHAR(32)` | `NOT NULL` | `XAI` / `MINIMAX` / `KLING` / `PIXVERSE` / `DEEPSEEK` / `OPENAI` / `ANTHROPIC` / `BGSSAI_TOKENHUB` / `TENCENT_TOKENHUB` |
| `model_code` | `VARCHAR(128)` | `NOT NULL` | 上游模型编码 |
| `model_name` | `VARCHAR(128)` | `NOT NULL` | 用户展示名称 |
| `base_url` | `VARCHAR(255)` | `NOT NULL DEFAULT 'https://api.x.ai/v1'` | 上游公网 `https://` 根地址 |
| `api_key` | `TEXT` | `NOT NULL` | 明文 API Key 或 Access Key |
| `secret_key` | `TEXT` | `NULL` | 辅助密钥（可灵 Secret Key；其余供应商留空） |
| `is_enabled` | `TINYINT(1)` | `NOT NULL DEFAULT 1` | `0=停用`，`1=启用` |
| `is_default_model` | `TINYINT(1)` | `NOT NULL DEFAULT 0` | 同用户同能力默认标记 |
| `last_check_status` | `VARCHAR(16)` | `NULL` | `OK` / `FAILED` |
| `last_check_time` | `DATETIME` | `NULL` | 最近连通检测时间 |
| `del_flag` / `creator` / `modifier` / `gmt_create` / `gmt_modified` | 标准六列 | 见 `sql/DDL.sql` | 审计与逻辑删除 |

## 2. 解析、调度与零额度链路

```text
HTTP 请求（UserContext）或 Worker（UsageScope）
  -> AiModelResolveService.resolve(abilityType, requestedCode)
     -> 取 currentUserOrScopeUserId()
     -> 命中 umk-...（显式选中自有模型）：
        -> UserAiModelKeyService.resolveUserModel(userId, ability, requestedCode)
           -> 仅查 user_ai_model_key(user_id, ability_type, is_enabled=1, del_flag=0)
           -> 未选中/未启用/未配置 -> 回退 BGSSAI 自建（平台）模型解析，不因缺少自有模型抛 4014
           -> 构造内存态 AiModelConfig（modelId=umk-..., unitPriceCent=0, configJson={credential_source=USER_KEY, user_key_id, user_id, base_url}）
  -> PipelineReadiness.assertReady：显式自有密钥时只校验 user_ai_model_key 可用性，不探测平台 TokenHub；其余情况按平台就绪校验
  -> AiTaskQueue / OfficialShotVideoService / DramaAssistService / AiTaskService：
     -> credential_source=USER_KEY 时 estimated_cost_cent = 0，跳过 QuotaConsumeService.assertAffordable
  -> ShortAiTaskWorker / DramaAssistService
     -> AiProviderRegistry.resolve：USER_KEY 直接分派 provider_code 对应 Bean：
        MINIMAX -> MiniMaxAiProvider
        KLING -> KlingAiProvider（即时调用 KlingJwtSupport.resolveBearerToken 签发 HS256 JWT）
        PIXVERSE -> PixVerseAiProvider（携带 API-KEY + 每次独立 Ai-trace-id）
        DEEPSEEK -> DeepSeekAiProvider
        XAI -> XaiGrokAiProvider
        OPENAI -> OpenAiAiProvider
        ANTHROPIC -> AnthropicAiProvider
        BGSSAI_TOKENHUB -> TencentTokenHubAiProvider 的用户 Key 路径（不写平台用量回执）
        TENCENT_TOKENHUB -> TencentCloudTokenHubAiProvider（用户 Key，不写平台用量回执）
     平台目录里的 `TENCENT_TOKENHUB` 仍走 TencentTokenHubAiProvider 的平台密钥，不是用户自有腾讯云 TokenHub。
     -> ShortTaskSettlementService.settle：cost = 0，跳过 QuotaConsumeService.consume，写 ai_usage_record(cost_cent=0)
```

- **异步重试与视频/图片任务防重提交**：各异步供应商在提交取得任务标识后立即写入 `short_ai_task.request_json`（不写入 `api_key` 或 `secret_key`），Worker 重跑同一任务时直接续查原任务：
  - `XAI`：`xai_video_request_id`
  - `MINIMAX`：`minimax_video_task_id`
  - `KLING`：`kling_video_task_id`、`kling_video_kind`（`image2video` / `text2video`）、`kling_image_task_id`
  - `PIXVERSE`：`pixverse_video_id`
- **表缺失容错**：`UserAiModelKeyService` 在只读模型解析与网关就绪路径捕获缺表类 `DataAccessException` 并按未开启处理，写操作接口正常抛出异常。

## 3. 第三方 API 七维度核对（Standards §7）

### 3.1 MiniMax（`provider_code = MINIMAX`，`https://api.minimaxi.com`）

| 核对维度 | 文本 (`TEXT`) | 图片 (`IMAGE`) | 视频 (`VIDEO`) | 语音 (`VOICE`) | 识别 (`ASR`) | 音乐 (`MUSIC`) |
| --- | --- | --- | --- | --- | --- | --- |
| 端点是否存在 | `POST /v1/chat/completions` | `POST /v1/image_generation` | 提交 `POST /v1/video_generation`；轮询 `GET /v1/query/video_generation`；取档 `GET /v1/files/retrieve` | `POST /v1/t2a_v2` | `POST /v1/speech_to_text`（multipart） | `POST /v1/music_generation` |
| 请求 / 响应字段 | `{model, messages, stream:false}` -> `choices[0].message.content`, `usage.total_tokens` | `{model, prompt, aspect_ratio, n:1, response_format:"base64"}`；参考图传 `subject_reference` -> `data.image_base64[0]` / `image_urls[0]` | `{model, prompt, duration(6\|10), resolution:"768P", first_frame_image, last_frame_image}` -> `task_id` -> `status=Success` + `file_id` -> `file.download_url` | `{model, text, stream:false, output_format:"hex", voice_setting, audio_setting}` -> `data.audio`(hex) / `extra_info.audio_length` | `file`, `model`, `response_format=verbose_json`, `timestamp_level=word` -> `text`, `duration`, `segments` | `{model, prompt, is_instrumental, lyrics, output_format:"hex", audio_setting}` -> `data.audio`(hex) / `extra_info.music_duration` |
| 异步 / 回调机制 | 同步 JSON | 同步 JSON | 异步轮询（4s 间隔，最长 `max(900, timeout_sec)` 秒，容错 502/503/504） | 同步 JSON | 同步 JSON | 同步 JSON |
| 配额与限流 | 按账户并发/RPM 限流，超限 HTTP 429 或 `base_resp.status_code` | 同左 | 同左 | 同左 | 同左 | 同左 |
| 鉴权方式 | `Authorization: Bearer <MINIMAX_API_KEY>` | 同左 | 同左 | 同左 | 同左 | 同左 |
| 模型与版本 | `MiniMax-M2.5`、`MiniMax-M2.5-highspeed`、`abab6.5s-chat` | `image-01`、`image-01-live` | `MiniMax-Hailuo-02`、`MiniMax-Hailuo-2.3`、`T2V-01-Director`、`I2V-01-Director` | `speech-2.8-hd`、`speech-2.8-turbo`、`speech-02-hd` | `asr-1.0` | `music-2.5`、`music-2.0` |
| 合规与降级 | `POST /v1/get_voice` 零计费探活 | 不支持 `subject_reference` 时自动降级重试 | 下载 URL 校验公网 HTTPS | 预设音色映射官方系统音色 ID | `segments` 规范为 `sentences[{begin_ms, end_ms, text}]` | `music-2.0` 无词时自动补 `[Instrumental]` |

### 3.2 可灵（`provider_code = KLING`，`https://api-beijing.klingai.com`）

| 核对维度 | 图片 (`IMAGE`) | 视频 (`VIDEO`) | 语音 (`VOICE`) | 文本 / 识别 / 音乐 |
| --- | --- | --- | --- | --- |
| 端点是否存在 | 提交 `POST /v1/images/generations`；轮询 `GET /v1/images/generations/{task_id}` | 文生 `POST /v1/videos/text2video`；图生 `POST /v1/videos/image2video`；轮询 `GET /v1/videos/{kind}/{task_id}` | 提交 `POST /v1/audio/tts`；轮询 `GET /v1/audio/tts/{task_id}` | 无官方对等生成端点 |
| 请求 / 响应字段 | `{model_name, prompt, n:1, aspect_ratio, image, image_reference, image_fidelity}` -> `data.task_id` -> `task_result.images[0].url` | `{model_name, prompt, mode, duration(5\|10), image, image_tail, aspect_ratio}` -> `data.task_id` -> `task_result.videos[0].url` | `{text, voice_id, voice_language:"zh", voice_speed:1}` -> `data.task_id` -> `task_result.audios[0].url` | 不支持（返回 `4014`） |
| 异步 / 回调机制 | 异步轮询（3s 间隔） | 异步轮询（4s 间隔，最长 `max(900, timeout_sec)` 秒） | 异步轮询 | 无 |
| 配额与限流 | 按账户资源包与并发数限制，失败见 `code` / `task_status_msg` | 同左 | 同左 | 无 |
| 鉴权方式 | Access Key (`api_key`) + Secret Key (`secret_key`) 签名 HS256 JWT，头部 `Authorization: Bearer <JWT>`（`iss=ak, nbf=now-5, exp=now+1800`） | 同左 | 同左 | 无 |
| 模型与版本 | `kling-v3`、`kling-v2-1`、`kling-v1-5` | `kling-v3`、`kling-v2-6`、`kling-v2-5-turbo`、`kling-v2-1-master`、`kling-v1-6` | `kling-tts` | 无 |
| 合规与降级 | `image_reference` 报错时自动剥离并重试 | 持久化 `kling_video_task_id` 与 `kling_video_kind` | 预设音色映射可灵官方中文音色 | 需在混合模式或配用其他供应商 |

- 连通性探活：`GET /v1/videos/text2video?pageNum=1&pageSize=1`（HS256 JWT，不产生计费任务）。

### 3.3 PixVerse（`provider_code = PIXVERSE`，`https://app-api.pixverse.ai`）

| 核对维度 | 视频 (`VIDEO`) | 文本 / 图片 / 音频 |
| --- | --- | --- |
| 端点是否存在 | 传图 `POST /openapi/v2/image/upload`（multipart `image`）；文生 `POST /openapi/v2/video/text/generate`；图生 `POST /openapi/v2/video/img/generate`；首尾帧 `POST /openapi/v2/video/transition/generate`；轮询 `GET /openapi/v2/video/result/{video_id}` | 无对等端点 |
| 请求 / 响应字段 | 上传返回 `Resp.img_id`；生成返回 `ErrCode=0, Resp.video_id`；轮询返回 `Resp.status`（`1=完成`, `5=生成中`, `7=审核拒绝`, `8=失败`）与 `Resp.url` | 不支持（返回 `4014`） |
| 异步 / 回调机制 | 异步轮询（4s 间隔，最长 `max(900, timeout_sec)` 秒） | 无 |
| 配额与限流 | 按积分扣除，支持 `GET /openapi/v2/account/balance` 零计费查询余额探活 | 无 |
| 鉴权方式 | 请求头 `API-KEY: <key>` + 每次独立 UUID `Ai-trace-id` | 无 |
| 模型与版本 | `v6`、`v6-max`、`v5.6`、`v5`、`v4.5` | 无 |
| 合规与降级 | 持久化 `pixverse_video_id`；输出 URL 校验公网 HTTPS 后下载落库 | 需在混合模式或配用其他供应商 |

### 3.4 DeepSeek（`provider_code = DEEPSEEK`，`https://api.deepseek.com`）

| 核对维度 | 文本 (`TEXT`) | 图片 / 视频 / 音频 |
| --- | --- | --- |
| 端点是否存在 | `POST /chat/completions`（兼容 `https://api.deepseek.com/chat/completions`） | 无对等端点 |
| 请求 / 响应字段 | `{model, messages, stream:false}` -> `choices[0].message.content`, `usage.total_tokens`（不强传 `temperature` 以兼容 `deepseek-reasoner`） | 不支持（返回 `4014`） |
| 异步 / 回调机制 | 同步 JSON | 无 |
| 配额与限流 | 按账户余额与并发限制；`GET /models` 零计费探活 | 无 |
| 鉴权方式 | `Authorization: Bearer <DEEPSEEK_API_KEY>` | 无 |
| 模型与版本 | `deepseek-chat`、`deepseek-reasoner` | 无 |
| 合规与降级 | 直接提取终态答复文本 | 需在混合模式或配用其他供应商 |

### 3.5 Grok（`provider_code = XAI`，`https://api.x.ai/v1`）

| 核对维度 | 文本 (`TEXT`) | 图片 (`IMAGE`) | 视频 (`VIDEO`) | 语音 (`VOICE`) | 识别 (`ASR`) |
| --- | --- | --- | --- | --- | --- |
| 端点是否存在 | `POST /v1/chat/completions` | 文生 `POST /v1/images/generations`；图生 `POST /v1/images/edits` | 提交 `POST /v1/videos/generations`；轮询 `GET /v1/videos/{request_id}` | `POST /v1/tts` | `POST /v1/stt`（multipart） |
| 请求 / 响应字段 | `{model, messages, stream:false}` -> `choices[0].message.content` | `response_format:"b64_json"` -> `data[0].b64_json` / `url` | `{model, prompt, duration, aspect_ratio, resolution, image:{url}}` -> `status:done` + `video.url` | `{text, voice_id, language:"zh"}` -> 音频字节 | `file`, `model`, `format=true`, `language=zh` -> `text`, `words` |
| 异步 / 回调机制 | 同步 JSON | 同步 JSON | 异步轮询（4s，最长 `max(900, timeout_sec)` 秒） | 同步流 | 同步 JSON |
| 配额与限流 | `GET /v1/models` 零计费探活；超限 HTTP 429 | 同左 | 同左 | 同左 | 同左 |
| 鉴权方式 | `Authorization: Bearer <XAI_API_KEY>` | 同左 | 同左 | 同左 | 同左 |
| 模型与版本 | `grok-4.7`、`grok-4`、`grok-3`、`grok-3-mini` | `grok-imagine-image-2.0`、`grok-2-image` | `grok-imagine-video-1.5`、`grok-imagine-video-1.5-lite` | `grok-tts` | `grok-voice-transcribe-2.0` |
| 合规与降级 | 规范化根路径 `https://api.x.ai` -> `/v1` | 不支持字段 400 自动降级重试 | 持久化 `xai_video_request_id` | 时长由 `MediaMetadata` 实测 | 按标点/停顿归并为 `sentences` |

### 3.6 OpenAI（`provider_code = OPENAI`，`https://api.openai.com/v1`）

| 核对维度 | 文本 (`TEXT`) | 图片 (`IMAGE`) | 识别 (`ASR`) | 视频 / 配音 / 音乐 |
| --- | --- | --- | --- | --- |
| 端点是否存在 | `POST /v1/chat/completions` | 文生 `POST /v1/images/generations`；有参考图 `POST /v1/images/edits` | `POST /v1/audio/transcriptions` | 无对等接入 |
| 请求 / 响应字段 | `{model, messages, stream:false}` -> `choices[0].message.content` | 生成 `{model, prompt, n:1}`；`dall-e*` 另传 `response_format=b64_json` 与 `size`。编辑为 multipart `image` + `prompt` + `model`。结果 `data[0].b64_json` 或公网 `url` | multipart `file` + `model` + `language` -> `text` | 不支持（返回能力不可用） |
| 异步 / 回调机制 | 同步 JSON | 同步 JSON | 同步 JSON | 无 |
| 配额与限流 | `GET /v1/models` 探活 | 同左 | 同左 | 无 |
| 鉴权方式 | `Authorization: Bearer` | 同左 | 同左 | 无 |
| 模型与版本 | `gpt-6-luna`、`gpt-5.4`、`gpt-4o` | `gpt-image-2.6-sunburst`、`gpt-image-2.6-flare` | `gpt-transcribe`、`whisper-1` | 无 |
| 合规与降级 | 根地址无路径时补 `/v1` | 下载 URL 走公网 HTTPS 校验 | 无分段时由字幕流程按整段文本落句 | 无余额查询接口 |

### 3.7 Claude（`provider_code = ANTHROPIC`，别名 `CLAUDE`，`https://api.anthropic.com`）

| 核对维度 | 文本 (`TEXT`) | 图片 / 视频 / 音频 |
| --- | --- | --- |
| 端点是否存在 | `POST /v1/messages` | 无图像生成、视频、配音、识别或音乐端点 |
| 请求 / 响应字段 | `{model, max_tokens, messages}` -> `content[].text`；`usage.input_tokens` + `output_tokens` | 不支持 |
| 异步 / 回调机制 | 同步 JSON | 无 |
| 配额与限流 | `GET /v1/models` 探活 | 无 |
| 鉴权方式 | `x-api-key` 与 `anthropic-version: 2023-06-01` | 无 |
| 模型与版本 | `claude-opus-5-5`、`claude-sonnet-5-5`、`claude-haiku-5-5` | 无 |
| 合规与降级 | 文本请求可附带已有 `image_base64` 作为消息图片块，不把它当成图片生成 | 无余额查询接口 |

### 3.8 BGSSAI TokenHub（`provider_code = BGSSAI_TOKENHUB`，别名 `TOKENHUB` / `BGSSAI-TOKENHUB`，`https://www.bgssai-tokenhub.cn`）

用户 Key 复用公司中枢客户端已有路由。请求带 `Authorization: Bearer <用户 Key>`。不调用 `UsageReceiptService.begin`，不读 `ai.tokenhub_cn.api_key`。境外主机与腾讯云 TokenHub 主机直接拒绝。

已保存或新提交的 `TENCENT_TOKENHUB`，当 `base_url` 为空、为公司默认主机或 BGSSAI TokenHub 主机时，改记为 `BGSSAI_TOKENHUB`。

| 能力 | 模型 | 端点 |
| --- | --- | --- |
| `TEXT` | `deepseek-chat`、`hy4-preview`、`kimi-k3` | `POST /v1/chat/completions` |
| `IMAGE` | `kling-v3` | `POST /v1/images/generations` |
| `VIDEO` | `minimax-video-h3`、`kling-video-v3`、`pixverse-video-v6.0` | `POST /v1/videos/generations` |
| `VOICE` | `minimax-speech-2.8-hd` | `POST /v1/audio/speech` |
| `ASR` | `hy-asr-3.0-preview` | `POST /v1/wand/asrproxy/async_create` |
| `MUSIC` | `minimax-music-v2.6` | `POST /v1/native` |

探活：`GET /v1/models`。无账户余额接口接入。

### 3.9 腾讯云 TokenHub（`provider_code = TENCENT_TOKENHUB`，别名 `TENCENT-TOKENHUB`，`https://tokenhub.tencentmaas.com`）

与 BGSSAI TokenHub 同级。只接受官方主机 `tokenhub.tencentmaas.com`、`tokenhub.tencentmaas.cn`、`tokenhub-intl.tencentmaas.com`、`tokenhub-intl.tencentmaas.cn`，路径为空或 `/v1`。请求带 `Authorization: Bearer <用户 Key>`。不调用 `UsageReceiptService.begin`，不读平台密钥。

| 能力 | 模型 | 端点 |
| --- | --- | --- |
| `TEXT` | `hy4`、`deepseek-v4-pro`、`kimi-k3` | `POST /v1/chat/completions` |
| 图片 / 视频 / 配音 / 识别 / 音乐 | 无 | 不支持 |

探活：`GET /v1/models`。无账户余额接口接入。

## 4. SSRF 与凭证边界

1. `base_url` 仅接受 `https://`，禁止 userinfo、`?`、`#`；通过 `InetAddress.getAllByName` 拒绝环回（`127.0.0.0/8` / `::1` / `localhost`）、私网（`10.0.0.0/8` / `172.16.0.0/12` / `192.168.0.0/16`）、链路本地（`169.254.0.0/16`）、`100.64.0.0/10`、多播及通配地址。
2. 上游媒体下载 URL（`data[0].url`、`video.url`、`file.download_url`、`Resp.url` 等）执行同一公网 `https://` 校验。
3. 列表与错误文本仅使用 `api_key_masked`、`secret_key_masked` 与用户配置 `model_name`，不回显任何明文密钥。
