# 配音与字幕（voice-subtitle）

## 一、背景与目标

- 背景：画面出完只完成一半。创作者接着遇到三件麻烦事：一是多角色配音音色对不上、同一角色在不同集
  换了嗓子；二是配音时长和镜头时长对不齐，声音说完画面还在、或者画面切了话没说完；
  三是字幕要逐句手打时间轴。这三件事单集就要耗掉一两个小时，且全是重复劳动。
- 目标：台词一键成声、按镜头时长自动对齐、字幕自动生成，单集配音与字幕耗时降到 5 分钟内，
  且同一角色全剧音色一致。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望角色卡上绑定的音色被全剧自动沿用，不必逐集逐句挑音色。
- 用户故事二：作为 `USER`，我希望配音自动和镜头时长对齐，对不齐的地方系统直接告诉我差几秒、
  并给出「调语速」或「改镜头时长」两种解法。
- 用户故事三：作为 `USER`，我希望字幕自动按台词与时间轴生成，能改样式、能单句重配。

- 业务规则：
  1. 配音以**镜头**为单位：一个镜头的 `dialogue_text` 生成一条 `AUDIO` 素材，
     音色取该镜头首位出场角色卡绑定的 `voice_id`；镜头内出现多角色对白时按台词行拆成多条音轨片段，
     各自取所属角色的音色。旁白使用作品级 `narrator_voice_id`。
  2. **时长对齐**：生成后比较 `audio_duration_sec` 与 `shot_duration_sec`：
     - 偏差在 ±0.5 秒内视为对齐（`align_status=ALIGNED`）；
     - 音频更短：在镜头尾部补静音（`PAD_SILENCE`），免费，系统自动执行，不重生成画面；
     - 音频更长且提速可解（1.25 倍上限内）：自动执行 `ADJUST_SPEED`（提高语速，免费，只重配音）；
     - 音频更长且提速不可解：给出 `EXTEND_SHOT`（延长镜头时长，需重新生成视频并产生费用，
       单独列出等用户确认后才执行）或修改台词两条出路。
     免费动作（`ADJUST_SPEED` / `PAD_SILENCE`）默认自动执行，只做结果告知（`auto_fixed_flag=1`），
     不要求逐镜确认；页面另提供「按建议一键处理全部未对齐」批量入口。免费项自动处理完成后
     该集合成入口即放开；仍处于 `MISALIGNED` 的镜头均为需付费确认的 `EXTEND_SHOT` 待办，
     不阻塞配音完成，但阻塞该集合成（见 `film-compose-export.md`），合成前须确认处理。
  3. **逐句重配**：支持对单个镜头或单条台词重新配音，可临时覆盖 `voice_speed`、`emotion`、
     `voice_id`（临时覆盖不写回角色卡，除非显式点「更新到角色卡」）。
  4. **字幕**：按台词与对齐后的时间轴自动生成字幕轨（`SUBTITLE` 素材，SRT 结构化存储），
     单条字幕最长 15 字，超长自动断句为两条；字幕样式（字号 / 颜色 / 描边 / 位置）为作品级设置，
     合成时烧录（见 `film-compose-export.md`）。
  5. **试听**：镜头级与整集连续试听（按镜头顺序拼接播放），无需等待合成。
  6. 配音走统一任务体系（`task_type=VOICE`），费用、重试、批量、断点续跑口径与 `shot-generation.md` 一致。
  7. `dialogue_text` 为空的镜头（空镜）不产生配音任务，但参与整集时间轴排布。

## 三、验收标准

1. 对一集 42 个镜头点「批量配音」，仅对有台词的镜头产生任务；同一角色在全部镜头中音色一致。
2. 配音完成后每镜展示 `audio_duration_sec` 与 `shot_duration_sec` 对比及 `align_status`；
   免费解法（补静音 / 提速）自动执行并以「已自动处理」告知；仍未对齐的镜头集中列出，
   均附「延长镜头 / 改台词」处理入口与预估费用。
3. `ADJUST_SPEED` 可在语速上限内对齐时自动执行；无法在上限内对齐时不自动动手，提示改用
   `EXTEND_SHOT` 或改台词。
4. 选择 `EXTEND_SHOT` 时明确提示需重生成该镜视频及预估费用，确认后自动提交对应 `VIDEO` 任务。
5. 字幕自动生成，单条不超过 15 字，时间轴与配音一致；导出的 SRT 可被通用播放器正确加载。
6. 单条台词重配后只有该条音频与对应字幕变化，其它片段逐字不变。
7. 仅当存在需付费确认的 `MISALIGNED` 镜头（待 `EXTEND_SHOT`）时，该集合成入口禁用并给出
   待处理镜头数；免费可解项自动处理完成后合成入口即放开。
8. 点「按建议一键处理全部未对齐」：免费项全部按建议执行完毕，需付费的 `EXTEND_SHOT` 项
   单独列出并显示各镜预估费用，等用户确认。

## 四、接口契约（snake_case）

- `POST /bgssai/user/dubbings/submit`（`episode_id` + 可选 `shot_id_list`；返回 `batch_id`，走统一任务体系）
- `GET /bgssai/user/dubbings/list?episode_id=e-20001`
  ```json
  { "result": { "dubbing_list": [
      { "shot_id": "s-40001", "shot_no": 1,
        "line_list": [
          { "line_no": 1, "character_id": "c-30001", "character_name": "沈知微",
            "voice_id": "v-female-cold-01", "voice_speed": 1.0, "emotion": "COLD",
            "line_text": "退婚书我签，但有个条件。",
            "audio_asset_id": "a-51001", "audio_duration_sec": 3.2 } ],
        "shot_duration_sec": 4.0, "audio_duration_sec": 3.2,
        "align_status": "ALIGNED", "align_status_name": "已对齐",
        "align_gap_sec": -0.8, "suggest_action": "PAD_SILENCE", "auto_fixed_flag": 1 } ],
      "misaligned_count": 0 } }
  ```
- `POST /bgssai/user/dubbings/regenerate`（`shot_id` + 可选 `line_no` + `voice_id` / `voice_speed` / `emotion`）
- `POST /bgssai/user/dubbings/align`（`shot_id` + `align_action`：`ADJUST_SPEED` / `EXTEND_SHOT` / `PAD_SILENCE`）
- `POST /bgssai/user/dubbings/align-batch`（`episode_id`；按各镜 `suggest_action` 执行全部免费未对齐项，
  返回已处理清单与待确认的 `EXTEND_SHOT` 清单及各镜 `estimated_cost_cent`）
- `POST /bgssai/user/dubbings/apply-voice-to-character`（把临时音色参数写回角色卡）
- `GET /bgssai/user/voices/list`（可用音色库：`voice_id`、`voice_name`、`gender`、`age_range`、`style`、`preview_url`）
- `GET /bgssai/user/subtitles/detail?episode_id=e-20001`（字幕条目 + 时间轴）
- `POST /bgssai/user/subtitles/regenerate` · `POST /bgssai/user/subtitles/update`（单条文本 / 时间微调）
- `POST /bgssai/user/subtitles/style-save`（作品级字幕样式：`font_size`、`font_color`、`stroke_color`、`position`）
- 鉴权：需 `Jwttoken`；按 JWT `user_id` 校验归属。
- 错误码：`4003` 越权 / `4006` 剧本或分镜未就绪即配音 / `4007` 余额不足 / `429` 限流。

## 五、流程 / 状态机（须配图）

配音生成、时长对齐与字幕派生链路见 [`flow-post-production.html`](./flow-post-production.html)。

`dubbing_status`（剧集级）：`NONE` → `GENERATING` → `DRAFT`（已出音频，存在未对齐项）→
`READY`（全部镜头 `ALIGNED` 且字幕已生成）。
`align_status`（镜头级）：`PENDING` → `ALIGNED` / `MISALIGNED`；免费解法（`PAD_SILENCE` /
可解的 `ADJUST_SPEED`）由系统自动执行后直接落 `ALIGNED` 并记 `auto_fixed_flag=1`，
仍为 `MISALIGNED` 的均为待付费确认项。

## 六、数据模型影响

- 新增表 `short_dubbing`：`dubbing_id`(PK)、`shot_id`、`episode_id`、`work_id`、`user_id`、`line_no`、
  `character_id`、`voice_id`、`voice_speed`、`emotion`、`line_text`、`audio_asset_id`、
  `audio_duration_sec`、`align_status`、`align_gap_sec`、`auto_fixed_flag`、`deleted_flag`、时间戳。
- 新增表 `short_subtitle`：`subtitle_id`(PK)、`episode_id`、`work_id`、`user_id`、`subtitle_no`、
  `start_ms`、`end_ms`、`subtitle_text`、`shot_id`、`deleted_flag`、时间戳。
- 作品级新增列：`short_work.narrator_voice_id`、`short_work.subtitle_style_json`。
- 索引：`short_dubbing(episode_id, shot_id, line_no)`、`short_subtitle(episode_id, subtitle_no)`。

## 七、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/voice-studio.html`](../demo-static/web/user/voice-studio.html)
  （角色音色绑定 + 逐镜配音列表 + 对齐处理 + 字幕表与样式）。
- 跳转关系：`work-detail.html` / `storyboard.html` → `voice-studio.html` → `compose-export.html`，
  登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`docs/demo-static/web/user/voice-studio.html` 及 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
