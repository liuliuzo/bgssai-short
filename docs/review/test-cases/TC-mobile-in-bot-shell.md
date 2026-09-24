# 测试用例：用户端壳内窄栏布局（mobile-in-bot-shell）

- **功能文档**: `docs/feature/mobile-in-bot-shell.md`
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 5
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-MOBI-IN-BOT-SHEL-01 | AC-01 | P0 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：`?bgssai_shell=1` 或 `?chat_pane=1` 进入窄栏：无侧栏、有底栏、登录为单列。 | `?bgssai_shell=1` 或 `?chat_pane=1` 进入窄栏：无侧栏、有底栏、登录为单列。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MOBI-IN-BOT-SHEL-02 | AC-02 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：无上述参数时，独立桌面布局与本切片前一致。 | 无上述参数时，独立桌面布局与本切片前一致。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MOBI-IN-BOT-SHEL-03 | AC-03 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：壳内跳转到无 query 的路由后仍保持窄栏；`?bgssai_shell=0` 恢复独立布局。 | 壳内跳转到无 query 的路由后仍保持窄栏；`?bgssai_shell=0` 恢复独立布局。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MOBI-IN-BOT-SHEL-04 | AC-04 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：文档与原型不出现密钥、token、口令样例。 | 文档与原型不出现密钥、token、口令样例。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MOBI-IN-BOT-SHEL-05 | AC-05 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：管理端不改。 | 管理端不改。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
