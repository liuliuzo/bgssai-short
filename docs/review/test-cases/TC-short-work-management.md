# 测试用例：作品与剧集管理（short-work-management）

- **功能文档**: `docs/feature/short-work-management.md`
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 6
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-SHORT-WORK-MANAGEME-01 | AC-01 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：新建作品首屏只有剧名与一句话梗概两个输入，高级设置不展开也能提交；提交后立即返回 `work_id` | 新建作品首屏只有剧名与一句话梗概两个输入，高级设置不展开也能提交；提交后立即返回 `work_id` | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SHORT-WORK-MANAGEME-02 | AC-02 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：作品工作台的剧集看板对每一集展示六个阶段徽标（剧本 / 分镜 / 画面 / 配音 / 合成 / 审核） | 作品工作台的剧集看板对每一集展示六个阶段徽标（剧本 / 分镜 / 画面 / 配音 / 合成 / 审核） | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SHORT-WORK-MANAGEME-03 | AC-03 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：作品列表支持按 `work_status`、`genre` 过滤与按 `updated_time` 排序，分页走 `PageResponse` | 作品列表支持按 `work_status`、`genre` 过滤与按 `updated_time` 排序，分页走 `PageResponse` | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SHORT-WORK-MANAGEME-04 | AC-04 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：已合成成片的剧集执行删除时返回业务错误码 `4006` 并给出可读原因，数据未被删除。 | 已合成成片的剧集执行删除时返回业务错误码 `4006` 并给出可读原因，数据未被删除。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SHORT-WORK-MANAGEME-05 | AC-05 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：任意用户携带他人 `work_id` 调用详情 / 更新 / 删除接口，一律返回 `4003`，且不泄漏该作品是否存在。 | 任意用户携带他人 `work_id` 调用详情 / 更新 / 删除接口，一律返回 `4003`，且不泄漏该作品是否存在。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SHORT-WORK-MANAGEME-06 | B-PAGE | P2 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：分页 page_size 超上限应被夹到默认/最大值，不报 5xx | 分页 page_size 超上限应被夹到默认/最大值，不报 5xx | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 补充边界/负向 |
