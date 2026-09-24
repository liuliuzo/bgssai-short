# 测试用例：分镜画布编排（storyboard-canvas）

- **功能文档**: `docs/feature/storyboard-canvas.md`
- **范围**: in-scope
- **验收标准数**: 8
- **生成用例数**: 8
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-STORYBOA-CANVAS-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：一集 44 镜、7 个场次，画布视图内 7 行排布，一屏可见全部镜头与各自状态色。 | 一集 44 镜、7 个场次，画布视图内 7 行排布，一屏可见全部镜头与各自状态色。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-CANVAS-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：列表视图改了某镜的人话分镜并保存后，切到画布视图该镜内容同步（同一接口读取，无第二份状态）。 | 列表视图改了某镜的人话分镜并保存后，切到画布视图该镜内容同步（同一接口读取，无第二份状态）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-CANVAS-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：框选第 3 场的 8 个镜头，点「估费用」返回 8 镜合计费用；点「批量生成」按该费用提交， | 框选第 3 场的 8 个镜头，点「估费用」返回 8 镜合计费用；点「批量生成」按该费用提交， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-CANVAS-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：把镜 12 拖到镜 9 的位置，本场次重新编号为连续序列，其余场次镜号不变；已生成素材与状态不变。 | 把镜 12 拖到镜 9 的位置，本场次重新编号为连续序列，其余场次镜号不变；已生成素材与状态不变。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-CANVAS-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：跨场次拖动不生效，给出「跨场请回剧本层改场次划分」提示。 | 跨场次拖动不生效，给出「跨场请回剧本层改场次划分」提示。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-CANVAS-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：重排后，开启了「接上一镜」且上一镜发生变化的镜头出现 stale 标记，且系统不自动提交任何任务。 | 重排后，开启了「接上一镜」且上一镜发生变化的镜头出现 stale 标记，且系统不自动提交任何任务。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-CANVAS-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：`LOCKED` 镜头显示锁标记且拖不动。 | `LOCKED` 镜头显示锁标记且拖不动。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORYBOA-CANVAS-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：画布与列表切换后当前选中集与筛选条件保持不变。 | 画布与列表切换后当前选中集与筛选条件保持不变。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
