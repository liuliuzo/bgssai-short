# 作品与剧集管理（short-work-management）

## 一、背景与目标

- 背景：创作者做一部 AI 短剧，实际要管理的是「一部剧 → 几十集 → 每集几十个镜头」的三级结构，
  外加角色、场景、配音、成片等一堆产物。目前创作者普遍用「网盘目录 + 表格 + 若干个 AI 网站」拼凑，
  产物散落、进度靠记、返工靠翻聊天记录——这是做 AI 短剧最先撞上的痛点：**没有一个能承载整部剧的容器**。
- 目标：把一部短剧的全部产出收敛到唯一容器（作品 → 剧集 → 镜头），任意时刻能一眼看出「哪一集卡在哪一步」；
  新建作品到拿到第一版分集大纲不超过 3 分钟、必填输入只有 2 项（剧名 + 一句话梗概）。
- 关联角色：`USER`（创作者，主）、`PLATFORM_ADMIN`（只读查看与审核，见 `content-compliance.md`）。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望只填剧名和一句话梗概就建出一部剧的骨架（题材 / 集数 / 单集时长 / 画风
  都有靠谱默认值，想改再展开高级设置），以便不必从空白页开始写、也不被一堆选项吓退。
- 用户故事二：作为 `USER`，我希望在一个看板上看到每一集处在剧本 / 分镜 / 生成 / 配音 / 合成 / 审核的哪一步，
  以便知道下一步该干什么、哪一集拖了后腿。
- 用户故事三：作为 `USER`，我希望作品级的设定（画风、画幅、角色、场景）被所有剧集继承，
  以便不必逐集重复设置、也不会各集画风打架。

- 业务规则：
  1. 三级结构固定为 `short_work`（作品）→ `short_episode`（剧集）→ `short_shot`（镜头）；
     角色卡 / 场景卡挂在**作品**级（跨集复用），剧本 / 配音 / 成片挂在**剧集**级。
  2. 新建作品必填仅两项：`work_title`（剧名）与 `one_line_pitch`（一句话梗概，必填），
     提交后自动触发分集大纲生成（见 `script-studio.md`）。其余字段收进「高级设置」（默认折叠），
     省略时走默认值：`genre` 默认 `REVENGE`（复仇逆袭）、`episode_count` 默认 24、
     `episode_duration_sec` 默认 90、`visual_style` 默认 `MODERN_URBAN`（都市写实）、
     `template_id` 默认该题材采用数最多的爆款结构模板（见 `quality-pipeline.md`；
     可选「不套用模板」但须确认质量风险提示）。
     `cost_budget_cent`（预算）与 `quality_threshold_score`（质量阈值）不出现在创建流程：
     预算是作品设置里的可选项（默认不限，`cost_budget_cent` 为空即不限）；
     质量阈值由结构模板给默认值，创建时不询问，创建后可随时调整（调整留痕，
     阈值条款本体见 `quality-pipeline.md`）。
     创建页展示低承诺路径说明：「创建后先做第 1 集试试（约 1.1 元文字成本），满意再批量做」。
  3. 画幅固定 `aspect_ratio=VERTICAL_9_16`，本轮不支持横屏；`episode_duration_sec` 取值 60 / 90 / 120，
     超出范围不予创建（短剧节奏由时长约束，见 `script-studio.md` 节奏体检）。
  4. `episode_count` 创建时生成等量的 `short_episode` 占位（`episode_no` 从 1 连续编号），
     后续可追加 / 删除；删除剧集为软删除（`deleted_flag=1`），已合成成片的剧集不允许删除。
  5. 作品级设定变更（`visual_style` / `aspect_ratio`）只影响**变更后**新建的生成任务，
     不回溯已生成素材；界面须显式提示「已生成的 N 个镜头不会自动重做」。
  6. 作品状态由下游产出自动推进，不允许手工改状态；唯一手工动作是「归档」（`ARCHIVED`）与「恢复」。
  7. 数据可见范围：用户端一律只返回 `user_id` 等于 JWT 身份的作品，`user_id` 不从请求参数取。

## 三、验收标准

1. 新建作品首屏只有剧名与一句话梗概两个输入，高级设置不展开也能提交；提交后立即返回 `work_id`
   并跳转作品工作台，工作台上出现一条 `TEXT` 类生成任务（大纲生成中，梗概必填故必触发）。
2. 作品工作台的剧集看板对每一集展示六个阶段徽标（剧本 / 分镜 / 画面 / 配音 / 合成 / 审核）
   与该集质量分（`episode_quality_score`）及四道门状态，徽标与各自模块的真实状态一致，
   不存在「看板说完成、点进去没做」的偏差。
3. 作品列表支持按 `work_status`、`genre` 过滤与按 `updated_time` 排序，分页走 `PageResponse`
   （`page_num` / `page_size` / `total_size`）。
4. 已合成成片的剧集执行删除时返回业务错误码 `4006` 并给出可读原因，数据未被删除。
5. 任意用户携带他人 `work_id` 调用详情 / 更新 / 删除接口，一律返回 `4003`，且不泄漏该作品是否存在。

## 四、接口契约（snake_case）

> 契约正文在详设阶段登记到 `docs/api/`；此处给出关键字段以便评审。列表查询用 GET query，写操作用 POST body。

- `GET /bgssai/user/short-works/list?page_num&page_size&work_status&genre&keyword`
- `POST /bgssai/user/short-works/create`（必填仅 `work_title` 与 `one_line_pitch`，
  其余为高级设置字段，省略走默认值；不含 `cost_budget_cent` 与 `quality_threshold_score`）
  ```json
  { "work_title": "退婚三年后她成了首富",
    "one_line_pitch": "被退婚的落魄千金三年后带着资本回归，收购前未婚夫的公司" }
  ```
  展开高级设置时可追加：
  ```json
  { "genre": "REVENGE", "episode_count": 24, "episode_duration_sec": 90,
    "visual_style": "MODERN_URBAN", "aspect_ratio": "VERTICAL_9_16",
    "template_id": "tpl-001" }
  ```
- `GET /bgssai/user/short-works/detail?work_id=w-10001`
  ```json
  { "code": "0", "message": "OK", "success": true,
    "result": {
      "work_id": "w-10001", "work_title": "退婚三年后她成了首富",
      "genre": "REVENGE", "genre_name": "复仇逆袭",
      "work_status": "PRODUCING", "work_status_name": "制作中",
      "episode_count": 30, "episode_duration_sec": 90,
      "visual_style": "MODERN_URBAN", "aspect_ratio": "VERTICAL_9_16",
      "character_count": 6, "scene_card_count": 9,
      "template_id": "tpl-001", "template_name": "复仇逆袭 · 当众打脸型",
      "overall_quality_score": 82,
      "cost_spent_cent": 128650, "cost_budget_cent": 300000,
      "skip_level_check_flag": 0,
      "episode_list": [
        { "episode_id": "e-20001", "episode_no": 1, "episode_title": "退婚",
          "script_status": "READY", "storyboard_status": "READY",
          "shot_total": 42, "shot_ready": 42,
          "dubbing_status": "READY", "compose_status": "READY",
          "review_status": "APPROVED", "film_duration_sec": 88,
          "gate_status_map": { "G1_SCRIPT": "PASSED", "G2_STORYBOARD": "PASSED",
                               "G3_VISUAL": "FORCE_PASSED", "G4_FILM": "PASSED" },
          "episode_quality_score": 86 }
      ] } }
  ```
- `POST /bgssai/user/short-works/update`（`work_id` + 可改字段；作品设置项也走本接口：
  `cost_budget_cent` 预算上限（可选，传空即「不限」）、`quality_threshold_score`
  质量阈值（可随时调整，每次调整留痕）、`skip_level_check_flag` 预演跳级约束开关
  （0 约束开启为默认，1 允许跳级并提示成本风险，见 `previz-text-storyboard.md` 规则 12））
- `POST /bgssai/user/short-works/archive`（`work_id` + `archived_flag`）
- `POST /bgssai/user/episodes/create`（`work_id` + `episode_title`，追加到末尾）
- `POST /bgssai/user/episodes/update` · `POST /bgssai/user/episodes/delete`
- 鉴权：全部需 `@NeedAop`（请求头 `Jwttoken`）；数据范围按 JWT `user_id` 过滤。
- 错误码：`2002` token 失效 / `4003` 无权限或越权访问他人数据 / `4006` 业务规则拒绝（如删除已合成剧集）/ `429` 限流。

## 五、流程 / 状态机（须配图）

全链路制作管线与作品 / 剧集状态推进见 [`flow-production-pipeline.html`](./flow-production-pipeline.html)。

作品状态 `work_status`：

| 值 | 名称 | 进入条件 |
| --- | --- | --- |
| `DRAFT` | 草稿 | 作品创建完成，尚无任何一集剧本定稿 |
| `SCRIPTING` | 剧本中 | 至少一集剧本生成中或已定稿，且无镜头产出 |
| `PRODUCING` | 制作中 | 至少一集完成分镜拆解并产生镜头生成任务 |
| `COMPOSING` | 合成中 | 至少一集进入配音 / 合成阶段 |
| `REVIEWING` | 审核中 | 至少一集成片已提交平台审核且无待处理驳回 |
| `COMPLETED` | 已完成 | 全部剧集成片审核通过 |
| `ARCHIVED` | 已归档 | 用户手工归档（可恢复到归档前状态） |

状态自动推进，取全部剧集中「进度最靠前」的阶段；`ARCHIVED` 例外，为手工态。

## 六、数据模型影响

- 新增表 `short_work`：`work_id`(PK)、`user_id`、`work_title`、`one_line_pitch`（必填）、`genre`、`work_status`、
  `episode_count`、`episode_duration_sec`、`visual_style`、`aspect_ratio`、`cover_asset_id`、
  `cost_budget_cent`（可空，空即「不限」）、`cost_spent_cent`、`skip_level_check_flag`、
  `archived_flag`、`deleted_flag`、`created_time`、`updated_time`。
- 新增表 `short_episode`：`episode_id`(PK)、`work_id`、`episode_no`、`episode_title`、`episode_synopsis`、
  `script_status`、`storyboard_status`、`dubbing_status`、`compose_status`、`review_status`、
  `film_asset_id`、`film_duration_sec`、`deleted_flag`、`created_time`、`updated_time`。
- 索引：`short_work(user_id, work_status, updated_time)`；`short_episode(work_id, episode_no)` 唯一
  （软删除后需允许同号重建，唯一键含 `deleted_flag`）。
- 同步 `sql/DDL.sql` 与 MyBatis Generator 产物（代码阶段执行）。

## 七、原型

- 涉及端：web 用户端（web 管理端不建作品，只在审核页只读查看）。
- 页面：[`../demo-static/web/user/works.html`](../demo-static/web/user/works.html)（作品列表）、
  [`../demo-static/web/user/work-create.html`](../demo-static/web/user/work-create.html)（新建向导）、
  [`../demo-static/web/user/work-detail.html`](../demo-static/web/user/work-detail.html)（作品工作台 / 剧集看板）。
- 跳转关系：登记在 [`../demo-static/web/user/PAGE-FLOW.html`](../demo-static/web/user/PAGE-FLOW.html)。
- 页面 → API 映射：[`../api/page-api-mapping.md`](../api/page-api-mapping.md) 对应行。

## 八、附录：全产品统一枚举字典（各功能文档共同引用）

本附录是本产品枚举值的**单一登记处**，其它功能文档只引用不复制；新增枚举一律加在此处。

| 枚举 | 值 |
| --- | --- |
| `genre` 题材 | `REVENGE` 复仇逆袭 / `CEO` 豪门总裁 / `TIME_TRAVEL` 穿越重生 / `SWEET` 甜宠 / `SUSPENSE` 悬疑 / `FANTASY` 玄幻 |
| `visual_style` 画风 | `MODERN_URBAN` 都市写实 / `ANCIENT_CHINESE` 古装 / `ANIME` 动漫 / `CINEMATIC` 电影感 |
| `aspect_ratio` 画幅 | `VERTICAL_9_16` 竖屏 9:16（本轮唯一值） |
| `work_status` 作品状态 | `DRAFT` / `SCRIPTING` / `PRODUCING` / `COMPOSING` / `REVIEWING` / `COMPLETED` / `ARCHIVED` |
| `script_status` 剧本状态 | `NONE` 未开始 / `GENERATING` 生成中 / `DRAFT` 草稿 / `READY` 已定稿 |
| `storyboard_status` 分镜状态 | `NONE` / `SPLITTING` 拆镜中 / `DRAFT` / `READY` |
| `shot_status` 镜头状态 | `PENDING` 待生成 / `IMAGE_GENERATING` 首帧生成中 / `IMAGE_READY` 首帧已出 / `VIDEO_GENERATING` 视频生成中 / `VIDEO_READY` 视频已出 / `LOCKED` 已定稿 / `FAILED` 生成失败 |
| `shot_size` 景别 | `PANORAMA` 全景 / `LONG` 远景 / `MEDIUM` 中景 / `CLOSE` 近景 / `EXTREME_CLOSE` 特写 |
| `camera_move` 运镜 | `FIXED` 固定 / `PUSH` 推 / `PULL` 拉 / `PAN` 摇 / `FOLLOW` 跟 / `HANDHELD` 手持 |
| `dubbing_status` 配音状态 | `NONE` / `GENERATING` / `DRAFT` / `READY` |
| `compose_status` 合成状态 | `NONE` / `COMPOSING` 合成中 / `READY` 已出成片 / `FAILED` |
| `review_status` 审核状态 | `NOT_SUBMITTED` 未提交 / `PENDING` 待审核 / `APPROVED` 已通过 / `REJECTED` 已驳回 |
| `task_type` 任务类型 | `TEXT` 文本 / `IMAGE` 图像 / `VIDEO` 视频 / `VOICE` 语音 / `COMPOSE` 合成 |
| `task_status` 任务状态 | `QUEUED` 排队中 / `PAUSED` 已暂停（手工暂停批次或预算触顶进入，恢复转回 `QUEUED`）/ `RUNNING` 执行中 / `SUCCEEDED` 成功 / `FAILED` 失败 / `CANCELED` 已取消 |
| `fail_reason_code` 任务失败原因 | `MODEL_TIMEOUT` 模型超时 / `CONTENT_BLOCKED` 内容拦截 / `ASSET_MISSING` 素材缺失 |
| `notify_type` 站内通知类型 | `BATCH_DONE` 批次完成 / `BATCH_FAILED` 批次出现失败 |
| `compose_mode` 合成方式 | `FULL` 全量合成 / `SUBTITLE_ONLY` 仅重烧字幕（轻量合成，不重编码画面与音频） |
| `ability_type` AI 能力类型 | 同 `task_type` |
| `risk_level` 合规风险等级 | `PASS` 通过 / `WARN` 提示 / `BLOCK` 拦截 |
| `risk_category` 合规风险类别 | `POLITICS` 涉政 / `PORN` 色情低俗 / `VIOLENCE` 血腥暴力 / `MINOR` 未成年人相关 / `SUPERSTITION` 封建迷信 / `IP_RISK` 疑似侵权 / `OTHER` 其他（见 `content-compliance.md`） |
| `foreshadow_status` 伏笔状态 | `PLANTED` 已埋未收 / `PAID_OFF` 已回收 / `ORPHAN` 回收了没埋过 |
| `episode_gap_type` 集间断裂 | `HOOK_MISMATCH` 钩子未接 / `TIMELINE_JUMP` 时间线跳跃 / `EMOTION_RESET` 情绪无过渡归零 |
| `batch_action` 批量动作 | `GEN_SCRIPT` 批量生成剧本 / `SUBMIT_GENERATE` 批量提交生成 / `SUBMIT_REVIEW` 批量提审 / `EXPORT` 批量导出 |
| `map_scope` 图谱范围 | `WORK` 全剧 / `EPISODE` 单集 |
| `relation_type` 人物关系 | `ALLY` 盟友 / `RIVAL` 对手 / `LOVE` 爱慕 / `FAMILY` 血缘 / `EMPLOY` 雇佣 / `SECRET` 隐藏关系 |
| `deviation_type` 张力偏离 | `FLAT` 塌陷段 / `WEAK_HOOK` 钩子无力 / `WEAK_ENDING` 结尾悬念偏弱 / `OVER_EARLY` 爽点前置过早 |
| `previz_level` 预演级别 | `TEXT` 文字稿 / `DRAFT_IMAGE` 低清草图 / `IMAGE` 高清首帧 / `VIDEO` 视频 |
| `previz_status` 文字稿状态 | `NONE` / `GENERATING` / `READY` |
| `continuity_issue_type` 连贯性问题 | `SPACE` 空间跳变 / `LIGHT` 光线突变 / `OUTFIT` 服装跳变 / `PROP` 道具异常 / `EYELINE` 轴线矛盾 |
| `prompt_element` 提示词要素 | `SUBJECT` 主体 / `OUTFIT` 服装 / `SCENE` 场景 / `ACTION` 动作 / `SHOT_SIZE` 景别 / `CAMERA` 运镜 / `LIGHTING` 光线 / `STYLE` 风格 / `NEGATIVE` 否定词 |
| `prompt_issue_type` 提示词问题 | `MISSING` 缺要素 / `CONFLICT` 自相矛盾 / `REDUNDANT` 冗余 / `TOO_LONG` 超长 |
| `gate_code` 质量门 | `G1_SCRIPT` 剧本门 / `G2_STORYBOARD` 分镜门 / `G3_VISUAL` 画面门 / `G4_FILM` 成片门 |
| `gate_status` 门禁状态 | `NOT_RUN` 未运行 / `RUNNING` 评审中 / `PASSED` 已通过 / `FAILED` 未达标 / `FORCE_PASSED` 强制放行 |
| `judge_role` 评审视角 | `AUDIENCE` 观众视角 / `CENSOR` 平台审核视角 / `DIRECTOR` 导演视角 |
| `fix_action` 返工动作 | `REWRITE_SCENE` 重写场次 / `ADJUST_SHOT` 调整镜头参数 / `REGENERATE_SHOT` 重抽镜头 / `REDUB_LINE` 重配台词 / `RECOMPOSE` 重新合成 |
| `severity` 问题严重度 | `CRITICAL` 致命 / `MAJOR` 严重 / `MINOR` 轻微 |
| `issue_handle_status` 质量问题处理状态 | `OPEN` 待处理 / `FIXED` 已修复 / `WONT_FIX` 不改（须选理由）/ `ACKNOWLEDGED` 知悉不改（仅 `MINOR`，不入质量档案） |
| `wont_fix_reason` 不改理由 | `INTENDED` 有意为之 / `COST_TRADEOFF` 成本取舍 / `SCHEDULE_RUSH` 赶进度 / `OTHER` 其它 |
| `recharge_apply_status` 充值申请状态 | `SUBMITTED` 已提交 / `PROCESSING` 处理中 / `COMPLETED` 已完成 / `REJECTED` 已驳回 |
| `variant_node` 多方案节点 | `EPISODE_OPENING` 单集开场 / `EPISODE_HOOK` 集末钩子 / `KEY_SHOT` 关键反转镜头 / `WORK_COVER` 作品封面 |
| `align_status` 音画对齐 | `PENDING` 待处理 / `ALIGNED` 已对齐 / `MISALIGNED` 未对齐 |
| `align_action` 对齐解法 | `ADJUST_SPEED` 提高语速 / `EXTEND_SHOT` 延长镜头 / `PAD_SILENCE` 尾部补静音 |
| `card_status` 资产卡状态 | `CANDIDATE` 待建卡 / `DRAFT` 草稿 / `GENERATING` 候选生成中 / `LOCKED` 已定妆 |
| `asset_type` 素材类型 | `IMAGE` 图片 / `VIDEO` 视频 / `AUDIO` 音频 / `SUBTITLE` 字幕 / `FILM` 成片 |
| `quality_level` 模型质量档 | `FAST` 快速 / `STANDARD` 标准 / `HIGH` 高质 |
| `price_unit` 计费单位 | `PER_CALL` 按次 / `PER_SECOND` 按视频秒 / `PER_KILO_TOKEN` 按千 token / `PER_MINUTE` 按音频分钟 |

## 九、四层同步检查清单

- [x] 需求（本文件 / `PRODUCT_VISION.md`）
- [x] 原型（`docs/demo-static/web/user/**` 及其 `PAGE-FLOW.html`）
- [ ] 设计与契约（`docs/architecture` / `docs/api`；本次仅同步 `page-api-mapping.md`，契约正文待详设阶段）
- [ ] 代码与数据库（`src/**`、前端工程、`sql/**`）——待详设定稿后进入
