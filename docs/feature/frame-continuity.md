# 首尾帧衔接（frame-continuity）

## 一、背景与目标

- 背景：现在每个镜头只有首帧（`first_frame_asset_id`），视频按「首帧 + 提示词」生成。
  结果是每一镜各拍各的：上一镜结束时人站在门左边、手抬着、逆光，下一镜开头人可能瞬移到门右边、
  手放下、顺光。观众看到的是一串**互相不认识的画面**。创作者现在只能靠反复重抽碰运气，
  或者在提示词里手写「延续上一镜」——而模型根本看不到上一镜长什么样。
- 目标：相邻镜头能接上。上一镜的最后一帧直接作为下一镜的开头依据，把「跳」压在肉眼可接受范围内；
  支持首尾帧的模型还能同时收首帧与尾帧，由模型在两帧之间补运动。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望同一场戏里相邻两镜的画面能接上，人物位置、姿态、光线不要突变。
- 用户故事二：作为 `USER`，我希望能给一个镜头单独指定尾帧，让它「往哪个画面收」，
  而不是抽完才知道收在哪。
- 用户故事三：作为 `USER`，我希望改了上一镜之后，被它带着走的下游镜头会被标出来，
  而不是等我合成完才发现接不上了。

- 业务规则：
  1. **镜头新增尾帧**：`short_shot` 新增 `last_frame_asset_id`。尾帧有两个来源：
     - **自动抽帧**（默认）：该镜视频生成成功后，用本机 ffmpeg 抽出最后一帧存为素材。
       这条路免费、且与视频画面必然一致。
     - **单独出图**：还没出视频但想先定「这一镜往哪收」时，按尾帧提示词单独跑一次 `IMAGE`，
       按 `IMAGE` 单价计费。
  2. **衔接模式**（`frame_link_mode`，镜头级）：
     - `NONE`（默认）：各拍各的，与现状一致。
     - `INHERIT_PREV`：把**上一镜的尾帧**作为本镜首帧参考送进生成。
     - `BOOKEND`：同时把本镜自己的首帧与尾帧送进模型（要求所选视频模型支持首尾帧，
       见规则 5），由模型在两帧之间补运动。
  3. **衔接只在同一场次内生效（关键约束）**：`INHERIT_PREV` 仅在**相邻且 `scene_no` 相同**的
     镜头之间成立。跨场次时自动降级为 `NONE` 并在界面标注「跨场不衔接」。
     理由：跨场硬接会把两个不同场景的画面糊在一起——上一场是雨夜车里，下一场是白天办公室，
     强行拿雨夜的尾帧当办公室的开头，比不接更糟。**换场本来就该跳**，这是剪辑语言，不是缺陷。
  4. **上一镜没有尾帧时不阻断**：`INHERIT_PREV` 但上一镜还没出视频、也没单独出尾帧时，
     本镜按 `NONE` 正常生成，并在界面标注「上一镜尚无尾帧，本次未衔接」——不静默、也不卡住。
  5. **模型能力按配置判定**：视频模型是否支持首尾帧，取 `ai_model_config.config_json.support_last_frame`
     （1 支持 / 缺省 0 不支持）。选了不支持的模型却设了 `BOOKEND` 时，提交被拒（`4006`）
     并提示换模型或改用 `INHERIT_PREV`。不做「悄悄丢掉尾帧照样出片」的兜底——
     用户以为按首尾帧生成、实际只用了首帧，是最难查的一类问题。
  6. **上游变更的影响标记**：某镜的尾帧变化（重抽视频 / 重出尾帧）时，把**下一镜**（同场次、
     且 `frame_link_mode=INHERIT_PREV`）置 `stale_flag=1`，复用现有的过期提示机制，
     页面提示「上一镜尾帧已变，本镜可能接不上」。不自动重抽——重抽要花钱，花不花由创作者定。
  7. **整集一键开启**：提供「本集同场次镜头全部衔接」的批量开关，一次把该集所有同场次相邻镜头
     置 `INHERIT_PREV`，跨场处自动留 `NONE`。
  8. **抽帧依赖 ffmpeg**：与成片合成同一条口径——运行 / 部署机需预装 ffmpeg 并在 PATH 中；
     缺失时抽帧任务明确失败并提示「本机未安装 ffmpeg」，不静默跳过。

## 三、验收标准

1. 同一场次内镜 5 设为 `INHERIT_PREV` 后生成视频，请求中携带的首帧参考即镜 4 的尾帧素材。
2. 镜 6 与镜 5 的 `scene_no` 不同时，即便设了 `INHERIT_PREV` 也按 `NONE` 生成，
   界面标注「跨场不衔接」。
3. 上一镜尚无尾帧时，本镜照常生成成功，界面标注「上一镜尚无尾帧，本次未衔接」。
4. 视频生成成功后自动产生尾帧素材（`asset_type=IMAGE`，与首帧同规格），镜头
   `last_frame_asset_id` 落库。
5. 选择 `config_json.support_last_frame != 1` 的模型并设 `BOOKEND` 时，提交返回 `4006`
   并提示换模型。
6. 重抽镜 4 的视频后，镜 5（同场次、`INHERIT_PREV`）`stale_flag=1`，页面出现提示，
   且系统**不**自动为镜 5 提交新任务。
7. 「本集全部衔接」一次生效：同场次相邻镜头全部变 `INHERIT_PREV`，跨场处保持 `NONE`。
8. 部署机未装 ffmpeg 时抽帧任务失败并提示「本机未安装 ffmpeg」，视频本身不受影响。

## 四、接口契约（snake_case）

- `POST /bgssai/user/shots/frame-link-save`
  ```json
  { "shot_id": "s-40005", "frame_link_mode": "INHERIT_PREV" }
  ```
  响应 `{ "result": { "shot_id": "s-40005", "frame_link_mode": "INHERIT_PREV",
  "effective_flag": 1, "downgrade_reason": null } }`
  （`effective_flag=0` 且 `downgrade_reason=CROSS_SCENE` 表示跨场自动降级）
- `POST /bgssai/user/shots/frame-link-batch`
  ```json
  { "episode_id": "e-20001", "frame_link_mode": "INHERIT_PREV" }
  ```
  响应 `{ "result": { "linked_count": 36, "cross_scene_count": 6 } }`
- `POST /bgssai/user/shots/last-frame-generate`
  ```json
  { "shot_id": "s-40005", "last_frame_prompt": "她转身推门，背影留在画面右侧", "model_code": "..." }
  ```
  响应 `{ "result": { "task_id": "t-..." } }`（`task_type=IMAGE`、`target_type=LAST_FRAME`）
- `GET /bgssai/user/shots/list?episode_id=` 增加返回 `last_frame_asset_id`、`last_frame_url`、
  `frame_link_mode`、`link_effective_flag`、`link_hint`。
- 鉴权：需 `Jwttoken`；错误码：`50000001` 参数非法 / `4003` 越权 /
  `4006` 所选模型不支持首尾帧 / `4007` 余额不足 / `429000` 限流。

## 五、流程 / 状态机（须配图）

衔接判定与降级规则见 [`flow-frame-continuity.html`](./flow-frame-continuity.html)。

`frame_link_mode`：`NONE` / `INHERIT_PREV` / `BOOKEND`，镜头级、可随时改，改动不触发重抽。
尾帧变化把下一镜（同场次且 `INHERIT_PREV`）置 `stale_flag=1`，由创作者决定是否重抽。

## 六、数据模型影响

- `short_shot` 新增列：`last_frame_asset_id` VARCHAR(64)（尾帧素材）、
  `frame_link_mode` VARCHAR(16) DEFAULT 'NONE'（衔接模式）、
  `last_frame_prompt` VARCHAR(1024)（单独出尾帧时的提示词）。
- `ai_model_config.config_json` 约定键 `support_last_frame`（1 支持首尾帧），不加列。

## 七、原型

- 涉及端：web 用户端
- 页面：`../demo-static/web/user/storyboard.html`（分镜工作台内新增衔接列与批量开关）
- 跳转关系：无新增页面，`PAGE-FLOW.html` 无需增行
- 页面 -> API 映射：同步 `../api/page-api-mapping.md` 的 `storyboard.html` 行

## 八、四层同步检查清单

- [x] 需求（本文件 / `PRODUCT_VISION.md`）
- [x] 原型（`docs/demo-static/web/user/storyboard.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`、`docs/api/page-api-mapping.md`）
- [x] 代码与数据库（双端 `src/**`、用户端前端工程、`sql/DDL.sql`）
