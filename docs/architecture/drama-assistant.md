# 详设：AI 戏剧多专家协同

## 组件

1. 前端 `DramaAssistantDrawer.jsx`：角色切换、提问、loading/错误展示、确认写回。
2. `DramaAssistController`：`/bgssai/user/drama-assist/advise|apply`。
3. `DramaAssistService`：归属校验、上下文装配、CN TEXT 调用、结构化解析、额度预检与扣费、写回委托 `ShotService.update`。

## 时序

1. 用户发送 → `advise`。
2. 服务端组装上下文 → `pipelineReadiness.assertReady(TEXT)` → resolve 模型 → `assertAffordable`。
3. `AiProvider.execute`；失败映射用户可见文案。
4. 成功扣费并返回结构化建议。
5. 用户确认 → `apply` → 更新 `shot_desc` / `dialogue_text` / `extra_prompt`。

## 约束

- 不改 TokenHub 路由与跨区策略。
- 不自动写回。
- 与图生视频 PR 无文件重叠。
