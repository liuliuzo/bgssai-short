# 剪辑工程导出（edit-project-export）

## 一、背景与目标

- 背景：本产品当前的交付物只有成片 mp4（烧字幕版 / 纯净版）与 SRT 字幕文件。但 AI 生成的镜头在
  剪辑点、语速、转场、花字、封面这些地方几乎不可能一次到发布水准，而这些恰恰是**剪辑活**，不是
  重新抽一遍视频能解决的。创作者的真实动作是把成片拿进剪映（或达芬奇 / Premiere）再修一版。
  现状下这一步要付出的代价是：把几十个镜头素材逐个下载，在剪辑软件里从零拖一遍时间轴，
  重新对一遍配音与字幕——平台已经算准的镜头顺序、时长、音画对齐、台词文本在跨出平台的那一刻
  全部丢失，等于让创作者把最费神的排轨工作重做一遍。**最后一公里断在平台边界上。**
- 目标：一次导出，创作者在自己的剪辑软件里打开就能看到已经排好的时间轴——视频轨按镜号排布、
  配音轨与画面对齐、字幕轨带好台词文本、BGM 轨铺在底下，直接开始改，而不是重新排。
  覆盖国内主力剪映与专业线达芬奇 / Premiere。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望把一集导出成剪映草稿，解压进剪映草稿目录就能打开继续剪，
  不必逐个下载素材重排时间轴。
- 用户故事二：作为 `USER`，我希望导出 FCPXML 或 EDL，把这一集接进达芬奇 / Premiere 的既有流程。
- 用户故事三：作为 `USER`，我希望**还没合成成片、还没过审**的时候就能导出去剪——因为我导出的目的
  正是「拿去改」，不是「拿去发」。
- 用户故事四：作为 `USER`，我希望导出包里带着素材，解压即用；素材太大时也能只拿工程文件加一份
  下载清单。

- 业务规则：
  1. **前置条件与成片合成不同，且刻意更宽**：导出**不要求**该集已合成成片、不要求镜头
     `shot_status=LOCKED`、不要求配音对齐、不要求合规审核通过。唯一硬前置是**该集至少有一个镜头
     具备可用视频**（`video_asset_id` 非空且资产未删除）。理由：剪辑工程的用途就是拿出去改，
     照搬合成的前置会把这个功能锁死在「已经不需要改」的时刻，等于废掉。
  2. **缺片镜头不阻断导出**：没有视频的镜头按镜号列进 `missing_shot_list` 返回并写入导出记录，
     工程时间轴上该镜头**留空档**（后续镜头依次前移，不插黑场、不插占位图）——插占位素材会让
     创作者在剪辑软件里误以为该镜已经生成。
  3. **三种工程格式，一次导出一种**（`project_format`）：
     - `JIANYING`：剪映草稿。子版本 `jianying_version` 取 `V6`（剪映 6.0 及以上，主文件
       `draft_info.json`）或 `V5`（剪映 5.x，主文件 `draft_content.json`），两者内容结构一致，
       仅主文件名不同；缺省 `V6`。面向中国大陆版剪映桌面端，**未验证 CapCut 兼容性**，
       界面按「仅支持剪映」表述。
     - `FCPXML`：Final Cut Pro X XML 1.9，达芬奇 Resolve 与 Final Cut Pro 可直接导入。
     - `EDL`：CMX3600 剪辑决策表，Premiere 与达芬奇通用交换格式。
  4. **两种素材模式**（`media_mode`）：
     - `WITH_MEDIA`（默认）：把该集的镜头视频、配音音频、BGM 一并打进 ZIP 的 `assets/` 目录，
       解压即用。
     - `LINK_ONLY`：只出工程文件，随包附 `manifest.csv`（列：`file_name`、`asset_id`、
       `asset_type`、`shot_no`、`duration_sec`、`download_url`），下载链接为 30 分钟有效签名 URL。
       适合素材已经在本地、或素材体积过大的情况。
  5. **剪映草稿必须写绝对路径**：剪映按草稿 JSON 里的绝对路径找素材，因此 `project_format=JIANYING`
     时 `draft_dir`（用户本机剪映草稿目录）**必填**，导出时把 `{draft_dir}/{草稿文件夹名}/assets`
     写进草稿 JSON 的素材路径；`FCPXML` / `EDL` 用相对路径，不要求 `draft_dir`。
     `draft_dir` 按用户维度记住上次填写值，下次导出自动带出。
  6. **时间轴与成片时间轴同源**：镜头顺序取 `shot_no` 升序，每镜起点为前序镜头 `shot_duration_sec`
     累加，配音与字幕落在所属镜头区间内，BGM 从 0 铺到全集时长——与
     `GET /bgssai/user/films/timeline` 是同一份计算，不另算一遍，避免两处口径漂移。
  7. **导出不调模型、不计费、不占配额**：本功能是本机文件打包，`short_ai_task` 不参与，
     不写 `ai_usage_record`，不扣 `user_quota`。走独立的 `short_edit_export` 记录异步执行，
     状态机 `QUEUED` -> `RUNNING` -> `READY` / `FAILED`，前端按 `export_id` 轮询。
  8. **导出不改变任何上游状态**：不置成片过期、不回落审核结论、不改镜头状态、不产生新素材版本。
     导出产物本身作为 `asset_type=EDIT_PROJECT` 的素材落 `short_asset`，不进合成时间轴、
     不进正式候选。
  9. **保留上限**：每集每格式最多保留最近 10 条 `READY` 记录，超出时软删最旧的记录与其产物，
     避免对象存储无限堆积。
  10. **体积闸门**：`WITH_MEDIA` 模式下素材总字节超过 2 GB 时拒绝导出（`4017`），提示改用
      `LINK_ONLY`；`LINK_ONLY` 无体积限制。
  11. **字幕落点**：`JIANYING` 与 `FCPXML` 把台词写入**可编辑文本轨**（创作者进剪辑软件后可直接
      改字改样式）；`EDL` 格式本身不支持文本轨，随包附 `subtitle.srt`。
  12. **命名规则**：导出包 `{work_title}_E{episode_no}_{project_format}.zip`；包内剪映草稿文件夹名
      `{work_title}_第{episode_no}集`；素材文件名 `shot_{shot_no}.mp4` / `voice_{shot_no}.mp3` /
      `bgm.mp3`。作品名中的路径分隔符与 `..` 一律替换为下划线。
  13. **下载**：产物走与成片一致的 30 分钟签名 URL，不暴露对象存储永久地址；链接过期后重新调
      `detail` 接口取新链接即可，无需重新导出。

## 三、验收标准

1. 一集有 20 个镜头、其中 18 个已出视频、2 个未出，导出成功，`missing_shot_list` 点名这 2 个镜号，
   工程时间轴上只有 18 段视频且首尾相接无黑场。
2. 该集 `compose_status=NONE`（从未合成）、`review_status=NOT_SUBMITTED`（从未提审）时，
   导出照常成功——前置条件不与合成看齐。
3. 该集一个镜头视频都没有时导出被拒（`4006`），提示「本集尚无已生成的镜头视频」。
4. `project_format=JIANYING` 且 `draft_dir` 为空时导出被拒（`50000001`），提示填写本机剪映草稿目录。
5. 导出 `JIANYING` + `V6`，解压包内含 `{作品名}_第N集/draft_info.json`、`draft_meta_info.json`
   与 `assets/` 目录；选 `V5` 时主文件名为 `draft_content.json`，其余一致。
6. 导出 `FCPXML`，产物为合法 XML，根元素 `fcpxml version="1.9"`，含 `asset-clip` 序列与
   `title` 文本元素，镜头数与已出视频镜头数一致。
7. 导出 `EDL`，产物首行为 `TITLE:`，事件行编号自 `001` 递增，时间码为 `HH:MM:SS:FF`（30 帧），
   每个事件带 `* FROM CLIP NAME:` 注释；包内另有 `subtitle.srt`。
8. `media_mode=WITH_MEDIA` 时 ZIP 内 `assets/` 含各镜视频与配音；`LINK_ONLY` 时不含媒体文件，
   但含 `manifest.csv`，其 `download_url` 列可直接下载到对应素材。
9. 素材总量超过 2 GB 时 `WITH_MEDIA` 被拒（`4017`）并提示改用 `LINK_ONLY`，`LINK_ONLY` 仍可导出。
10. 导出前后该集 `compose_status`、`review_status`、各镜 `shot_status` 逐字不变；用量流水不新增记录、
    配额余额不变。
11. 同一集同一格式连续导出 11 次后，列表只剩最近 10 条，最旧一条转为软删且其下载链接不再可用。
12. 导出记录状态从 `QUEUED` 经 `RUNNING` 到 `READY`，前端轮询可见；打包过程中出错时状态为 `FAILED`
    并给出可读原因（素材不可达 / 体积超限 / 格式生成失败）。

## 四、接口契约（snake_case）

- `GET /bgssai/user/edit-projects/precheck?episode_id=e-20001`
  ```json
  { "result": { "episode_id": "e-20001", "exportable_flag": 1,
      "ready_shot_count": 18, "total_shot_count": 20,
      "missing_shot_list": [ { "shot_no": 7, "shot_desc": "女主推门" },
                             { "shot_no": 15, "shot_desc": "雨中对峙" } ],
      "estimated_media_byte": 486539264, "media_limit_byte": 2147483648,
      "last_draft_dir": "C:\Users\demo\AppData\Local\JianyingPro\User Data\Projects\com.lveditor.draft" } }
  ```
- `POST /bgssai/user/edit-projects/export`
  ```json
  { "episode_id": "e-20001", "project_format": "JIANYING", "jianying_version": "V6",
    "media_mode": "WITH_MEDIA",
    "draft_dir": "C:\Users\demo\AppData\Local\JianyingPro\User Data\Projects\com.lveditor.draft" }
  ```
  响应 `{ "result": { "export_id": "x-60001", "export_status": "QUEUED" } }`
- `GET /bgssai/user/edit-projects/detail?export_id=x-60001`
  ```json
  { "result": { "export_id": "x-60001", "episode_id": "e-20001", "project_format": "JIANYING",
      "jianying_version": "V6", "media_mode": "WITH_MEDIA", "export_status": "READY",
      "clip_count": 18, "missing_shot_list": [ { "shot_no": 7 } ], "package_byte": 486539264,
      "asset_id": "a-54001", "package_name": "\u9000\u5a5a\u540e\u6211\u53d1\u8fbe\u4e86_E1_JIANYING.zip",
      "download_url": "https://obs.example.com/...", "expire_at": "2026-09-07T09:30:00Z",
      "fail_reason": null, "created_time": "2026-09-07T09:00:00" } }
  ```
- `GET /bgssai/user/edit-projects/list?episode_id=e-20001&page_num=1&page_size=20`
  （分页返回历史导出记录，字段同 `detail`，不含 `download_url`）
- 鉴权：全部需 `Jwttoken`（`@NeedAop`）；素材归属校验到 `user_id`。
- 错误码：`50000001` 请求参数非法（如剪映格式未填 `draft_dir`）/ `4003` 越权 /
  `4006` 本集尚无已生成镜头视频 / `4017` 素材体积超限须改用 `LINK_ONLY` / `429000` 限流。

## 五、流程 / 状态机（须配图）

导出管线与三种格式的分支见 [`flow-edit-project-export.html`](./flow-edit-project-export.html)。

`export_status`：`QUEUED` -> `RUNNING` -> `READY` / `FAILED`。`READY` 记录被保留上限挤出时置
`deleted_flag=1`，其产物素材同步软删。导出状态**不与** `compose_status`、`review_status`、
`shot_status` 发生任何联动。

## 六、数据模型影响

- 新增表 `short_edit_export`：`export_id`(PK 业务键)、`episode_id`、`work_id`、`user_id`、
  `project_format`（`JIANYING` / `FCPXML` / `EDL`）、`jianying_version`（`V5` / `V6`）、
  `media_mode`（`WITH_MEDIA` / `LINK_ONLY`）、`draft_dir`、`export_status`、`clip_count`、
  `package_byte`、`asset_id`、`package_name`、`missing_json`、`fail_reason`、`finished_time`、
  `deleted_flag`、`created_time`、`updated_time`。
- `short_asset`：`asset_type` 新增取值 `EDIT_PROJECT`（导出包），沿用既有列，不加列。
- 用户上次填写的剪映草稿目录取自该用户最近一条 `short_edit_export.draft_dir`，不单独建配置表。

## 七、原型

- 涉及端：web 用户端
- 页面：`../demo-static/web/user/edit-export.html`（自成片与提审页 `compose-export.html` 进入）
- 跳转关系：登记到 `../demo-static/web/user/PAGE-FLOW.html`
- 页面 -> API 映射：同步 `../api/page-api-mapping.md`

## 八、已知风险与验证缺口（交付前必须处理）

1. **剪映草稿格式未经真机验证**：剪映草稿是逆向格式、随客户端版本演进，本仓按剪映 5.x / 6.x 共同的
   结构生成 `draft_info.json` / `draft_content.json` 与 `draft_meta_info.json`，单元测试只能验证
   我们写出的结构符合设计（轨道数、时间轴对齐、素材绝对路径），**无法证明剪映一定能打开**。
   上线前必须在装有剪映桌面版的机器上实测一次：导出、解压到草稿目录、重启剪映、打开草稿，
   确认视频轨与配音轨对齐、字幕为可编辑文本、素材不显示离线。5.x 与 6.x 各验一次。
   实测发现字段缺漏时改 `JianyingDraftWriter` 并补测试，不要改需求把问题绕过去。
2. **FCPXML 与 EDL 是公开标准，风险低于剪映**，但仍建议在达芬奇上各导入一次确认时间码不错帧。
3. **本功能不承诺 CapCut 兼容**：CapCut 与剪映是不同产品，未做验证，界面与文档一律按「仅支持剪映」
   表述，不做没验证过的承诺。

## 九、四层同步检查清单

- [x] 需求（本文件 / `PRODUCT_VISION.md`）
- [x] 原型（`docs/demo-static/web/user/edit-export.html` 及 `PAGE-FLOW.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`、`docs/api/page-api-mapping.md`）
- [x] 代码与数据库（双端 `src/**`、用户端前端工程、`sql/DDL.sql`）
