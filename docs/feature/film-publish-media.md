# 成片发布到 bgssai-media（film-publish-media）

## 一、背景与目标

- 背景：成片合成与审核通过后，创作者仍需把可播放 MP4 推到 `bgssai-media`，才能在媒体侧播放与分发；short 与 media 约定了统一 ingest 契约，short 侧需先落地客户端。
- 目标：用户端一键「发布到 Media」；仅允许已合成且审核通过的成片；调用 media ingest 后落库最近一次结果；幂等重发安全。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事：作为 `USER`，我希望在成片就绪且审核通过后，把本集成片发布到 bgssai-media，并拿到 `media_id` / 播放链接。
- 业务规则：
  1. **闸门（从严）**：`short_film.compose_status=READY`、`expired_flag=0`、存在可播放 `film_asset_id`；且本集 `review_status=APPROVED`。任一不满足返回 `4006`。
  2. **视频地址**：对成片素材走既有资产预签名 / 可播放 URL（与 `films/detail.download_url` 同源能力）。
  3. **契约**：`POST {bgssai.media.base-url}/bgssai/user/media/ingest/short-drama`，请求头 `X-Bgssai-Ingest-Token`，Body snake_case（见第四节）。
  4. **幂等键**：`short:{work_id}:{episode_id}:{film_id}`；重复发布允许，由 media 侧幂等；short 更新最近一次结果。
  5. **配置**：`bgssai.media.base-url` 与 `bgssai.media.ingest-token` 写在各环境 `application-*.properties`（token 仅占位，不提交真实密钥）；运行期优先 properties，空则回退 `sys_secret.bgssai.media.ingest-token`。
  6. **失败**：media 不可达 / 非 2xx / 响应缺字段 → `BizException`（`502000` 或 `4006`），文案可读，并写入 `media_last_error`。

## 三、验收标准

1. READY + APPROVED 成片可发布，返回 `media_id`、`play_url`、`status`。
2. 未 READY / 已过期 / 未过审 / 无素材 → 拒绝且不调 media。
3. 重复点击发布不炸；落库最近一次 `media_id` / `status` / `published_at` / `last_error`。
4. 用户端成片区有「发布到 Media」按钮；成功展示播放链接与 media_id；失败由统一错误提示。
5. 单测覆盖 payload 字段与闸门（HTTP mock）；本地 media 404 不影响单测。

## 四、接口契约（snake_case）

### 用户端 → short

- 方法与路径：`POST /bgssai/user/films/publish-to-media`
- 请求体：
  ```json
  { "episode_id": "ep-dm-004-01", "film_id": "fl-dm-004-01" }
  ```
  `episode_id` 与 `film_id` 至少其一；同时给出时须属于同一成片。
- 响应 `result`：
  ```json
  {
    "film_id": "fl-dm-004-01",
    "episode_id": "ep-dm-004-01",
    "work_id": "w-dm-004",
    "media_id": "md-xxxx",
    "play_url": "https://media.example/play/md-xxxx",
    "status": "READY",
    "idempotency_key": "short:w-dm-004:ep-dm-004-01:fl-dm-004-01",
    "published_at": "2026-09-09T14:00:00Z"
  }
  ```
- 鉴权：需 `@NeedAop`（请求头 `Jwttoken`），角色 `USER`。
- 错误码：`4006` 闸门拒绝 / 参数非法；`502000` media 上游失败；`2002` token 失效。

### short → media（共享契约，MUST 与 media 侧一致）

```
POST {bgssai.media.base-url}/bgssai/user/media/ingest/short-drama
Header: X-Bgssai-Ingest-Token: {token}
```

```json
{
  "source_system": "bgssai-short",
  "source_work_id": "w-dm-004",
  "source_episode_id": "ep-dm-004-01",
  "source_film_id": "fl-dm-004-01",
  "title": "穿越之女帝归来 第1集 开场",
  "cover_url": "https://...",
  "video_url": "https://...presigned.mp4",
  "duration_sec": 88,
  "aspect_ratio": "9:16",
  "language": "zh-CN",
  "tags": [],
  "idempotency_key": "short:w-dm-004:ep-dm-004-01:fl-dm-004-01"
}
```

期望结果字段：`media_id`、`play_url`、`status`（可包在统一 `result` 内）。

## 五、流程

1. 用户在成片页点「发布到 Media」。
2. short 校验闸门 → 预签名 video_url（及可选封面）→ 组 payload → POST media。
3. 成功：更新 `short_film` 发布字段并返回；失败：写 `media_last_error` 并抛 BizException。

## 六、数据模型影响

- 表 `short_film` 新增列：
  - `media_id` VARCHAR(64)
  - `media_play_url` VARCHAR(1024)
  - `media_publish_status` VARCHAR(32) DEFAULT `NONE`
  - `media_published_at` DATETIME
  - `media_last_error` VARCHAR(1024)
- 同步 Entity / Example / Mapper XML（用户端与管理端三件套）。
- `sys_secret` 种子：`bgssai.media.ingest-token`（空值占位，Admin 可写）。

## 七、原型

- 涉及端：web 用户端
- 页面：`../demo-static/web/user/compose-export.html`（成片就绪区增加「发布到 Media」）
- 跳转：沿用既有出片页，无新路由
- 页面 → API：`../api/page-api-mapping.md` 的 `compose-export.html` 行追加本接口

## 八、联调与验证步骤

1. 配置 `bgssai.media.base-url` / `ingest-token`（或 Admin 写 `sys_secret`）。
2. 使用样例已过审成片（如 `fl-dm-004-01`）调 `POST /bgssai/user/films/publish-to-media`。
3. media 未就绪时：期望可读 `502000` 文案，库表 `media_last_error` 有记录。
4. 跑用户端单测：`FilmPublishToMediaServiceTest`（mock HTTP，不断言真实 media）。
5. 前端：登录 demo → 作品工作台 → 成片提审 →「发布到 Media」。

## 九、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`compose-export.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`、`page-api-mapping.md`）
- [x] 代码与数据库（用户端服务 / API / React / `sql/**` / 单测）
