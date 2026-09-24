# 测试用例：站内在线客服（留言 / 会话）

- **功能文档**: `docs/feature/in-app-support.md`
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 5
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-IN-APP-SUPPORT-01 | AC-01 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：用户侧：打开悬浮入口 → **无联系方式表单** → 直接发一条留言 → 看到会话与己方气泡。 | **无联系方式表单** → 直接发一条留言 → 看到会话与己方气泡。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-IN-APP-SUPPORT-02 | AC-02 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：用户侧：在聊天面板点「提工单」→ 生成关联工单（主题可缺省）→ 面板可见工单状态；仍可继续发消息。 | 生成关联工单（主题可缺省）→ 面板可见工单状态；仍可继续发消息。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-IN-APP-SUPPORT-03 | AC-03 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：管理侧：列表 / 详情可见工单状态 → 可更新工单状态；回复会话 → 用户侧可见回复。 | 可更新工单状态；回复会话 → 用户侧可见回复。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-IN-APP-SUPPORT-04 | AC-04 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：关闭会话后用户不可再往该会话发消息，可再开新会话。 | 关闭会话后用户不可再往该会话发消息，可再开新会话。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-IN-APP-SUPPORT-05 | AC-05 | P0 | both | 未登录 / 无业务令牌 | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：访客（未登录）凭 `guest_token` 可建会话并回看；登录用户会话归属本人。 | 访客（未登录）凭 `guest_token` 可建会话并回看；登录用户会话归属本人。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
