# 三个问题体验主线（ux-three-questions）

> 本功能不新增制作能力，而是把已有能力**按创作者关心的三个问题重新组织**。
> 做短剧的人打开产品，心里只有三件事：我的视频会是什么样子、我的剧情连不连贯、
> 管我的剧方不方便。按制作阶段组织的导航回答不了这三个问题——本功能给每个问题一个直达入口。

## 一、背景与目标

- 背景：产品能力已按管线拆成剧本 / 分镜 / 预演 / 生成等九个工作台，功能齐但**体验是工程师视角的**：
  创作者想知道「这一集出来会是什么样」，得在文字预演、草图墙、分镜表之间自己拼；
  想确认「剧情连不连贯」，得跑三个页面看散落的检查结果；想「接着做」，得先想清楚自己在管线第几步。
  用户不关心管线，用户关心答案。
- 目标：三个问题各有一个页面直达答案，且每个答案都能一键跳到修复位置：
  1. **我生成的视频会是什么样子** → 「看样片」：用文字与草图把整集**当成片看一遍**，零视频费用；
  2. **我的剧情连不连贯** → 「连贯性中心」：全剧 / 集间 / 镜头间三层连贯问题收敛到一张清单；
  3. **管理我的短剧方不方便** → 「管理看板」强化：卡点驱动 + 批量操作 + 三击直达原则。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

### 2.1 问题一：我生成的视频会是什么样子（看样片，episode-preview）

- 用户故事一：作为 `USER`，我希望在花视频钱之前，就能**像刷短剧一样把这一集看一遍**——
  竖屏画框、字幕、镜头切换、时长，用文字和草图模拟出来。

- 业务规则：
  1. **文字样片**：按 `shot_no` 顺序把整集串成一条可「播放」的竖屏序列，每一帧由三层构成：
     画面层（该镜当前最高预演级素材：草图 / 首帧，无则用文字稿占位）、
     字幕层（该镜 `dialogue_text`，按字幕断句规则呈现）、
     信息层（镜号 / 景别运镜 / 时长）。连播模式按 `shot_duration_sec` 自动翻页，也可手动逐镜翻。
  2. **观感自检卡**：样片旁固定回答四个观众视角的问题——前 3 秒有没有钩子、
     多久出现第一个反转、结尾停在哪句话、全集时长多少。数据取自节奏体检与张力曲线，不重复计算。
  3. **画面完成度**：标注整集处于哪一级（如「38 镜草图 + 6 镜首帧」），并给出
     「把样片升级到下一级」的一键入口与费用预估（走阶梯预演口径，见 `previz-text-storyboard.md`）。
  4. 样片页**只读**：发现问题点该镜即跳对应工作台修改，本页不提供编辑，避免第二处可改而漂移。
  5. 零视频费用：本页全部内容由文字与图片构成；唯一可能花钱的动作是显式点击「升级预演级别」。

### 2.2 问题二：我的剧情连不连贯（连贯性中心，continuity-center）

- 用户故事二：作为 `USER`，我希望所有「接不上」的问题在一个地方看完，
  而不是在文字预演、剧情图谱、质量报告三个页面之间跳。

- 业务规则：
  6. 三层收敛，同一清单、按严重度排序，每条带定位与 `fix_action`：
     - **全剧层**：伏笔台账（埋下未回收 / 回收了没埋过）、关系揭晓时机（`SECRET` 关系揭晓前铺垫不足）、
       角色掉线（连续 5 集无出场）——数据来自 `story-map.md`；
     - **集间层**：集尾钩子与下集开场的衔接（`HOOK_MISMATCH` 上集钩子下集没接、
       `TIMELINE_JUMP` 时间线无交代跳跃、`EMOTION_RESET` 情绪无过渡归零）——新增检查；
     - **镜头层**：五类跨镜头断裂（`SPACE` / `LIGHT` / `OUTFIT` / `PROP` / `EYELINE`）——
       数据来自 `previz-text-storyboard.md` 与走位轴线判定，本页只聚合不另算。
  7. **伏笔台账** `short_foreshadow`：每条伏笔记录埋设位置（集 / 场）、计划回收位置、当前状态
     （`PLANTED` 已埋未收 / `PAID_OFF` 已回收 / `ORPHAN` 回收了没埋过）。剧本生成时自动抽取候选，
     创作者可增删改；全剧完结时仍为 `PLANTED` 的伏笔在质量档案中列出。
  8. **一键重查**：上游内容变更后本页标 `stale_flag`，重查为 `TEXT` 任务（整剧约 0.05 元），
     查看不计费。
  9. 连贯性问题不硬性阻塞（合规 `BLOCK` 除外），但计入对应质量门评分——口径与既有功能一致。

### 2.3 问题三：管理我的短剧方不方便（管理看板强化）

- 用户故事三：作为 `USER`，我希望打开作品就知道**现在该干什么**，并且重复动作能批量做。

- 业务规则：
  10. **三击直达原则**（体验红线）：从首页出发，到任何一集当前卡点的修复位置，点击不超过 3 次。
      落地：首页「接着做」按卡点排序 → 作品工作台顶部常驻卡点条 → 点击直达工作台并预填定位。
  11. **三个问题入口常驻**：作品工作台顶部固定三张问题卡——看样片 / 查连贯 / 剧集看板，
      每张卡带当前状态摘要（如「样片就绪：草图级」「连贯问题 5 条」「2 集卡在门禁」）。
  12. **批量操作**：剧集看板支持多选剧集执行批量动作——批量生成剧本（未写的集）、
      批量提交生成、批量提审（成片就绪的集）、批量导出；每个批量动作先给费用 / 影响预估，
      口径同任务体系。
  13. **卡点排序**：剧集看板默认在「集号顺序」外提供「按卡点排序」视图，
      待处理的集置顶、原因前置（门禁未过 / 未定妆 / 未对齐 / 审核驳回）。
  14. 导航语义从「管线阶段」转向「问题」：侧边导航保留制作分区（专业用户直达），
      但顶部三问入口是新用户的默认动线——两套入口指向同一批页面，不造副本。

## 三、验收标准

1. 看样片页可按镜连播整集：竖屏画框 + 字幕 + 镜头信息，画面取当前最高预演级素材，缺素材以文字稿占位。
2. 观感自检卡展示钩子秒位、首个反转位置、结尾句、全集时长，数据与节奏体检一致（不另算一套）。
3. 样片页任一镜点击可跳到分镜工作台或文字预演对应镜（预填定位），本页无编辑入口。
4. 连贯性中心一张清单收敛三层问题，每条带定位与 `fix_action`；镜头层数据与文字预演页逐条一致。
5. 集间检查能检出「上集钩子下集未接」：E14 结尾抛出母亲名字，E15 开场若无人提及则命中 `HOOK_MISMATCH`。
6. 伏笔台账列出全部伏笔与状态；剧终仍 `PLANTED` 的伏笔出现在质量档案。
7. 从首页到任一集卡点修复位置，点击路径不超过 3 次（首页接着做 → 直达工作台定位）。
8. 剧集看板多选 16 至 30 集执行「批量生成剧本」，先展示预估费用，确认后产生批量 `TEXT` 任务。
9. 三个问题入口在作品工作台常驻，状态摘要与真实数据一致。

## 四、接口契约（snake_case）

- `GET /bgssai/user/preview/summary?episode_id=e-20014`
  ```json
  { "result": {
      "episode_id": "e-20014", "shot_count": 44, "total_duration_sec": 92,
      "previz_level_stat": { "TEXT": 0, "DRAFT_IMAGE": 38, "IMAGE": 6, "VIDEO": 0 },
      "hook_at_sec": 2, "first_turn_at_sec": 62,
      "ending_line": "沈…宛如？", "upgrade_cost_cent": 3040,
      "watch_check": {
        "hook_ok_flag": 1, "turn_in_beat_flag": 1,
        "ending_hook_flag": 0, "duration_in_range_flag": 1 } } }
  ```
  （样片帧序列复用 `GET /bgssai/user/previz/list` 与 `GET /bgssai/user/shots/list`，不新增接口。）
- `GET /bgssai/user/continuity/overview?work_id=w-10001`
  ```json
  { "result": {
      "total_count": 5, "stale_flag": 0,
      "layer_stat": { "WORK": 2, "INTER_EPISODE": 1, "SHOT": 2 },
      "issue_list": [
        { "layer": "INTER_EPISODE", "issue_type": "HOOK_MISMATCH", "severity": "MAJOR",
          "from_episode_no": 14, "to_episode_no": 15,
          "issue_desc": "E14 结尾抛出母亲名字，E15 开场三场戏无人提及",
          "suggestion": "在 E15 场 1 加一句对该名字的反应",
          "fix_action": "REWRITE_SCENE", "target_id": "e-20015" } ] } }
  ```
- `GET /bgssai/user/continuity/foreshadows?work_id=w-10001`
  （伏笔列表：`foreshadow_desc` · `setup_episode_no` / `setup_scene_no` ·
  `payoff_episode_no` · `foreshadow_status`）
- `POST /bgssai/user/continuity/foreshadow-save` · `POST /bgssai/user/continuity/recheck`
- `POST /bgssai/user/episodes/batch`（`episode_id_list` + `batch_action`：
  `GEN_SCRIPT` / `SUBMIT_GENERATE` / `SUBMIT_REVIEW` / `EXPORT`；返回预估或 `batch_id`）
- 鉴权：全部需 `Jwttoken`；按 JWT `user_id` 校验归属。
- 错误码：`4003` 越权 / `4006` 批量动作含不满足前置条件的集（逐集列出原因）/ `429` 限流。

## 五、流程 / 图示（须配图）

三问动线与数据来源见 [`flow-ux-three-questions.html`](./flow-ux-three-questions.html)。

本功能无新增状态机；伏笔状态 `PLANTED` → `PAID_OFF`（在回收场次落笔时流转），
`ORPHAN` 为检查产出的异常态（需人工处理：补埋设或删除回收）。

## 六、数据模型影响

- 新增表 `short_foreshadow`：`foreshadow_id`(PK)、`work_id`、`user_id`、`foreshadow_desc`、
  `setup_episode_no`、`setup_scene_no`、`payoff_episode_no`、`foreshadow_status`、
  `auto_extracted_flag`、`deleted_flag`、`created_time`、`updated_time`。
- 新增表 `short_episode_gap`：`gap_id`(PK)、`work_id`、`from_episode_no`、`to_episode_no`、
  `gap_type`、`issue_desc`、`suggestion`、`resolved_flag`、`created_time`。
- 看样片与连贯性中心其余数据均为既有表的聚合视图，不新增存储。

## 七、补充枚举（登记回 `short-work-management.md` 附录）

| 枚举 | 值 |
| --- | --- |
| `foreshadow_status` 伏笔状态 | `PLANTED` 已埋未收 / `PAID_OFF` 已回收 / `ORPHAN` 回收了没埋过 |
| `episode_gap_type` 集间断裂 | `HOOK_MISMATCH` 钩子未接 / `TIMELINE_JUMP` 时间线跳跃 / `EMOTION_RESET` 情绪无过渡归零 |
| `batch_action` 批量动作 | `GEN_SCRIPT` 批量生成剧本 / `SUBMIT_GENERATE` 批量提交生成 / `SUBMIT_REVIEW` 批量提审 / `EXPORT` 批量导出 |

## 八、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/episode-preview.html`](../demo-static/web/user/episode-preview.html)
  （看样片：竖屏连播 + 观感自检 + 完成度与升级）、
  [`../demo-static/web/user/continuity-center.html`](../demo-static/web/user/continuity-center.html)
  （连贯性中心：三层清单 + 伏笔台账 + 集间衔接）；
  [`../demo-static/web/user/work-detail.html`](../demo-static/web/user/work-detail.html)
  增加三问入口卡与批量操作。
- 跳转关系：登记在用户端 `PAGE-FLOW.html`（三问动线为新用户默认旅程）。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 九、四层同步检查清单

- [x] 需求（本文件；枚举同步登记到 `short-work-management.md` 附录）
- [x] 原型（`episode-preview.html`、`continuity-center.html`、`work-detail.html` 及 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
