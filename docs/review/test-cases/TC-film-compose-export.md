# 测试用例：成片合成与导出（film-compose-export）

- **功能文档**: `docs/feature/film-compose-export.md`
- **范围**: in-scope
- **验收标准数**: 9
- **生成用例数**: 9
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-FILM-COMPOSE-EXPORT-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：前置条件任一不满足时，「合成本集」按钮禁用并逐条列出缺口（含直达修复页的链接）。 | 前置条件任一不满足时，「合成本集」按钮禁用并逐条列出缺口（含直达修复页的链接）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：时间轴预览四条轨道对齐显示，点击任一镜头可跳转到分镜工作台对应镜头。 | 时间轴预览四条轨道对齐显示，点击任一镜头可跳转到分镜工作台对应镜头。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：一键合成产生 `COMPOSE` 任务，进度可见；成功后页面出现成片播放器与导出面板。 | 一键合成产生 `COMPOSE` 任务，进度可见；成功后页面出现成片播放器与导出面板。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：导出勾选 `WITH_SUBTITLE` + `NO_SUBTITLE` 两个版本，产出两个文件，命名符合规则。 | 导出勾选 `WITH_SUBTITLE` + `NO_SUBTITLE` 两个版本，产出两个文件，命名符合规则。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：重抽任一镜头或重配任一镜头后，该集成片标注「已过期」，`compose_status=NONE`、`expired_flag=1`， | 重抽任一镜头或重配任一镜头后，该集成片标注「已过期」，`compose_status=NONE`、`expired_flag=1`， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：仅修改字幕文本或字幕样式后，成片不置过期，页面提示「字幕已变更」并提供「仅重烧字幕」入口 | 仅修改字幕文本或字幕样式后，成片不置过期，页面提示「字幕已变更」并提供「仅重烧字幕」入口 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：整剧打包导出仅包含已合成且未过期的剧集，过期集在打包前明确列出并要求确认。 | 整剧打包导出仅包含已合成且未过期的剧集，过期集在打包前明确列出并要求确认。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：下载链接 30 分钟后失效，点「重新获取链接」重取新签名 URL 即可下载，无需重新导出。 | 下载链接 30 分钟后失效，点「重新获取链接」重取新签名 URL 即可下载，无需重新导出。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FILM-COMPOSE-EXPORT-09 | AC-09 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：输入 BGM 描述生成音乐后出现可试听候选；选用候选后时间轴 BGM 轨展示对应素材，重新生成不删除旧候选。 | 输入 BGM 描述生成音乐后出现可试听候选；选用候选后时间轴 BGM 轨展示对应素材，重新生成不删除旧候选。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
