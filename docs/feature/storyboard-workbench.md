# 分镜工作台（storyboard-workbench）

## 一、背景与目标

- 背景：剧本到画面之间隔着一道专业墙——分镜。创作者要把台词切成镜头，为每个镜头定景别、机位、运镜、
  时长，再把这些翻译成一段模型看得懂的提示词。不懂镜头语言的人卡在这里，懂的人也要为一集 40 多个镜头
  手写 40 多段提示词，一集耗掉大半天。这是全链路里**最耗人力**的一环。
- 目标：剧本一键拆成镜头表，每镜自动带齐镜头语言参数与合成好的提示词，人只做「改」不做「写」；
  一集 40 镜的分镜从数小时压缩到 10 分钟内完成审阅与微调。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望点一下就把定稿剧本拆成镜头表，每镜带景别、运镜、时长、台词和出场角色。
- 用户故事二：作为 `USER`，我希望看到每镜最终发给模型的提示词，并能在不懂英文提示词语法的情况下
  用中文选项（景别 / 运镜 / 光线 / 情绪）把它调对。
- 用户故事三：作为 `USER`，我希望能拆镜、并镜、调整顺序、插入空镜，而不是被 AI 的拆法锁死。

- 业务规则：
  1. 拆镜输入是**已定稿剧本**（`script_status=READY`），输出是该集的 `short_shot` 列表；
     剧本未定稿时拆镜入口禁用。重复拆镜为**全量覆盖**，须二次确认；
     重复拆镜时已定稿（`LOCKED`）镜头**一律保留**（按 `scene_no` + `line_no` 对齐），无需勾选。
  2. 拆镜规则：一个镜头默认承载 1 至 3 句连续台词或一个动作单元，`shot_duration_sec` 取 2 至 8 秒；
     全集镜头时长合计对齐 `episode_duration_sec ± 15%`。对话场景按「正打 / 反打 / 双人中景」交替给景别，
     情绪高点自动给 `CLOSE` 或 `EXTREME_CLOSE`。
  3. 每个镜头的结构化字段：`shot_no`、`scene_no`、`shot_size`、`camera_move`、`shot_duration_sec`、
     `shot_desc`（画面描述）、`dialogue_text`（台词）、`emotion`、`lighting`、`character_ref_json`
     （出场角色 + `outfit_code`）、`scene_card_id`、`extra_prompt`（用户补充）。
  4. **提示词合成（只读派生）**：`image_prompt` 由系统按固定模板拼装，不允许手工直接编辑正文，
     只能通过改上述结构化字段、`extra_prompt`，或改画面文字稿 `previz_text`（见
     `previz-text-storyboard.md`，文字稿可编辑并反向回写结构化字段）间接改变。拼装顺序：
     `visual_style` + `scene_card.appearance_prompt` + 各出场角色 `appearance_prompt`（携带定妆图引用）
     + `shot_size` + `camera_move` + `lighting` + `emotion` + `shot_desc` + `extra_prompt`。
     页面实时展示合成结果与各段来源，让创作者看得懂「这句是从哪来的」。
  5. 镜头编辑操作：拆分（一镜拆两镜，时长按台词切分）、合并（相邻同场次镜头）、上移 / 下移、
     插入空镜（无台词的氛围镜）、删除。任何顺序变化后 `shot_no` 自动重排为连续整数。
  6. 引用校验：镜头引用未 `LOCKED` 的角色卡 / 场景卡时，该镜标记 `blocked_flag=1` 且不允许提交生成，
     页面给出「去定妆」直达链接。
  7. 合规预检在提交生成前对 `image_prompt` 与 `dialogue_text` 执行（见 `content-compliance.md`），
     `risk_level=BLOCK` 的镜头不允许提交生成。

## 三、验收标准

1. 对一集定稿剧本执行拆镜，10 秒内返回镜头表（异步任务，页面显示进度），镜头数与场次覆盖完整，
   全集时长合计落在目标 ± 15% 内。
2. 每个镜头默认带齐景别 / 运镜 / 时长 / 台词 / 出场角色，无需人工补齐即可提交生成。
3. 镜头详情面板展示 `image_prompt` 合成结果，并分段标注来源（画风 / 场景卡 / 角色卡 / 镜头参数 / 补充）；
   修改任一结构化字段后合成结果实时更新。
4. 拆分 / 合并 / 移动 / 插入 / 删除后 `shot_no` 连续无空洞，且不影响其它镜头已有素材。
5. 引用未定妆角色的镜头被标红且「生成」按钮禁用，点击提示直达角色卡页。
6. 重复拆镜时已 `LOCKED` 镜头的素材与参数保持逐字不变（系统按 `scene_no` + `line_no`
   对齐自动保留，无需任何勾选）。

## 四、接口契约（snake_case）

- `POST /bgssai/user/shots/split-from-script`（`episode_id`；已 `LOCKED` 镜头一律保留，
  无 `keep_locked_flag` 之类参数；返回 `task_id`）
- `GET /bgssai/user/shots/list?episode_id=e-20001&page_num&page_size&shot_status`
  ```json
  { "result": { "shot_list": [
      { "shot_id": "s-40001", "episode_id": "e-20001", "shot_no": 1, "scene_no": 1,
        "shot_size": "MEDIUM", "shot_size_name": "中景",
        "camera_move": "PUSH", "camera_move_name": "推",
        "shot_duration_sec": 4, "emotion": "COLD", "lighting": "COLD_BLUE",
        "shot_desc": "沈知微立于婚礼台前，众人哗然",
        "dialogue_text": "退婚书我签，但有个条件。",
        "character_ref_json": [ { "character_id": "c-30001", "outfit_code": "WEDDING" } ],
        "scene_card_id": "sc-31001",
        "image_prompt": "都市写实画风，婚礼现场，26 岁女性 鹅蛋脸 黑色长直发 冷感眼神 婚纱，中景，镜头推进，冷蓝光，冷冽情绪，沈知微立于婚礼台前众人哗然",
        "prompt_source_list": [
          { "segment": "都市写实画风", "source": "WORK_STYLE" },
          { "segment": "婚礼现场", "source": "SCENE_CARD" },
          { "segment": "26 岁女性 鹅蛋脸 黑色长直发 冷感眼神 婚纱", "source": "CHARACTER_CARD" },
          { "segment": "中景，镜头推进，冷蓝光，冷冽情绪", "source": "SHOT_PARAM" },
          { "segment": "沈知微立于婚礼台前众人哗然", "source": "SHOT_DESC" } ],
        "shot_status": "VIDEO_READY", "blocked_flag": 0, "risk_level": "PASS",
        "first_frame_asset_id": "a-50101", "video_asset_id": "a-50102",
        "retry_count": 1, "cost_spent_cent": 320 } ],
      "page_num": 1, "page_size": 20, "total_size": 42 } }
  ```
- `POST /bgssai/user/shots/update`（单镜结构化字段更新，返回重新合成的 `image_prompt`）
- `POST /bgssai/user/shots/split` · `/merge` · `/move` · `/insert` · `/delete`
- `GET /bgssai/user/shots/prompt-preview?shot_id=s-40001`（改前预览合成结果）
- 鉴权：需 `Jwttoken`；按 JWT `user_id` 校验作品归属。
- 错误码：`4003` 越权 / `4006` 剧本未定稿即拆镜、引用未定妆卡片、合规 `BLOCK` 镜头提交生成 /
  `4009` 镜头版本冲突 / `429` 限流。

## 五、流程 / 状态机（须配图）

拆镜链路与镜头生命周期见 [`state-shot-lifecycle.html`](./state-shot-lifecycle.html)。

`storyboard_status`（剧集级）：`NONE` → `SPLITTING`（拆镜任务执行中）→ `DRAFT`（镜头表已出，可编辑）→
`READY`（全部镜头 `LOCKED`，可进入配音与合成）。剧本回落 `DRAFT` 时本状态同步回落 `DRAFT`。

## 六、数据模型影响

- 新增表 `short_shot`：`shot_id`(PK)、`episode_id`、`work_id`、`user_id`、`shot_no`、`scene_no`、
  `shot_size`、`camera_move`、`shot_duration_sec`、`shot_desc`、`dialogue_text`、`emotion`、`lighting`、
  `character_ref_json`、`scene_card_id`、`extra_prompt`、`image_prompt`、`shot_status`、`blocked_flag`、
  `risk_level`、`first_frame_asset_id`、`video_asset_id`、`retry_count`、`cost_spent_cent`、
  `shot_version`、`deleted_flag`、`created_time`、`updated_time`。
- 索引：`short_shot(episode_id, shot_no)`、`short_shot(user_id, shot_status)`。
- `image_prompt` 为派生字段，落库用于任务留痕与复现，不作为编辑入口。

## 七、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html)
  （镜头表 + 镜头详情面板 + 提示词来源分段 + 批量生成入口——「全部生成」带目标级别选择
  （草图 / 首帧 / 视频）与费用预估，批量口径见 `previz-text-storyboard.md`）。
  镜头卡行内展示连贯性提示（不阻塞生成，可「忽略这条（计入分镜门评分）」）与合规预检命中
  （命中片段高亮 + 风险类别中文 + 改写建议 + 「采纳改写」，契约见 `content-compliance.md`）；
  生成区旁展示剧本门（`G1_SCRIPT`）与合规预检状态徽标（中文）。
- 跳转关系：`script-studio.html` → `storyboard.html` → `task-center.html` / `character-bible.html`，
  登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`docs/demo-static/web/user/storyboard.html` 及 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
