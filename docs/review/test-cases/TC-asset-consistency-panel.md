# 测试用例：资产一致性面板（asset-consistency-panel）

- **功能文档**: `docs/feature/asset-consistency-panel.md`
- **范围**: in-scope
- **验收标准数**: 4
- **生成用例数**: 4
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-ASSET-CONSISTE-PANEL-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：一致性面板按角色 / 场景 / 道具三栏列出，含 `card_status`、`used_shot_count`、定妆缩略图。 | 一致性面板按角色 / 场景 / 道具三栏列出，含 `card_status`、`used_shot_count`、定妆缩略图。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-ASSET-CONSISTE-PANEL-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：勾选镜头后绑定角色 + 场景 + 道具，刷新分镜可见引用芯片；解除引用后芯片消失。 | 勾选镜头后绑定角色 + 场景 + 道具，刷新分镜可见引用芯片；解除引用后芯片消失。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-ASSET-CONSISTE-PANEL-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：引用未定妆道具的镜头提交生成被拒，文案指向「请先锁定道具卡」。 | 引用未定妆道具的镜头提交生成被拒，文案指向「请先锁定道具卡」。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-ASSET-CONSISTE-PANEL-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：锁定道具卡后，引用它且未定稿的镜头出现「定妆已变」。 | 锁定道具卡后，引用它且未定稿的镜头出现「定妆已变」。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
