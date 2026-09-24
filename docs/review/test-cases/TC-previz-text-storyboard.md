# 测试用例：文字预演与提示词优化（previz-text-storyboard）

- **功能文档**: `docs/feature/previz-text-storyboard.md`
- **范围**: in-scope
- **验收标准数**: 9
- **生成用例数**: 9
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：拆镜完成后每个镜头都带 `previz_text`，五要素齐全，且不含提示词术语（不出现权重、括号、英文参数）。 | 拆镜完成后每个镜头都带 `previz_text`，五要素齐全，且不含提示词术语（不出现权重、括号、英文参数）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：连读模式可把整集文字稿连成一篇通读文本，顺序与 `shot_no` 一致，可整段选中重写。 | 连读模式可把整集文字稿连成一篇通读文本，顺序与 `shot_no` 一致，可整段选中重写。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：编辑 `previz_text` 后，结构化字段与 `image_prompt` 同步更新，且更新内容与文字改动一致 | 编辑 `previz_text` 后，结构化字段与 `image_prompt` 同步更新，且更新内容与文字改动一致 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：连贯性检查能检出空间跳变、光线突变、服装跳变、道具异常、轴线矛盾五类问题并定位到 `shot_no`。 | 连贯性检查能检出空间跳变、光线突变、服装跳变、道具异常、轴线矛盾五类问题并定位到 `shot_no`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：提交生成前自动出 `prompt_check`：缺项、冲突、超长、否定词建议逐条可见，每条可一键采纳。 | 提交生成前自动出 `prompt_check`：缺项、冲突、超长、否定词建议逐条可见，每条可一键采纳。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：提示词 A/B 对照可并排展示两版差异，且不产生任何生成费用。 | 提示词 A/B 对照可并排展示两版差异，且不产生任何生成费用。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：未出高清首帧的镜头提交视频生成时被拒（`4016`），提示先出首帧或在作品设置中关闭跳级约束。 | 未出高清首帧的镜头提交视频生成时被拒（`4016`），提示先出首帧或在作品设置中关闭跳级约束。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：整集草图预演一次出齐 44 张低清草图，总耗时 1 分钟内、费用与预估偏差不超过 10%； | 整集草图预演一次出齐 44 张低清草图，总耗时 1 分钟内、费用与预估偏差不超过 10%； | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-PREVIZ-TEXT-STORYBOA-09 | AC-09 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：草图升级到高清首帧时，构图与草图一致（同 `consistency_seed` 与同提示词）。 | 草图升级到高清首帧时，构图与草图一致（同 `consistency_seed` 与同提示词）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
