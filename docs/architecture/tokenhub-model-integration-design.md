# TokenHub 多模型接入详设

对齐需求：`docs/feature/ai-model-integration.md`。横切能力：`ai-provider-obs-design.md`；配音字幕：
`voice-subtitle-design.md`；接口：`docs/api/SHORT-PRODUCTION.md`。

## 1. 设计目标

- 以 `provider_code=TENCENT_TOKENHUB` 接入 TEXT / VIDEO / VOICE / ASR / MUSIC，业务 Service
  仍只提交标准化的 `AiProviderRequest`，不感知供应商 JSON。
- **Short 固定 CN 中枢**（REQ-SHORT-COMMON-010）：`TokenHubClientSupport.resolveHub` 恒为
  `cn`；`defaultBaseUrl` 忽略境外主机。配置或模型若指向 Global 则直接失败，不跨区回退，
  也不读取 `ai.tokenhub_global.api_key`。密钥优先 `ai.tokenhub_cn.api_key`
  （旧 CN 键空值回填兼容）。第三方直连（VEO/KLING/MINIMAX/SEEDANCE，以及指向其它主机的
  OPENAI_COMPAT）的 `execute` 拒绝，计费生成经 registry 走境内中枢。唯一例外是公司主机
  `https://chat.bgssai.com`：仅当中枢密钥为空且 `ai.openai_compat.api_key` 已填写时直连。
- 用户端只见 BGSSAI 产品别名（`bgssai-chat` / `bgssai-image` / `bgssai-video` / `bgssai-tts` / `bgssai-asr` / `bgssai-music`，展示名为 BGSSAI Chat / Image / Video / TTS / ASR / Music）与 `0.5×` / `1×` / `1.5×`。客户端提交的 `model_code` 是别名，服务端再映射到网关编号。错误与 `fail_reason` 脱敏，不含中枢品牌或供应商品牌。对账列表不回显网关主机或网关编号。
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
Service / Worker 内完成。`AiProviderRegistry` 按 `TENCENT_TOKENHUB` 注册该实现。调用主机只来自
`bgssai.llm.tokenhub-cn-base-url`。境外主机不是回退地址。

## 3. 模型与端点映射

| 中枢 | 主机（只在环境 profile） | secret_ref |
| --- | --- | --- |
| CN | `bgssai.llm.tokenhub-cn-base-url` | `ai.tokenhub_cn.api_key` |

Short 不配置境外 base URL，也不把境外密钥当作空值回退。

鉴权统一为 `Authorization: Bearer <api_key>`。properties 仅可写中枢 base URL，**禁止**写 API Key。
Short 只使用 CN profile。模型行不保存主机。

| 能力 | 模型 | 提交端点 | 查询端点 | 完成结果 |
| --- | --- | --- | --- | --- |
| TEXT | `hy4-preview` / `kimi-k3` / `deepseek-chat`（仅 CN） | `POST /v1/chat/completions` | 同步 | `choices[0].message.content` |
| VIDEO | `minimax-video-h3` | `POST /v1/wand/minimax-video-v2/generation` | `GET /v1/wand/minimax-video-v2/tasks/{task_id}` | `task.content.url` |
| VIDEO | `pixverse-video-v6.0` | 文生 `/v1/wand/pixverse/text-to-video`；图生 `/v1/wand/pixverse/image-to-video` | `GET /v1/wand/pixverse/tasks/{task_id}` | `Resp.url` |
| VIDEO | `kling-video-v3` | 文生 `/v1/wand/kling/text-to-video`；图生 `/v1/wand/kling/image-to-video` | `GET /v1/wand/kling/tasks/{task_id}` | `data[].outputs[].url` |
| VOICE | `minimax-speech-2.8-hd` | `POST /v1/wand/minimax-tts/sync_tts` | 同步 | `data.audio` |
| ASR | `hy-asr-3.0-preview` | `POST /v1/wand/asrproxy/async_create` | `GET /v1/wand/asrproxy/tasks/{task_id}` | `output.text` / `sentences` |
| MUSIC | `minimax-music-v2.6` | `POST /v1/native`（`minimax/music`） | 同步 | `data.audio` |

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

轮询间隔取 `config_json.poll_interval_ms`，默认视频 3000ms、ASR 2000ms。画面和视频的现行客户端
走 OpenAI 兼容媒体接口（见 3.3），轮询总时限是 `max(180 秒, timeout_sec)`，避免长视频在 180 秒被提前判失败。
超时对用户展示「生成超时，任务可能仍在处理，请到任务中心查看或稍后重试」。审核失败与其它上游失败仍按
既有失败码回写。Worker 只重试平台允许的超时和 5xx，不重试审核失败或参数错误。

### 3.3 现行客户端（OpenAI 兼容媒体）

Java 客户端对画面、视频、配音实际调用的是兼容接口，不读取视频行里的 wand `submit_path`：

| 能力 | 提交 | 未完成时查询 |
| --- | --- | --- |
| IMAGE | `POST /v1/images/generations` | 同步，或同一套视频查询直到出图 |
| VIDEO | `POST /v1/videos/generations` | `GET /v1/videos/generations/{id}?model=` |
| VOICE | `POST /v1/audio/speech` | 同步；JSON `audio` 或原始音频字节 |

公司直连只接受 `https://chat.bgssai.com`（无用户信息、查询串、片段，端口仅 443）。模型编号限于
`qwen` / `qwen2*` / `bgssai-chat`、`sd-turbo` / `bgssai-image`、`cogvideox*` / `bgssai-video`、
`piper` / `bgssai-tts` / `edge-tts`。`qwen-tts` 不当作文本模型。开跑前对公司网关只做 `GET /v1/models`。
视频任务在中枢上只按目录核对 `video_generation`，不 POST 视频探测，避免探测本身扣费。

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
  "secret_ref": "ai.tokenhub_cn.api_key",
  "poll_interval_ms": 3000
}
```

历史 Global 行保持停用。短剧不调用它们，也不在 CN 失败时改走境外。

各端点可由 `chat_path`、`submit_path`、`task_path`、`text2video_path`、`image2video_path` 覆盖，便于
供应商升级时通过管理端调整；代码内保留本文件 §3 的安全默认值。

网关主机只来自环境 profile `bgssai.llm.tokenhub-cn-base-url`。DEV 是
`https://dev.user.bgssai-tokenhub.cn`，生产是 `https://www.bgssai-tokenhub.cn`。模型行不再保存
`base_url`：共享 DML 与 `sql/dev`、`sql/prod` 都会 `JSON_REMOVE` 掉已有主机，避免 DEV 库里留着
生产地址、再靠调用时覆盖。profile 未配置时才回退到代码里的生产默认主机。

## 6. 安全与可观测性

- 日志只记录 `provider_code`、`model_code`、`hub`、上游 `request_id`、task_id、状态和耗时；不记录
  Authorization、完整输入临时 URL 或供应商输出 URL。
- 下载只接受 `https` URL；单文件设置响应体上限，且按能力校验 MIME（VIDEO 仅视频，VOICE/MUSIC 仅音频）。
- 中枢 Key 缺失、空值或停用时在调用前失败；接口、`fail_reason` 与用户错误文案均不回显 Key，也不出现
  TokenHub 品牌字样。
- 任务重试沿用同一业务参数，新建上游任务；`fallback_model_flag=1` 时可换备用模型；成功计费一次，失败不计费。
- 网关错误体按 `error.code` 翻译。`provider_not_configured` 的用户文案是
  「模型服务暂未开通，请稍后重试」；供应商名、`code` 与 `type` 只打日志。

## 7. 开跑前检查

用户发起大纲、定妆、分镜、图片、配音、视频，或重试失败任务时，`PipelineReadiness` 先看当前环境网关：

1. `GET /v1/models` 读取当前环境目录（含 `id`、`capabilities`、`owned_by`）。
2. 解析文本、图片、配音三个将要使用的模型。未传 `model_code` 时取该能力的配置默认，
   且只有目录列出该编号并带上对应能力时才采用：
   - 图片配置默认是 `kling-v3`。若库里仍是未注册的可灵编号，而目录里有可灵文生图，
     改用已注册编号，优先 `kling-v3`。目录里没有可灵文生图时，预检查点明配置编号未开放。
   - 配音配置默认是 `minimax-speech-2.8-hd`。目录把它登记为 `audio_speech` 时用它；
     否则若目录里已有具备语音能力的 MiniMax 模型（编号或 `owned_by` 属于 MiniMax），
     改用那个已注册编号。还没有时，预检查点明配置编号未开放，不改用其它厂商的语音。
   - 文本配置默认不在目录时，该步骤不能开始，不改用其它文本模型。
   - 用户点选的编号不改写；不在目录则该步骤不能开始。
3. 已采用的模型发一条不会成片的探测（文本不带 messages，图片 `n=0`，配音不带台词）。
   网关若返回 `provider_not_configured`，该步骤不能开始；若只因探测体不完整返回 400，视为可用。
   公司图片、配音模型对同一探测体返回 HTTP 422，且 `detail` 只校验探测故意留空或非法的字段
   （图片 `n` / `size`，配音 `input` / `voice`，文本 `messages` / `prompt`）时，同样视为可用。
   422 落在其它字段，或表示模型未登记、服务未开通时，该步骤不能开始。
4. 任一步骤不可用时接口返回 `4014`，文案点明步骤，例如
   「大纲、剧本、分镜现在不能开始：模型服务暂未开通，请稍后重试」
   或「配音现在不能开始：该模型暂未开放：BGSSAI TTS，请稍后重试」。
   此时不写 `short_ai_task`，也不扣费。入队时写入的是服务端采用的网关编号；
   返回给用户的 `model_code` 是第 1 节的产品别名。
   合成走本机 ffmpeg，不走这道检查。
   镜头图片成功后自动排视频是同一次提交的后续任务，不再重复探测。

## 8. 公司密钥与账号额度

文本、图片、配音的 HTTP 调用只带平台 `ai.tokenhub_cn.api_key`，不带用户的 bgssai-chat OAuth token。
结算分两条：已有 `oauth_binding.platform=bgssai` 时扣中央账本；没有绑定时扣本产品 `user_quota`，
并在没有本地订阅时发放 FREE。创建作品先做额度预检，不足返回 `4007`，不入队。Worker 在请求模型前再查一次。
运维依赖：中枢公司密钥已配置，且网关目录提供 `bgssai-chat`。个人 Chat 授权不是大纲前置条件。
账号若已经绑定 Chat，中央账本余额不足仍会在入队前被拒绝。

### 音乐原生网关契约（2026-11-11）

MUSIC 使用环境 profile 指定的国内 BGSSAI TokenHub，固定提交 `/v1/native`，请求 envelope 为 `provider=minimax`、`operation=music`、`payload`。该操作的 `model_required=false`，不得传 envelope 顶层 `model`；模型只放入 `payload.model`。历史模型行中的 `music_path` 不再控制请求地址，种子也不再写入旧腾讯专用路径，无须修改已有任务记录。

模型目录保留创作者已选择的 ID；发送原生请求时，将腾讯别名 `minimax-music-v2.6` / `minimax-music-v3.0` 分别转为供应商 ID `music-2.6` / `music-3.0`。已是原生 ID 时保留。`prompt`、可选 `lyrics`、`is_instrumental`、`audio_setting` 与 `output_format=url` 放入 payload。继续使用中枢凭证与用量回执，供应商凭证由中枢管理。响应仍按 `base_resp` 检查错误，再解析 `data.audio` 和 `extra_info` 音频时长。

契约依据：[MiniMax 官方音乐 API](https://platform.minimax.cn/docs/api-reference/music-generation)、TokenHub CN 的 `NativeRequestFactory` 与只读 `/v1/native/operations` 目录。供应商文档注明音乐付费 API 的账户资格限制，路由存在和离线测试通过不代表当前供应商账户已获调用资格，实际生成需单独验收。

回归覆盖：dev/prod 国内地址、旧路径配置失效、native envelope 顶层字段限制、模型别名转换、歌词/纯音乐参数、URL/hex 音频结果和供应商错误。用户界面、业务请求 DTO、表结构及任务状态不变。
