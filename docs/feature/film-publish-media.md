# 成片发布到 bgssai-tube（film-publish-media）

## 一、背景与目标

- 背景：成片合成与审核通过后，创作者仍需把可播放 MP4 推到 `bgssai-tube`，才能在媒体侧播放与分发；short 与 tube 约定了统一 ingest 契约，short 侧落地客户端。
- 目标：单集一键发布 + 作品批量发布 READY 集；闸门从严；幂等重发安全；可读错误与 publish_log。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事：作为 `USER`，我希望在成片就绪且审核通过后，把本集成片（或整部作品全部 READY 集）发布到 bgssai-tube-pc，并看到作品号与播放链接。界面不出现媒体产品名。
- 业务规则：
  1. **闸门（从严）**：`short_film.compose_status=READY`、`expired_flag=0`、存在可播放 `film_asset_id`；且本集 `review_status=APPROVED`。任一不满足返回 `4006`。
  2. **视频地址**：对成片素材走既有资产预签名 / 可播放 URL（与 `films/detail.download_url` 同源能力）。
  3. **契约（MUST 与 tube 一致）**：见第四节 short → media。
  4. **幂等**：键为 `short:{work_id}:{episode_id}:{film_id}`，写入 Body 并带 `Idempotency-Key` 头。
     Body 同时带 `status=READY`、`approved=true`（与 Tube `ShortDramaPublishJob` 一致）。
     同一成片已 READY 且已有 `media_id` / `play_url` 时，再次发布 **Short 侧回放**（不调 Tube，
     `replayed=true`），避免双目录。`force=true` 才用同一键再 POST（期望 Tube upsert 同一 `media_id`）。
     重新合成成功后，Short 把 `media_publish_status` 置回 `NONE` 并清空当前 `play_url`，
     下次发布必须再 POST（同一键，由 Tube UNIQUE upsert），不能回放旧目录。
     PENDING / FAILED 允许重试，但 PENDING、PROCESSING、QUEUED 与 UNKNOWN 都先按同一幂等键查询 Tube。查到同一 `media_id` 就采纳；查询没有可读结果，或返回的作品号与已记录的不一致时记 UNKNOWN，不再 POST。查无且本地还没有 `media_id` 才允许再 POST 一次；已有 `media_id` 时不再另建作品。UNKNOWN 不是成功也不是失败，批量计入 `unknown`。Tube 对端 MUST UNIQUE upsert，见 [`../api/short-to-media-publish.md`](../api/short-to-media-publish.md)。
  5. **配置**：`bgssai.tube.base-url` 与 `bgssai.tube.ingest-token` 写在各环境 `application-*.properties`（token 仅占位，不提交真实密钥）；运行期优先 properties，空则回退 `sys_secret.bgssai.tube.ingest-token`。
     **主机**：发布目标是 bgssai-tube-pc 的 `POST /bgssai/user/tube/ingest/short`。本仓不分配、不编造主机 IP。dev/prod 的 base-url 是约定域名；地址为空时发布返回 `4006`，文案写明未配置，不返回作品号。域名不可达时返回 `502000` / `504000`，同样不写成成功。
  6. **失败**：tube 不可达 / 非 2xx / 响应业务码（含 6xxxx）/ 响应缺字段 → `BizException`（`502000`、`504000` 或 `4006`），文案可读，并写入 `media_last_error` + `media_publish_log`。
  7. **预检**：`GET .../preflight` 检查配置与闸门；传 `work_id` 时附带批量候选数；不调用 tube。
     `warnings` 只写主机或令牌未配置。本集尚未合成时没有成片行，写入 `gate_reason`，不写 `film not found`，也不放进 `warnings`。
     剧集已是 `READY` 或已有 `film_asset_id` 但没有成片行时，`film_record_missing=true`，请重新合成，不插入空成片。
  8. **UX**：发布 loading；成功展示作品号与播放链接（打开/复制），文案不写媒体产品名；成片区展示最近一次 publish_log。响应字段仍为 `media_id` / `play_url`。
  9. **可靠性**：瞬时 HTTP 5xx 在同一请求里可再试 1 次，幂等键不变。超时不再自动重 POST；结果记 UNKNOWN，下次发布先查状态。连接被明确拒绝仍是失败。
  10. **批量**：`POST .../batch` 按剧集顺序发布本作品全部 READY+APPROVED 成片，返回进度字段。
  11. **下架 / 撤回（skip）**：当前 bgssai-tube 未提供 short unpublish/retract 契约；本仓**不实现**下架按钮与 API。待 tube 侧补齐后再开需求。


## SHORT-01 交接状态（商业门禁）

| Media `status` | Short 落库 | `commercial_ready` | 含义 |
| --- | --- | --- | --- |
| READY / SUCCEEDED | READY | true | 发布成功 |
| PENDING / PROCESSING / QUEUED | PENDING | false | 已受理未就绪，不是 FAIL。Tube 可以不给 `media_id` / `play_url` |
| FAILED / ERROR，或信封 `code=422` | FAILED + 抛错 | false | 明确失败。文案用 Tube 的 `message`，不说成「缺字段」 |
| 无法识别的状态、超时、响应丢失 | UNKNOWN | false | 结果未知。不重复发布，不当成成功或失败。批量计入 `unknown`，不计入 `succeeded` / `failed` |

未配置 Tube、或返回 `stub://` 等占位 `play_url`：失败，禁止冒充成功。详见 [short-01-commercial-gate.md](short-01-commercial-gate.md)。

## 三、验收标准

1. READY + APPROVED 成片可发布，返回 `media_id`、`play_url`、`status`。
2. 未 READY / 已过期 / 未过审 / 无素材 → 拒绝且不调 tube；文案可读。
3. 重复点击发布不炸：已 READY 回放既有 `media_id`，不新建目录；落库最近一次结果与 `media_publish_log`。
4. 用户端成片区有「发布到 Tube」与「批量发布 READY 集」；成功可打开/复制播放链接。
5. 缺 base-url / ingest-token 时 preflight 的 `warnings` 只写配置。缺 ingest-token 时「发布到 Tube」停用，按钮旁写明「未配置 Tube 令牌」，不编造令牌，也不在成片页顶部出黄条。尚未合成不写入 `warnings`。
6. 批量返回 `total` / `succeeded` / `failed` / `progress_percent` / `item_list`。
7. 单测覆盖 payload、闸门、重试；本地 media 404 不影响单测。
8. 文档明确 unpublish 为 skip。

## 四、接口契约（snake_case）

### POST /bgssai/user/films/publish-to-media

```json
{ "episode_id": "ep-dm-004-01", "film_id": "fl-dm-004-01", "force": false }
```

`episode_id` 与 `film_id` 至少其一。`force` 可选，默认 false。响应 `result`：

```json
{
  "film_id": "fl-dm-004-01",
  "episode_id": "ep-dm-004-01",
  "work_id": "w-dm-004",
  "media_id": "md-xxxx",
  "play_url": "https://media.example/play/md-xxxx",
  "status": "READY",
  "idempotency_key": "short:w-dm-004:ep-dm-004-01:fl-dm-004-01",
  "replayed": false,
  "published_at": "2026-09-09T14:00:00Z",
  "http_status": 200,
  "attempts": 1,
  "publish_log": {
    "idempotency_key": "short:w-dm-004:ep-dm-004-01:fl-dm-004-01",
    "media_id": "md-xxxx",
    "play_url": "https://media.example/play/md-xxxx",
    "status": "READY",
    "http_status": 200,
    "error": "",
    "attempts": 1,
    "published_at": "2026-09-09T14:00:00Z"
  }
}
```

鉴权：`@NeedAop` + `Jwttoken`，角色 `USER`。错误：`4006` / `502000` / `504000` / `2002`。

### POST /bgssai/user/films/publish-to-media/batch

```json
{ "work_id": "w-dm-004" }
```

响应 `result`：

```json
{
  "work_id": "w-dm-004",
  "total": 2,
  "succeeded": 2,
  "failed": 0,
  "skipped": 0,
  "progress_percent": 100,
  "item_list": [
    {
      "progress_index": 1,
      "progress_total": 2,
      "film_id": "fl-1",
      "episode_id": "ep-1",
      "work_id": "w-dm-004",
      "ok": true,
      "media_id": "md-1",
      "play_url": "https://media.example/play/md-1",
      "status": "READY",
      "idempotency_key": "short:w-dm-004:ep-1:fl-1",
      "replayed": false,
      "error": ""
    }
  ]
}
```

候选：本作品剧集按 `episode_no` 升序；每集取最新一条 `compose_status=READY` 且未过期、有素材、本集 `review_status=APPROVED` 的成片。单集失败不中断整批。

### GET /bgssai/user/films/publish-to-media/preflight

查询参数：`episode_id` / `film_id` / `work_id`（均可选）。不调用 tube。

字段：`config_ready` `base_url_configured` `token_configured` `film_present` `film_record_missing`
`film_ready` `review_approved` `publishable` `gate_reason` `warnings[]`，可选 `publish_log`；带 `work_id` 时另有
`batch_ready_count` `batch_candidate_list`。

`warnings` 只放主机或令牌未配置。本集尚未合成时 `film_present=false`、`film_record_missing=false`，
`gate_reason` 为「本集尚未合成，合成完成后再发布」，不出现 `film not found`。剧集已是 `READY` 或已有
`film_asset_id` 但没有成片行时，`film_record_missing=true`，`gate_reason` 为「成片记录缺失，请重新合成后再发布」。

### short → media（共享 ingest 契约，MUST 逐字段一致）

```
POST {bgssai.tube.base-url}/bgssai/user/tube/ingest/short
Header: Content-Type: application/json
Header: X-Bgssai-Ingest-Token: {token}
Header: Idempotency-Key: short:{work_id}:{episode_id}:{film_id}
```

请求 Body（snake_case，字段集合固定）：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `source_system` | string | 是 | 固定 `bgssai-short` |
| `source_work_id` | string | 是 | short 作品 ID |
| `source_episode_id` | string | 是 | short 剧集 ID |
| `source_film_id` | string | 是 | short 成片 ID |
| `title` | string | 是 | 展示标题 |
| `cover_url` | string | 否 | 有封面才带；空白省略字段 |
| `video_url` | string | 是 | 可播放 / 预签名 MP4 URL |
| `duration_sec` | int | 是 | 秒；缺省 0 |
| `aspect_ratio` | string | 是 | 固定 `9:16` |
| `language` | string | 是 | 固定 `zh-CN` |
| `tags` | string[] | 是 | 可空数组；常放 genre |
| `status` | string | 是 | 固定 `READY`（审核通过的成片包） |
| `approved` | boolean | 是 | 固定 `true`。`false` 会被 Tube 以 `422` 拒绝 |
| `idempotency_key` | string | 是 | `short:{work_id}:{episode_id}:{film_id}` |

期望响应（可包在统一 `result` 内，或顶层）：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `media_id` | string | 是 | media 侧媒体 ID |
| `play_url` | string | 是 | 播放页 / 直链 |
| `status` | string | 否 | `READY` 才要求 `media_id` + `play_url`。`PENDING` 允许两者为空，Short 落库 PENDING，`commercial_ready=false`，不抛错。`FAILED` 或信封 `code=422` 抛错 |
| `playable` | boolean | 否 | 显式 `false` 且 status 为 READY 时不得当成可播放成功 |
| `message` | string | 否 | PENDING / FAILED 的原因，写入 `media_last_error` |

统一信封若带 `code`：仅 `0` / `200` / `OK` 视为成功；`6xxxx` 等业务码按上游失败处理（文案可读，不重试）。

### 下架 / 撤回（skip）

media 侧暂无 `unpublish` / `retract` 共享契约 → **本仓不实现**。后续若 tube 提供路径与幂等语义，再补 short API 与 UX。

## 五、流程

配图：[`flow-film-publish-media.html`](flow-film-publish-media.html)。

1. 进入成片页 → preflight（配置 / 闸门 / 批量候选）。
2. 单集：校验闸门 → 若已 READY 且同键则回放 → 否则预签名 → POST media（可重试一次）→ 落库。
3. 批量：枚举候选 → 逐集调用单集发布（含回放）→ 汇总进度。
4. 失败：写 `media_last_error` + `media_publish_log`，抛 BizException（批量则记入 item）。

## 六、数据模型影响

- 表 `short_film` 列：
  - `media_id` VARCHAR(64)
  - `media_play_url` VARCHAR(1024)
  - `media_publish_status` VARCHAR(32) DEFAULT `NONE`
  - `media_published_at` DATETIME
  - `media_last_error` VARCHAR(1024)
  - `media_publish_log` LONGTEXT（JSON：idempotency_key / media_id / play_url / status / http_status / error / attempts / published_at / replayed）
- 同步 Entity / Mapper XML（用户端与管理端）。
- `sys_secret` 种子：`bgssai.tube.ingest-token`（空值占位）。

## 七、原型

- 涉及端：web 用户端
- 页面：`../demo-static/web/user/compose-export.html`
- 「发布到 Tube」+「批量发布 READY 集」+ 最近 publish_log / 批量进度
- 页面 → API：`../api/page-api-mapping.md`

## 八、联调与验证步骤（smoke）

1. 配置 `bgssai.tube.base-url` / `ingest-token`（或 Admin 写 `sys_secret`）；缺令牌时「发布到 Tube」停用，按钮旁短提示「未配置 Tube 令牌」，成片页顶部无黄条，合成与提审仍可用。
2. READY+APPROVED 单集发布：成功可打开/复制 `play_url`。
3. 未 READY / 未过审：拒绝且不调 tube。
4. Tube 宕机：可读 `502000`，库表有 `media_last_error` / `media_publish_log`。
5. 重复发布：已 READY 回放既有目录，不二次 ingest；`force=true` 才再 POST。
6. 批量发布 READY 集：有进度汇总。
7. `mvn -Dtest=FilmPublishToMediaServiceTest,RestMediaIngestClientTest test`。
8. 前端：登录 demo → 作品工作台 → 成片提审 → 单集 / 批量发布。
9. 契约与 Tube 对端验收：[`../api/short-to-media-publish.md`](../api/short-to-media-publish.md)。

## 九、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`compose-export.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`、`page-api-mapping.md`、`short-to-media-publish.md`）
- [x] 代码与数据库（用户端服务 / API / React / `sql/**` / 单测）
