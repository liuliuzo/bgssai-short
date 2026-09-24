# 测试用例：首尾帧衔接（frame-continuity）

- **功能文档**: `docs/feature/frame-continuity.md`
- **范围**: in-scope
- **验收标准数**: 8
- **生成用例数**: 8
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-FRAME-CONTINUI-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：同一场次内镜 5 设为 `INHERIT_PREV` 后生成视频，请求中携带的首帧参考即镜 4 的尾帧素材。 | 同一场次内镜 5 设为 `INHERIT_PREV` 后生成视频，请求中携带的首帧参考即镜 4 的尾帧素材。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FRAME-CONTINUI-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：镜 6 与镜 5 的 `scene_no` 不同时，即便设了 `INHERIT_PREV` 也按 `NONE` 生成， | 镜 6 与镜 5 的 `scene_no` 不同时，即便设了 `INHERIT_PREV` 也按 `NONE` 生成， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FRAME-CONTINUI-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：上一镜尚无尾帧时，本镜照常生成成功，界面标注「上一镜尚无尾帧，本次未衔接」。 | 上一镜尚无尾帧时，本镜照常生成成功，界面标注「上一镜尚无尾帧，本次未衔接」。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FRAME-CONTINUI-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：视频生成成功后自动产生尾帧素材（`asset_type=IMAGE`，与首帧同规格），镜头 | 视频生成成功后自动产生尾帧素材（`asset_type=IMAGE`，与首帧同规格），镜头 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FRAME-CONTINUI-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：选择 `config_json.support_last_frame != 1` 的模型并设 `BOOKEND` 时，提交返回 `4006` | 选择 `config_json.support_last_frame != 1` 的模型并设 `BOOKEND` 时，提交返回 `4006` | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FRAME-CONTINUI-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：重抽镜 4 的视频后，镜 5（同场次、`INHERIT_PREV`）`stale_flag=1`，页面出现提示， | 重抽镜 4 的视频后，镜 5（同场次、`INHERIT_PREV`）`stale_flag=1`，页面出现提示， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FRAME-CONTINUI-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：「本集全部衔接」一次生效：同场次相邻镜头全部变 `INHERIT_PREV`，跨场处保持 `NONE`。 | 「本集全部衔接」一次生效：同场次相邻镜头全部变 `INHERIT_PREV`，跨场处保持 `NONE`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-FRAME-CONTINUI-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：部署机未装 ffmpeg 时抽帧任务失败并提示「本机未安装 ffmpeg」，视频本身不受影响。 | 部署机未装 ffmpeg 时抽帧任务失败并提示「本机未安装 ffmpeg」，视频本身不受影响。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
