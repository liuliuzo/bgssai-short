# 资产一致性面板（asset-consistency-panel）

## 一、背景与目标

- 背景：角色卡、场景卡已能定妆，但创作台上仍像「另开一页的资料库」。镜头多半靠拆镜时按名字自动挂卡，
  创作者看不到、也改不了本镜引用了谁；道具/造型只作为场景卡的 `card_type=PROP`，没有一等公民入口。
  对照 Jellyfish / ArcReel 的能力（只对照交互，不拷贝源码）：资产库要贴在分镜旁边，镜头必须显式引用
  角色 / 场景 / 道具，生成时带上已锁定的参考图与外观描述，减少跨镜漂移。
- 目标：作品级资产一致性面板成为第一入口；镜头可绑定角色、场景、道具；未锁定引用继续拦截生成。
- 关联角色：`USER`。成片播放仍在 `bgssai-media`，本仓不重做播放器。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望在创作台右侧看到本剧角色 / 场景 / 道具，勾选镜头后一键绑定，
  以免每镜重新描述长相和布景。
- 用户故事二：作为 `USER`，我希望道具（婚纱、退婚书、金表）和角色、场景一样能定妆锁定，
  被引用镜头生成时带上同一张参考图。
- 用户故事三：作为 `USER`，我希望改卡或换绑后，未定稿镜头标「定妆已变」，已定稿镜头不动。

- 业务规则：
  1. 资产三类：`CHARACTER`（`short_character`）、`SCENE` / `PROP`（`short_scene_card.card_type`）。
     道具复用场景卡表，不另起一张。
  2. 镜头引用：`character_ref_json`、`scene_card_id`、`prop_ref_json`（字符串 id 数组或
     `{character_id}` / `{scene_card_id}` 对象数组均可）。
  3. 绑定接口覆盖写传入字段；未传的字段保持原值；空数组 / 空字符串表示解除该类引用。
  4. 引用未 `LOCKED` 卡时 `blocked_flag=1`，提交 IMAGE / VIDEO 返回 `4006`。
  5. 拆镜仍按名字自动挂卡（含道具名），之后以面板手工绑定为准。
  6. 改卡锁定或换绑未定稿镜头置 `stale_flag=1`。已 `LOCKED` 镜头不回溯。
  7. 播放成片不在本仓；面板只展示参考缩略图。

## 三、验收标准

1. 一致性面板按角色 / 场景 / 道具三栏列出，含 `card_status`、`used_shot_count`、定妆缩略图。
2. 勾选镜头后绑定角色 + 场景 + 道具，刷新分镜可见引用芯片；解除引用后芯片消失。
3. 引用未定妆道具的镜头提交生成被拒，文案指向「请先锁定道具卡」。
4. 锁定道具卡后，引用它且未定稿的镜头出现「定妆已变」。

## 四、接口契约（snake_case）

- `GET /bgssai/user/assets/consistency-panel?work_id=w-10001`
  ```json
  { "result": {
      "character_list": [
        { "character_id": "c-30001", "character_name": "沈知微", "card_status": "LOCKED",
          "used_shot_count": 42, "ref_image_url": "/bgssai/user/assets/a-50012" } ],
      "scene_list": [ { "scene_card_id": "sc-31001", "card_name": "宴会厅", "card_type": "SCENE",
          "card_status": "LOCKED", "used_shot_count": 12 } ],
      "prop_list": [ { "scene_card_id": "sc-32001", "card_name": "退婚书", "card_type": "PROP",
          "card_status": "LOCKED", "used_shot_count": 5 } ],
      "unbound_shot_count": 2, "unlocked_ref_count": 1 } }
  ```
- `POST /bgssai/user/shots/bind-assets`
  ```json
  { "shot_id": "s-40001", "character_id_list": ["c-30001"],
    "scene_card_id": "sc-31001", "prop_id_list": ["sc-32001"] }
  ```
  返回该镜视图（含 `character_ref_json` / `scene_card_id` / `prop_ref_json` / `blocked_flag`）。
- `GET /bgssai/user/scene-cards/list?work_id=&card_type=PROP`（`card_type` 可选：`SCENE` / `PROP`）
- 鉴权：需 `Jwttoken`。错误码：`4003` 越权 / `4006` 未锁定卡被引用或定稿镜不可改绑。

## 五、流程 / 状态机（须配图）

见 [`flow-asset-consistency-panel.html`](./flow-asset-consistency-panel.html)。
`card_status` 仍走 `character-bible.md`：`CANDIDATE` → `DRAFT` → `GENERATING` → `LOCKED`。

## 六、数据模型影响

- `short_shot` 新增 `prop_ref_json` LONGTEXT：本镜引用的道具卡 id 列表。
- 角色 / 场景表不改；道具继续落 `short_scene_card` 且 `card_type=PROP`。

## 七、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/character-bible.html`](../demo-static/web/user/character-bible.html)
  （三栏资产 + 绑定条）；创作台侧栏见
  [`../demo-static/web/user/storyboard.html`](../demo-static/web/user/storyboard.html)。
- 跳转：`work-detail` / `storyboard` ↔ `character-bible`，登记用户端 `PAGE-FLOW.html`。
- 页面 → API：`../api/page-api-mapping.md`。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`character-bible.html` / `storyboard.html` / `PAGE-FLOW.html`）
- [x] 设计与契约（`docs/architecture/short-production-design.md`、`docs/api/SHORT-PRODUCTION.md`）
- [x] 代码与数据库
