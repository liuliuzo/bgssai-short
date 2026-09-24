# 测试用例：三个问题体验主线（ux-three-questions）

- **功能文档**: `docs/feature/ux-three-questions.md`
- **范围**: in-scope
- **验收标准数**: 9
- **生成用例数**: 9
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-UX-THREE-QUESTION-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：看样片页可按镜连播整集：竖屏画框 + 字幕 + 镜头信息，画面取当前最高预演级素材，缺素材以文字稿占位。 | 看样片页可按镜连播整集：竖屏画框 + 字幕 + 镜头信息，画面取当前最高预演级素材，缺素材以文字稿占位。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：观感自检卡展示钩子秒位、首个反转位置、结尾句、全集时长，数据与节奏体检一致（不另算一套）。 | 观感自检卡展示钩子秒位、首个反转位置、结尾句、全集时长，数据与节奏体检一致（不另算一套）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：样片页任一镜点击可跳到分镜工作台或文字预演对应镜（预填定位），本页无编辑入口。 | 样片页任一镜点击可跳到分镜工作台或文字预演对应镜（预填定位），本页无编辑入口。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：连贯性中心一张清单收敛三层问题，每条带定位与 `fix_action`；镜头层数据与文字预演页逐条一致。 | 连贯性中心一张清单收敛三层问题，每条带定位与 `fix_action`；镜头层数据与文字预演页逐条一致。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：集间检查能检出「上集钩子下集未接」：E14 结尾抛出母亲名字，E15 开场若无人提及则命中 `HOOK_MISMATCH`。 | 集间检查能检出「上集钩子下集未接」：E14 结尾抛出母亲名字，E15 开场若无人提及则命中 `HOOK_MISMATCH`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：伏笔台账列出全部伏笔与状态；剧终仍 `PLANTED` 的伏笔出现在质量档案。 | 伏笔台账列出全部伏笔与状态；剧终仍 `PLANTED` 的伏笔出现在质量档案。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：从首页到任一集卡点修复位置，点击路径不超过 3 次（首页接着做 → 直达工作台定位）。 | 直达工作台定位）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：剧集看板多选 16 至 30 集执行「批量生成剧本」，先展示预估费用，确认后产生批量 `TEXT` 任务。 | 剧集看板多选 16 至 30 集执行「批量生成剧本」，先展示预估费用，确认后产生批量 `TEXT` 任务。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-UX-THREE-QUESTION-09 | AC-09 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：三个问题入口在作品工作台常驻，状态摘要与真实数据一致。 | 三个问题入口在作品工作台常驻，状态摘要与真实数据一致。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
