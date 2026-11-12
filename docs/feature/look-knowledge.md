# 定妆知识库

## 一、背景与目标

定妆不能只靠模型现画。创作者可以上传自己的角色、场景、道具图片，直接锁定为定妆照，也可以把这些图放进知识库，生成候选时作为参考。

## 二、需求澄清

- 用户故事：作为创作者，我上传一张自己的图片，锁定后全剧按这张图保持形象。
- 用户故事：作为创作者，我把多张定妆素材放进知识库。生成该角色或场景的候选时，系统按名称和说明选出最接近的一张，交给图像模型作主体参考。
- 业务规则：
  1. 图片只接受 JPG、PNG、WebP，最大 6MB。
  2. 角色定妆照分四面：正脸、前面、背面、侧面。正脸上传后立即锁定，作为全剧生成时交给模型的那一张。另外三面保存在这张角色卡和知识库里，生成时如果名称对得上，优先用正脸。场景和道具仍是一张参考图。
  3. 知识库可以有多个。新建时默认挂到定妆。归档或关闭「定妆参考」后，生成不再使用里面的图。
  4. 匹配看名称和说明里的汉字是否出现在外观描述中。对上至少 3 个字，或名称被描述包含，才把这张图交给模型。对不上就不附参考图，仍按文字生成。
  5. 上传不扣模型费用。生成候选仍按原价确认。

## 三、验收标准

1. 角色卡上传自己的图片后，候选里出现这张图，锁定后状态为已定妆。
2. 同一张图进入「我的定妆」或创作者指定的已挂载知识库。
3. 知识库页可以新建库、上传图片、移除图片。
4. 外观描述能对上素材名称或说明时，定妆生成把该图作为主体参考；对不上则不附带。

## 四、接口

- `GET /bgssai/user/look-libraries/list`
- `POST /bgssai/user/look-libraries/save`
- `POST /bgssai/user/look-materials/upload`
- `POST /bgssai/user/look-materials/delete`
- `POST /bgssai/user/characters/portrait-upload`
- `POST /bgssai/user/scene-cards/portrait-upload`

鉴权需 `Jwttoken`，只操作当前用户自己的库和卡。

## 五、原型

- [`../demo-static/web/user/look-library.html`](../demo-static/web/user/look-library.html)
- 角色定妆页增加「用我的图片」，见 [`../demo-static/web/user/character-bible.html`](../demo-static/web/user/character-bible.html)
