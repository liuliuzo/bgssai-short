# 图生视频设计

需求：`docs/feature/image-to-video.md`。契约版本 `v0.2-validated-20261111-1137Z`。
接口：`docs/api/SHORT-PRODUCTION.md`。密钥：`docs/security/ai-provider-secrets.md`。

## 1. 边界

- 镜头文生视频仍走原来的 `SHOT` + `VIDEO` 任务和原来的供应商。图生视频不进入这条解析，避免被改写成中枢文生视频。
- 适配器 `IMAGE_CONDITIONED_VIDEO` 在注册表中直接返回，不因为 `base_url` 像中枢而被改道。
- `execute` 不发网络请求。生产调用只有 `submit` / `poll` / `fetch`，由任务入队后的短提交和独立轮询完成，避免四小时占用生成线程池。
- 模型目录和默认视频解析都跳过这个供应商。用户即使提交内部模型码，也得到「图生视频暂不可用」，而不是合成一条文生视频。

## 2. 调用

- 公司图生视频源站固定为 `https://chat.bgssai.com/i2v`（再拼 `/v1/videos`）。`https://chat.bgssai.com` 根路径、`/v1`、明文 http、以及模型机 8007 端口仍然拒绝。模型行 `base_url` 为空时用这一条公网前缀。其它主机仍只接受根路径。
- Bearer 先读 `ai.image_to_video.api_key`。该行为空时改读 `ai.openai_compat.api_key`。不读中枢键，不把密钥写进仓库。`secret_ref` 指向其它键时视为未配置。
- 提交是 multipart `POST {origin}/v1/videos`。字段为图片文件和 prompt、width、height、num_frames、fps、steps、seed。不传 model、duration、image_url、回调或音频。
- 只接受 HTTP 202，且 `id` 为 `i2v_` 加 32 位小写十六进制、`status_url` 恰好为 `/v1/videos/{id}`。否则记为结果不确定，不按返回的地址再请求。
- 状态与成片地址都由任务号在同一源站上拼出相对路径。HTTP 客户端不跟随重定向。
- 状态 HTTP 200 仍要看 `status`。`failed` 是终态，错误原文不入库、不返回前端。`completed` 且 `content` 恰好为 `/v1/videos/{id}/content` 才取成片。
- 成片必须是 `video/mp4`，并且前 64 字节内能看到 `ftyp`。409、429、503 和 5xx 表示还不能取，不把它当成新的提交。
- 轮询间隔 5、7、10、12、15 秒，之后保持 15 秒。已观测的 CPU 成片墙钟是 1258 秒，截止时间 14400 秒盖住这段等待。连接失败只延后，不重新提交。界面只说大约二十多分钟，不报精确剩余时间。
- 提交超时 60 秒，状态 30 秒，成片 120 秒。阶段上限 7200 秒，整单截止 14400 秒。没有任务号且提交已开始超过 180 秒，记为结果不确定。

## 3. 任务、权限与计费

- 不新增表。任务号、指纹、轮询时间和供应商任务号放在 `short_ai_task.request_json`。返回给前端的结果只有原图、成片、宽高、帧数、帧率、时长和 `ai_generated`。
- 上传和提交都重新按魔数和解码校验图片。存储键若是 URL 或 SVG，拒绝读取。
- 同一用户的 `client_request_id` 加图片与参数指纹做应用侧幂等。已有供应商任务号、结果不确定，或要求换模型时，不允许重试。
- 只有排队且还没有供应商任务号时可以取消。之后供应商没有取消接口。
- 费用只读模型行。`PER_CALL` 收一次；按时长的单位按帧数除以帧率向上取整。种子是 0 元。成功结算沿用原账本的任务幂等键。失败不进入结算，因此不发生退款。
- 成片写入前走与其它生成物相同的媒体校验。校验失败不结算，供应商任务号保留，所以不会自动再提交。
- 未定稿镜头在成功后写入 `video_asset_id` 和 `VIDEO_READY`。已定稿镜头不改。

## 4. 官方镜头

- 开发种子启用 `m-bgssai-image-to-video`，地址 `https://chat.bgssai.com/i2v`，超时 14400 秒，单价与文生视频同为每秒 20 分，按 25 帧 / 8 fps 向上取整计费。生产种子不写这一行。
- 官方按钮有真实 PNG/JPEG 时入队图生视频，并沿用 `OfficialShotVideoService.submit` 分配的 `batch_id` 以便 `batch-progress` 追踪整批进度；成功后才把 `video_asset_id` 换成新成片，同时清零 `stale_flag` 并调用 `FinishedFilmLifecycle.expireAfterUpstreamChange` 将旧成片置为过期。失败把 `VIDEO_GENERATING` 按仍绑定的真实成片或首帧退回。种子占位不算照片，改走文生视频。
- 供应商技术成片是 384×256、25 帧、8 fps、约 3 秒。界面照实写，不说成 9:16 或 6 秒。
- v0.2 在解码前拒绝超过 16000000 像素，返回 HTTP 400。完成前读取内容返回 409。应用侧仍先做同样的像素检查，并且只在 `completed` 且相对路径吻合时取成片。
- 供应商没有幂等、取消和任务保留期限。超时后的提交结果不确定时不自动重发。
- 本机若没有 ffprobe，真实成片即使返回也不能入库。单测用替身校验，不代替这条。
- 没有可访问的新版本时，浏览器端到端保持未验收。现有开发环境不是这条分支。
