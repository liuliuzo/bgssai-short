# 测试用例：成片发布到 bgssai-media（film-publish-media）

- **功能文档**: `docs/feature/film-publish-media.md`
- **范围**: in-scope
- **验收标准数**: 8
- **生成用例数**: 8
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-FILM-PUBLISH-MEDIA-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：READY + APPROVED 成片可发布，返回 `media_id`、`play_url`、`status`。 | READY + APPROVED 成片可发布，返回 `media_id`、`play_url`、`status`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-PUBLISH-MEDIA-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：未 READY / 已过期 / 未过审 / 无素材 → 拒绝且不调 media；文案可读。 | 拒绝且不调 media；文案可读。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-PUBLISH-MEDIA-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：重复点击发布不炸：已 READY 回放既有 `media_id`，不新建目录；落库最近一次结果与 `media_publish_log`。 | 重复点击发布不炸：已 READY 回放既有 `media_id`，不新建目录；落库最近一次结果与 `media_publish_log`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-PUBLISH-MEDIA-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：用户端成片区有「发布到 Media」与「批量发布 READY 集」；成功可打开/复制播放链接。 | 用户端成片区有「发布到 Media」与「批量发布 READY 集」；成功可打开/复制播放链接。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-PUBLISH-MEDIA-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：缺 base-url / ingest-token 时 preflight 警告。 | 缺 base-url / ingest-token 时 preflight 警告。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-PUBLISH-MEDIA-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：批量返回 `total` / `succeeded` / `failed` / `progress_percent` / `item_list`。 | 批量返回 `total` / `succeeded` / `failed` / `progress_percent` / `item_list`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-PUBLISH-MEDIA-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：单测覆盖 payload、闸门、重试；本地 media 404 不影响单测。 | 单测覆盖 payload、闸门、重试；本地 media 404 不影响单测。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-PUBLISH-MEDIA-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：文档明确 unpublish 为 skip。 | 文档明确 unpublish 为 skip。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
