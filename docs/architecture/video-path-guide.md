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

未锁定定妆：`card_status` 不是 `LOCKED`，或没有 `ref_image_asset_id`。角色、场景、道具都算。一次只处理列表里的第一张。

## 八步映射与路由规则

1. 八步环节映射核心工作区：
   - `1 outline` / `2 script` / `3 ready` → `script`
   - `4 looks` → `characters`
   - `5 shots` / `6 frame` / `7 video` → `storyboard`
   - `8 film` → `compose`
2. 点击任一环节跳转 `/works/:workId/<tab>?stage=<key>`，工作区依据 `stage` 参数定位子区块并高亮正在查看的环节；若查看非当前待办环节，提供 `返回当前环节：<环节名>` 按钮。
3. 无 `:tab` 进入 `/works/:workId`（作品列表入口 `/works/:workId`）或切换当前剧集时，等待当前剧集步骤状态判定完成后自动落位到当前待办环节所在工作区；步骤推进或工作区内完成定稿/定妆/拆镜/出图/合成后同步刷新步骤状态与主工作区。
4. `ScriptPane` 在大纲已有数据且当前焦点不在 `outline` 时默认将 `全集大纲` 收折为单行摘要栏（保留 `导入原著` 与 `展开编辑大纲` 入口），直接置顶显示当前剧集剧本与定稿区；`StoryboardPane` 首帧操作按钮统一为 `估首帧费用` / `生成首帧`。
5. 移除主工作区上方重复的 12 页签 `Tabs` 栏；扩展路由（`source`、`voice`、`preview`、`story-map`、`quality`、`continuity`、`tasks`、`edit-export`）保留原路由不变，由页头 `辅助工具` 下拉菜单统一进入；处于辅助工具视图时在工作区顶部提供工具标题与 `返回当前环节` 入口。

## 按钮调用的已有接口

| 步骤 | 调用 |
| --- | --- |
| 大纲 | `POST /bgssai/user/scripts/outline-generate` |
| 剧本 | `POST /bgssai/user/scripts/generate` |
| 定稿 | `POST /bgssai/user/scripts/ready`（`force_ready_flag` 0，低分确认后传 1） |
| 锁定已有定妆 | `POST /bgssai/user/characters/lock` 或 `POST /bgssai/user/scene-cards/lock` |
| 出定妆并锁定第一张 | `POST /bgssai/user/characters/portrait-generate` 或 `POST /bgssai/user/scene-cards/portrait-generate`，`candidate_count=2`；成功后再 lock |
| 拆镜 | `POST /bgssai/user/shots/split-from-script`，然后 `POST /bgssai/user/previz/generate-text` |
| 人话分镜 | `POST /bgssai/user/previz/generate-text` |
| 首帧 / 视频 | `POST /bgssai/user/ai-tasks/estimate` 确认后 `POST /bgssai/user/ai-tasks/submit`，`auto_next_flag=0` |

模型用对应能力的默认项：`GET /bgssai/user/ai-models/list`。
