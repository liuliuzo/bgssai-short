# 原著导入与改编（source-import）

## 一、背景与目标

- 背景：当前一部作品只有一个入口——「输入一句话梗概」从零生成。这个入口把一整类创作者挡在门外：
  手里握着网文 IP 的、已经写好成品剧本的、在别处做完分集大纲的。这批人恰恰是产量最高、
  最舍得付费的一群（MCN、内容工作室、IP 方），他们要的不是「帮我想个故事」，
  而是「把我这个故事拍出来」。现状下他们只能把原文一段段粘进梗概框里反复试，
  平台既留不住原文，也对不上原文与成片的关系。
- 目标：小说全文、成品剧本、已有大纲都能直接进来，自动分章、按章生成分集大纲与逐集剧本，
  并且每一集都能回查它取自原文哪几章。原著到可拍剧本，不超过 3 次交互。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望把一部网文粘贴或上传进来，让平台自动分章，
  再按章改编成分集大纲，而不是自己一段段喂梗概。
- 用户故事二：作为 `USER`，我希望已经写好的成品剧本能直接导入，跳过剧本生成这一步。
- 用户故事三：作为 `USER`，我希望每一集都标着它取自原文第几章，改稿时能一键回看原文。
- 用户故事四：作为 `USER`，我希望改编前知道这次要花多少钱、涉及多少字。

- 业务规则：
  1. **三种源类型**（`source_type`）：
     - `NOVEL` 小说全文——需要改编成分集大纲与剧本；
     - `SCRIPT` 成品剧本——已有场次与台词，导入后按集切分直接进剧本层，不再生成台词；
     - `OUTLINE` 分集大纲——已有每集梗概，导入后直接落大纲层，跳过大纲生成。
  2. **录入走文本**：粘贴文本与「选择文件」两种入口，前端读取 `.txt` / `.md` 后一律以文本
     提交（`source_content`），后端不接 multipart——上传通道多一条就多一处大小限制、编码判定
     与安全校验要维护，而这里要的只是一段文字。单次上限 100 万字符，超出时明确告知实际字数与上限。
  3. **自动分章，可手工纠正**：按常见章节标题切分（`第X章` / `第X节` / `Chapter X` /
     Markdown 一级二级标题），识别不到就按空行段落聚合成等长块（默认每块约 3000 字）。
     分章结果**必须可编辑**——自动分章在网文里出错是常态，不给纠正入口等于逼用户回去改原文再传一遍。
  4. **原文留底，不是喂完就丢**：原文按章落 `short_source_chapter`，改编后仍可逐章回看、
     可重新改编。作品与源之间是多对一：一个源可以改编出多部作品（不同集数、不同风格各试一版）。
  5. **改编产出大纲时带出处**：每集写入 `source_ref_json`（`source_id` +
     `chapter_no_list`），页面上标注「取自第 3-5 章」并可点开原文对照。
  6. **改编照样过节奏体检**：改编出的剧本与原创剧本走同一套 `rhythm_check`，不因为「有原著」
     就放行。小说改编最典型的翻车就是照搬小说节奏——铺垫长、对白书面、一集讲不完一个爽点，
     这正是体检要拦的东西。**有原著不是节奏合格的理由**。
  7. **费用先算后花**：改编前按选中章节的总字数与目标集数给出预估（`estimated_cost_cent`
     与涉及字数），确认后才提交 `TEXT` 任务；费用与配额口径与 `ai-capability-usage.md` 一致。
  8. **权利确认是硬前置**：导入时必须勾选「我确认对本内容拥有合法使用权或已获授权」
     （`rights_confirm_flag=1`），未勾选不允许导入（`50000001`）。勾选动作与时间戳落库留痕。
     这条不做成可跳过的提示——导入他人网文改编再发行是本产品最现实的法律风险，
     平台至少要把责任边界与留痕做清楚。
  9. **不覆盖已定稿的集**：改编生成大纲时沿用 `script-studio.md` 规则 8——只更新大纲层
     （标题 / 梗概 / 钩子），已定稿集的 `scene_json` 正文逐字保留，执行前展示影响范围。
  10. **`SCRIPT` 源的落法**：按「场次分隔符」切分（`场景` / `INT.` / `EXT.` / `第X场`），
      每集对应一段连续场次，落进 `short_script.scene_json` 并置 `script_status=DRAFT`
      （不是 `READY`）——导进来的剧本仍要过一次体检与人工确认，直接置就绪等于把外部内容
      无审查地放进生产链路。
  11. **删除语义**：源可软删；已改编出的作品与剧集不受影响（原文引用置空并在页面标注
      「原文已删除」），不做级联删除。

## 三、验收标准

1. 粘贴一部 12 万字网文，导入成功并自动切出章节，章节数与原文一致；章节标题可逐条改，
   可合并、可拆分，改完保存后重新进入仍是改后的结果。
2. 未勾选权利确认时导入被拒（`50000001`），提示语明确指向勾选项。
3. 超过 100 万字符时导入被拒，提示实际字数与上限。
4. 选择第 1-5 章、目标 6 集，提交前展示涉及字数与预计费用；确认后产生 `TEXT` 任务，
   完成后作品出现 6 集大纲，每集标注取自的章号。
5. 已有 3 集剧本定稿时再次改编，3 集正文逐字不变，仅大纲层更新，执行前展示影响范围。
6. 改编生成的单集剧本照常出现节奏体检卡片，总分低于 60 时同样禁止直接定稿。
7. 导入 `SCRIPT` 源后各集 `script_status=DRAFT`，需人工确认才能置 `READY`。
8. 源软删后，已生成的作品与剧集仍可正常编辑，页面在出处位置显示「原文已删除」。
9. 一个源可被两部作品分别改编，互不影响。

## 四、接口契约（snake_case）

- `POST /bgssai/user/sources/import`
  ```json
  { "work_id": "w-10001", "source_type": "NOVEL", "source_title": "退婚三年后她成了首富",
    "source_content": "第一章 退婚\n三年前那纸退婚书...", "rights_confirm_flag": 1 }
  ```
  响应 `{ "result": { "source_id": "src-70001", "chapter_count": 42, "char_count": 121840 } }`
- `GET /bgssai/user/sources/list?work_id=w-10001&page_num=1&page_size=20`
  （分页，元素含 `source_id`、`source_type`、`source_title`、`char_count`、`chapter_count`、
  `adapted_flag`、`created_time`）
- `GET /bgssai/user/sources/detail?source_id=src-70001&page_num=1&page_size=50`
  （分页返回章节：`chapter_no`、`chapter_title`、`char_count`、`chapter_text`）
- `POST /bgssai/user/sources/chapters-save`
  ```json
  { "source_id": "src-70001", "chapter_list": [
      { "chapter_no": 1, "chapter_title": "退婚", "chapter_text": "..." } ] }
  ```
  整份替换该源的章节（前端提交调整后的完整分章结果）。
- `POST /bgssai/user/sources/adapt-estimate`
  ```json
  { "source_id": "src-70001", "chapter_no_from": 1, "chapter_no_to": 5, "target_episode_count": 6 }
  ```
  响应 `{ "result": { "char_count": 18420, "estimated_cost_cent": 12, "model_code": "hy4" } }`
- `POST /bgssai/user/sources/adapt`
  （同上参数加可选 `model_code`；返回 `task_id`，`task_type=TEXT`、`target_type=ADAPT_OUTLINE`）
- `POST /bgssai/user/sources/delete`（`source_id`，软删）
- 鉴权：全部需 `Jwttoken`；源归属校验到 `user_id`。
- 错误码：`50000001` 请求参数非法（未勾选权利确认 / 超字数 / 章节范围非法）/ `4003` 越权 /
  `4006` 源已删除或无可用章节 / `4007` 余额不足 / `429000` 限流。

## 五、流程 / 状态机（须配图）

导入与改编管线见 [`flow-source-import.html`](./flow-source-import.html)。

`import_status`：`IMPORTED`（已导入待改编）→ `ADAPTED`（已改编出大纲）。源被软删时置
`deleted_flag=1`，已生成内容不受影响。

## 六、数据模型影响

- 新增表 `short_source_doc`：`source_id`(PK 业务键)、`work_id`、`user_id`、`source_type`、
  `source_title`、`char_count`、`chapter_count`、`rights_confirm_flag`、`rights_confirm_time`、
  `import_status`、`adapted_flag`、`deleted_flag`、`created_time`、`updated_time`。
- 新增表 `short_source_chapter`：`chapter_id`(PK 业务键)、`source_id`、`work_id`、`user_id`、
  `chapter_no`、`chapter_title`、`chapter_text`（LONGTEXT）、`char_count`、`deleted_flag`、
  `created_time`、`updated_time`。
- `short_episode` 新增列 `source_ref_json`：该集取自的源与章号（`{"source_id","chapter_no_list"}`）。

## 七、原型

- 涉及端：web 用户端
- 页面：`../demo-static/web/user/source-import.html`（自新建作品页与总剧本页进入）
- 跳转关系：登记到 `../demo-static/web/user/PAGE-FLOW.html`
- 页面 -> API 映射：同步 `../api/page-api-mapping.md`

## 八、四层同步检查清单

- [x] 需求（本文件 / `PRODUCT_VISION.md`）
- [x] 原型（`docs/demo-static/web/user/source-import.html` 及 `PAGE-FLOW.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`、`docs/api/page-api-mapping.md`）
- [x] 代码与数据库（双端 `src/**`、用户端前端工程、`sql/DDL.sql`）
