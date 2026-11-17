# 用户自定义模型 API Key 与 Grok 接入详设

对齐：`docs/feature/user-custom-ai-keys.md`、`docs/api/account-settings.md`、`docs/security/ai-provider-secrets.md`。

## 1. 数据模型

### 1.1 `user_ai_setting`（用户密钥模式设置）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | `NOT NULL AUTO_INCREMENT PK` | 主键 |
| `user_id` | `VARCHAR(64)` | `NOT NULL UNIQUE` | 用户标识 |
| `is_only_user_key` | `TINYINT(1)` | `NOT NULL DEFAULT 0` | `0=混合模式`，`1=仅使用自有密钥模型` |
| `del_flag` / `creator` / `modifier` / `gmt_create` / `gmt_modified` | 标准六列 | 见 `sql/DDL.sql` | 审计与逻辑删除 |

### 1.2 `user_ai_model_key`（用户自定义模型密钥）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | `NOT NULL AUTO_INCREMENT PK` | 主键 |
| `key_id` | `VARCHAR(64)` | `NOT NULL UNIQUE` | 业务标识（`umk-...`） |
| `user_id` | `VARCHAR(64)` | `NOT NULL` | 用户标识 |
| `modality_type` | `VARCHAR(16)` | `NOT NULL` | `TEXT` / `IMAGE` / `VIDEO` / `AUDIO` |
| `ability_type` | `VARCHAR(16)` | `NOT NULL` | `TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC` |
| `provider_code` | `VARCHAR(32)` | `NOT NULL` | `XAI` |
| `model_code` | `VARCHAR(128)` | `NOT NULL` | 上游模型编码（如 `grok-4.7`） |
| `model_name` | `VARCHAR(128)` | `NOT NULL` | 用户展示名称 |
| `base_url` | `VARCHAR(255)` | `NOT NULL DEFAULT 'https://api.x.ai/v1'` | 上游公网 `https://` 根地址 |
| `api_key` | `TEXT` | `NOT NULL` | 明文 API Key（开发期策略） |
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
     -> 命中 umk-... 或 is_only_user_key = 1：
        -> UserAiModelKeyService.resolveUserModel(userId, ability, requestedCode)
           -> 仅查 user_ai_model_key(user_id, ability_type, is_enabled=1, del_flag=0)
           -> 未配置且 is_only_user_key = 1 -> 抛 4014（不回退平台模型）
           -> 构造内存态 AiModelConfig（modelId=umk-..., unitPriceCent=0, configJson={credential_source=USER_KEY, user_key_id, user_id, base_url}）
  -> PipelineReadiness.assertReady：自有密钥或 only_user_key=1 时只校验 user_ai_model_key 可用性，不探测平台 TokenHub
  -> AiTaskQueue / OfficialShotVideoService / DramaAssistService / AiTaskService：
     -> credential_source=USER_KEY 时 estimated_cost_cent = 0，跳过 QuotaConsumeService.assertAffordable
  -> ShortAiTaskWorker / DramaAssistService
     -> AiProviderRegistry.resolve：USER_KEY 直接分派 provider_code 对应 Bean（XaiGrokAiProvider）
     -> XaiGrokAiProvider.execute：按 (user_id, user_key_id) 按需读取明文 api_key 并执行 HTTPS 调用
     -> ShortTaskSettlementService.settle：cost = 0，跳过 QuotaConsumeService.consume，写 ai_usage_record(cost_cent=0)
```

- **异步重试与视频任务防重提交**：`XaiGrokAiProvider.video` 在提交 `POST /v1/videos/generations` 取得 `request_id` 后立即将 `xai_video_request_id` 写入 `short_ai_task.request_json`（不写入 `api_key`）。工作线程如重新执行同一任务且已存在 `xai_video_request_id`，直接轮询 `GET /v1/videos/{request_id}`。
- **表缺失容错**：`UserAiModelKeyService` 在只读模型解析与网关就绪路径捕获缺表类 `DataAccessException` 并按未开启处理，写操作接口正常抛出异常。

## 3. Grok（xAI）第三方 API 能力核对（Standards §7）

官方文档出处：`https://docs.x.ai`（2026-11 核对）。

| 核对维度 | 文本 (`TEXT`) | 图片 (`IMAGE`) | 视频 (`VIDEO`) | 语音 (`VOICE`) | 识别 (`ASR`) | 音乐 (`MUSIC`) |
| --- | --- | --- | --- | --- | --- | --- |
| 端点是否存在 | `POST /v1/chat/completions` | 文生 `POST /v1/images/generations`；图生/参考 `POST /v1/images/edits` | 提交 `POST /v1/videos/generations`；轮询 `GET /v1/videos/{request_id}` | `POST /v1/tts` | `POST /v1/stt`（multipart） | 无官方生成端点 |
| 请求 / 响应字段 | `{model, messages, stream:false}` -> `choices[0].message.content`, `usage.total_tokens` | `{model, prompt, aspect_ratio, response_format:"b64_json", n:1}`；edits 加 `image:{url,type:"image_url"}` -> `data[0].b64_json` / `url` | `{model, prompt, duration(1..15), aspect_ratio, resolution, image:{url}}` -> `request_id` -> `status: done` + `video.url` + `video.duration` | `{text, voice_id, language:"zh"}` -> 原始音频字节（MP3/WAV） | `file`, `model`, `format=true`, `language=zh` -> `text`, `duration`, `words[{start,end,text}]` | 不支持 |
| 异步 / 回调机制 | 同步 JSON | 同步 JSON（`response_format=b64_json`） | 异步轮询（4s 间隔，最长 `max(900, timeout_sec)` 秒，容错 502/503/504） | 同步二进制流 | 同步 JSON | 无 |
| 配额与限流 | 按账户历史消费 Tier 限流（RPS/TPM），超限 HTTP 429 | 按秒级并发限制，超限 HTTP 429 | 按秒级并发限制，超限 HTTP 429 | 标准 API 限流 | 标准 API 限流 | 无 |
| 鉴权方式 | `Authorization: Bearer <XAI_API_KEY>` | 同左 | 同左 | 同左 | 同左 | 无 |
| 模型与版本 | `grok-4.7`、`grok-4`、`grok-3`、`grok-3-mini` | `grok-imagine-image-2.0`、`grok-2-image`（400 自动降级去除不支持字段） | `grok-imagine-video-1.5`、`grok-imagine-video-1.5-lite` | `grok-tts`（音色映射 `eve/ara/rex/leo/sal`） | `grok-voice-transcribe-2.0` | 无 |
| 合规与降级 | `GET /v1/models` 零计费探测 | 参考图支持 HTTPS URL 与 data URI | 下载 `video.url` 后转存对象存储 | 时长由 `MediaMetadata` 实测 | `words` 按标点/停顿归并为 `sentences` | `XAI` 不注册 `MUSIC` 预设；开启 `only_user_key=1` 时返回 `4014`，合成支持无 BGM 导出 |

## 4. SSRF 与凭证边界

1. `base_url` 仅接受 `https://`，禁止 userinfo、`?`、`#`；通过 `InetAddress.getAllByName` 拒绝环回（`127.0.0.0/8` / `::1` / `localhost`）、私网（`10.0.0.0/8` / `172.16.0.0/12` / `192.168.0.0/16`）、链路本地（`169.254.0.0/16`）、`100.64.0.0/10`、多播及通配地址。
2. 上游媒体下载 URL（`data[0].url`、`video.url`）执行同一公网 `https://` 校验。
3. 列表与错误文本仅使用 `api_key_masked` 与用户配置 `model_name`，不回显 `api_key` 明文。
