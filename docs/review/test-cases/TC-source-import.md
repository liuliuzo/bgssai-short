# 测试用例：原著导入与改编（source-import）

- **功能文档**: `docs/feature/source-import.md`
- **范围**: in-scope
- **验收标准数**: 9
- **生成用例数**: 9
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-SOURCE-IMPORT-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：粘贴一部 12 万字网文，导入成功并自动切出章节，章节数与原文一致；章节标题可逐条改， | 粘贴一部 12 万字网文，导入成功并自动切出章节，章节数与原文一致；章节标题可逐条改， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：未勾选权利确认时导入被拒（`50000001`），提示语明确指向勾选项。 | 未勾选权利确认时导入被拒（`50000001`），提示语明确指向勾选项。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：超过 100 万字符时导入被拒，提示实际字数与上限。 | 超过 100 万字符时导入被拒，提示实际字数与上限。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：选择第 1-5 章、目标 6 集，提交前展示涉及字数与预计费用；确认后产生 `TEXT` 任务， | 选择第 1-5 章、目标 6 集，提交前展示涉及字数与预计费用；确认后产生 `TEXT` 任务， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：已有 3 集剧本定稿时再次改编，3 集正文逐字不变，仅大纲层更新，执行前展示影响范围。 | 已有 3 集剧本定稿时再次改编，3 集正文逐字不变，仅大纲层更新，执行前展示影响范围。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：改编生成的单集剧本照常出现节奏体检卡片，总分低于 60 时同样禁止直接定稿。 | 改编生成的单集剧本照常出现节奏体检卡片，总分低于 60 时同样禁止直接定稿。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：导入 `SCRIPT` 源后各集 `script_status=DRAFT`，需人工确认才能置 `READY`。 | 导入 `SCRIPT` 源后各集 `script_status=DRAFT`，需人工确认才能置 `READY`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：源软删后，已生成的作品与剧集仍可正常编辑，页面在出处位置显示「原文已删除」。 | 源软删后，已生成的作品与剧集仍可正常编辑，页面在出处位置显示「原文已删除」。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SOURCE-IMPORT-09 | AC-09 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：一个源可被两部作品分别改编，互不影响。 | 一个源可被两部作品分别改编，互不影响。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
