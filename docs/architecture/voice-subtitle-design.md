# 配音与字幕详设

对齐：`docs/feature/voice-subtitle.md`。AI/OBS：`ai-provider-obs-design.md`。

## 范围

镜头级配音（`VOICE` 任务）、时长对齐、字幕生成与样式、试听；阻塞合成的 `MISALIGNED` 门禁。

## 表

`short_dubbing`、`short_subtitle`；作品列 `narrator_voice_id`、`subtitle_style_json`。

## 流程

1. `POST /dubbings/submit`：按有台词镜头入队 VOICE；音色取角色卡 `voice_id` 或旁白。
2. Worker 调 `AiProvider.VOICE`，音频进 OBS，写 `short_dubbing`。
3. 对齐：|audio-shot|≤0.5s → ALIGNED；短则 PAD_SILENCE；长且 ≤1.25x → ADJUST_SPEED；否则 MISALIGNED+EXTEND_SHOT。
4. 字幕：按对齐后时间轴断句（≤15 字）写 `short_subtitle`；作品级样式 JSON。
5. 剧集 `dubbing_status`：GENERATING → DRAFT（有 MISALIGNED）→ READY。

API 见 `page-api-mapping.md` voice-studio 行；契约并入 `docs/api/SHORT-PRODUCTION.md` 扩展节。
