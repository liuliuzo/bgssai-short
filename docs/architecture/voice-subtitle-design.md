# 配音与字幕详设

对齐：`docs/feature/voice-subtitle.md`。AI/OBS：`ai-provider-obs-design.md`。

## 范围

镜头级配音（`VOICE` 任务）、时长对齐、Hy-ASR 校准、字幕生成与样式、试听；阻塞合成的
`MISALIGNED` 门禁。

## 表

`short_dubbing`、`short_subtitle`；作品列 `narrator_voice_id`、`subtitle_style_json`。

## 流程

1. `POST /dubbings/submit`：按有台词镜头入队 VOICE；音色取角色卡 `voice_id` 或旁白。
2. Worker 调 `AiProvider.VOICE`，音频进 OBS，写 `short_dubbing`；音频时长以供应商返回值为真值。
3. 对齐：|audio-shot|≤0.5s → ALIGNED；短则 PAD_SILENCE；长且 ≤1.25x → ADJUST_SPEED；否则 MISALIGNED+EXTEND_SHOT。
4. 字幕：按对齐后时间轴断句（≤15 字）写 `short_subtitle`；作品级样式 JSON。
5. 剧集 `dubbing_status`：GENERATING → DRAFT（有 MISALIGNED）→ READY。
6. `POST /subtitles/transcribe`：确认覆盖规则后，按已有配音入队 ASR；Worker 用句级时间码替换对应镜头
   的字幕并保留镜头在本集的绝对时间偏移。无配音素材直接拒绝，不创建任务、不计费。

API 见 `page-api-mapping.md` voice-studio 行；契约并入 `docs/api/SHORT-PRODUCTION.md` 扩展节。

## 字幕分段时间边界

`DubbingService.regenerateSubtitles` 按 `shot_no` 排序镜头，按 `line_no` 读取该镜配音；先将本镜
全部非空台词拆成每段最多 15 个 Unicode code point 的片段，再按总片段数 `n` 分配时间。
片段 `i` 使用 `[shot_start + floor(duration_ms*i/n), shot_start + floor(duration_ms*(i+1)/n))`，
采用 long 中间乘积避免整除累计误差；最后边界等于镜头结束。不得以配音行数代替片段数，也不得
加上会让总时长超出镜头的每段最小时长。空镜推进累计时钟；正文、标点及配音记录保持不变。
零/负窗口或无法为每片段分配至少 1 ms 的异常输入拒绝生成，不能写入无效字幕。
全部派生行先通过边界检查，再替换已有字幕，避免验证失败先清空旧字幕。

`finishTranscription` 在已钳制到本镜的 ASR 句级区间内采用同一累计分配法，去除每片段最少
100 ms 导致的倒置/越界。ASR 文本来源和原有覆盖确认不变。

已有正式 `subtitleRegenerate(SubtitleRegenReq)` 入口保留 `requireEpisode` 归属校验及事务，
只重建派生字幕，不调用模型、不提交队列、不修改音频/对白/镜长。`align_dubbing` 对应的配音
对齐入口不会无条件覆盖已有字幕；需显式调用字幕重建接口。此次不改变 API 字段或数据库结构。
