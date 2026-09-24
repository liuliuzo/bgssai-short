# 测试用例：配音与字幕（voice-subtitle）

- **功能文档**: `docs/feature/voice-subtitle.md`
- **范围**: in-scope
- **验收标准数**: 9
- **生成用例数**: 9
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-VOICE-SUBTITLE-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：对一集 42 个镜头点「批量配音」，仅对有台词的镜头产生任务；同一角色在全部镜头中音色一致。 | 对一集 42 个镜头点「批量配音」，仅对有台词的镜头产生任务；同一角色在全部镜头中音色一致。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：配音完成后每镜展示 `audio_duration_sec` 与 `shot_duration_sec` 对比及 `align_status`； | 配音完成后每镜展示 `audio_duration_sec` 与 `shot_duration_sec` 对比及 `align_status`； | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：`ADJUST_SPEED` 可在语速上限内对齐时自动执行；无法在上限内对齐时不自动动手，提示改用 | `ADJUST_SPEED` 可在语速上限内对齐时自动执行；无法在上限内对齐时不自动动手，提示改用 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：选择 `EXTEND_SHOT` 时明确提示需重生成该镜视频及预估费用，确认后自动提交对应 `VIDEO` 任务。 | 选择 `EXTEND_SHOT` 时明确提示需重生成该镜视频及预估费用，确认后自动提交对应 `VIDEO` 任务。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：字幕自动生成，单条不超过 15 字，时间轴与配音一致；导出的 SRT 可被通用播放器正确加载。 | 字幕自动生成，单条不超过 15 字，时间轴与配音一致；导出的 SRT 可被通用播放器正确加载。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：单条台词重配后只有该条音频与对应字幕变化，其它片段逐字不变。 | 单条台词重配后只有该条音频与对应字幕变化，其它片段逐字不变。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：仅当存在需付费确认的 `MISALIGNED` 镜头（待 `EXTEND_SHOT`）时，该集合成入口禁用并给出 | 仅当存在需付费确认的 `MISALIGNED` 镜头（待 `EXTEND_SHOT`）时，该集合成入口禁用并给出 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：点「按建议一键处理全部未对齐」：免费项全部按建议执行完毕，需付费的 `EXTEND_SHOT` 项 | 点「按建议一键处理全部未对齐」：免费项全部按建议执行完毕，需付费的 `EXTEND_SHOT` 项 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-VOICE-SUBTITLE-09 | AC-09 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：选择 MiniMax-Speech 配音后得到可播放音频与真实时长；选择 Hy-ASR 执行校准后，字幕文本和时间戳 | 选择 MiniMax-Speech 配音后得到可播放音频与真实时长；选择 Hy-ASR 执行校准后，字幕文本和时间戳 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
