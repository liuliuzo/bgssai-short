# 测试用例：MCP 服务（用户端）

- **功能文档**: `docs/feature/mcp-server.md`
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 5
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-MCP-SERVER-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：无效令牌 `POST /api/mcp` 返回 HTTP 401 + JSON-RPC error -32001。 | 无效令牌 `POST /api/mcp` 返回 HTTP 401 + JSON-RPC error -32001。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：`tools/list` 返回 26 个工具，包含：`list_login_logs` / `get_liveness` / `get_readiness`。 | `tools/list` 返回 26 个工具，包含：`list_login_logs` / `get_liveness` / `get_readiness`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-03 | AC-03 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：用户端 MCP Connectors 页可创建 / 列表 / 吊销令牌，并展示五类 Agent 配置指引与 tagline。 | 用户端 MCP Connectors 页可创建 / 列表 / 吊销令牌，并展示五类 Agent 配置指引与 tagline。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：GET /api/mcp 返回 405。 | GET /api/mcp 返回 405。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-MCP-SERVER-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：JSON-RPC 批量数组返回 -32600。 | JSON-RPC 批量数组返回 -32600。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |

## 制作闭环回归（2026-11-11）

1. 工具列表共 26 项；所有 array（含 scene_list/line_list/shots）必须有 items；每项均可被 hasTool 识别。
2. 缺失/false/字符串 true/数字 confirm_cost 都不得提交；确认后必须委托正式制作服务，MCP TaskMapper insert 次数为零。
3. IMAGE/VIDEO 保留 episode/shot/model，VOICE 走 DubbingService，MUSIC 保留 prompt/歌词/纯音乐，COMPOSE 保留预检拒绝，不接受错误的镜头或模型。
4. 外部用户分集、跨分集镜头、跨卡候选图拒绝；分镜批量有一个不合法时不部分更新。
5. ready 默认不强制低分；split 未确认替换不调用正式拆分；保存镜头时长必须是正整数。
6. 任务查询拒绝其他用户，输出真实成功/失败状态、公开结果，不包含 request_json；查询不会触发生成。
7. 过期/未完成成片保持无可播放 URL；字幕/BGM 设置保留当前作品/分集及正式资产校验。
8. 本地测试不付费调用真实模型、不更改线上作品、不发布 Tube；上线后另记录真实模型任务、媒体流、下载验证与费用。

9. `list_login_logs` 输出正式 DTO 的 `ip_location`，与原始 `ip_address` 同时保留，不另作精确定位推断。

10. list_voices 为正式目录，set_character_voice 拒绝未知声音/越权角色；原始 12 镜的每行说话者必须排第一，配音兼容对象/ID 数组引用并选正确角色声音。

11. 公司 bgssai-tts/edge-tts 的五个产品音色映射至已核实 Edge ID，缺省中文，原生 ID 和其他模型保持不变；测试不发起付费语音生成。

12. get_asset_download 使用真实 AssetService 的权限链路，验证本人原样获得预签名与过期时间，外人、缺失资产和未登录拒绝；不产生生成任务或输出存储配置。
13. lock_shot 的缺失/false/字符串/数字/对象/数组确认均不读写镜头；外人分集、外人镜头、跨集镜头、无视频或未就绪状态不得更新。本人同集 VIDEO_READY/LOCKED 显式确认才调用正式锁定，资产字段不被覆盖，后续合成预检保持。
14. 新工具 schema 为 26 项实际目录的一部分，字段白名单由服务端再次检查；任意 user_id/asset_id/video_asset_id 覆盖字段不能绕过协议描述。本地用模拟存储/Mapper 验证，无真实签名、作品锁定或费用。
