# Short → Media 发布契约（幂等交接）

本文件是 **bgssai-short（本仓）** 对 **bgssai-media** 的 ingest 调用真值。
Media 仓对等文件：`docs/contracts/short-to-media-publish.md`。字段必须逐字一致。

## 何时发布

只在成片 **export / compose READY** 且本集审核 `APPROVED` 时调用 Media。剪辑工程导出
（`short_edit_export`）与本交接无关，不作为发布闸门。

闸门（任一不满足则 **不调 Media**）：

- `short_film.compose_status = READY`
- `expired_flag = 0`
- 存在可播放 `film_asset_id`，解析出的 `video_url` 为 `http(s)`，且不是 stub/占位
- 本集 `review_status = APPROVED`
- 已配置 `bgssai.media.base-url` 与 ingest-token（properties 占位或 `sys_secret`）

## Ingest 调用

```
POST {bgssai.media.base-url}/bgssai/user/media/ingest/short
Content-Type: application/json
X-Bgssai-Ingest-Token: {token}
Idempotency-Key: short:{work_id}:{episode_id}:{film_id}
```
> 注：旧路径 `/bgssai/user/media/ingest/short-drama` 已弃用（保留为兼容量）。

`X-Bgssai-Ingest-Token` 为 Media 当前鉴权头。`Idempotency-Key` 与 Body 字段同值；
Media 今日按 Body `idempotency_key` UNIQUE upsert，头供对端后续闸门复用。

Body（snake_case，`additionalProperties=false`）：

| 字段 | 必填 | 值 |
| --- | --- | --- |
| `source_system` | 是 | 固定 `bgssai-short` |
| `source_work_id` | 是 | short 作品 ID |
| `source_episode_id` | 是 | short 剧集 ID |
| `source_film_id` | 是 | short 成片 ID |
| `title` | 是 | 展示标题 |
| `cover_url` | 否 | 有封面才带 |
| `video_url` | 是 | 可播放 http(s) MP4 |
| `duration_sec` | 是 | 秒；缺省 0 |
| `aspect_ratio` | 是 | `9:16` |
| `language` | 是 | `zh-CN` |
| `tags` | 是 | 可空数组 |
| `status` | 是 | 固定 `READY` |
| `approved` | 是 | 固定 `true` |
| `idempotency_key` | 是 | `short:{work_id}:{episode_id}:{film_id}` |

期望响应（可包在统一 `result` 内）：`status`（READY / PENDING / FAILED），以及 READY 时的 `media_id`、`play_url`。
`playable=false` 不得当成可播放成功。

- **READY**：必须有 `media_id` 与 http(s) `play_url`。`commercial_ready=true`。
- **PENDING**：Media 可以不给 `media_id` / `play_url`（素材尚未探活）。Short 落库 `PENDING`，`commercial_ready=false`，**不**记成 FAILED，**不**补造播放地址。
- **FAILED** 或信封 `code=422`：抛错，落库 FAILED，文案用 Media `message`。

统一信封 `code` 仅 `0` / `200` / `OK` 为成功。Media 明确失败用 body `code=422`（HTTP 仍可能是 200）。

## 幂等

1. **Short 侧回放**：同一成片已 `media_publish_status=READY` 且已有 `media_id` / `play_url` /
   同一幂等键时，再次发布 **不调 Media**，返回既有目录项，`replayed=true`。
2. **force=true**：跳过回放，用同一幂等键再 POST Media（期望 upsert，同一 `media_id`）。
3. **PENDING / FAILED**：允许再调 Media，键不变。
4. **Media 对端 MUST**：`media_ingest_log.idempotency_key` UNIQUE；重复键 upsert 元数据 / URL，
   **禁止**再插一条目录。返回同一 `media_id`。首发并发靠 UNIQUE + 回读赢家。

## Short 落库

`short_film`：`media_id` / `media_play_url` / `media_publish_status` / `media_published_at` /
`media_last_error` / `media_publish_log`（含 `idempotency_key`、`replayed`）。

## Media 对端验收（本仓不能改 media）

| 项 | 期望 | 现状（develop 已读） |
| --- | --- | --- |
| 路径 `POST /bgssai/user/media/ingest/short` | 必须 | 已有（`/short-drama` 兼容保留） |
| Body 字段与幂等键格式 | 必须 | 已有 |
| 同键 upsert，同一 `media_id` | 必须 | `ShortDramaContractService` + UNIQUE |
| READY 才给可播 `play_url` | 必须 | MEDIA-01 |
| FAILED 不冒充 READY | 必须 | HTTP 422 / `playable=false` |
| 鉴权 `X-Bgssai-Ingest-Token` | 必须 | 已有；亦接受 Bearer / `X-Ingest-Token` |
| 读 `Idempotency-Key` 头 | 建议 | 尚未；Body 已够用 |
| 直播 Short 工作室 → 播放器 E2E | 联调 | **NOT_VERIFIED**。MEDIA_* 主机未在本仓分配，不编造 IP。base-url 为空或主机不可达时发布失败 |

对端无需为本 PR 改契约即可联调。若 Media 未守 UNIQUE，重复发布会双目录——Short 侧 READY 回放可挡住二次点击，挡不住首发并发；并发必须靠 Media UNIQUE。


## 目录与版本（2026-10-17）

交付 Media 时 payload 含 `catalog_dir=short`、`product_line=short`、`publish_version=<film_id>`，与作品/集数/发布状态一并保留。重复交付同 idempotency_key 不产生重复作品。
