# 用户自定义模型 API Key（user-custom-ai-keys）

## 1. 功能范围

- 入口路由：`/account`（账号设置页「模型密钥」区块）。
- 支持模态与能力映射：
  - 文本模型：`modality_type=TEXT` -> `ability_type=TEXT`（大纲、剧本、改写、原著改编、AI 协同专家）。
  - 图片模型：`modality_type=IMAGE` -> `ability_type=IMAGE`（角色/场景/道具定妆、多视角补全、分镜首帧、尾帧）。
  - 视频模型：`modality_type=VIDEO` -> `ability_type=VIDEO`（分镜文生视频、分镜图生视频）。
  - 音频模型：`modality_type=AUDIO` -> 细分 `ability_type` 为 `VOICE`（配音合成）、`ASR`（语音转文字字幕校准）、`MUSIC`（背景音乐）。
- 支持操作：
  - 单条新增、一键批量创建 Grok 多模态配置、编辑（留空密钥则保留原值）、启停、设为同能力默认、删除、连通性测试。
  - 全局开关「仅使用自有密钥模型」（`only_user_key`）。

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
5. **首发接入供应商：Grok（`provider_code = XAI`）**：
   - 默认根地址：`https://api.x.ai/v1`，允许用户输入自定义 `https://` 地址与自定义上游模型编码。
   - 各能力预设模型与官方接口覆盖：
     - `TEXT`：`grok-4.7`（默认）、`grok-4`、`grok-3`、`grok-3-mini`（`POST /chat/completions`）。
     - `IMAGE`：`grok-imagine-image-2.0`（默认）、`grok-2-image`（文生图 `POST /images/generations`，含参考图/图生图走 `POST /images/edits`）。
     - `VIDEO`：`grok-imagine-video-1.5`（默认）、`grok-imagine-video-1.5-lite`（异步提交 `POST /videos/generations` + 轮询 `GET /videos/{request_id}`，存在首帧或锁定定妆图时自动传 `image.url` 走图生视频）。
     - `VOICE`：`grok-tts`（`POST /tts`）。
     - `ASR`：`grok-voice-transcribe-2.0`（`POST /stt`）。
     - `MUSIC`：xAI 官方当前未开放音乐生成接口，`XAI` 预设不包含 `MUSIC`；开启 `only_user_key = 1` 且未配置其他 `MUSIC` 模型时，BGM 生成返回 `4014`，成片合成支持无 BGM 正常合成。
6. **默认模型唯一性与删改联动**：
   - 同一用户、同一 `ability_type` 下最多一条 `is_default_model = 1`。
   - 新增同能力首条启用模型时自动设为默认；停用或删除当前默认模型时，自动将该能力下其余首条已启用模型升为默认。
7. **密钥安全与传输约束**：
   - `api_key` 明文入库（符合开发期存储规范），接口列表仅回传掩码 `api_key_masked`，禁止回显完整明文；日志与异常消息严禁打印 `api_key`。
   - `base_url` 与上游媒体下载地址强制要求公网 `https://`，拒绝内网/环回/元数据地址（SSRF 防护）。
   - `short_ai_task.model_code` 存储自有配置业务标识 `umk-...`（长度不超过 64 字符），`request_json` 不保存 `api_key`。

## 3. 字段与枚举

| 字段 / 枚举 | 取值与含义 |
| --- | --- |
| `modality_type` | `TEXT`（文本模型）/ `IMAGE`（图片模型）/ `VIDEO`（视频模型）/ `AUDIO`（音频模型） |
| `ability_type` | `TEXT` / `IMAGE` / `VIDEO` / `VOICE`（配音）/ `ASR`（字幕识别）/ `MUSIC`（背景音乐） |
| `provider_code` | `XAI`（展示名 `Grok (xAI)`） |
| `credential_source` | `USER_KEY`（自有密钥）/ `PLATFORM`（平台默认） |
| `only_user_key` | `0`（混合模式）/ `1`（仅使用自有密钥模型，停用默认 BGSSAI 模型） |
| `last_check_status` | `OK` / `FAILED` / `null` |

## 4. 验收条目

1. 用户在 `/account` 添加 Grok API Key 后，首次自动启用 `only_user_key = 1`。
2. `only_user_key = 1` 时，`GET /bgssai/user/ai-models/list` 与制作工作台各模态选择器均只显示自有模型，不出现 `BGSSAI Chat / Image / Video / TTS / ASR / Music`。
3. `only_user_key = 1` 且某能力未配置启用密钥时，触发该能力任务返回 `4014`，不会调用平台 TokenHub。
4. 选定 Grok 文本/图片/视频/音频模型提交创作任务后，任务经 `XaiGrokAiProvider` 调用 `https://api.x.ai/v1` 完成生成并落库资产，费用 `cost_cent = 0`，平台钱包余额不变。
5. 列表与页面展示掩码密钥（如 `xai-****9k2a`），支持单条测试连接、一键接入 Grok、修改模型、启停、切换默认和删除。
