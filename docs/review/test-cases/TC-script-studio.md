# 测试用例：剧本工作台与节奏体检（script-studio）

- **功能文档**: `docs/feature/script-studio.md`
- **范围**: in-scope
- **验收标准数**: 8
- **生成用例数**: 8
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-SCRIPT-STUDIO-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：输入一句话梗概 + 集数，生成 30 集大纲，每集含标题、梗概、结尾钩子，且相邻集之间钩子与开场衔接。 | 输入一句话梗概 + 集数，生成 30 集大纲，每集含标题、梗概、结尾钩子，且相邻集之间钩子与开场衔接。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SCRIPT-STUDIO-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：单集剧本生成后自动出现节奏体检卡片，四维度分与总分可见，`issue_list` 每条可点击定位到对应场次。 | 单集剧本生成后自动出现节奏体检卡片，四维度分与总分可见，`issue_list` 每条可点击定位到对应场次。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SCRIPT-STUDIO-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：总分低于 60 时「定稿」按钮不可直接提交，勾选强制确认后方可提交，且 `force_ready_flag=1` 落库。 | 总分低于 60 时「定稿」按钮不可直接提交，勾选强制确认后方可提交，且 `force_ready_flag=1` 落库。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SCRIPT-STUDIO-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：选中一个场次点「重写」，仅该场次内容变化，其它场次逐字不变，版本号 +1 且可回滚。 | 选中一个场次点「重写」，仅该场次内容变化，其它场次逐字不变，版本号 +1 且可回滚。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SCRIPT-STUDIO-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：全集 `estimated_sec` 合计与 `episode_duration_sec` 偏差超过 15% 时，体检给出明确的秒数差与建议删改场次。 | 全集 `estimated_sec` 合计与 `episode_duration_sec` 偏差超过 15% 时，体检给出明确的秒数差与建议删改场次。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SCRIPT-STUDIO-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：剧本已定稿的剧集再次修改剧本，该集 `storyboard_status` 变为 `DRAFT` 并提示受影响镜头数； | 剧本已定稿的剧集再次修改剧本，该集 `storyboard_status` 变为 `DRAFT` 并提示受影响镜头数； | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SCRIPT-STUDIO-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：已有大纲时点「重新生成大纲」，先展示影响范围与费用说明（集数、约多少元、 | 已有大纲时点「重新生成大纲」，先展示影响范围与费用说明（集数、约多少元、 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SCRIPT-STUDIO-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：三个文本模型均能独立提交任务，任务中心记录所选 `model_code`；未选模型时使用 Hy4 默认配置。 | 三个文本模型均能独立提交任务，任务中心记录所选 `model_code`；未选模型时使用 Hy4 默认配置。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
