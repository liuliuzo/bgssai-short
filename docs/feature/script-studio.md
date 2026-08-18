# 剧本工作台与节奏体检（script-studio）

## 一、背景与目标

- 背景：创作者直接让通用大模型写短剧，产出的几乎都是「小说腔」——铺垫长、对白书面、没有钩子、
  一集讲不完一个爽点。短剧真正吃饭的本事是**节奏**：前 3 秒抓住人、每 15 秒给一个信息点、
  结尾必须留悬念。这套隐性规则不写进产品，创作者只能靠反复重生成碰运气，是「AI 写不出短剧味」的根因。
- 目标：把短剧节奏规则显性化为可生成、可校验的结构；一句话梗概到可拍剧本（分集大纲 + 逐场台词）
  全程不超过 4 次交互；每集剧本给出可解释的节奏体检分，创作者据此定点重写而非整集重抽。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望输入一句话梗概就得到全剧分集大纲（每集标题 + 梗概 + 结尾钩子），
  以便先把骨架定下来再逐集展开。
- 用户故事二：作为 `USER`，我希望单集剧本按「场次 → 台词」结构产出并标注情绪与出场角色，
  以便下一步能直接拆成镜头。
- 用户故事三：作为 `USER`，我希望系统告诉我这集「哪 15 秒是平的、钩子不够强、台词太书面」，
  以便只重写那一段，而不是整集重来。

- 业务规则：
  1. 剧本分两层：`outline`（全剧分集大纲，作品级）与 `script`（单集剧本，剧集级），先出大纲再出剧本，
     大纲未定稿不允许生成单集剧本。
  2. 单集剧本结构固定为「场次数组」，每个场次含 `scene_no`、`scene_location`、`scene_summary`、
     `line_list`（台词数组：`character_name`、`line_text`、`emotion`、`estimated_sec`）。
  3. 节奏体检（`rhythm_check`）在剧本每次保存 / 生成后自动执行，产出四个维度分（0-100）与总分：
     - `hook_score` 开场钩子：前 3 秒是否有冲突 / 悬念 / 反常；
     - `beat_score` 打点密度：是否每 15 秒至少一个信息点或情绪转折；
     - `dialogue_score` 台词口语度：句长、书面词、旁白占比；
     - `ending_score` 结尾悬念：末尾 5 秒是否留下未解决问题。
     每个维度附 `issue_list`（定位到 `scene_no` / 行号的具体问题与改写建议），总分低于 60 时
     禁止把剧本置为 `READY`（可强制覆盖，但须勾选确认，且记录 `force_ready_flag`）。
  4. 时长对齐：`estimated_sec` 按台词字数与情绪推算，全集合计须落在
     `episode_duration_sec ± 15%` 内，超出时体检给出「超时 / 不足 X 秒」提示并标出可删场次。
  5. 局部重写：支持选中单个场次或单条台词发起 `TEXT` 任务重写，只替换选中片段，
     其余内容与 `script_version` 不变；每次生成保存一个版本，可回滚到任意历史版本（保留最近 20 版）。
  6. 剧本定稿（`script_status=READY`）后才允许进入分镜拆解；定稿后再改剧本会把该集
     `storyboard_status` 标为 `DRAFT` 并提示受影响的镜头数（不自动删除已生成素材）。
     例外：定稿后仅错别字级的文字修改（不改变场次划分与台词切分，只替换个别字词）
     不回落 `storyboard_status`，避免改一个字就把整集分镜打回。
  7. 角色首次出现时自动登记为该作品的角色卡候选（见 `character-bible.md`），不重复登记同名角色。
  8. 重新生成大纲仅更新大纲层（各集标题 / 梗概 / 结尾钩子），**不覆盖已定稿集的正文**——
     已写好的单集剧本 `scene_json` 一律保留。执行前须展示影响范围与费用说明
     （如「重新生成 30 集大纲 约 0.15 元，已写好的 15 集剧本不会被覆盖」）；
     梗概修改触发重新生成大纲时同样先给出该说明。
     空态语义：无大纲时入口文案为「生成大纲」，已有大纲时才是「重新生成大纲」。

## 三、验收标准

1. 输入一句话梗概 + 集数，生成 30 集大纲，每集含标题、梗概、结尾钩子，且相邻集之间钩子与开场衔接。
2. 单集剧本生成后自动出现节奏体检卡片，四维度分与总分可见，`issue_list` 每条可点击定位到对应场次。
3. 总分低于 60 时「定稿」按钮不可直接提交，勾选强制确认后方可提交，且 `force_ready_flag=1` 落库。
4. 选中一个场次点「重写」，仅该场次内容变化，其它场次逐字不变，版本号 +1 且可回滚。
5. 全集 `estimated_sec` 合计与 `episode_duration_sec` 偏差超过 15% 时，体检给出明确的秒数差与建议删改场次。
6. 剧本已定稿的剧集再次修改剧本，该集 `storyboard_status` 变为 `DRAFT` 并提示受影响镜头数；
   仅错别字级文字修改（不改变场次与台词切分）时 `storyboard_status` 保持不变。
7. 已有大纲时点「重新生成大纲」，先展示影响范围与费用说明（集数、约多少元、
   已写好的 N 集剧本不会被覆盖），确认后执行且已定稿集正文逐字不变；无大纲时入口文案为「生成大纲」。

## 四、接口契约（snake_case）

- `POST /bgssai/user/scripts/outline-generate`
  ```json
  { "work_id": "w-10001", "one_line_pitch": "被退婚的落魄千金三年后带资本回归",
    "episode_count": 30, "episode_duration_sec": 90, "genre": "REVENGE" }
  ```
  返回 `task_id`（`task_type=TEXT`），结果异步回写，前端按 `task_id` 轮询（见 `shot-generation.md` 任务中心口径）。
- `GET /bgssai/user/scripts/outline?work_id=w-10001`
- `POST /bgssai/user/scripts/outline-save`（整份大纲保存，含各集 `episode_title` / `episode_synopsis` / `ending_hook`）
- `POST /bgssai/user/scripts/generate`（单集剧本生成：`episode_id` + `extra_prompt`）
- `GET /bgssai/user/scripts/detail?episode_id=e-20001`
  ```json
  { "result": {
      "episode_id": "e-20001", "script_version": 7, "script_status": "DRAFT",
      "estimated_total_sec": 94, "target_duration_sec": 90,
      "scene_list": [
        { "scene_no": 1, "scene_location": "婚礼现场", "scene_summary": "退婚当众发生",
          "line_list": [
            { "line_no": 1, "character_name": "沈知微", "line_text": "退婚书我签，但有个条件。",
              "emotion": "COLD", "estimated_sec": 3 } ] } ],
      "rhythm_check": {
        "total_score": 72, "hook_score": 85, "beat_score": 64,
        "dialogue_score": 78, "ending_score": 60,
        "issue_list": [
          { "dimension": "beat_score", "scene_no": 3, "line_no": 12,
            "issue_desc": "第 30 至 48 秒无情绪转折，观众易划走",
            "suggestion": "把第 4 场的身份揭露提前到此处" } ] } } }
  ```
- `POST /bgssai/user/scripts/save`（整集保存，含 `script_version` 乐观锁）
- `POST /bgssai/user/scripts/rewrite`（局部重写：`episode_id` + `scene_no` + 可选 `line_no` + `rewrite_instruction`）
- `POST /bgssai/user/scripts/ready`（定稿：`episode_id` + `force_ready_flag`）
- `GET /bgssai/user/scripts/versions?episode_id=e-20001` · `POST /bgssai/user/scripts/rollback`
- 鉴权：全部需 `Jwttoken`；作品归属校验按 JWT `user_id`。
- 错误码：`4003` 越权 / `4006` 大纲未定稿即生成剧本、体检不达标未强制确认 / `4009` 版本冲突（`script_version` 不匹配）/ `429` 限流。

## 五、流程 / 状态机（须配图）

生成与体检链路见 [`flow-script-generation.html`](./flow-script-generation.html)。

`script_status` 流转：`NONE` → `GENERATING` → `DRAFT` →（体检通过或强制确认）→ `READY`；
`READY` 状态下保存操作回落到 `DRAFT`，并联动该集 `storyboard_status=DRAFT`
（仅错别字级文字修改除外，见业务规则 6：`script_status` 与 `storyboard_status` 均不回落）。

## 六、数据模型影响

- 新增表 `short_script`：`script_id`(PK)、`episode_id`、`work_id`、`user_id`、`script_version`、
  `script_status`、`scene_json`（场次与台词结构化正文）、`estimated_total_sec`、
  `rhythm_json`（体检结果）、`total_score`、`force_ready_flag`、`created_time`、`updated_time`。
- 新增表 `short_script_version`：`version_id`(PK)、`script_id`、`script_version`、`scene_json`、
  `total_score`、`change_summary`、`created_time`（保留最近 20 版，超出按时间淘汰）。
- 全剧大纲存 `short_work.outline_json`，同时把每集标题 / 梗概回写 `short_episode`。

## 七、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/script-studio.html`](../demo-static/web/user/script-studio.html)
  （每集一行大纲、点开直接编辑整集剧本；展开集内含「历史版本」抽屉——复用
  `GET /bgssai/user/scripts/versions` 展示最近版本列表、`POST /bgssai/user/scripts/rollback` 回滚；
  台词行内展示合规预检命中（命中片段高亮 + 风险类别中文 + 改写建议 + 「采纳改写」），
  「定稿」按钮旁展示剧本门（`G1_SCRIPT`）与合规预检状态徽标，中文文案，
  预检契约见 `content-compliance.md`）。
- 跳转关系：`work-detail.html` → `script-studio.html` → `storyboard.html`，登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`docs/demo-static/web/user/script-studio.html` 及 `PAGE-FLOW.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`；AI 为 MockAiProvider）
- [x] 代码与数据库
