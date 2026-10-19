# TokenHub 多模型接入（ai-model-integration）

## 一、背景与目标

- 背景：平台已有 `ai_model_config`、统一任务、用量计费和 Provider SPI，但当前模型目录仍以早期直连
  模型为主，TEXT / VOICE 缺少可供创作者选择的生产模型，PixVerse、ASR 与音乐能力尚未进入制作闭环。
- 目标：通过腾讯云 TokenHub 一份凭证接入文本、视频、语音识别、语音合成和音乐生成模型，模型切换仍由
  管理端配置完成；创作者能在对应制作步骤选用模型，生成结果统一进入任务、素材、计费和对象存储链路。
- 关联角色：`PLATFORM_ADMIN`（配置、启停、调价）、`USER`（选模与生成）。

## 二、需求澄清（WHAT）

### 2.1 首批生产模型目录

| 能力类型 | 展示名称 | `model_code` | 默认 | 用户落点 |
| --- | --- | --- | --- | --- |
| `VIDEO` | MiniMax-Video-H3 | `minimax-video-h3` | 是 | 创作台镜头视频生成 |
| `VIDEO` | PixVerse-Video-v6 | `pixverse-video-v6.0` | 否 | 创作台镜头视频生成 |
| `VIDEO` | Kling-Video-V3 | `kling-video-v3` | 否 | 创作台镜头视频生成 |
| `TEXT` | Hy4 preview | `hy4-preview` | 是 | 大纲、剧本、局部重写 |
| `TEXT` | Kimi K3 | `kimi-k3` | 否 | 大纲、剧本、局部重写 |
| `TEXT` | DeepSeek-V4-Pro | `deepseek-v4-pro` | 否 | 大纲、剧本、局部重写 |
| `ASR` | Hy-ASR-3.0-Preview | `hy-asr-3.0-preview` | 是 | 配音成品的字幕转写与时间轴校准 |
| `VOICE` | MiniMax-Speech-2.8-HD | `minimax-speech-2.8-hd` | 是 | 镜头台词配音 |
| `MUSIC` | MiniMax-Music-v2.6 | `minimax-music-v2.6` | 是 | 成片背景音乐生成 |

以上模型统一使用 `provider_code=TENCENT_TOKENHUB`。首批不删除 `OPENAI_COMPAT`、`KLING`、
`MINIMAX` 等既有直连配置，运营可停用或保留为备用，但同一能力只能有一个默认模型。

### 2.2 能力边界

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
7. TokenHub 生产调用走 **双中枢**：TokenHub-CN（`hub=cn`）与 TokenHub-Global（`hub=global`）。
   密钥分别落 `sys_secret.ai.tokenhub_cn.api_key` / `ai.tokenhub_global.api_key`（旧键
   `ai.tencent_tokenhub.api_key` 仅作 CN 空值回填兼容）。`config_json` 只保存 `hub`、
   `secret_ref`、端点路径和非敏感默认参数，不保存网关主机，禁止保存 API Key。网关主机只写环境
   profile 的 `bgssai.llm.tokenhub-cn-base-url`（DEV 为 `dev.user`，生产为 `www`）。
8. 上游异步任务由当前 `short_ai_task` Worker 内部轮询；轮询间隔、超时由模型 `config_json` 与
   `timeout_sec` 控制。超时、内容审核失败、上游 5xx 分别映射现有失败码，不返回伪造素材。
9. 管理端模型页必须支持 `ASR` 与 `MUSIC` 筛选、新增和默认切换；用户端模型目录只返回
   `display_name`（及 `model_code` 供提交），**不返回** `provider_code` / `config_json` / 密钥，
   文案与错误不得出现 TokenHub 字样。
10. 用户点选的 `model_code` 不可用时，解析层优雅回退到该能力默认（或任一启用）模型；
    重试可带 `fallback_model_flag=1` 换备用模型。
11. 网关非 2xx 时解析错误 JSON。`provider_not_configured` 对用户展示
    「模型服务暂未开通：{provider}，请稍后重试」；`code` 只写入服务日志，不写入 `fail_reason`。
12. 大纲、定妆、分镜、图片、配音、视频的入队和失败任务重试之前，检查文本、图片、配音三个模型
    在当前环境网关是否可用。任一不可用则返回 `4014`，点明不能开始的步骤，不入队、不扣费。

## 三、验收标准

1. 管理端可看到表中 9 个 TokenHub 模型，并能按 7 类能力筛选；`ASR`、`MUSIC` 各有独立默认模型。
2. 在剧本工作台分别选择 Hy4、Kimi K3、DeepSeek-V4-Pro 提交生成，任务行记录对应 `model_code`，
   省略选择时使用 Hy4。
3. 同一首帧分别选择 MiniMax-Video-H3、PixVerse-Video-v6、Kling-Video-V3 生成，三条任务分别调用
   正确的 TokenHub 端点，结果均转存对象存储而非长期引用供应商临时 URL。
4. 批量配音选择 MiniMax-Speech 后，产出可播放 MP3，`audio_duration_sec` 来自供应商返回值并参与对齐。
5. 对已有配音的一集执行 Hy-ASR 校准，字幕文本与时间戳按返回句子更新；无配音素材时返回可读提示且不扣费。
6. 输入 BGM 描述并提交 MiniMax-Music，任务成功后返回 `bgm_asset_id`，合成设置与时间轴能读取该素材。
7. `ai.tokenhub_cn.api_key` / `ai.tokenhub_global.api_key` 为空或停用时，任务以能力不可用失败，
   日志与接口均不泄露密钥与 TokenHub 品牌；不存在 MOCK 或占位二进制兜底。
8. TokenHub 视频、语音和音乐的临时下载 URL 均在 Worker 内下载转存；供应商 URL 不写入前端永久状态。
9. 用户端模型选择器仅展示 `display_name`（及区域/价格），不展示中枢名或 `provider_code`。
10. 指定模型不可用时回退默认模型；重试 `fallback_model_flag=1` 可换备用模型。
11. 网关返回 `provider_not_configured` 时，任务失败原因是「模型服务暂未开通：deepseek，请稍后重试」
    这一类中文，日志保留 `code`，界面不出现 `http 503`。
12. 文本、图片或配音模型在网关不可用时，创建作品和后续生成在入队前被拒绝，提示里写明不能开始的步骤。

## 四、接口契约（snake_case）

- 既有 `GET /bgssai/user/ai-models/list?ability_type=` 增加 `ASR` / `MUSIC` 枚举。
- 剧本生成相关接口增加可选 `model_code`：
  `POST /bgssai/user/scripts/outline-generate`、`/generate`、`/rewrite`。
- 配音接口增加可选 `model_code`：
  `POST /bgssai/user/dubbings/submit`、`/regenerate`。
- `POST /bgssai/user/subtitles/transcribe`
  ```json
  { "episode_id": "e-20001", "model_code": "hy-asr-3.0-preview", "confirm_overwrite_flag": 1 }
  ```
  返回 `batch_id`、`task_id_list`、`task_count`。
- `POST /bgssai/user/films/bgm-generate`
  ```json
  { "work_id": "w-10001", "episode_id": "e-20001", "model_code": "minimax-music-v2.6",
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
- `short_work.compose_setting_json` 增加 `bgm_asset_id`、`bgm_volume`、`duck_flag` 键。
- ASR 结果复用 `short_subtitle`；音乐与配音结果复用 `short_asset(asset_type=AUDIO)`。
- `sys_secret` 增加空值种子 `ai.tokenhub_cn.api_key`、`ai.tokenhub_global.api_key`（及兼容旧键
  `ai.tencent_tokenhub.api_key`），由 Admin / 运维写入真实值。

## 七、原型

- 管理端：`docs/demo-static/web/admin/ai-models.html`。
- 用户端：`script-studio.html`、`storyboard.html`、`voice-studio.html`、`compose-export.html`。
- 跳转关系不新增页面，更新现有控件和两端 `PAGE-FLOW.html` 字段说明。

## 八、四层同步检查清单

- [x] 需求（本文件及受影响功能需求）
- [x] 原型
- [x] 设计与契约
- [x] 代码与数据库
