# 剧情图谱与走位示意（story-map）

> 本功能与 `previz-text-storyboard.md` 同属**零成本的文字层**：文字稿负责「读得懂」，
> 本功能负责「一眼看得出」。两者互补——通读能发现逻辑断裂，看图能发现节奏塌陷与空间错乱。

## 一、背景与目标

- 背景：一集 90 秒、44 个镜头、一部剧 30 集，全靠通读文字很难回答三类问题：
  **这部剧的起伏对不对**（哪几集是平的、爽点是不是堆在前面、反转是不是没铺垫）、
  **人物关系走到哪一步了**（谁和谁现在是敌是友，第 15 集揭身份时观众有没有被喂够信息）、
  **这一镜两个人站在哪、机位在哪**（正反打有没有越轴、上一镜她在左边这一镜怎么跑到右边了）。
  这些问题用文字描述要读半天，用图看一眼就知道；而它们恰恰是「片子看着不对劲但说不上哪不对」的根源。
- 目标：把剧情的**节奏、关系、空间**三件事可视化，全部在生成画面之前完成，零成本、可交互、可定位修改。
  口径：张力曲线能定位到具体秒数区间；人物关系图按集演进；走位图覆盖全部有两人及以上出场的镜头，
  并作为轴线（`EYELINE`）连贯性检查的数据来源。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

### 2.1 张力曲线（剧情起伏，按集与按全剧两级）

- 用户故事一：作为 `USER`，我希望看到这一集的情绪张力曲线，一眼看出哪 15 秒是平的，
  并与结构模板期望的曲线对比。

- 业务规则：
  1. **单集张力曲线**：横轴为秒（0 至 `episode_duration_sec`），纵轴为张力值 `tension_score`（0-100）。
     系统按场次与台词内容为每个节拍与镜头估算张力，连成实线；同时把结构模板的**期望曲线**
     （由 `beat_sheet_json` 的各拍目的推导）画成虚线叠加。
  2. **偏离标注**：实线低于期望曲线 15 分以上且持续 10 秒以上的区间标为 `FLAT`（塌陷段），
     直接给出该区间对应的 `scene_no` 与 `shot_no` 范围，点击定位到剧本或分镜；
     实线在 0-3 秒低于 60 标为 `WEAK_HOOK`（钩子无力）；末 5 秒低于 55 标为 `WEAK_ENDING`。
  3. **全剧张力条带**：每集一格，按该集平均张力着色深浅，反转集加标记；
     用于回答「爽点是不是堆在前十集」「中段是不是连着五集都平」这类只有整体看才看得出的问题。
  4. 张力值为**派生估算**，不落最终判定权：它服务于定位讨论，不参与质量门评分
     （评分仍由 `quality-pipeline.md` 的四道门负责），避免用一个估算数字绑架创作。

### 2.2 人物关系图（按集演进）

- 用户故事二：作为 `USER`，我希望看到一张人物关系图，并能拖动集数看关系怎么变化。

- 业务规则：
  5. 节点为角色卡（`drama_character`），边为关系，`relation_type` 取
     `ALLY` 盟友 / `RIVAL` 对手 / `LOVE` 爱慕 / `FAMILY` 血缘 / `EMPLOY` 雇佣 / `SECRET` 隐藏关系。
     边带 `relation_strength`（1-5）与 `reveal_episode_no`（该关系对观众揭晓的集数）。
  6. **按集演进**：拖动集数滑块，图按「截至该集观众已知的信息」渲染——
     `SECRET` 关系在 `reveal_episode_no` 之前以虚线灰显（表示观众还不知道），揭晓后转为实线。
     这让创作者直观检查「信息是不是揭得太早或太晚」。
  7. 关系由剧本自动抽取生成初版，创作者可增删改；改动只影响图谱与相关检查，不改剧本正文。
  8. **孤立角色提示**：某角色连续 5 集无出场且无关系变化时提示「角色掉线」，
     附其最后出场集数——短剧里配角凭空消失是常见硬伤。

### 2.3 走位示意图（俯视，镜头级）

- 用户故事三：作为 `USER`，我希望在生成画面之前，先用一张俯视小图确认这一镜里谁站哪、
  面朝哪、机位在哪、镜头怎么动。

- 业务规则：
  9. 每个镜头可生成 `blocking_json`（俯视走位），包含：
     - `stage`：场景边界与关键陈设（如长桌、门、窗）的粗略位置；
     - `actor_list`：出场角色的位置 `x` / `y`（归一化 0-1）与朝向 `facing`（角度，0 为画面上方）；
     - `camera`：机位 `x` / `y`、朝向 `angle`、`shot_size`、`camera_move` 与移动路径 `path`（可选）。
  10. 走位由 `previz_text` 与镜头结构化字段自动推导初版，创作者可直接拖动调整
      （原型以静态示意呈现，交互在前端实现阶段落地）。
  11. **轴线检查（180 度规则）**：同一场戏内相邻镜头的机位若跨越两个主要角色的连线（轴线），
      判定为越轴，产出 `EYELINE` 连贯性问题（口径与 `previz-text-storyboard.md` 的
      `continuity_issue_type` 统一，本功能是其**数据来源**，不另立一套）。
  12. **走位反哺提示词**：走位中的空间关系（谁在左谁在右、谁面向谁、机位高低）会被翻译成
      提示词中的方位描述，随 `image_prompt` 一并下发，减少「两人明明在对话却各看各的」这类画面。
  13. 走位仅对**两人及以上出场**或**有移动路径**的镜头强制生成；单人静止镜头可跳过（节省 token）。

### 2.4 场次时间带与角色出场热力

- 用户故事四：作为 `USER`，我希望看到一集里每场戏占了多少时间、每个角色在哪些集出场。

- 业务规则：
  14. **场次时间带**：一集内各场次按时长占比排成横向条带，标注场景地点与镜头数，
      单场超过全集时长 40% 时标为过长并提示拆场。
  15. **角色出场热力**：横轴为集、纵轴为角色的网格，格子深浅表示该角色在该集的出场镜头数，
      用于发现「主角连续三集只有两个镜头」这类结构问题。

### 2.5 文字层体检统一口径（与 `quality-pipeline.md` 2.6 同一约定）

- 业务规则：
  16. 内容（剧本 / 分镜 / 走位）保存后自动触发**统一的文字层体检**；图谱 `stale` 时自动重查
      （`TEXT` 任务，约 0.03 元 / 集，随保存自动发起），查看与拖动始终免费。
      剧情图谱、连贯性中心、质量报告三页展示的是**同一份按 `issue_id` 去重的清单**，
      任一页处理一条，三页同步消掉；同一问题（如越轴 / 空间跳变）在三页的措辞与
      严重度标签（严重 / 轻微）统一。
  17. 三页侧栏统一「三个问题」分区入口（看样片、连贯性中心、剧集看板），
      入口角标数字采用同一口径（同一份去重清单的待处理条数）。
  18. **草稿态也可生成图谱**：剧本处于草稿态即可生成，结果标注「估算，定稿后自动更新」
      （`draft_flag=1`），定稿后自动重新生成并去掉标注；不设「剧本未定稿不可生成」的硬限制。

## 三、验收标准

1. 单集张力曲线与模板期望曲线叠加显示，塌陷段（`FLAT`）被标出并给出对应 `scene_no` / `shot_no` 范围，
   点击可定位到剧本或分镜。
2. 前 3 秒钩子弱、末 5 秒悬念弱时分别标为 `WEAK_HOOK` / `WEAK_ENDING`。
3. 全剧张力条带按集着色，反转集有标记，可一眼看出爽点分布与连续平段。
4. 人物关系图可按集滑动演进；`SECRET` 关系在揭晓集之前为虚线灰显，之后转实线。
5. 某角色连续 5 集无出场时出现「角色掉线」提示并给出最后出场集数。
6. 两人及以上出场的镜头可查看俯视走位图，含角色位置朝向与机位朝向。
7. 相邻镜头越轴时产出 `EYELINE` 连贯性问题，定位到镜号，与文字预演页的连贯性清单是同一条数据。
8. 场次时间带按时长占比呈现，单场超 40% 时给出拆场提示。
9. 全部图谱均为零成本视图（张力与关系抽取走 `TEXT` 任务，单集约 0.03 元；查看与拖动不产生费用）。
10. 剧本草稿态可生成图谱，页面标注「估算，定稿后自动更新」；定稿后自动重新生成并去掉标注。
11. 内容保存后图谱自动重查（无需手动点重新生成）；本页问题清单与连贯性中心、质量报告
    按 `issue_id` 去重同源，任一页处理后三页同步消失。

## 四、接口契约（snake_case）

- `POST /bgssai/user/story-map/generate`（`work_id` 或 `episode_id` + `map_scope`；返回 `task_id`；
  剧本草稿态可调用，结果带 `draft_flag=1`）
- `GET /bgssai/user/story-map/tension?episode_id=e-20014`
  ```json
  { "result": {
      "episode_id": "e-20014", "duration_sec": 90, "draft_flag": 0,
      "point_list": [
        { "at_sec": 0, "tension_score": 82, "shot_no": 1, "scene_no": 1 },
        { "at_sec": 34, "tension_score": 41, "shot_no": 12, "scene_no": 3 } ],
      "expect_list": [
        { "at_sec": 0, "tension_score": 80 }, { "at_sec": 34, "tension_score": 62 } ],
      "deviation_list": [
        { "deviation_type": "FLAT", "start_sec": 30, "end_sec": 48,
          "scene_no_list": [3], "shot_no_list": [11, 12, 13, 14],
          "issue_desc": "第 30 至 48 秒张力低于模板期望 21 分，观众易划走",
          "suggestion": "把第 4 场的身份揭露提前到此处", "fix_action": "REWRITE_SCENE" } ] } }
  ```
- `GET /bgssai/user/story-map/tension-band?work_id=w-10001`（全剧按集张力条带 + 反转集标记）
- `GET /bgssai/user/story-map/relations?work_id=w-10001&as_of_episode_no=15`
  ```json
  { "result": {
      "node_list": [
        { "character_id": "c-30001", "character_name": "沈知微", "character_role": "FEMALE_LEAD",
          "appear_shot_count": 42 } ],
      "edge_list": [
        { "from_character_id": "c-30001", "to_character_id": "c-30002",
          "relation_type": "RIVAL", "relation_type_name": "对手", "relation_strength": 5,
          "reveal_episode_no": 1, "revealed_flag": 1 },
        { "from_character_id": "c-30001", "to_character_id": "c-30003",
          "relation_type": "FAMILY", "relation_type_name": "血缘", "relation_strength": 4,
          "reveal_episode_no": 15, "revealed_flag": 0 } ],
      "offline_list": [
        { "character_id": "c-30004", "character_name": "苏曼",
          "last_appear_episode_no": 9, "absent_episode_count": 6 } ] } }
  ```
- `POST /bgssai/user/story-map/relation-save`（增删改单条关系）
- `GET /bgssai/user/story-map/blocking?shot_id=s-40008`
  ```json
  { "result": {
      "shot_id": "s-40008", "shot_no": 8,
      "stage": { "scene_card_id": "sc-31001", "stage_desc": "长桌横向居中，门在左侧，落地窗在上方",
        "prop_list": [ { "prop_name": "长桌", "x": 0.5, "y": 0.5, "width": 0.6, "height": 0.12 } ] },
      "actor_list": [
        { "character_id": "c-30001", "character_name": "沈知微", "x": 0.24, "y": 0.5, "facing": 90 },
        { "character_id": "c-30002", "character_name": "陆时衍", "x": 0.76, "y": 0.5, "facing": 270 } ],
      "camera": { "x": 0.5, "y": 0.86, "angle": 0, "shot_size": "MEDIUM",
        "camera_move": "FIXED", "path": null },
      "axis_flag": 1, "axis_from_character_id": "c-30001", "axis_to_character_id": "c-30002" } }
  ```
- `POST /bgssai/user/story-map/blocking-save`（`shot_id` + `blocking_json`；保存后重算轴线检查与提示词方位描述）
- `GET /bgssai/user/story-map/scene-band?episode_id=e-20014`（场次时长占比与镜头数）
- `GET /bgssai/user/story-map/appearance-heat?work_id=w-10001`（角色 × 集 的出场镜头数网格）
- 鉴权：全部需 `Jwttoken`；按 JWT `user_id` 校验作品归属。
- 错误码：`4003` 越权 / `429` 限流（剧本草稿态可生成图谱，不再返回 `4006`，见规则 18）。

## 五、流程 / 状态机（须配图）

三类图谱的数据来源与回流路径见 [`flow-story-map.html`](./flow-story-map.html)。

图谱本身无状态机，均为派生视图；唯一持久化的新数据是镜头级 `blocking_json` 与作品级关系边。
上游变更（剧本改写、镜头增删）时图谱标记 `stale_flag=1` 并随内容保存**自动重查**
（`TEXT` 任务，约 0.03 元 / 集），查看免费，无需手动重新生成；手动重查入口保留。
剧本草稿态生成的图谱带 `draft_flag=1`（标注「估算，定稿后自动更新」），定稿后自动重生成并清除。

## 六、数据模型影响

- 新增表 `drama_character_relation`：`relation_id`(PK)、`work_id`、`user_id`、`from_character_id`、
  `to_character_id`、`relation_type`、`relation_strength`、`reveal_episode_no`、`relation_desc`、
  `deleted_flag`、`created_time`、`updated_time`。
- 新增表 `drama_story_map`：`map_id`(PK)、`work_id`、`episode_id`、`user_id`、`map_scope`
  （`WORK` / `EPISODE`）、`tension_json`、`expect_json`、`deviation_json`、`scene_band_json`、
  `appearance_heat_json`、`stale_flag`、`draft_flag`、`source_task_id`、`created_time`、`updated_time`。
- `drama_shot` 新增列：`blocking_json`、`axis_flag`。
- 索引：`drama_character_relation(work_id, from_character_id)`、`drama_story_map(work_id, episode_id, map_scope)`。
- 轴线检查结果复用 `drama_continuity_issue`（`issue_type=EYELINE`），不新建表。

## 七、补充枚举（登记回 `drama-work-management.md` 附录）

| 枚举 | 值 |
| --- | --- |
| `map_scope` 图谱范围 | `WORK` 全剧 / `EPISODE` 单集 |
| `relation_type` 人物关系 | `ALLY` 盟友 / `RIVAL` 对手 / `LOVE` 爱慕 / `FAMILY` 血缘 / `EMPLOY` 雇佣 / `SECRET` 隐藏关系 |
| `deviation_type` 张力偏离 | `FLAT` 塌陷段 / `WEAK_HOOK` 钩子无力 / `WEAK_ENDING` 结尾悬念偏弱 / `OVER_EARLY` 爽点前置过早 |

## 八、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/story-map.html`](../demo-static/web/user/story-map.html)
  （全剧张力条带 + 单集张力曲线 + 人物关系图 + 场次时间带 + 角色出场热力）；
  镜头级走位示意图内嵌于
  创作台 [`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html)（文字稿旁）与
  [`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html) 镜头详情。
- 跳转关系：`script-studio.html` / `work-detail.html` → `story-map.html` → 剧本 / 分镜（按偏离点定位），
  登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 九、四层同步检查清单

- [x] 需求（本文件；枚举同步登记到 `drama-work-management.md` 附录）
- [x] 原型（`docs/demo-static/web/user/story-map.html`、`storyboard.html` 及 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
