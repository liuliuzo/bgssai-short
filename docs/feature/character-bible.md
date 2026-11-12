# 角色卡与场景卡（character-bible）

## 一、背景与目标

- 背景：AI 短剧最致命、也最劝退的痛点是**一致性**——同一个女主，第 3 镜是圆脸、第 8 镜变尖下巴，
  换了场景连发型和衣服都变了；场景同理，同一间办公室每镜都换装修。观众一眼出戏，创作者只能反复抽卡，
  成本翻几倍还未必对得上。根因是每次生成都是独立的一次「文生图」，上下文不共享。
- 目标：把角色 / 场景从「每次现写的提示词」升级为**可复用的资产卡**：一次定妆，全剧锁定；
  所有镜头生成强制引用资产卡，人物与场景跨镜头、跨集一致。目标口径：同一角色在一集内的
  跨镜头一致性人工抽检通过率不低于 90%。
- 关联角色：`USER`。

## 二、需求澄清（WHAT）

- 用户故事一：作为 `USER`，我希望给女主做一次「定妆」，选定一张形象照后，全剧所有镜头里她都长这样。
- 用户故事二：作为 `USER`，我希望常用场景（婚礼现场、总裁办公室）也能存成卡片复用，
  以便同一场景在不同集里保持同一套布景。
- 用户故事三：作为 `USER`，我希望角色卡上直接绑定音色，配音时不用再逐集挑一遍。

- 业务规则：
  1. 角色卡 `short_character` 挂在**作品**级，跨全部剧集复用；剧本定稿时除角色名外同步抽取
     外观描述与出场场次清单，预填 `appearance_prompt` 后进入「待建卡」列表（`card_status=CANDIDATE`）；
     剧本中出现的新场景名同样自动创建场景卡（`card_status=CANDIDATE`）。待建卡不自动生成图，
     避免无谓消耗。
  2. 定妆流程：确认或修改预填的外观描述（`appearance_prompt`：年龄 / 体型 / 发型 / 面部特征 / 服装）→
     生成候选（`candidate_count` 可选 2 / 4 / 8，默认 4，提交前当场显示对应价格）→
     选定 1 张为 `ref_image_asset_id`（定妆照）→ `card_status=LOCKED`。
     也可以上传自己的 JPG、PNG 或 WebP（不超过 6MB）。上传图进入该卡候选，并写入已挂到定妆的知识库；
     锁定后全剧按这张图走。见 `look-knowledge.md`。
     候选区提供「换一批（同参数同价）」与「微调描述后重出」快捷入口（均为再次生成候选）；
     另提供「全部待建卡一键出候选」批量入口，提交前显示总价。
     未 `LOCKED` 的角色卡不允许被镜头引用（生成时拦截并提示先定妆）。
  3. **一致性锁**：镜头生成时，系统把 `ref_image_asset_id`（图生图参考）+ `appearance_prompt`
     + `consistency_seed` 一并下发给图像模型，三者构成该角色的一致性指纹，用户不可在单镜内改写，
     只能改「本镜表演相关」的字段（表情、动作、镜头参数）。需要改形象只能改角色卡本身。
  4. 角色卡支持多套「造型」（`outfit_list`：如现代装 / 婚纱 / 三年后职业装），每套造型有各自的
     定妆照与 `outfit_code`；镜头引用角色时选择 `outfit_code`，默认取 `default_outfit_code`。
     造型用于处理剧情跨越时间线的换装需求，同一角色不同造型仍共享面部一致性指纹。
  5. 角色卡绑定音色 `voice_id` 与 `voice_speed`（见 `voice-subtitle.md`），配音默认取此绑定。
  6. 场景卡 `short_scene_card` 结构与角色卡对齐（描述 + 候选 + 定妆图 + 锁定），
     `card_type` 取 `SCENE`（场景）或 `PROP`（关键道具）。
  7. 改角色卡定妆照会影响后续生成，不回溯已生成镜头；界面须提示「已有 N 个镜头使用旧形象，
     是否加入重生成队列」，并按镜列出重生成预估费用（`estimated_cost_cent`）；批量重生成弹窗
     按镜可勾选，默认只勾选非 `LOCKED` 镜头（已定稿的不动），确认后批量加入重生成
     （走 `shot-generation.md` 的批量任务）。MVP 实现：`lockCharacter` / `lockScene` 将引用
     该卡且未定稿的镜头置 `stale_flag=1`（已定稿不动）。
  8. **一致性下发（MVP）**：分镜拆分按角色名/场景名写入 `character_ref_json` / `scene_card_id`。
     IMAGE 把已定妆 `ref_image_asset_id` 作为 subject 参考图（可灵 `image_reference=subject`）
     并拼接外观描述；VIDEO 至少把外观描述写入提示词。提交时未定妆即拦截。
  8. 删除角色卡：被镜头引用中的角色卡不允许删除，须先解除引用。

## 三、验收标准

1. 剧本定稿后进入角色卡页，剧本中出现的角色全部以 `CANDIDATE` 状态列出，外观描述与出场场次
   已预填，无需手工录入；新场景名同步出现在场景卡待建卡列表。
2. 定妆候选张数可选 2 / 4 / 8 并当场显示价格，选定后角色卡状态变 `LOCKED` 并展示定妆照；
   候选不满意可「换一批（同价）」或「微调描述后重出」；未定妆角色出现在镜头引用列表时
   被禁用并提示「请先完成定妆」。
3. 同一角色卡在同一集连续 10 个镜头生成的画面，人工抽检面部与服装一致（不少于 9/10）。
4. 为角色新增一套造型后，镜头引用处可切换 `outfit_code`，切换后生成的画面换装但面部一致。
5. 修改已 `LOCKED` 角色卡的定妆照，系统按镜列出受影响镜头与重生成预估费用，批量重生成弹窗
   默认只勾选非 `LOCKED` 镜头、可逐镜调整；不点则已有镜头不变。
6. 存在多张待建卡时，「全部待建卡一键出候选」显示总价，确认后一次性提交全部候选生成任务。
7. 删除被引用的角色卡返回 `4006` 并列出引用它的镜头数。

## 四、接口契约（snake_case）

- `GET /bgssai/user/characters/list?work_id=w-10001`
  ```json
  { "result": { "character_list": [
      { "character_id": "c-30001", "work_id": "w-10001", "character_name": "沈知微",
        "character_role": "FEMALE_LEAD", "card_status": "LOCKED",
        "appearance_prompt": "26 岁女性，鹅蛋脸，黑色长直发，冷感眼神，身形清瘦",
        "consistency_seed": 88213417,
        "ref_image_asset_id": "a-50012", "ref_image_url": "/mock/char-shen.png",
        "default_outfit_code": "MODERN_SUIT",
        "outfit_list": [
          { "outfit_code": "MODERN_SUIT", "outfit_name": "三年后职业装",
            "ref_image_asset_id": "a-50012" },
          { "outfit_code": "WEDDING", "outfit_name": "婚纱", "ref_image_asset_id": "a-50013" } ],
        "voice_id": "v-female-cold-01", "voice_speed": 1.0,
        "used_shot_count": 42 } ] } }
  ```
- `POST /bgssai/user/characters/create`（`work_id` + `character_name` + `character_role` + `appearance_prompt`）
- `POST /bgssai/user/characters/portrait-generate`（`character_id` + `outfit_code` + `candidate_count`，
  取 2 / 4 / 8，默认 4；返回 `task_id`）
- `POST /bgssai/user/characters/portrait-generate-batch`（`work_id` + 可选 `character_id_list`，
  缺省为作品下全部待建卡；返回 `batch_id` 与总价 `estimated_cost_cent`；场景卡同构）
- `POST /bgssai/user/characters/lock`（`character_id` + `outfit_code` + 选中的 `ref_image_asset_id`）
- `POST /bgssai/user/characters/update` · `POST /bgssai/user/characters/delete`
- `POST /bgssai/user/characters/bind-voice`（`character_id` + `voice_id` + `voice_speed`）
- `GET /bgssai/user/assets/consistency-panel?work_id=w-10001`（角色 / 场景 / 道具三栏 + 引用计数，见 `asset-consistency-panel.md`）
- `POST /bgssai/user/shots/bind-assets`（镜头显式绑定角色、场景、道具）
- `GET /bgssai/user/scene-cards/list?work_id=w-10001&card_type=SCENE`
- `POST /bgssai/user/scene-cards/create` · `/portrait-generate` · `/lock` · `/update` · `/delete`
- `GET /bgssai/user/characters/affected-shots?character_id=c-30001`（改卡前查受影响镜头；返回
  `shot_list`[`shot_id` · `shot_no` · `shot_status` · `estimated_cost_cent`] 与合计 `estimated_cost_cent`，
  供批量重生成弹窗按镜勾选，默认只勾选非 `LOCKED` 镜头）
- 鉴权：需 `Jwttoken`，按 JWT `user_id` 校验作品归属。
- 错误码：`4003` 越权 / `4006` 未定妆即被引用、删除被引用卡 / `4010` 一致性指纹缺失（模型未返回可用参考图）。

## 五、流程 / 状态机（须配图）

一致性指纹的产生与在镜头生成中的引用链见
[`flow-character-consistency.html`](./flow-character-consistency.html)。

`card_status` 流转：`CANDIDATE`（剧本抽取待建卡，外观描述与场次清单已预填）→ `DRAFT`（已确认描述未定妆）→
`GENERATING`（存在排队或运行中的候选任务）→ `DRAFT`（候选已完成，待选择）→ `LOCKED`（选定真实候选，可被引用）。
候选任务结束不得覆盖用户已完成的锁定；同卡仍有其他活跃候选任务时仍显示 `GENERATING`。
一张角色、场景或道具卡出参考图时，只占用该卡的按钮；其它卡可以同时打开确认并提交，
页面不用整页加载态挡住后续点击。参考图缩略图可点击放大。
历史遗留的 `GENERATING` 在没有活跃任务时按 `DRAFT` 展示，不把图像任务成功等同于定妆锁定。
`LOCKED` 后实际修改名称或外观描述回落 `DRAFT`；保留历史候选供重新选择，旧参考图不再通过锁定校验。
相同描述重复保存不使镜头过期。场景与道具通过 `POST /bgssai/user/scene-cards/update`、
`scene_card_id`、可选 `card_name` / `appearance_prompt` 编辑已有卡，不能变更作品、归属或卡类型。
角色、场景和道具的显式外观描述直接作为参考图提示词，只有未填写描述时才回退卡名；
不自动附加中文名称、重复模板或自动翻译。模型文本编码器仍可能截断长描述，应优先填写关键视觉信息。

## 六、数据模型影响

- 新增表 `short_character`：`character_id`(PK)、`work_id`、`user_id`、`character_name`、`character_role`、
  `card_status`、`appearance_prompt`、`consistency_seed`、`default_outfit_code`、`outfit_json`、
  `voice_id`、`voice_speed`、`deleted_flag`、`created_time`、`updated_time`。
- 新增表 `short_scene_card`：`scene_card_id`(PK)、`work_id`、`user_id`、`card_type`、`card_name`、
  `appearance_prompt`、`consistency_seed`、`ref_image_asset_id`、`card_status`、`deleted_flag`、时间戳。
- 唯一约束：`short_character(work_id, character_name, deleted_flag)`。
- 引用计数不落库，按 `short_shot.character_ref_json` 实时统计。

## 七、原型

- 涉及端：web 用户端。
- 页面：[`../demo-static/web/user/character-bible.html`](../demo-static/web/user/character-bible.html)
  （角色卡网格 + 定妆候选选择 + 造型页签 + 场景卡分区）。
- 跳转关系：`work-detail.html` / `storyboard.html` → `character-bible.html`，登记在用户端 `PAGE-FLOW.html`。
- 页面 → API 映射：`../api/page-api-mapping.md` 对应行。

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（`docs/demo-static/web/user/character-bible.html` 及 `PAGE-FLOW.html`）
- [x] 设计与契约（`docs/api/SHORT-PRODUCTION.md`；AI 为 MockAiProvider）
- [x] 代码与数据库
