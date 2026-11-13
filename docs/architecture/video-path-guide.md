# 出视频步骤设计

作品页顶部步骤条只在用户端 React 里计算下一步，不新增后端接口，不改表。

## 判断

打开作品或切换剧集时读取已有数据：

- `GET /bgssai/user/scripts/outline`
- `GET /bgssai/user/scripts/detail`
- `GET /bgssai/user/assets/consistency-panel`
- `GET /bgssai/user/shots/list`
- `GET /bgssai/user/ai-tasks/list`

进行中的任务（`QUEUED` / `RUNNING`）按 `target_type` 停在对应步骤：`OUTLINE`、`SCRIPT`、`PORTRAIT`、`SCENE_PORTRAIT`、`SHOT`（再按 `IMAGE` / `VIDEO`）。

未锁定定妆：`card_status` 不是 `LOCKED`，或没有 `ref_image_asset_id`。角色、场景、道具都算。一次只处理列表里的第一张。

## 按钮调用的已有接口

| 步骤 | 调用 |
| --- | --- |
| 大纲 | `POST /bgssai/user/scripts/outline-generate` |
| 剧本 | `POST /bgssai/user/scripts/generate` |
| 定稿 / 强制定稿 | `POST /bgssai/user/scripts/ready`（`force_ready_flag` 0 或 1） |
| 锁定已有定妆 | `POST /bgssai/user/characters/lock` 或 `POST /bgssai/user/scene-cards/lock` |
| 出定妆并锁定第一张 | `POST /bgssai/user/characters/portrait-generate` 或 `POST /bgssai/user/scene-cards/portrait-generate`，`candidate_count=2`；成功后再 lock |
| 拆镜 | `POST /bgssai/user/shots/split-from-script`，然后 `POST /bgssai/user/previz/generate-text` |
| 人话分镜 | `POST /bgssai/user/previz/generate-text` |
| 首帧 / 视频 | `POST /bgssai/user/ai-tasks/estimate` 确认后 `POST /bgssai/user/ai-tasks/submit`，`auto_next_flag=0` |

模型用对应能力的默认项：`GET /bgssai/user/ai-models/list`。

主按钮会切到已有页签：剧本、角色场景、分镜、成片。不新增路由。
