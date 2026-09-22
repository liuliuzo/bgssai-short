# TokenHub 多模型接入详设

对齐需求：`docs/feature/ai-model-integration.md`。横切能力：`ai-provider-obs-design.md`；配音字幕：
`voice-subtitle-design.md`；接口：`docs/api/SHORT-PRODUCTION.md`。

## 1. 设计目标

- 以 `provider_code=TENCENT_TOKENHUB` 接入 TEXT / VIDEO / VOICE / ASR / MUSIC，业务 Service
  仍只提交标准化的 `AiProviderRequest`，不感知供应商 JSON。
- **双中枢**：`config_json.hub=cn|global` 分别走 TokenHub-CN / TokenHub-Global；密钥
  `ai.tokenhub_cn.api_key` / `ai.tokenhub_global.api_key`（旧 CN 键空值回填兼容）。
- 用户端只见 `display_name` 模型选择器；错误与 `fail_reason` 脱敏，不含 TokenHub 字样。
- 所选模型不可用时回退同能力默认模型；重试可换备用模型。
- 上游返回的临时视频、音频、字幕 URL 只在 Worker 内使用；成功后立即下载并转存对象存储。
- 同步和异步上游调用统一落入 `short_ai_task` 状态、重试、计费与失败原因，不生成占位结果。

## 2. 组件与职责

```text
业务 Service
  -> AiModelResolveService 解析 ability_type + model_code（不可用则回退默认）
  -> AiTaskQueue 写入 QUEUED
    -> ShortAiTaskWorker 组装标准请求
       -> TencentTokenHubAiProvider 按 hub + model_code 分派端点
       -> 同步取结果，或在 timeout_sec 内轮询
       -> 临时 URL 下载为 bytes
       -> ObjectStorage.put + short_asset
       -> 业务结果回写 + ai_usage_record
```

`TencentTokenHubAiProvider` 只负责供应商协议转换与结果归一化；作品、剧集、镜头、字幕、BGM 归属校验均在
Service / Worker 内完成。`AiProviderRegistry` 按 `TENCENT_TOKENHUB` 注册该实现，并对
`www.bgssai-tokenhub.cn` / `www.bgssai-tokenhub.com` / 历史 `tokenhub.tencentmaas.com`
host 做 resolvable 回退。

## 3. 模型与端点映射

| 中枢 | 默认 base_url | secret_ref |
| --- | --- | --- |
| CN | `https://www.bgssai-tokenhub.cn` | `ai.tokenhub_cn.api_key` |
| Global | `https://www.bgssai-tokenhub.com` | `ai.tokenhub_global.api_key` |

鉴权统一为 `Authorization: Bearer <api_key>`。properties 仅可写中枢 base URL，**禁止**写 API Key。

| 能力 | 模型 | 提交端点 | 查询端点 | 完成结果 |
| --- | --- | --- | --- | --- |
| TEXT | `hy4-preview` / `kimi-k3` / `deepseek-chat`（CN）；`gpt-4o-mini` / `claude-3-5-haiku-latest`（Global） | `POST /v1/chat/completions` | 同步 | `choices[0].message.content` |
| VIDEO | `minimax-video-h3` | `POST /v1/wand/minimax-video-v2/generation` | `GET /v1/wand/minimax-video-v2/tasks/{task_id}` | `task.content.url` |
| VIDEO | `pixverse-video-v6.0` | 文生 `/v1/wand/pixverse/text-to-video`；图生 `/v1/wand/pixverse/image-to-video` | `GET /v1/wand/pixverse/tasks/{task_id}` | `Resp.url` |
| VIDEO | `kling-video-v3` | 文生 `/v1/wand/kling/text-to-video`；图生 `/v1/wand/kling/image-to-video` | `GET /v1/wand/kling/tasks/{task_id}` | `data[].outputs[].url` |
| VOICE | `minimax-speech-2.8-hd` | `POST /v1/wand/minimax-tts/sync_tts` | 同步 | `data.audio` |
| ASR | `hy-asr-3.0-preview` | `POST /v1/wand/asrproxy/async_create` | `GET /v1/wand/asrproxy/tasks/{task_id}` | `output.text` / `sentences` |
| MUSIC | `minimax-music-v2.6` | `POST /v1/wand/minimax-music/generation` | 同步 | `data.audio` |

### 3.1 标准请求映射

- TEXT：`prompt` 进入单条 `messages[{role:"user",content}]`，`stream=false`。
- VIDEO：`prompt`、`duration_sec`、`aspect_ratio`、`image_url` 归一化。存在首帧时必须使用对象存储短时
  URL：MiniMax 写 `content[type=image_url]`，PixVerse 写 `img_id`，Kling 写
  `contents[type=first_frame].url`。图生视频不传画幅，让输出跟随首帧。
- VOICE：`text`、`voice_id`、`speed` 写 `voice_setting`，输出固定 MP3 URL；
  `extra_info.audio_length` 毫秒转换为 `duration_sec`，不得按字数估算。
- ASR：`input_url`、`subtitle_format=srt`；输入必须是当前用户所属配音素材的短时 URL。
- MUSIC：`prompt`、可选 `lyrics`、`is_instrumental` 与 MP3 音频参数；本产品默认纯音乐。

TEXT 的 `usage.total_tokens`、PixVerse 的 `tokenhub_usage.total_tokens` 等可用量优先回填
`AiProviderResult.usage_amount`；上游未返回可核算单位时按当前模型配置的一次调用快照计费。

### 3.2 异步状态映射

| 供应商 | 处理中 | 成功 | 失败 |
| --- | --- | --- | --- |
| MiniMax VIDEO | `queued` / `running` | `succeeded` | `failed` / `cancelled` |
| PixVerse VIDEO | `Resp.status=5` | `Resp.status=1` | `6` / `7` / `8` |
| Kling VIDEO | `processing` / `submitted` | `succeeded` | `failed` |
| Hy ASR | `queued` / `in_progress` | `completed` | `failed` |

轮询间隔取 `config_json.poll_interval_ms`，默认视频 3000ms、ASR 2000ms；总时限取
`ai_model_config.timeout_sec`。超时抛 `PROVIDER_TIMEOUT`，审核失败抛 `PROVIDER_CONTENT_REJECTED`，
其它上游失败抛 `PROVIDER_ERROR`。Worker 只重试平台允许的超时和 5xx，不重试审核失败或参数错误。

## 4. 业务回写

### 4.1 文本与视频

- OUTLINE / SCRIPT / REWRITE 任务把最终 `model_code` 固化到任务行，文本按现有结构化解析回写。
- VIDEO 成功后下载供应商 URL，写 `short_asset(asset_type=VIDEO)`；数据库与前端不保存供应商 URL。

### 4.2 配音与 ASR

- DUBBING 成功后写 AUDIO 素材和 `short_dubbing.audio_duration_sec`，再执行现有音画对齐。
- `POST /subtitles/transcribe` 按本集已有配音逐条创建 ASR 任务。Worker 将句级相对时间加上镜头在本集的
  起始偏移，替换该镜字幕；人工修改痕迹存在且 `confirm_overwrite_flag!=1` 时提交前拒绝。

### 4.3 AI BGM

- MUSIC 成功后写 `short_asset(asset_type=AUDIO, shot_id=NULL)`，任务 `result_json` 返回
  `bgm_asset_id`；旧候选不删除。
- `GET /films/detail` 返回本集 `bgm_candidate_list`。只有用户调用 `/films/settings-save` 选用候选后，
  才写 `compose_setting_json.bgm_asset_id` 并进入 BGM 时间轴与 FFmpeg 合成输入。

## 5. 数据与配置

本次不改表结构：复用 `ai_model_config`、`short_ai_task`、`short_asset`、`short_dubbing`、
`short_subtitle`、`ai_usage_record` 和 `short_work.compose_setting_json`。DML 新增九个模型行及空值密钥种子。

TokenHub `config_json` 共同字段：

```json
{
  "hub": "cn",
  "base_url": "https://www.bgssai-tokenhub.cn",
  "secret_ref": "ai.tokenhub_cn.api_key",
  "poll_interval_ms": 3000
}
```

Global 行使用 `"hub":"global"` 与 `https://www.bgssai-tokenhub.com` / `ai.tokenhub_global.api_key`。

各端点可由 `chat_path`、`submit_path`、`task_path`、`text2video_path`、`image2video_path` 覆盖，便于
供应商升级时通过管理端调整；代码内保留本文件 §3 的安全默认值。

## 6. 安全与可观测性

- 日志只记录 `provider_code`、`model_code`、`hub`、上游 `request_id`、task_id、状态和耗时；不记录
  Authorization、完整输入临时 URL 或供应商输出 URL。
- 下载只接受 `https` URL；单文件设置响应体上限，且按能力校验 MIME（VIDEO 仅视频，VOICE/MUSIC 仅音频）。
- 中枢 Key 缺失、空值或停用时在调用前失败；接口、`fail_reason` 与用户错误文案均不回显 Key，也不出现
  TokenHub 品牌字样。
- 任务重试沿用同一业务参数，新建上游任务；`fallback_model_flag=1` 时可换备用模型；成功计费一次，失败不计费。
