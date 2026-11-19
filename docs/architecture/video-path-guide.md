# 出视频步骤设计

作品页顶部步骤条只在用户端 React 里计算并驱动主流程切换，不新增后端接口，不改表，不改 `App.jsx` 路由表。

## 判断

打开作品或切换剧集时读取已有数据：

- `GET /bgssai/user/scripts/outline`
- `GET /bgssai/user/scripts/detail`
- `GET /bgssai/user/assets/consistency-panel`
- `GET /bgssai/user/shots/list`
- `GET /bgssai/user/ai-tasks/list`

进行中的任务（`QUEUED` / `RUNNING`）按 `target_type` 停在对应步骤：`OUTLINE`、`SCRIPT`、`PORTRAIT`、`SCENE_PORTRAIT`、`SHOT`（再按 `IMAGE` / `VIDEO`）。

未锁定定妆：`card_status` 不是 `LOCKED`，或没有 `ref_image_asset_id`。`4 定妆` 只算角色与道具，场景在 `5 场景` 单独计。各步一次只处理列表里的第一张；`SCENE_PORTRAIT` 进行中停在场景步，`PORTRAIT` 进行中停在定妆步。

## 七步映射与路由规则

1. 七步环节映射核心工作区：
   - `1 outline` / `2 script` / `3 ready` → `script`
   - `4 looks` → `characters`（人物定妆 + 道具卡）
   - `5 scenes` → `scenes`（只含场景卡，与定妆共用同一个卡片面板组件并按分区渲染）
   - `6 shots`（镜头制作，旧链接 frame、video 仍进入该环节） → `storyboard`
   - `7 film` → `compose`
2. 点击任一环节跳转 `/works/:workId/<tab>?stage=<key>`，工作区依据 `stage` 参数定位子区块并高亮正在查看的环节；若查看非当前待办环节，提供 `返回当前环节：<环节名>` 按钮。
3. 无 `:tab` 进入 `/works/:workId`（作品列表入口 `/works/:workId`）或切换当前剧集时，等待当前剧集步骤状态判定完成后自动落位到当前待办环节所在工作区；步骤推进或工作区内完成定稿/定妆/拆镜/出图/合成后同步刷新步骤状态与主工作区。
4. `ScriptPane` 在大纲已有数据且当前焦点不在 `outline` 时默认将 `全集大纲` 收折为单行摘要栏（保留 `导入原著` 与 `展开编辑大纲` 入口），直接置顶显示当前剧集剧本与定稿区；`StoryboardPane` 顶部单栏提供「上下列表 | 按场次排」切换、「生成首帧」「生成视频」（单步 `estimate` + 确认弹窗后 `submit`）与「更多操作」（含 `frame-link-batch` / `previz/generate-text` / `split-from-script`），全部视频就绪且未勾选时收折为成片与「批量重做」入口；模型选择、局部资产改绑（`bind-assets` 仅传非空字段）与图生视频收在默认折叠的 `更多工具`。
5. 移除主工作区上方重复的 12 页签 `Tabs` 栏；扩展路由（`source`、`voice`、`preview`、`story-map`、`quality`、`continuity`、`tasks`、`edit-export`）保留原路由不变，由页头 `辅助工具` 下拉菜单统一进入；处于辅助工具视图时在工作区顶部提供工具标题与 `返回当前环节` 入口。

## 按钮调用的已有接口

| 步骤 | 调用 |
| --- | --- |
| 大纲 | `POST /bgssai/user/scripts/outline-generate` |
| 剧本 | `POST /bgssai/user/scripts/generate` |
| 定稿 | `POST /bgssai/user/scripts/ready`（`force_ready_flag` 0，低分确认后传 1） |
| 锁定已有定妆 | `POST /bgssai/user/characters/lock` 或 `POST /bgssai/user/scene-cards/lock` |
| 出定妆并锁定第一张 | `POST /bgssai/user/characters/portrait-generate` 或 `POST /bgssai/user/scene-cards/portrait-generate`，`candidate_count=2`；成功后再 lock |
| 拆镜 | `POST /bgssai/user/shots/split-from-script`（同场次后续镜默认置 `INHERIT_PREV`），然后 `POST /bgssai/user/previz/generate-text` |
| 补全画面描述 | `POST /bgssai/user/previz/generate-text` |
| 首帧 / 视频 | `POST /bgssai/user/ai-tasks/estimate` 确认后 `POST /bgssai/user/ai-tasks/submit`，`auto_next_flag=0` |

镜头列表和画布通过 `POST /bgssai/user/shots/update` 保存 `shot_id` 与 `camera_move`。视频生成读取镜头运镜，已有首帧、视频和画面描述保留。

逐镜画面风格通过现有 previz/save 写入画面描述的【画面风格：…】标记。没有逐镜选择时使用既有生成规则。选定风格应用于首帧和视频的画面表现；作品题材仍由作品数据维护。

模型用对应能力的默认项：`GET /bgssai/user/ai-models/list`。
