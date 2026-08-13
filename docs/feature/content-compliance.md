# 合规预检与内容审核（content-compliance）

## 一、背景与目标

- 背景：短剧题材天然贴着红线走（复仇、豪门、擦边、暴力），创作者的两难是：
  写得太安全没人看，写得过火整集白做——而且往往是花完几百块生成费、成片导出后才发现不能用。
  平台侧同样需要在内容出站前有一道可追溯的关卡。
- 目标：把风险发现点**前移到花钱之前**——提示词与台词在提交生成前先过一遍预检；
  成片出站前由平台审核并给出可执行的驳回原因。口径：`BLOCK` 级风险 100% 拦在生成任务提交之前；
  审核结论可定位到具体剧集 / 镜头 / 台词。
- 关联角色：`USER`（预检与提交审核）、`PLATFORM_ADMIN`（审核处置）。

## 二、需求澄清（WHAT）

### 用户端（`USER`）

- 用户故事一：作为 `USER`，我希望在提交生成前就知道哪句台词、哪段提示词有风险，以及为什么。
- 用户故事二：作为 `USER`，我希望被驳回时能看到具体是哪一集、哪个镜头、什么原因，改完能直接重提。

- 业务规则：
  1. **预检触发点**（三处，均在花钱之前）：剧本定稿时（全集台词）、镜头提交生成前（`image_prompt`
     与 `dialogue_text`）、成片提交审核前（整集汇总）。
  2. **风险分级**：`PASS` 通过 / `WARN` 提示（可继续，须知晓）/ `BLOCK` 拦截（不允许继续）。
     每条命中给出 `risk_category`（`POLITICS` 涉政 / `PORN` 色情低俗 / `VIOLENCE` 血腥暴力 /
     `MINOR` 未成年人相关 / `SUPERSTITION` 封建迷信 / `IP_RISK` 疑似侵权 / `OTHER`）、
     命中位置（`scene_no` / `line_no` / `shot_no`）、命中片段与改写建议。
  3. `BLOCK` 镜头的 `blocked_flag=1`，生成提交接口直接拒绝（`4006`）；修改后重新预检自动解除。
  4. **提交审核**：以**剧集**为单位提交（成片 `compose_status=READY` 且未过期）；
     提交后 `review_status=PENDING`，用户不可再改该集素材（编辑入口置只读），撤回后恢复可编辑。
     排队期间用户可见**排队位次**（`queue_position`）与**预计审核时长**（`estimated_review_hours`，
     按队列长度与近期处理速率估算），不让创作者盲等。
  5. 驳回后 `review_status=REJECTED`，页面按 `reject_item_list` **逐项**展示驳回内容与定位
     （见规则 7）；用户修改后重新合成再重提，重提次数不限但每次都留痕。
  5.1 **轻量重合成**：整改后若仅涉及**字幕 / 台词文本类**修改（错别字、断句、字幕措辞、台词文案），
     允许走轻量重合成路径——仅重烧字幕，不整集全量重合成（路径与费用口径与
     `film-compose-export.md` 的「重烧字幕」一致，具体流程见该文件，本文件只做衔接）；
     涉及画面 / 配音 / 镜头内容的整改仍走整集重合成。

### 管理端（`PLATFORM_ADMIN`）

- 用户故事三：作为 `PLATFORM_ADMIN`，我希望在一个队列里按待审时间处理提交，能看成片、看剧本、
  看预检命中，给出通过或带原因的驳回。

- 业务规则：
  6. 审核队列按 `review_status=PENDING` 与提交时间正序，支持按创作者、题材、风险等级筛选。
  7. 审核详情提供成片播放、剧集台词全文、预检命中清单、创作者与作品基本信息；
     处置动作为 `APPROVE` 或 `REJECT`。**驳回必须结构化**：`REJECT` 必填 `reject_item_list`
     （至少一项，替代自由文本驳回），每项含：
     - `risk_category`（风险类别，枚举同预检）；
     - `episode_no` 与定位（`shot_no` 或 `line_no`，二者至少其一）；
     - `reject_desc`（该项驳回说明）；
     - `suggestion`（改写建议，口径与预检 `suggestion` 一致，复用同一改写建议生成逻辑）。
     整体可另附 `review_comment`（选填的总评）。
  8. 审核动作全部落 `drama_review_record`（谁、何时、什么结论、什么原因），不可删除、可追溯。
  9. 管理端对创作内容**只读**：不得修改创作者的剧本、镜头与素材，只能给结论。
  10. 全部剧集审核通过后，作品 `work_status` 推进为 `COMPLETED`。

## 三、验收标准

1. 台词中含 `BLOCK` 级内容时，剧本定稿被拒并定位到 `scene_no` / `line_no`，给出改写建议。
2. 含 `BLOCK` 项的镜头在分镜工作台标红且「生成」按钮禁用；修改文本后重新预检自动解除。
3. `WARN` 级不阻塞流程，但提交生成前弹窗列出全部 `WARN` 项并需确认。
4. 提交审核后该集全部编辑入口变只读；撤回后恢复可编辑。`PENDING` 状态下用户端可见
   排队位次与预计审核时长。
5. 管理端队列按提交时间正序，处理后从待审队列移除并出现在已处理列表。
6. 驳回必须带至少一条结构化驳回项；用户端逐项可见风险类别、集数、镜头或台词定位、
   驳回说明与改写建议；修改重提后重新回到队列。
6.1 整改仅涉及字幕 / 台词文本类修改时，可走轻量重合成（仅重烧字幕）后直接重提，
    不强制整集全量重合成。
7. 全部剧集审核通过后作品状态自动变为 `COMPLETED`。
8. 任一审核动作在 `drama_review_record` 留下不可删除的记录，含审核人 `user_id` 与时间。

## 四、接口契约（snake_case）

### 用户端

- `POST /bgssai/user/compliance/precheck`
  ```json
  { "check_scope": "SCRIPT", "episode_id": "e-20001" }
  ```
  `check_scope` 取 `SCRIPT` / `SHOT` / `EPISODE`；`SHOT` 时传 `shot_id_list`。
  ```json
  { "result": { "risk_level": "WARN",
      "hit_list": [
        { "risk_category": "VIOLENCE", "risk_category_name": "血腥暴力", "risk_level": "WARN",
          "scene_no": 4, "line_no": 9, "shot_no": 17,
          "hit_text": "一刀捅进去", "suggestion": "改为不展示器械接触的冲突表达" } ],
      "block_count": 0, "warn_count": 1 } }
  ```
- `POST /bgssai/user/reviews/submit`（`episode_id`）· `POST /bgssai/user/reviews/withdraw`
- `GET /bgssai/user/reviews/list?work_id=w-10001`
  ```json
  { "result": { "episode_review_list": [
      { "episode_id": "e-20001", "episode_no": 1, "review_status": "PENDING",
        "submitted_time": "2026-08-13 09:40:11",
        "queue_position": 6, "estimated_review_hours": 4 },
      { "episode_id": "e-20002", "episode_no": 2, "review_status": "REJECTED",
        "reviewed_time": "2026-08-12 17:02:40",
        "reject_item_list": [
          { "risk_category": "VIOLENCE", "risk_category_name": "血腥暴力",
            "episode_no": 2, "shot_no": 17, "line_no": null,
            "reject_desc": "镜 17 打斗画面展示器械接触，超出平台尺度",
            "suggestion": "改为不展示器械接触的冲突表达" } ],
        "light_recompose_flag": 0 } ] } }
  ```
  `queue_position` 与 `estimated_review_hours` 仅 `PENDING` 状态返回；
  `light_recompose_flag=1` 表示本次整改仅涉及字幕 / 台词文本类修改、可走轻量重合成（规则 5.1）。

### 管理端

- `GET /bgssai/admin/reviews/list?page_num&page_size&review_status&risk_level&keyword`
  ```json
  { "result": { "review_list": [
      { "review_id": "r-80001", "work_id": "w-10001", "work_title": "退婚三年后她成了首富",
        "episode_id": "e-20001", "episode_no": 1, "genre": "REVENGE",
        "creator_user_id": "u-demo-0001", "creator_name": "演示用户",
        "review_status": "PENDING", "risk_level": "WARN", "warn_count": 1, "block_count": 0,
        "film_duration_sec": 88, "submitted_time": "2026-08-13 09:40:11" } ],
      "page_num": 1, "page_size": 20, "total_size": 37 } }
  ```
- `GET /bgssai/admin/reviews/detail?review_id=r-80001`（成片地址、台词全文、预检命中、历史处置）
- `POST /bgssai/admin/reviews/approve`（`review_id` + `review_comment`）
- `POST /bgssai/admin/reviews/reject`（`review_id` + `reject_item_list`（至少一项，每项含
  `risk_category`、`episode_no`、`shot_no` 或 `line_no`、`reject_desc`、`suggestion`）+
  可选 `review_comment`）
- 鉴权：用户端需 `Jwttoken`；管理端需 `Jwttoken` + `PLATFORM_ADMIN`（角色取自 JWT，不取自参数）。
- 错误码：`4003` 无权限 / `4006` 成片未就绪或已过期即提交、驳回未带结构化驳回项 /
  `4013` 重复提交（已在审核中）。

## 五、流程 / 状态机（须配图）

预检拦截点与审核泳道见 [`state-review-compliance.html`](./state-review-compliance.html)。

`review_status`：`NOT_SUBMITTED` → `PENDING` →（管理端处置）→ `APPROVED` / `REJECTED`；
`REJECTED` 修改重提回到 `PENDING`；成片过期时由 `APPROVED` / `PENDING` 回落 `NOT_SUBMITTED`。

## 六、数据模型影响

- 新增表 `drama_review_record`：`review_id`(PK)、`work_id`、`episode_id`、`creator_user_id`、
  `review_status`、`risk_level`、`warn_count`、`block_count`、`film_asset_id`、`submitted_time`、
  `reviewer_user_id`、`reviewed_time`、`review_comment`、`reject_item_json`（结构化驳回项数组，
  每项含 `risk_category` / `episode_no` / `shot_no` 或 `line_no` / `reject_desc` / `suggestion`）、
  `submit_seq`（第几次提交）、`created_time`、`updated_time`。
- `queue_position` 与 `estimated_review_hours` 为查询时按队列实时派生，不落库。
- 新增表 `drama_compliance_hit`：`hit_id`(PK)、`work_id`、`episode_id`、`shot_id`、`check_scope`、
  `risk_category`、`risk_level`、`scene_no`、`line_no`、`shot_no`、`hit_text`、`suggestion`、
  `resolved_flag`、`created_time`。
- `drama_episode.review_status`、`drama_shot.risk_level` / `blocked_flag` 由本功能维护。
- 审核记录只增不删（`drama_review_record` 无 `deleted_flag`）。

## 七、原型

- 涉及端：web 用户端 + web 管理端。
- 页面：用户端预检结果内嵌于
  [`../demo-static/web/user/script-studio.html`](../demo-static/web/user/script-studio.html)、
  [`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html)、
  [`../demo-static/web/user/compose-export.html`](../demo-static/web/user/compose-export.html)；
  管理端 [`../demo-static/web/admin/content-review.html`](../demo-static/web/admin/content-review.html)（队列）、
  [`../demo-static/web/admin/review-detail.html`](../demo-static/web/admin/review-detail.html)（详情与处置）。
- 跳转关系：分别登记在用户端与管理端各自的 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行（两端各一组）。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（用户端三页内嵌 + 管理端两页，及两端 `PAGE-FLOW.html`）
- [ ] 设计与契约（本次仅同步 `page-api-mapping.md`；安全设计待 `docs/security` 补）
- [ ] 代码与数据库
