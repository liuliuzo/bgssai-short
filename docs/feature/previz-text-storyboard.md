# 文字预演与提示词优化（previz-text-storyboard）

> 本功能插在「剧本定稿」与「画面生成」之间，是**成本与时间的分水岭**：
> 在它之前，改一次是零成本零等待；在它之后，改一次要花钱、还要等几十秒到几分钟。

## 一、背景与目标

- 背景：视频生成一次要等数十秒至数分钟，且不便宜。创作者的实际工作方式却是「看到结果才知道想要什么」——
  于是陷入「生成 → 不满意 → 改提示词 → 再等 → 再不满意」的循环，一个镜头折腾四五轮，
  一集下来时间全耗在等待上，钱也烧掉大半。更糟的是，很多问题根本不是提示词的问题，
  而是**画面构想本身没想清楚**（这一镜到底要拍什么、和上一镜怎么接），却要用最贵的一级去发现它。
- 目标：把试错从「视频级」压到「文字级」。剧本定稿后先产出一份**人话写的文字分镜稿**
  （每镜画面里有谁、在哪、做什么、镜头怎么走、什么光线情绪），创作者在纯文本上把剧情连贯性与画面构想
  改顺——这一层修改**零等待、零费用**；改顺之后再由文字稿派生提示词，并对提示词做体检与低成本预演。
  口径：进入视频生成前的返工，90% 以上应发生在文字层与首帧层，视频级重抽次数每镜不超过 1 次。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

### 2.1 文字分镜稿（人读的那一版）

- 用户故事一：作为 `USER`，我希望先看到一份用大白话写的分镜稿，像读故事板一样从头读到尾，
  在文字上就发现「这一镜跟上一镜接不上」「这里少了个交代」，而不是等画面出来才发现。

- 业务规则：
  1. 拆镜完成后自动为每个镜头生成 `previz_text`（画面文字稿），**中文自然语言、不含提示词术语**，
     固定包含五个要素：**谁**（出场角色与状态）、**在哪**（场景与位置关系）、**做什么**（动作与走位）、
     **怎么拍**（景别与运镜的口语表达，如「镜头从她背后推上去」）、**什么感觉**（光线与情绪）。
  2. 提供**连读模式**：把整集所有镜头的 `previz_text` 按 `shot_no` 顺序连成一篇可通读的文本，
     创作者像读小说分镜一样一口气读完，判断整集是否连贯、是否好看。
  3. `previz_text` **可直接编辑**（这是与 `image_prompt` 的关键区别——提示词只读派生，文字稿可写）。
     编辑后由文字稿反向更新结构化字段与提示词（见 2.3 派生链）。
  4. 支持在文字稿层做镜头级操作：改写本镜、合并 / 拆分、插入一镜、整段重写（选中连续几镜一起重写），
     全部为 `TEXT` 任务，秒级返回、成本极低（千 token 计价）。

### 2.2 连贯性检查（在文字层做，因为在视频层做太贵）

- 用户故事二：作为 `USER`，我希望系统在文字层就告诉我哪里接不上，比如上一镜她还在会议室、
  下一镜突然在走廊却没有转场交代。

- 业务规则：
  5. 对整集 `previz_text` 执行 `continuity_check`，覆盖五类跨镜头断裂：
     - `SPACE` 空间跳变（位置突变且无转场交代）；
     - `LIGHT` 光线突变（同场景相邻镜头光线不一致）；
     - `OUTFIT` 服装 / 造型跳变（同一角色相邻镜头 `outfit_code` 不同且剧情未换装）；
     - `PROP` 道具消失或凭空出现；
     - `EYELINE` 视线与轴线（对话正反打的朝向矛盾）——判定依据为镜头走位数据
       `blocking_json`，由 `story-map.md` 产出，本功能只消费不另算。
  6. 每条命中给出定位（`shot_no`）、原因与修复建议，并可一键按建议改写该镜文字稿；
     连贯性问题不阻塞生成（对应镜头的生成按钮不因此禁用），创作者也可对单条命中执行
     「忽略这条（计入分镜门评分）」——忽略后提示行收起、`resolved_flag=1`（留痕），
     该条仍计入分镜门 `G2_STORYBOARD` 评分（见 `quality-pipeline.md`）。

### 2.3 提示词优化（文字稿 → 提示词，可诊断、可对照、可复用）

- 用户故事三：作为 `USER`，我希望在按下生成之前就知道这条提示词哪里不完整、哪里自相矛盾，
  而不是靠生成结果去猜。

- 业务规则：
  7. **派生链（单向，唯一）**：
     `previz_text`（可编辑，人话）→ 结构化字段（景别 / 运镜 / 光线 / 情绪 / 出场角色 / 场景卡）
     → `image_prompt`（只读派生，模型语言）。
     创作者只在前两层作业，提示词正文不可直接编辑（防止绕过角色一致性指纹，见 `character-bible.md`）。
  8. **提示词体检 `prompt_check`**（提交生成前自动执行，秒级、免费）：
     - **要素齐全度**：主体 / 服装 / 场景 / 动作 / 景别 / 运镜 / 光线 / 风格 八项，缺项逐条列出；
     - **冲突检测**：互斥表达（如「特写」与「全身入镜」、「固定机位」与「镜头快速推进」、
       「夜景」与「明亮日光」）；
     - **冗余与超长**：重复语义、超出模型有效长度的部分（给出建议裁剪处）；
     - **否定词建议**：按题材与画风给出该镜建议补充的负面提示（如「避免多手指」「避免文字水印」）。
     产出 `prompt_score`（0-100）与 `issue_list`，每条可一键采纳（采纳即改结构化字段，不改提示词正文）。
  9. **提示词对照（A/B）**：同一镜可并排看两个版本的提示词及其结构化字段差异（字段级 diff），
     便于判断「这次改动到底改了什么」；对照仅比对文本，不产生生成费用。
  10. **复用到相似镜头**：把本镜的镜头参数（景别 / 运镜 / 光线 / 情绪 / 补充提示）批量套用到
      选中的其它镜头，避免逐镜重复调；套用前展示将被覆盖的字段与镜头数。

### 2.4 阶梯预演（不满意就退回上一级，永远不用视频试错）

- 用户故事四：作为 `USER`，我希望在真正生成视频之前，先花几分钱几秒钟看一眼大概长什么样。

- 业务规则：
  11. 四级阶梯，**成本与等待逐级上升**，任一级不满意都退回上一级改：

  | 级别 | `previz_level` | 产出 | 典型耗时 | 典型成本 | 用途 |
  | --- | --- | --- | --- | --- | --- |
  | 一 | `TEXT` | 文字分镜稿 | 秒级 | 约 0.02 元 / 镜 | 改剧情、改构想、查连贯 |
  | 二 | `DRAFT_IMAGE` | 低清草图（长边 512） | 约 5 秒 | 约 0.10 元 / 镜 | 看构图与人物位置对不对 |
  | 三 | `IMAGE` | 高清首帧（1080x1920） | 约 20 秒 | 约 0.80 元 / 镜 | 定稿画面，作为图生视频的输入 |
  | 四 | `VIDEO` | 视频片段 | 约 60 至 120 秒 | 约 1.60 元 / 4 秒 | 最终成片素材 |

  12. **跳级约束**：未选定高清首帧（`shot_status` 至少 `IMAGE_READY`）的镜头不允许直接提交 `VIDEO`；
      作品设置可关闭该约束（提示成本风险），但默认开启。
  13. 支持**整集批量生成**，批量口径带「目标级别」参数 `target_level`
      （取 `previz_level` 的 `DRAFT_IMAGE` 草图 / `IMAGE` 高清首帧 / `VIDEO` 视频），
      提交前展示镜头数与费用预估；可选「首帧完成后自动接视频」
      （`auto_continue_video_flag`，默认关闭）——开启后每镜首帧出图即自动提交对应视频任务
      （首帧先行天然满足规则 12 的跳级约束，仍逐笔过预算闸门）。
      草图级整集预演（`DRAFT_IMAGE` 批量，一集 40 余镜约 4 元、1 分钟内出齐）
      形成一面「草图墙」，创作者一眼看完整集的画面走向，再决定哪些镜需要回文字层改。
  14. 草图不进素材库正式候选（`draft_flag=1`），不参与合成，仅供预览；升级到 `IMAGE` 时按新任务计费。

## 三、验收标准

1. 拆镜完成后每个镜头都带 `previz_text`，五要素齐全，且不含提示词术语（不出现权重、括号、英文参数）。
2. 连读模式可把整集文字稿连成一篇通读文本，顺序与 `shot_no` 一致，可整段选中重写。
3. 编辑 `previz_text` 后，结构化字段与 `image_prompt` 同步更新，且更新内容与文字改动一致
   （例如把「镜头推上去」改成「固定机位」，`camera_move` 随之由 `PUSH` 变为 `FIXED`）。
4. 连贯性检查能检出空间跳变、光线突变、服装跳变、道具异常、轴线矛盾五类问题并定位到 `shot_no`。
5. 提交生成前自动出 `prompt_check`：缺项、冲突、超长、否定词建议逐条可见，每条可一键采纳。
6. 提示词 A/B 对照可并排展示两版差异，且不产生任何生成费用。
7. 未出高清首帧的镜头提交视频生成时被拒（`4016`），提示先出首帧或在作品设置中关闭跳级约束。
8. 整集草图预演一次出齐 44 张低清草图，总耗时 1 分钟内、费用与预估偏差不超过 10%；
   草图不出现在合成时间轴中。
9. 草图升级到高清首帧时，构图与草图一致（同 `consistency_seed` 与同提示词）。

## 四、接口契约（snake_case）

- `POST /bgssai/user/previz/generate`（`episode_id` + 可选 `shot_id_list`；生成文字稿，返回 `task_id`）
- `GET /bgssai/user/previz/list?episode_id=e-20014`
  ```json
  { "result": {
      "episode_id": "e-20014", "previz_status": "READY",
      "previz_list": [
        { "shot_id": "s-40008", "shot_no": 8,
          "previz_text": "会议室里，沈知微站在长桌尽头，陆时衍在她对面三步远。她把文件推过去，手指停在桌沿。镜头在两人齐腰的高度固定不动，冷蓝色的顶光从上方压下来，气氛僵住。",
          "previz_level": "DRAFT_IMAGE",
          "draft_asset_id": "a-59001",
          "editable_flag": 1, "updated_time": "2026-08-13 14:02:11" } ] } }
  ```
- `POST /bgssai/user/previz/update`（`shot_id` + `previz_text`；返回反向更新后的结构化字段与新 `image_prompt`）
- `POST /bgssai/user/previz/rewrite`（`shot_id_list` + `rewrite_instruction`；整段重写）
- `GET /bgssai/user/previz/continuity-check?episode_id=e-20014`
  ```json
  { "result": { "issue_count": 2,
      "issue_list": [
        { "issue_type": "SPACE", "issue_type_name": "空间跳变",
          "from_shot_no": 12, "to_shot_no": 13,
          "issue_desc": "镜 12 在会议室，镜 13 直接到走廊，中间没有起身或开门的交代",
          "suggestion": "在镜 12 末尾加一个推门起身的动作，或在两镜之间插入一个空镜",
          "fix_action": "REWRITE_PREVIZ", "target_id": "s-40012" } ] } }
  ```
- `GET /bgssai/user/prompts/check?shot_id=s-40008`
  ```json
  { "result": {
      "prompt_score": 72,
      "element_list": [
        { "element": "SUBJECT", "element_name": "主体", "present_flag": 1 },
        { "element": "LIGHTING", "element_name": "光线", "present_flag": 1 },
        { "element": "NEGATIVE", "element_name": "否定词", "present_flag": 0 } ],
      "issue_list": [
        { "issue_type": "CONFLICT", "issue_desc": "同时出现「特写」与「两人同框」，模型会二选一",
          "suggestion": "改为近景，或拆成两个镜头", "fix_action": "ADJUST_SHOT" },
        { "issue_type": "MISSING", "issue_desc": "未给否定词，容易出现多指与文字水印",
          "suggestion": "补充负面提示：多余手指、文字水印、畸变", "fix_action": "APPLY_NEGATIVE" } ] } }
  ```
- `GET /bgssai/user/prompts/diff?shot_id=s-40008&left_version=3&right_version=4`（字段级差异，不计费）
- `POST /bgssai/user/prompts/apply-to-shots`（`source_shot_id` + `target_shot_id_list` + `field_list`；批量套用镜头参数）
- `POST /bgssai/user/previz/draft-generate`（`episode_id` 或 `shot_id_list` + `target_level`
  （`DRAFT_IMAGE` / `IMAGE` / `VIDEO`，默认 `DRAFT_IMAGE`）+ `auto_continue_video_flag`
  （可选，默认 0；仅 `target_level=IMAGE` 时有意义）；批量生成，提交前返回费用预估、确认后返回 `batch_id`）
- `POST /bgssai/user/previz/promote`（`shot_id_list`；草图升级为高清首帧，按新任务计费）
- 鉴权：全部需 `Jwttoken`；按 JWT `user_id` 校验作品归属。
- 错误码：`4003` 越权 / `4006` 剧本或分镜未就绪 / `4007` 余额不足 /
  `4016` 未出首帧即提交视频（跳级约束）/ `429` 限流。

## 五、流程 / 状态机（须配图）

四级阶梯与派生链见 [`flow-previz-prompt.html`](./flow-previz-prompt.html)。

`previz_level`（镜头当前预演到哪一级）：`TEXT` → `DRAFT_IMAGE` → `IMAGE` → `VIDEO`，
逐级升级；任一级退回上一级修改不清除已有产物（草图与首帧都保留，供对照）。
`previz_status`（剧集级）：`NONE` → `GENERATING` → `READY`（全镜有文字稿）。
文字稿被编辑时，该镜 `shot_status` 不回退，但其 `image_prompt` 版本号 +1，
已生成素材标记 `stale_flag=1`（提示「画面与当前构想已不一致」，不自动删除）。

## 六、数据模型影响

- 新增表 `short_shot_previz`：`previz_id`(PK)、`shot_id`、`episode_id`、`work_id`、`user_id`、
  `previz_text`、`previz_level`、`draft_asset_id`、`prompt_score`、`prompt_check_json`、
  `stale_flag`、`created_time`、`updated_time`。
- 新增表 `short_prompt_version`：`version_id`(PK)、`shot_id`、`prompt_version`、`image_prompt`、
  `shot_param_json`（当次结构化字段快照）、`change_summary`、`created_time`（保留最近 10 版，供 A/B 对照）。
- 新增表 `short_continuity_issue`：`issue_id`(PK)、`episode_id`、`work_id`、`issue_type`、
  `from_shot_no`、`to_shot_no`、`issue_desc`、`suggestion`、`resolved_flag`、`created_time`。
- `short_shot` 新增列：`previz_level`、`stale_flag`、`prompt_version`。
- `short_asset` 新增列：`draft_flag`（草图标记，不进合成、不进正式候选）。
- 索引：`short_shot_previz(episode_id, shot_id)`、`short_prompt_version(shot_id, prompt_version)`。

## 七、补充枚举（登记回 `short-work-management.md` 附录）

| 枚举 | 值 |
| --- | --- |
| `previz_level` 预演级别 | `TEXT` 文字稿 / `DRAFT_IMAGE` 低清草图 / `IMAGE` 高清首帧 / `VIDEO` 视频 |
| `previz_status` 文字稿状态 | `NONE` / `GENERATING` / `READY` |
| `continuity_issue_type` 连贯性问题 | `SPACE` 空间跳变 / `LIGHT` 光线突变 / `OUTFIT` 服装跳变 / `PROP` 道具异常 / `EYELINE` 轴线矛盾 |
| `prompt_element` 提示词要素 | `SUBJECT` 主体 / `OUTFIT` 服装 / `SCENE` 场景 / `ACTION` 动作 / `SHOT_SIZE` 景别 / `CAMERA` 运镜 / `LIGHTING` 光线 / `STYLE` 风格 / `NEGATIVE` 否定词 |
| `prompt_issue_type` 提示词问题 | `MISSING` 缺要素 / `CONFLICT` 自相矛盾 / `REDUNDANT` 冗余 / `TOO_LONG` 超长 |

## 八、原型

- 涉及端：web 用户端。
- 页面：原型已并入创作台 [`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html)
  （文字分镜稿连读与逐镜编辑 + 连贯性检查 + 草图墙 + 提示词体检与 A/B 对照）；
  阶梯预演入口与提示词体检结果同时内嵌于
  [`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html) 的镜头详情。
- 跳转关系：`script-studio.html` → `storyboard.html`（创作台，含文字预演）→ `task-center.html`，
  登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 九、四层同步检查清单

- [x] 需求（本文件；枚举同步登记到 `short-work-management.md` 附录）
- [x] 原型（并入 `docs/demo-static/web/user/storyboard.html` 及 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
