# 用户自定义模型 API Key（user-custom-ai-keys）

## 1. 功能范围

- 入口路由：`/account`（账号设置页「模型密钥」区块）。
- 支持模态与能力映射：
  - 文本模型：`modality_type=TEXT` -> `ability_type=TEXT`（大纲、剧本、改写、原著改编、AI 协同专家）。
  - 图片模型：`modality_type=IMAGE` -> `ability_type=IMAGE`（角色/场景/道具定妆、多视角补全、分镜首帧、尾帧）。
  - 视频模型：`modality_type=VIDEO` -> `ability_type=VIDEO`（分镜文生视频、分镜图生视频、首尾帧过渡）。
  - 音频模型：`modality_type=AUDIO` -> 细分 `ability_type` 为 `VOICE`（配音合成）、`ASR`（语音转文字字幕校准）、`MUSIC`（背景音乐）。
- 支持操作：
  - 单条新增、一键按供应商批量创建多模态配置、编辑（留空密钥则保留原值）、启停、设为同能力默认、删除、连通性测试。
  - 全局开关「仅使用自有密钥模型」（`only_user_key`）。
  - 多供应商混用：用户可按能力自由组合不同供应商（如 `DEEPSEEK` 文本 + `KLING` 或 `PIXVERSE` 视频 + `MINIMAX` 图片/配音/识别/背景音乐），系统按各能力默认（或前端指定）自有密钥路由。

## 2. 业务规则

1. **首次自动开启独占模式**：用户首次新增自定义模型密钥且尚未存在 `user_ai_setting` 记录时，系统自动开启 `only_user_key = 1`（请求显式传 `only_user_key = 0` 时按请求值落库）。用户可随时通过开关切换 `only_user_key` 为 `0` 或 `1`。
2. **仅使用自有密钥模式（`only_user_key = 1`）**：
   - `GET /bgssai/user/ai-models/list` 仅返回当前用户已启用（`is_enabled = 1`）的自有密钥模型，完全隐藏并停用默认 BGSSAI 平台模型（`bgssai-chat` / `bgssai-image` / `bgssai-video` / `bgssai-tts` / `bgssai-asr` / `bgssai-music`）。
   - 服务端解析 `TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC` 时，仅从当前用户已启用的自有密钥模型中选择；未传 `model_code` 或内部调用传入平台别名（如新建作品自动生成大纲、分镜一键生成视频）时，自动采用用户在该能力下的默认（`is_default_model = 1`）或首条已启用自有密钥模型。
   - 若对应能力下无已启用的自有密钥模型，**不得回退到平台 BGSSAI 模型**，直接拒绝并返回错误码 `4014`。
   - `COMPOSE`（`ffmpeg-compose-v1`）属本机音视频合成工具，不受 `only_user_key` 限制。
3. **非独占模式（`only_user_key = 0`）**：
   - 模型列表同时展示用户已启用的自有密钥模型与 BGSSAI 默认模型；未指定自有模型时可继续使用 BGSSAI 默认模型，显式选中 `umk-...` 时走用户自有密钥模型。
4. **零平台额度扣减**：
   - 凡解析为自有密钥模型（`credential_source = "USER_KEY"`），预估费用 `estimated_cost_cent = 0`、单价 `unit_price_cent = 0`、实扣 `cost_cent = 0`。
   - 跳过平台中央账本与本地额度钱包的余额预检及扣费（不要求绑定 `bgssai-chat` 账号）；`ai_usage_record` 正常记录调用次数，金额列记 `0`。
5. **支持供应商与能力矩阵**：
   - `MINIMAX`（`MiniMax (海螺)`，`auth_type=BEARER`，默认 `https://api.minimaxi.com`）：
     - `TEXT`：`MiniMax-M2.5`（默认）、`MiniMax-M2.5-highspeed`、`abab6.5s-chat`（`POST /v1/chat/completions`）。
     - `IMAGE`：`image-01`（默认）、`image-01-live`（`POST /v1/image_generation`，存在参考图时自动传 `subject_reference` 并支持不支持时降级重试）。
     - `VIDEO`：`MiniMax-Hailuo-02`（默认）、`MiniMax-Hailuo-2.3`、`T2V-01-Director`、`I2V-01-Director`（`POST /v1/video_generation` + `GET /v1/query/video_generation` + `GET /v1/files/retrieve`）。
     - `VOICE`：`speech-2.8-hd`（默认）、`speech-2.8-turbo`、`speech-02-hd`（`POST /v1/t2a_v2`）。
     - `ASR`：`asr-1.0`（默认）（`POST /v1/speech_to_text` multipart，`response_format=verbose_json`）。
     - `MUSIC`：`music-2.5`（默认）、`music-2.0`（`POST /v1/music_generation`）。
   - `KLING`（`可灵 (Kling AI)`，`auth_type=AK_SK`，`api_key` 存 Access Key、`secret_key` 存 Secret Key 并即时签发 HS256 JWT，默认 `https://api-beijing.klingai.com`）：
     - `VIDEO`：`kling-v3`（默认）、`kling-v2-6`、`kling-v2-5-turbo`、`kling-v2-1-master`、`kling-v1-6`（`POST /v1/videos/text2video` 或 `POST /v1/videos/image2video` + `GET /v1/videos/{kind}/{task_id}`）。
     - `IMAGE`：`kling-v3`（默认）、`kling-v2-1`、`kling-v1-5`（`POST /v1/images/generations` + `GET /v1/images/generations/{task_id}`）。
     - `VOICE`：`kling-tts`（默认）（`POST /v1/audio/tts` + `GET /v1/audio/tts/{task_id}`）。
   - `PIXVERSE`（`PixVerse`，`auth_type=API_KEY_HEADER`，请求头携带 `API-KEY` 与 `Ai-trace-id`，默认 `https://app-api.pixverse.ai`）：
     - `VIDEO`：`v6`（默认）、`v6-max`、`v5.6`、`v5`、`v4.5`（参考图先走 `POST /openapi/v2/image/upload`，文本/图生/首尾帧分别走 `POST /openapi/v2/video/text|img|transition/generate`，轮询 `GET /openapi/v2/video/result/{video_id}`）。
   - `DEEPSEEK`（`DeepSeek`，`auth_type=BEARER`，默认 `https://api.deepseek.com`）：
     - `TEXT`：`deepseek-chat`（默认）、`deepseek-reasoner`（`POST /chat/completions`）。
   - `XAI`（`Grok (xAI)`，`auth_type=BEARER`，默认 `https://api.x.ai/v1`）：
     - `TEXT`：`grok-4.7`（默认）、`grok-4`、`grok-3`、`grok-3-mini`（`POST /chat/completions`）。
     - `IMAGE`：`grok-imagine-image-2.0`（默认）、`grok-2-image`（`POST /images/generations` 或 `/images/edits`）。
     - `VIDEO`：`grok-imagine-video-1.5`（默认）、`grok-imagine-video-1.5-lite`（`POST /videos/generations` + `GET /videos/{request_id}`）。
     - `VOICE`：`grok-tts`（`POST /tts`）。
     - `ASR`：`grok-voice-transcribe-2.0`（`POST /stt`）。
   - `OPENAI`（`OpenAI`，`auth_type=BEARER`，默认 `https://api.openai.com/v1`）：
     - `TEXT`：`gpt-6-luna`（默认）、`gpt-5.4`、`gpt-4o`（`POST /v1/chat/completions`）。
     - `IMAGE`：`gpt-image-2.6-sunburst`（默认）、`gpt-image-2.6-flare`（无参考图 `POST /v1/images/generations`；有参考图 `POST /v1/images/edits`）。
     - `ASR`：`gpt-transcribe`（默认）、`whisper-1`（`POST /v1/audio/transcriptions`）。
     - 不提供 `VIDEO` / `VOICE` / `MUSIC`。
   - `ANTHROPIC`（展示名 `Claude`，接受别名 `CLAUDE`，`auth_type=BEARER`，默认 `https://api.anthropic.com`）：
     - `TEXT`：`claude-opus-5-5`（默认）、`claude-sonnet-5-5`、`claude-haiku-5-5`（`POST /v1/messages`，头 `x-api-key` 与 `anthropic-version: 2023-06-01`）。
   - `BGSSAI_TOKENHUB`（展示名 `BGSSAI TokenHub`，接受别名 `TOKENHUB` / `BGSSAI-TOKENHUB`，`auth_type=BEARER`，默认 `https://www.bgssai-tokenhub.cn`）：
     - 使用用户自己的 BGSSAI TokenHub Key，`credential_source=USER_KEY`，`cost_cent=0`，不写平台用量回执、不扣平台钱包，不读平台中枢密钥。
     - 拒绝境外主机（`https://www.bgssai-tokenhub.com`、`tokenhub-global`）以及腾讯云 TokenHub 主机（`tencentmaas.com` / `tencentmaas.cn`）。自定义公网 `https://` 不得指向腾讯云 TokenHub。
     - `TEXT`：`deepseek-chat`（默认）、`hy4-preview`、`kimi-k3`（`POST /v1/chat/completions`）。
     - `IMAGE`：`kling-v3`（`POST /v1/images/generations`）。
     - `VIDEO`：`minimax-video-h3`（默认）、`kling-video-v3`、`pixverse-video-v6.0`（`POST /v1/videos/generations`）。
     - `VOICE`：`minimax-speech-2.8-hd`（`POST /v1/audio/speech`）。
     - `ASR`：`hy-asr-3.0-preview`（`POST /v1/wand/asrproxy/async_create`）。
     - `MUSIC`：`minimax-music-v2.6`（`POST /v1/native`）。
   - `TENCENT_TOKENHUB`（展示名 `腾讯云 TokenHub`，接受别名 `TENCENT-TOKENHUB`，`auth_type=BEARER`，默认 `https://tokenhub.tencentmaas.com`）：
     - 腾讯云大模型服务平台 TokenHub。使用用户自己的 Key，`credential_source=USER_KEY`，`cost_cent=0`，不写平台用量回执、不扣平台钱包，不读平台中枢密钥。
     - 只允许官方主机：`https://tokenhub.tencentmaas.com`、`https://tokenhub.tencentmaas.cn`、`https://tokenhub-intl.tencentmaas.com`、`https://tokenhub-intl.tencentmaas.cn`。路径为空或 `/v1`。拒绝 BGSSAI TokenHub 主机及其他自定义主机。
     - 已保存或新提交的 `provider_code=TENCENT_TOKENHUB`，当 `base_url` 为空、为 `https://www.bgssai-tokenhub.cn`（可带 `/v1`）或主机属于 BGSSAI TokenHub 时，记为 `BGSSAI_TOKENHUB`。仅官方腾讯云主机保持 `TENCENT_TOKENHUB`。
     - `TEXT`：`hy4`（默认）、`deepseek-v4-pro`、`kimi-k3`（`POST /v1/chat/completions`，`Authorization: Bearer`）。
     - 不提供 `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC`。
   - 除上述主机隔离外，允许用户输入自定义公网 `https://` 地址与自定义上游模型编码。
6. **连通性检测（零计费探活）**：
   - `MINIMAX`：`POST /v1/get_voice`（`{"voice_type":"system"}`），HTTP 2xx 且 `base_resp.status_code == 0` 判定通过。
   - `KLING`：生成短期 HS256 JWT 请求 `GET /v1/videos/text2video?pageNum=1&pageSize=1`，HTTP 2xx 且 `code == 0` 判定通过。
   - `PIXVERSE`：`GET /openapi/v2/account/balance`（带 `API-KEY` 与 `Ai-trace-id`），HTTP 2xx 且 `ErrCode == 0` 判定通过。
   - `DEEPSEEK` / `XAI`：`GET /models`（xAI 默认根为 `https://api.x.ai/v1/models`），HTTP 2xx 判定通过并匹配已返回模型目录。
   - `OPENAI`：`GET /v1/models`。
   - `ANTHROPIC`：`GET /v1/models`，请求头 `x-api-key`、`Authorization: Bearer` 与 `anthropic-version: 2023-06-01`。
   - `BGSSAI_TOKENHUB` / `TENCENT_TOKENHUB`：`GET /v1/models`（`Authorization: Bearer`）。
   - 以上供应商均无单独的账户余额探活接口接入；连通性只走模型目录或既有零计费查询。
7. **默认模型唯一性与删改联动**：
   - 同一用户、同一 `ability_type` 下最多一条 `is_default_model = 1`。
   - 新增同能力首条启用模型时自动设为默认；停用或删除当前默认模型时，自动将该能力下其余首条已启用模型升为默认。
8. **密钥安全与传输约束**：
   - `api_key` 与 `secret_key` 明文入库（符合开发期存储规范），接口列表仅回传掩码 `api_key_masked` 与 `secret_key_masked`，禁止回显完整明文；日志与异常消息严禁打印任何密钥。
   - `base_url` 与上游媒体下载地址强制执行公网 `https://` 与 DNS 解析 IP 校验，拒绝内网/环回/元数据地址（SSRF 防护）。
   - `short_ai_task.model_code` 存储自有配置业务标识 `umk-...`（长度不超过 64 字符），`request_json` 不保存 `api_key` 或 `secret_key`。

## 3. 字段与枚举

| 字段 / 枚举 | 取值与含义 |
| --- | --- |
| `modality_type` | `TEXT`（文本模型）/ `IMAGE`（图片模型）/ `VIDEO`（视频模型）/ `AUDIO`（音频模型） |
| `ability_type` | `TEXT` / `IMAGE` / `VIDEO` / `VOICE`（配音）/ `ASR`（字幕识别）/ `MUSIC`（背景音乐） |
| `provider_code` | `MINIMAX` / `KLING` / `PIXVERSE` / `DEEPSEEK` / `XAI` / `OPENAI` / `ANTHROPIC` / `BGSSAI_TOKENHUB` / `TENCENT_TOKENHUB` |
| `auth_type` | `BEARER` / `AK_SK` / `API_KEY_HEADER` |
| `credential_source` | `USER_KEY`（自有密钥）/ `PLATFORM`（平台默认） |
| `only_user_key` | `0`（混合模式）/ `1`（仅使用自有密钥模型，停用默认 BGSSAI 模型） |
| `last_check_status` | `OK` / `FAILED` / `null` |

## 4. 验收条目

1. 用户在 `/account` 添加任意已支持供应商的 API Key 后，首次自动启用 `only_user_key = 1`。
2. `only_user_key = 1` 时，`GET /bgssai/user/ai-models/list` 与制作工作台各模态选择器均只显示自有模型，保留用户设置展示名与供应商真实名称，不显示平台默认模型。
3. 可灵配置支持 Access Key + Secret Key 双密钥录入与掩码展示（`ak**** / sk****`），运行时动态签发 HS256 JWT 完成连通测试与图片/视频/配音生成。
4. `MINIMAX` 覆盖 `TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC` 六种能力；`PIXVERSE` 覆盖 `VIDEO`（含图生/首尾帧过渡）；`DEEPSEEK` 覆盖 `TEXT`（含 `deepseek-chat` 与 `deepseek-reasoner`）。
5. 多供应商混用（如 DeepSeek 文本 + 可灵或 PixVerse 视频 + MiniMax 配音与 BGM）在同一作品全流程可执行通过，任务与对账费用 `cost_cent = 0`。
6. `OPENAI` 覆盖 `TEXT` / `IMAGE` / `ASR`；`ANTHROPIC` 覆盖 `TEXT`；`BGSSAI_TOKENHUB` 覆盖六种能力；`TENCENT_TOKENHUB` 覆盖 `TEXT`。两者均使用用户 Key、不扣平台额度，且密钥只发往各自产品的主机。

## 5. 厂商级 API Key（`user_ai_provider_key`）

- 接口：`GET /bgssai/user/ai-keys`、`POST /bgssai/user/ai-keys`、`DELETE /bgssai/user/ai-keys/{providerCode}`（及 `/api/user/ai-keys` 兼容路径）。
- 支持厂商：`DEEPSEEK`、`MINIMAX`、`KLING`（接受别名 `KLINGAI`，支持单 Bearer `api_key` 及 `access_key` + `secret_key` HS256 JWT）、`DASHSCOPE`、`ZHIPU`、`MOONSHOT`、`VOLCENGINE`、`HUNYUAN`、`OPENAI`、`ANTHROPIC`（接受别名 `CLAUDE`）、`GOOGLE`。
- `OPENAI` / `ANTHROPIC` 不是空登记：厂商密钥已能直连官方文本接口。账号「模型密钥」另按能力保存 `umk-` 配置，`OPENAI` 再覆盖图片与语音识别。
- 运行时与计费：匹配到已启用的厂商密钥时直连用户配置凭据（含可选 `base_url`），并跳过平台额度校验与扣费。
- 不在本表新增 `BGSSAI_TOKENHUB` 或 `TENCENT_TOKENHUB`。用户自有中枢 Key 走第 2 节的模型密钥，不与这 11 个厂商密钥重复占一行。
