# 测试用例：剧情图谱与走位示意（story-map）

- **功能文档**: `docs/feature/story-map.md`
- **范围**: in-scope
- **验收标准数**: 11
- **生成用例数**: 11
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-STORY-MAP-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：单集张力曲线与模板期望曲线叠加显示，塌陷段（`FLAT`）被标出并给出对应 `scene_no` / `shot_no` 范围， | 单集张力曲线与模板期望曲线叠加显示，塌陷段（`FLAT`）被标出并给出对应 `scene_no` / `shot_no` 范围， | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：前 3 秒钩子弱、末 5 秒悬念弱时分别标为 `WEAK_HOOK` / `WEAK_ENDING`。 | 前 3 秒钩子弱、末 5 秒悬念弱时分别标为 `WEAK_HOOK` / `WEAK_ENDING`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：全剧张力条带按集着色，反转集有标记，可一眼看出爽点分布与连续平段。 | 全剧张力条带按集着色，反转集有标记，可一眼看出爽点分布与连续平段。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：人物关系图可按集滑动演进；`SECRET` 关系在揭晓集之前为虚线灰显，之后转实线。 | 人物关系图可按集滑动演进；`SECRET` 关系在揭晓集之前为虚线灰显，之后转实线。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：某角色连续 5 集无出场时出现「角色掉线」提示并给出最后出场集数。 | 某角色连续 5 集无出场时出现「角色掉线」提示并给出最后出场集数。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-06 | AC-06 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：两人及以上出场的镜头可查看俯视走位图，含角色位置朝向与机位朝向。 | 两人及以上出场的镜头可查看俯视走位图，含角色位置朝向与机位朝向。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：相邻镜头越轴时产出 `EYELINE` 连贯性问题，定位到镜号，与文字预演页的连贯性清单是同一条数据。 | 相邻镜头越轴时产出 `EYELINE` 连贯性问题，定位到镜号，与文字预演页的连贯性清单是同一条数据。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：场次时间带按时长占比呈现，单场超 40% 时给出拆场提示。 | 场次时间带按时长占比呈现，单场超 40% 时给出拆场提示。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-09 | AC-09 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：全部图谱均为零成本视图（张力与关系抽取走 `TEXT` 任务，单集约 0.03 元；查看与拖动不产生费用）。 | 全部图谱均为零成本视图（张力与关系抽取走 `TEXT` 任务，单集约 0.03 元；查看与拖动不产生费用）。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-10 | AC-10 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：剧本草稿态可生成图谱，页面标注「估算，定稿后自动更新」；定稿后自动重新生成并去掉标注。 | 剧本草稿态可生成图谱，页面标注「估算，定稿后自动更新」；定稿后自动重新生成并去掉标注。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-STORY-MAP-11 | AC-11 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：内容保存后图谱自动重查（无需手动点重新生成）；本页问题清单与连贯性中心、质量报告 | 内容保存后图谱自动重查（无需手动点重新生成）；本页问题清单与连贯性中心、质量报告 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
