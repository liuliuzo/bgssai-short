# 优秀短剧质量流水线（quality-pipeline）

> 本文件是本产品的**主线功能**：其余功能解决「能不能做出来」，本功能解决「做出来的好不好」。
> 它横跨剧本、分镜、画面、成片四个阶段，不是一个独立页面，而是贯穿全流程的一条闭环。

## 一、背景与目标

- 背景：AI 让「做出一部短剧」变得容易，但做出来的绝大多数是**平庸**的——开场留不住人、
  中段没爽点、人设立不住、镜头语言单调、画面偶尔崩坏、成片音画错位。创作者的真实困境不是产不出，
  而是产出后**不知道差在哪、也不知道该改哪一句**，只能整集重抽，越抽越随机。
  优秀短剧从来不是一次生成碰运气碰出来的，是「对标结构 → 多方案择优 → 多视角评审 → 定点返工」
  循环出来的。这套循环在专业剧组里由编审、导演、剪辑师承担，本产品要把它交给 AI 承担。
- 目标：让平台产出的短剧**默认就在水准线以上**，且创作者始终知道下一步该改什么。可度量口径：
  1. 每部剧在四道质量门上有明确分数与阈值，未达标必有定点返工清单（不是「再试一次」）；
  2. 关键节点（首集开场、集末钩子、关键反转镜头）默认走多方案择优，而非单方案将就；
  3. 任一次「强制放行」都留痕，作品完成后可复盘「这部剧在哪里将就了」。
- 关联角色：`USER`（接受评审与返工建议）、`PLATFORM_ADMIN`（维护爆款结构模板库与门禁阈值）。

## 二、需求澄清（WHAT）

### 2.1 结构对标：不让 AI 自由发挥

- 用户故事一：作为 `USER`，我希望选一个「爆款结构模板」，让 AI 按经过验证的节拍来写，
  而不是自由发挥出一篇小说。

- 业务规则：
  1. 平台维护**结构模板库** `drama_reference_template`（管理端维护，用户端只读选用），
     每个模板按题材给出：
     - `hook_type_list`：开场钩子类型（如身份反差 / 当众羞辱 / 死而复生 / 误会顶点）；
     - `beat_sheet_json`：单集节拍表（第 0-3 秒钩子、3-30 秒建立、30-60 秒升级、60-85 秒反转、
       末 5 秒悬念），每个节拍标注目的与时长区间；
     - `turning_point_json`：全剧反转位（如第 3 / 8 / 15 / 24 集为大反转集）；
     - `taboo_list`：该题材的常见踩雷点（人设崩、逻辑断、爽点前置过早）。
  2. 新建作品时必选一个结构模板（可选「不套用模板」，但界面提示质量风险）；
     模板作为硬约束注入大纲与剧本生成提示词，并作为剧本门评分的对照基准。
  3. 模板不锁死创作：用户可在作品内覆盖任一节拍（`template_override_json`），覆盖处在评分时按
     用户设定而非模板判定。

### 2.2 四道质量门禁

- 用户故事二：作为 `USER`，我希望每个阶段过关前系统先替我把关，告诉我这一关是多少分、
  哪几处拖了后腿、该怎么改。

- 业务规则：
  4. 四道门与判定内容：

  | 门 | `gate_code` | 判定内容 | 依赖 |
  | --- | --- | --- | --- |
  | 剧本门 | `G1_SCRIPT` | 节奏体检四维度（复用 `script-studio.md` 的 `rhythm_check`，本功能不另定义）+ 人设一致性（同一角色前后言行是否自洽）+ 节拍覆盖率（对照结构模板）+ 反转有效性 | 剧本定稿前 |
  | 分镜门 | `G2_STORYBOARD` | 景别分布（避免连续三镜同景别）+ 情绪高点是否给到 `CLOSE` / `EXTREME_CLOSE` + 运镜多样性 + 单镜时长节奏（避免全片匀速）+ 关键信息镜是否缺失 | 提交批量生成前 |
  | 画面门 | `G3_VISUAL` | 角色一致性抽检（同角色跨镜头面部差异）+ 画面瑕疵（肢体畸变、多指、文字乱码、糊图）+ 风格统一度 + 首帧构图是否切题 | 镜头定稿前 |
  | 成片门 | `G4_FILM` | 音画对齐 + 字幕断句与错别字 + 黑帧 / 跳帧 + 首 3 秒与末 5 秒有效性 + 整集时长偏差 | 提交审核前 |

  5. 每道门产出 `gate_score`（0-100）、`gate_status` 与 `issue_list`（定位 + 原因 + 建议 + 一键返工动作）。
     默认阈值 75；**首集阈值 85**（首集决定完播与追更，标准更高，见规则 8）。阈值由管理端维护，
     作品级可上调不可下调。
  6. 未达标不硬性阻断：允许**强制放行**（`FORCE_PASSED`），但必须逐条确认，且写入作品质量档案；
     `BLOCK` 级合规问题除外——那是合规红线，由 `content-compliance.md` 硬拦截，与质量门无关。
  7. 每条 `issue` 必须可执行：给出 `fix_action`（`REWRITE_SCENE` 重写场次 / `ADJUST_SHOT` 调整镜头参数 /
     `REGENERATE_SHOT` 重抽镜头 / `REDUB_LINE` 重配台词 / `RECOMPOSE` 重新合成）与目标对象 id，
     点击直达对应页面并预填参数。严禁产出「节奏偏慢」这类无法执行的评语。

### 2.3 首集特权与关键节点多方案择优

- 用户故事三：作为 `USER`，我希望在最要命的几个地方（首集开场、集末钩子、关键反转镜头）
  一次看到几个不同方案，挑最好的，而不是接受 AI 给的第一版。

- 业务规则：
  8. **首集特权**：第 1 集自动启用更高阈值（默认 85）与更多方案数（开场默认 3 版），
     因为首集的完播与追更决定整部剧的命运。
  9. **多方案择优**（`drama_variant`）适用节点：单集开场（前 3 秒）、集末钩子、
     结构模板标注的反转集关键镜头、作品封面。每个节点生成 N 版（默认 3，上限 5），
     由 AI 评审团打分排序，人工终选；未选中的方案保留可回看，不重复计费。
  10. 多方案的费用在发起前预估并计入预算闸门（口径同 `shot-generation.md`），
      默认只在上述关键节点启用，避免全片 N 倍成本。

### 2.4 AI 评审团：三个视角，各说各话

- 用户故事四：作为 `USER`，我希望评审意见来自不同立场，而不是一个笼统的分数。

- 业务规则：
  11. 评审团固定三个视角（`judge_role`），各自独立打分与一句话结论：
      - `AUDIENCE` 观众视角：会不会在这一秒划走；
      - `CENSOR` 平台审核视角：能不能过审、有没有擦边（与合规预检互补，此处只做质量层面提醒）；
      - `DIRECTOR` 导演视角：镜头有没有把事讲清楚、情绪有没有递进。
  12. 三视角**分歧超过 20 分**时，该条目标记 `divergent_flag=1` 并在界面高亮——分歧处正是需要人来拍板的地方，
      系统不替创作者调和，只把冲突摆清楚。
  13. 评审走 `TEXT` 任务，计费口径同其它 AI 任务；同一对象重复评审有 5 分钟结果缓存，避免连点烧钱。

### 2.5 质量档案与复盘

- 用户故事五：作为 `USER`，我希望一部剧做完能看到它的质量档案，知道哪里将就了、下一部该注意什么。

- 业务规则：
  14. 作品级质量档案记录：四道门每集的分数与趋势、返工次数、强制放行处及其理由、
      多方案择优的选择记录、各集最终 `overall_quality_score`（四门加权：剧本 35%、分镜 20%、
      画面 25%、成片 20%）。
  15. 档案只读、不可编辑，作为创作者复盘与平台质量运营的共同依据。

## 三、验收标准

1. 新建作品时可选结构模板；选用后生成的大纲，其节拍与反转集位置与模板一致（覆盖处除外）。
2. 剧本定稿前自动跑 `G1_SCRIPT`，产出分数与 `issue_list`；每条 issue 点击可直达对应场次并预填重写指令。
3. 第 1 集的门禁阈值为 85，其余集为 75；未达标时定稿 / 提交按钮进入「需强制放行」态，
   放行须逐条确认并写入质量档案。
4. 提交批量生成前自动跑 `G2_STORYBOARD`，能检出「连续三镜同景别」「情绪高点未给特写」并给出可执行修改。
5. 画面生成完成后 `G3_VISUAL` 能检出角色一致性偏差与明显画面瑕疵，返工建议直接指向具体 `shot_no`。
6. 合成后 `G4_FILM` 能检出音画错位、字幕断句问题与首尾无效帧，`fix_action` 可一键跳转修复。
7. 首集开场默认产出 3 个方案，评审团三视角各自给分与结论；分歧超 20 分的条目高亮标记。
8. 作品完成后质量档案可查：四门分数趋势、返工次数、全部强制放行处及理由。
9. 同一对象 5 分钟内重复发起评审不重复计费，返回缓存结果。

## 四、接口契约（snake_case）

- `GET /bgssai/user/quality/templates?genre=REVENGE`（结构模板库，只读）
  ```json
  { "result": { "template_list": [
      { "template_id": "tpl-001", "template_name": "复仇逆袭 · 当众打脸型", "genre": "REVENGE",
        "hook_type_list": ["当众羞辱", "身份反差"],
        "turning_point_json": [3, 8, 15, 24],
        "beat_sheet_json": [
          { "beat_no": 1, "start_sec": 0, "end_sec": 3, "beat_name": "钩子",
            "beat_purpose": "用冲突或反常抓住注意力" },
          { "beat_no": 2, "start_sec": 3, "end_sec": 30, "beat_name": "建立",
            "beat_purpose": "交代处境与对立面" } ],
        "taboo_list": ["爽点前置过早导致后段无牌可打", "反派智商掉线"] } ] } }
  ```
- `POST /bgssai/user/quality/gate-run`（`gate_code` + `episode_id`；返回 `task_id`）
- `GET /bgssai/user/quality/gate-result?gate_code=G1_SCRIPT&episode_id=e-20001`
  ```json
  { "result": {
      "gate_code": "G1_SCRIPT", "gate_name": "剧本门",
      "gate_score": 71, "threshold_score": 85, "gate_status": "FAILED",
      "first_episode_flag": 1,
      "judge_list": [
        { "judge_role": "AUDIENCE", "judge_role_name": "观众视角", "judge_score": 62,
          "judge_comment": "第 30 秒后连续对白无新信息，容易划走" },
        { "judge_role": "CENSOR", "judge_role_name": "平台审核视角", "judge_score": 88,
          "judge_comment": "无明显擦边表达" },
        { "judge_role": "DIRECTOR", "judge_role_name": "导演视角", "judge_score": 74,
          "judge_comment": "反转铺垫不足，动机交代缺一场" } ],
      "divergent_flag": 1,
      "issue_list": [
        { "issue_id": "qi-1001", "dimension": "beat_coverage",
          "issue_desc": "模板第 4 拍（60-85 秒反转）在本集缺失",
          "suggestion": "把第 5 场的身份揭露提前到 62 秒处",
          "fix_action": "REWRITE_SCENE", "target_type": "SCENE", "target_id": "5",
          "scene_no": 5, "severity": "MAJOR" } ] } }
  ```
- `POST /bgssai/user/quality/force-pass`（`gate_code` + `episode_id` + `force_reason`，`force_reason` 必填）
- `POST /bgssai/user/quality/variants-generate`（`variant_node` + `target_id` + `variant_count`；返回 `batch_id`）
- `GET /bgssai/user/quality/variants?variant_node=EPISODE_OPENING&target_id=e-20001`
  （各方案素材、三视角评分、`recommended_flag`）
- `POST /bgssai/user/quality/variant-select`（`variant_id`）
- `GET /bgssai/user/quality/profile?work_id=w-10001`
  （质量档案：各集四门分数、`overall_quality_score`、返工次数、强制放行清单）
- 管理端：`GET /bgssai/admin/quality/templates/list` · `POST /bgssai/admin/quality/templates/create` ·
  `/update` · `/toggle-enabled`；`POST /bgssai/admin/quality/threshold-save`（门禁阈值）
- 鉴权：用户端需 `Jwttoken`；管理端另需 `PLATFORM_ADMIN`。
- 错误码：`4003` 越权 / `4006` 前置阶段未就绪即跑门禁、强制放行未填理由 / `4007` 余额不足 / `429` 限流。

## 五、流程 / 状态机（须配图）

四道门的位置、评审团与返工闭环见 [`flow-quality-pipeline.html`](./flow-quality-pipeline.html)。

`gate_status`：`NOT_RUN` → `RUNNING` → `PASSED`（`gate_score` >= 阈值）/ `FAILED`（低于阈值）；
`FAILED` 可经强制放行转 `FORCE_PASSED`（留痕）。上游内容变更时该门回落 `NOT_RUN`，须重跑。

## 六、数据模型影响

- 新增表 `drama_reference_template`：`template_id`(PK)、`template_name`、`genre`、`hook_type_json`、
  `beat_sheet_json`、`turning_point_json`、`taboo_json`、`enabled_flag`、`created_time`、`updated_time`。
- 新增表 `drama_quality_gate`：`gate_id`(PK)、`work_id`、`episode_id`、`user_id`、`gate_code`、
  `gate_score`、`threshold_score`、`gate_status`、`judge_json`（三视角评分与结论）、`divergent_flag`、
  `force_pass_flag`、`force_reason`、`run_task_id`、`created_time`、`updated_time`。
- 新增表 `drama_quality_issue`：`issue_id`(PK)、`gate_id`、`episode_id`、`dimension`、`issue_desc`、
  `suggestion`、`fix_action`、`target_type`、`target_id`、`scene_no`、`shot_no`、`severity`、
  `resolved_flag`、`created_time`。
- 新增表 `drama_variant`：`variant_id`(PK)、`work_id`、`episode_id`、`variant_node`、`target_id`、
  `variant_no`、`asset_id`、`content_json`、`judge_json`、`avg_score`、`recommended_flag`、
  `selected_flag`、`source_task_id`、`created_time`。
- 作品级新增列：`drama_work.template_id`、`drama_work.template_override_json`、
  `drama_work.quality_threshold_score`、`drama_work.overall_quality_score`。
- 索引：`drama_quality_gate(episode_id, gate_code)`、`drama_quality_issue(gate_id, resolved_flag)`、
  `drama_variant(variant_node, target_id)`。

## 七、补充枚举（登记回 `drama-work-management.md` 附录，此处仅列新增项）

| 枚举 | 值 |
| --- | --- |
| `gate_code` 质量门 | `G1_SCRIPT` 剧本门 / `G2_STORYBOARD` 分镜门 / `G3_VISUAL` 画面门 / `G4_FILM` 成片门 |
| `gate_status` 门禁状态 | `NOT_RUN` 未运行 / `RUNNING` 评审中 / `PASSED` 已通过 / `FAILED` 未达标 / `FORCE_PASSED` 强制放行 |
| `judge_role` 评审视角 | `AUDIENCE` 观众视角 / `CENSOR` 平台审核视角 / `DIRECTOR` 导演视角 |
| `fix_action` 返工动作 | `REWRITE_SCENE` / `ADJUST_SHOT` / `REGENERATE_SHOT` / `REDUB_LINE` / `RECOMPOSE` |
| `severity` 问题严重度 | `CRITICAL` 致命 / `MAJOR` 主要 / `MINOR` 次要 |
| `variant_node` 多方案节点 | `EPISODE_OPENING` 单集开场 / `EPISODE_HOOK` 集末钩子 / `KEY_SHOT` 关键反转镜头 / `WORK_COVER` 作品封面 |

## 八、原型

- 涉及端：web 用户端（评审与返工）+ web 管理端（模板库与阈值）。
- 页面：[`../demo-static/web/user/quality-report.html`](../demo-static/web/user/quality-report.html)
  （四门总览 + 三视角评审 + 返工清单 + 多方案择优 + 质量档案）；
  门禁状态条内嵌于 `script-studio.html` / `storyboard.html` / `compose-export.html`；
  管理端模板库与阈值在 [`../demo-static/web/admin/quality-templates.html`](../demo-static/web/admin/quality-templates.html)。
- 跳转关系：登记在两端各自的 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 九、四层同步检查清单

- [x] 需求（本文件；枚举同步登记到 `drama-work-management.md` 附录）
- [x] 原型（用户端 `quality-report.html` + 管理端 `quality-templates.html`，及两端 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`）
- [ ] 代码与数据库
