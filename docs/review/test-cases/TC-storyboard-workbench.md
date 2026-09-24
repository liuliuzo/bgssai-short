# 测试用例：分镜工作台（storyboard-workbench）

- **功能文档**: `docs/feature/storyboard-workbench.md`
- **范围**: in-scope
- **验收标准数**: 6
- **生成用例数**: 6
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-STORYBOA-WORKBENC-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：对一集定稿剧本执行拆镜，10 秒内返回镜头表（异步任务，页面显示进度），镜头数与场次覆盖完整， | 对一集定稿剧本执行拆镜，10 秒内返回镜头表（异步任务，页面显示进度），镜头数与场次覆盖完整， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-WORKBENC-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：每个镜头默认带齐景别 / 运镜 / 时长 / 台词 / 出场角色，无需人工补齐即可提交生成。 | 每个镜头默认带齐景别 / 运镜 / 时长 / 台词 / 出场角色，无需人工补齐即可提交生成。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-WORKBENC-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：镜头详情面板展示 `image_prompt` 合成结果，并分段标注来源（画风 / 场景卡 / 角色卡 / 镜头参数 / 补充）； | 镜头详情面板展示 `image_prompt` 合成结果，并分段标注来源（画风 / 场景卡 / 角色卡 / 镜头参数 / 补充）； | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-WORKBENC-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：拆分 / 合并 / 移动 / 插入 / 删除后 `shot_no` 连续无空洞，且不影响其它镜头已有素材。 | 拆分 / 合并 / 移动 / 插入 / 删除后 `shot_no` 连续无空洞，且不影响其它镜头已有素材。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-WORKBENC-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：引用未定妆角色的镜头被标红且「生成」按钮禁用，点击提示直达角色卡页。 | 引用未定妆角色的镜头被标红且「生成」按钮禁用，点击提示直达角色卡页。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-WORKBENC-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：重复拆镜时已 `LOCKED` 镜头的素材与参数保持逐字不变（系统按 `scene_no` + `line_no` | 重复拆镜时已 `LOCKED` 镜头的素材与参数保持逐字不变（系统按 `scene_no` + `line_no` | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
