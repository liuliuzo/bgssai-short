# 测试用例：角色卡与场景卡（character-bible）

- **功能文档**: `docs/feature/character-bible.md`
- **范围**: in-scope
- **验收标准数**: 7
- **生成用例数**: 7
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-CHARACTE-BIBLE-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：剧本定稿后进入角色卡页，剧本中出现的角色全部以 `CANDIDATE` 状态列出，外观描述与出场场次 | 剧本定稿后进入角色卡页，剧本中出现的角色全部以 `CANDIDATE` 状态列出，外观描述与出场场次 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-CHARACTE-BIBLE-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：定妆候选张数可选 2 / 4 / 8 并当场显示价格，选定后角色卡状态变 `LOCKED` 并展示定妆照； | 定妆候选张数可选 2 / 4 / 8 并当场显示价格，选定后角色卡状态变 `LOCKED` 并展示定妆照； | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-CHARACTE-BIBLE-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：同一角色卡在同一集连续 10 个镜头生成的画面，人工抽检面部与服装一致（不少于 9/10）。 | 同一角色卡在同一集连续 10 个镜头生成的画面，人工抽检面部与服装一致（不少于 9/10）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-CHARACTE-BIBLE-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：为角色新增一套造型后，镜头引用处可切换 `outfit_code`，切换后生成的画面换装但面部一致。 | 为角色新增一套造型后，镜头引用处可切换 `outfit_code`，切换后生成的画面换装但面部一致。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-CHARACTE-BIBLE-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：修改已 `LOCKED` 角色卡的定妆照，系统按镜列出受影响镜头与重生成预估费用，批量重生成弹窗 | 修改已 `LOCKED` 角色卡的定妆照，系统按镜列出受影响镜头与重生成预估费用，批量重生成弹窗 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-CHARACTE-BIBLE-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：存在多张待建卡时，「全部待建卡一键出候选」显示总价，确认后一次性提交全部候选生成任务。 | 存在多张待建卡时，「全部待建卡一键出候选」显示总价，确认后一次性提交全部候选生成任务。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-CHARACTE-BIBLE-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：删除被引用的角色卡返回 `4006` 并列出引用它的镜头数。 | 删除被引用的角色卡返回 `4006` 并列出引用它的镜头数。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
