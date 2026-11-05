# TokenHub 多模型接入（ai-model-integration）

## 一、背景与目标

- 背景：平台已有 `ai_model_config`、统一任务、用量计费和 Provider SPI，但当前模型目录仍以早期直连
  模型为主，TEXT / VOICE 缺少可供创作者选择的生产模型，PixVerse、ASR 与音乐能力尚未进入制作闭环。
- 目标：通过腾讯云 TokenHub 一份凭证接入文本、视频、语音识别、语音合成和音乐生成模型，模型切换仍由
  管理端配置完成；创作者能在对应制作步骤选用模型，生成结果统一进入任务、素材、计费和对象存储链路。
- 关联角色：`PLATFORM_ADMIN`（配置、启停、调价）、`USER`（选模与生成）。

## 二、需求澄清（WHAT）

### 2.1 用户可见名称

创作者、访客和用户端接口只看见 BGSSAI 产品别名。每个能力对外一个名字：

| 能力 | 用户提交与回显的 `model_code` | 展示名 |
| --- | --- | --- |
| `TEXT` | `bgssai-chat` | BGSSAI Chat |
| `IMAGE` | `bgssai-image` | BGSSAI Image |
| `VIDEO` | `bgssai-video` | BGSSAI Video |
| `VOICE` | `bgssai-tts` | BGSSAI TTS |
| `ASR` | `bgssai-asr` | BGSSAI ASR |
| `MUSIC` | `bgssai-music` | BGSSAI Music |

`ai_model_config.config_json` 的 `public_alias` / `public_name` 可以再登记一个以 `bgssai-` 开头、且不含上游品牌的别名。种子只在这两项为空时回填，不覆盖管理端已写入的非空值，也不改 `model_code` 与 `model_name`。用户目录按别名折叠成上面这一张表，不返回网关编号、供应商品牌、`provider_code` 或 `config_json`。任务表、用量行和管理端仍保存网关编号，只在服务端使用。本地合成 `ffmpeg-local` 不是模型别名，成片证据原样保留。

选择器、估价、任务、用量、失败原因和错误文案都使用上表。网关未开通时对用户写「模型服务暂未开通，请稍后重试」；模型未登记时写「该模型暂未开放：BGSSAI Chat」（按能力换成对应展示名）。文案不出现上游品牌或网关编号。

### 2.2 服务端模型目录（不对用户展示）

| 能力类型 | 管理端名称 | 网关 `model_code` | 默认 | 服务端落点 |
| --- | --- | --- | --- | --- |
| `VIDEO` | MiniMax-Video-H3 | `minimax-video-h3` | 是 | 创作台镜头视频生成 |
| `VIDEO` | PixVerse-Video-v6 | `pixverse-video-v6.0` | 否 | 创作台镜头视频生成 |
| `VIDEO` | Kling-Video-V3 | `kling-video-v3` | 否 | 创作台镜头视频生成 |
| `TEXT` | Hy4 preview | `hy4-preview` | 是 | 大纲、剧本、局部重写 |
| `TEXT` | Kimi K3 | `kimi-k3` | 否 | 大纲、剧本、局部重写 |
| `TEXT` | DeepSeek-V4-Pro | `deepseek-v4-pro` | 否 | 大纲、剧本、局部重写 |
| `ASR` | Hy-ASR-3.0-Preview | `hy-asr-3.0-preview` | 是 | 配音成品的字幕转写与时间轴校准 |
| `IMAGE` | 可灵文生图 | `kling-v3` | 是 | 定妆、镜头画面 |
| `VOICE` | MiniMax-Speech-2.8-HD | `minimax-speech-2.8-hd` | 是 | 镜头台词配音 |
| `MUSIC` | MiniMax-Music-v2.6 | `minimax-music-v2.6` | 是 | 成片背景音乐生成 |

上表只描述服务端解析和网关调用。创作台对应步骤只展示 2.1 的别名。
以上模型统一使用 `provider_code=TENCENT_TOKENHUB`。首批不删除 `OPENAI_COMPAT`、`KLING`、
`MINIMAX` 等既有直连配置，运营可停用或保留为备用，但同一能力只能有一个默认模型。

### 2.3 能力边界

1. `ability_type` 完整枚举为 `TEXT` / `IMAGE` / `VIDEO` / `VOICE` / `ASR` / `MUSIC` /
   `COMPOSE`。`VOICE` 仅表示文字转语音，`ASR` 仅表示音视频转文字，`MUSIC` 仅表示背景音乐生成；
   三者不得共用默认模型或互相替代。
2. 文本生成入口支持可选 `model_code`；未传时解析 `TEXT` 默认模型。大纲、单集剧本和局部重写均把
   最终选中的模型写入 `short_ai_task.model_code`。
3. 视频沿用创作台现有模型选择器。MiniMax / PixVerse / Kling 均支持文生视频；存在首帧素材时自动走
   图生视频，输出临时 URL 必须在任务成功后立即下载并转存对象存储。
4. 配音入口支持可选 `model_code`，未传时解析 `VOICE` 默认模型。MiniMax Speech 返回的音频时长作为
   对齐计算依据，不再按台词字数估算时长。
5. 字幕页提供“AI 转写校准”。用户选择 `ASR` 模型后，系统为本集已有配音素材逐条提交 ASR 任务；
   ASR 返回的文本与句级时间戳更新对应字幕。没有配音素材的镜头不提交；人工修改后的字幕再次校准前须确认，
   防止静默覆盖。
6. 合成页提供“AI 生成 BGM”。用户输入音乐描述并选择 `MUSIC` 模型，默认生成纯音乐；成功后保存为
   `short_asset.asset_type=AUDIO`，并把 `bgm_asset_id` 写入作品 `compose_setting_json`。重新生成产生新候选，
   不删除旧素材；用户明确选用后才参与合成。
7. 生产调用只走境内中枢（`hub=cn`）。配置或模型指向境外时直接失败，不跨区回退，也不读取
   `ai.tokenhub_global.api_key`。密钥落 `sys_secret.ai.tokenhub_cn.api_key`（旧键
   `ai.tencent_tokenhub.api_key` 仅作 CN 空值回填兼容）。`config_json` 只保存 `hub`、
   `secret_ref`、端点路径和非敏感默认参数，不保存网关主机，禁止保存 API Key。网关主机只写环境
   profile 的 `bgssai.llm.tokenhub-cn-base-url`（DEV 为 `dev.user`，生产为 `www`）。
   用户选择器展示模型名与 `0.5×` / `1×` / `1.5×`，不出现中枢品牌。
8. 上游异步任务由当前 `short_ai_task` Worker 内部轮询；轮询间隔、超时由模型 `config_json` 与
   `timeout_sec` 控制。超时、内容审核失败、上游 5xx 分别映射现有失败码，不返回伪造素材。
9. 管理端模型页必须支持 `ASR` 与 `MUSIC` 筛选、新增和默认切换，并可看到网关编号。用户端模型目录只返回
   第 2.1 节的 BGSSAI 别名（`model_code` 与 `display_name` 都是别名，供提交和展示），
   **不返回** 网关编号、供应商品牌、`provider_code` / `config_json` / 密钥。
   文案与错误不得出现上游品牌或中枢字样。
10. 用户点选的 `model_code` 不可用时，解析层优雅回退到该能力默认（或任一启用）模型；
    重试可带 `fallback_model_flag=1` 换备用模型。
11. 网关非 2xx 时解析错误 JSON。`provider_not_configured` 对用户展示
    「模型服务暂未开通，请稍后重试」，不带供应商名称；`code` 只写入服务日志，不写入 `fail_reason`。
12. 大纲、定妆、分镜、图片、配音、视频的入队和失败任务重试之前，检查文本、图片、配音三个模型
    在当前环境网关是否可用。任一不可用则返回 `4014`，点明不能开始的步骤，不入队、不扣费。
    公司图片、配音默认模型已在目录登记，且探测只因字段校验返回 HTTP 422（图片 `n`/`size`，
    配音 `input`/`voice`）时，视为可用，创建作品不因此被拦截。
13. 开跑前先读网关 `GET /v1/models`。配置的默认模型只有出现在目录里且能力匹配时才采用。
    图片默认是已注册的可灵文生图 `kling-v3`；若库里仍是未注册的可灵编号，改用目录中的可灵文生图
    （优先 `kling-v3`）。配音配置的 MiniMax 编号只有登记了语音能力才采用；尚未登记时，若目录里
    已有具备语音能力的 MiniMax 模型，改用那个已注册编号，否则预检查点明该编号未开放。
    不改用其它厂商的图片或语音模型，也不编造模型编号。配置编号仍不在目录里时，才改用已列出的公司模型
    （见第 15 条）。用户点选的编号不改写。
14. 公司默认文本模型 `bgssai-chat` 的调用凭证是平台 `ai.tokenhub_cn.api_key`，不读取用户的
    bgssai-chat OAuth token。未绑定 Chat 的账号按本产品 `user_quota` 结算；没有订阅时先发 FREE
    额度。创建作品在写入和入队之前做这笔预检，额度不足返回 `4007`，不创建作品、不启动会失败的大纲任务。
    已绑定 Chat 的账号仍扣中央账本。Worker 在真正请求模型前再查一次，避免大纲已写入而任务被记成
    `MODEL_TIMEOUT`。
15. 公司自托管多模态（文本 `qwen`、画面 `sd-turbo`、视频 `cogvideox`、配音 `piper`）不把密钥写入仓库，两条路互斥：
    境内中枢密钥非空时仍走中枢，主机只来自环境 profile。目录里没有当前默认编号时，若同一能力已列出公司模型
    （优先 `bgssai-chat` / `qwen`、`bgssai-image` / `sd-turbo`、`cogvideox`、`bgssai-tts` / `piper` / `edge-tts`），
    改用该编号；已经列出的可灵或 MiniMax 不改，也不把一种第三方换成另一种。
    中枢密钥为空且 `sys_secret.ai.openai_compat.api_key` 已填写时，只允许 `https://chat.bgssai.com/v1`。
    `api.openai.com`、可灵、海螺等其它主机仍不直连。这时开跑前只 `GET /v1/models`，不对画面、视频、配音发会扣费的探测。
    DEV 要直连该公司网关：管理端清空 `ai.tokenhub_cn.api_key`（开发库 init 会把占位符写回）后再填写兼容密钥。
    走中枢则把真实中枢密钥写入 `ai.tokenhub_cn.api_key`，DEV 主机已是 `bgssai.llm.tokenhub-cn-base-url`。
    配音音色 `default_voice_id` 与画面 `size` 由管理端改成网关实际值，种子只给占位 `zh` 与 `512x768`。
16. 画面和视频在中枢上的轮询时限是 `max(180 秒, timeout_sec)`。超时对用户写
    「生成超时，任务可能仍在处理，请到任务中心查看或稍后重试」，文案不出现中枢品牌或密钥名。
    视频任务入队前按目录核对 `video_generation`，不 POST 视频探测。

## 三、验收标准

1. 管理端可看到表中的 TokenHub 模型（图片默认为 `kling-v3`），并能按能力筛选；`ASR`、`MUSIC` 各有独立默认模型。
2. 剧本工作台只出现 BGSSAI Chat。提交 `bgssai-chat` 或省略选择时，用户看到的任务与估价都是该别名；
   任务表内部仍可记下网关编号。
3. 分镜视频选择器只出现 BGSSAI Video。服务端按该能力的默认网关编号调用，结果转存对象存储，
   不把供应商临时 URL 或供应商品牌回给用户。
4. 批量配音选择 BGSSAI TTS 后，产出可播放 MP3，`audio_duration_sec` 来自返回值并参与对齐。
5. 对已有配音的一集用 BGSSAI ASR 校准，字幕文本与时间戳按返回句子更新；无配音素材时返回可读提示且不扣费。
6. 输入 BGM 描述并提交 BGSSAI Music，任务成功后返回 `bgm_asset_id`，合成设置与时间轴能读取该素材。
7. `ai.tokenhub_cn.api_key` 为空或停用时，任务以能力不可用失败。境外密钥与境外模型不参与调用。
   日志与接口均不泄露密钥与中枢品牌；不存在 MOCK 或占位二进制兜底。
8. TokenHub 视频、语音和音乐的临时下载 URL 均在 Worker 内下载转存；供应商 URL 不写入前端永久状态。
9. 用户端模型选择器只展示 BGSSAI 别名（及区域/价格），不展示中枢名、供应商品牌或 `provider_code`。
10. 指定模型不可用时回退默认模型；重试 `fallback_model_flag=1` 可换备用模型。
11. 网关返回 `provider_not_configured` 时，任务失败原因是「模型服务暂未开通，请稍后重试」，
    日志保留 `code` 与供应商名，界面不出现 `http 503` 或供应商名。
12. 文本、图片或配音模型在网关不可用时，创建作品和后续生成在入队前被拒绝，提示里写明不能开始的步骤。
    公司图片与配音默认模型已在目录登记，探测只因 `n`/`size` 或 `input`/`voice` 返回 422 时，创建成功入队。
13. 图片默认任务使用网关目录中的 `kling-v3`。配音在 MiniMax 语音模型尚未登记时被拒绝并点明
    配置编号；登记之后使用该已注册的 MiniMax 语音模型，而不是其它厂商的语音模型。
14. 未绑定 bgssai-chat 的账号用公司默认 `bgssai-chat` 创建作品时，大纲任务可以成功并扣本地额度；
    本地额度不足时创建接口返回 `4007`，没有作品、没有任务。不出现「请先绑定或使用 bgssai-chat
    账号登录后再调用大模型」，也不把该原因记成 `MODEL_TIMEOUT`。

## 四、接口契约（snake_case）

- 既有 `GET /bgssai/user/ai-models/list?ability_type=` 增加 `ASR` / `MUSIC` 枚举。
- 剧本生成相关接口增加可选 `model_code`：
  `POST /bgssai/user/scripts/outline-generate`、`/generate`、`/rewrite`。
- 配音接口增加可选 `model_code`：
  `POST /bgssai/user/dubbings/submit`、`/regenerate`。
- `POST /bgssai/user/subtitles/transcribe`
  ```json
  { "episode_id": "e-20001", "model_code": "bgssai-asr", "confirm_overwrite_flag": 1 }
  ```
  返回 `batch_id`、`task_id_list`、`task_count`。
- `POST /bgssai/user/films/bgm-generate`
  ```json
  { "work_id": "w-10001", "episode_id": "e-20001", "model_code": "bgssai-music",
    "prompt": "悬疑、克制、低频弦乐，适合都市复仇短剧", "is_instrumental": 1 }
  ```
  返回 `task_id`；任务成功后的 `result_json` 含 `bgm_asset_id`。
- `GET /bgssai/user/films/detail` 与 `GET /bgssai/user/films/timeline` 增加 `bgm_asset_id`、`bgm_url`。
- 鉴权：全部用户端接口需 `Jwttoken` 并校验作品 / 剧集 / 素材归属。

## 五、流程 / 状态机（须配图）

- 视频与统一任务状态沿用 `flow-generation-task.html`。
- 配音 → ASR 字幕校准、MUSIC → BGM → 合成的后期流程更新至 `flow-post-production.html`。
- Provider 路由与异步轮询见后续 `docs/architecture/tokenhub-model-integration-design.md`。

## 六、数据模型影响

- `ai_model_config`、`short_ai_task`、`short_asset`、`ai_usage_record` 复用现有表，不新增列。
  用户别名写在 `ai_model_config.config_json` 的 `public_alias` / `public_name`。
- `short_work.compose_setting_json` 增加 `bgm_asset_id`、`bgm_volume`、`duck_flag` 键。
- ASR 结果复用 `short_subtitle`；音乐与配音结果复用 `short_asset(asset_type=AUDIO)`。
- `sys_secret` 增加空值种子 `ai.tokenhub_cn.api_key`（及兼容旧键
  `ai.tencent_tokenhub.api_key`），由 Admin / 运维写入真实值。境外键保持停用，短剧不读取。

## 七、原型

- 管理端：`docs/demo-static/web/admin/ai-models.html`。
- 用户端：`script-studio.html`、`storyboard.html`、`voice-studio.html`、`compose-export.html`。
- 跳转关系不新增页面，更新现有控件和两端 `PAGE-FLOW.html` 字段说明。

## 八、四层同步检查清单

- [x] 需求（本文件及受影响功能需求）
- [x] 原型
- [x] 设计与契约
- [x] 代码与数据库
