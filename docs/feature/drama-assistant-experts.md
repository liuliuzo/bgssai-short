# 功能：AI 戏剧多专家协同（真实文本通道）

版本：2026-11-12。范围：用户端作品工作室「AI 协同专家」抽屉。

## 1. 背景

对照 `waoowaoo` 右侧剧作协同抽屉。旧实现仅前端按角色拼固定文案，不调模型、不读作品上下文、无确认写回。本需求把它接到既有 CN 文本模型通道，保留角色分工与可验证产物。

## 2. 用户故事

1. 作为创作者，我在作品页打开「AI 协同专家」，切换编剧 / 分镜 / 视觉 / 剪辑角色提问，得到基于当前作品上下文的建议。
2. 作为创作者，我可以确认后再把建议写回镜头字段；未确认不改作品。
3. 作为创作者，模型失败时看到诚实可读错误，而不是本地假成功模板。

## 3. 业务规则

1. 角色固定：`SCRIPT` 编剧、`DIRECTOR` 分镜、`VISUAL` 视觉、`EDITOR` 剪辑；共用 CN `TEXT` 通道，不等于假专家——以职责提示、上下文、结构化字段与写回契约区分。
2. 请求必须带 `work_id`；可选 `episode_id` / `shot_id`。服务端校验归属后组装作品标题/梗概、角色名、本集梗概、邻近镜头摘要。
3. 调用既有 `AiModelResolveService` + `AiProviderRegistry` 的 `TEXT` 能力；禁止改跨区路由、禁止静默回落境外中枢。
4. 模型返回解析为结构化建议：`advice_text`、`summary`、`action_list`、`apply_target`、`apply_text`。非 JSON 时把原文放入 `advice_text`，`apply_target=none`，不得伪造成功字段。
5. 写回接口独立：`apply_target` ∈ `shot_desc` / `dialogue_text` / `extra_prompt`；必须用户确认后调用。剪辑默认 `none`。
6. 计费：按 `TEXT` 单价预检额度，成功后扣费；失败不扣。
7. 用户可见错误经 `TokenHubClientSupport.userFacingMessage` 脱敏，不暴露中枢品牌与密钥。

## 4. 验收标准

1. 打开抽屉发送提问时，浏览器 Network 出现 `POST /bgssai/user/drama-assist/advise`，不再出现本地固定对白模板（如「你以为你赢了」）作为唯一回复来源。
2. 请求体含 `work_id` 与 `role`；成功响应含 `advice_text` 与 `context_used`。
3. 点击确认写回后才调用 `POST /bgssai/user/drama-assist/apply`，镜头字段更新；取消不写。
4. 模型不可用时抽屉展示可读失败文案，无假成功。
5. 专家入口按钮使用站内主色，不再使用紫蓝渐变装饰。

## 5. 非目标

- 不做多专家自动互审闭环（可二期）。
- 不改图生视频 / I2V、支付、OAuth 密钥事件、生产库。
- 不在本功能里切换模型路由策略。
