# 测试用例：SHORT-01 商业门禁（成片生产 → Tube）

- **功能文档**: `docs/feature/short-01-commercial-gate.md`
- **范围**: in-scope
- **验收标准数**: 3
- **生成用例数**: 3
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-SHOR-01-COMM-GATE-01 | AC-01 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：未配置 ≠ 成功；AI Provider 缺密钥：任务失败（既有 `RULE_REJECT`），不写 SUCCEEDED。；Tube 缺 `base-url` / ingest-token：发布失败，文案明确「未配置」。；OBS：仅 `local` profile 允许 `LocalFileObjectStorage`；`dev`/`test`/`prod | 未配置 ≠ 成功；AI Provider 缺密钥：任务失败（既有 `RULE_REJECT`），不写 SUCCEEDED。；Tube 缺 `base-url` / ingest-token：发布失败，文案明确「未配置」。；OBS：仅 `local` profile 允许 `LocalFileObjectStorage`；`dev`/`test`/`prod` 未配 OBS 或 OBS 初始化失败时 **启动 fail-fast**，禁止静默本地回落看起来像已接通 OBS。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SHOR-01-COMM-GATE-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：禁止假成功 URL；`stub://`、`mock://`、`placeholder://`、`about:` 等，以及明显占位地址，在 Provider 下载、成片发布、Tube `play_url` 解析处一律拒绝。；不得把此类 URL 写成任务 SUCCEEDED 或 Tube READY。 | 禁止假成功 URL；`stub://`、`mock://`、`placeholder://`、`about:` 等，以及明显占位地址，在 Provider 下载、成片发布、Tube `play_url` 解析处一律拒绝。；不得把此类 URL 写成任务 SUCCEEDED 或 Tube READY。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-SHOR-01-COMM-GATE-03 | AC-03 | P0 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：Short→Tube 交接：PENDING vs FAIL；**READY**（含 SUCCEEDED 归一）：`commercial_ready=true`，可视为发布成功。；**PENDING**（PROCESSING/QUEUED 等）：交接已受理但未就绪；落库 status=PENDING，`commercial_ready=false`；**不是 | Tube 交接：PENDING vs FAIL；**READY**（含 SUCCEEDED 归一）：`commercial_ready=true`，可视为发布成功。；**PENDING**（PROCESSING/QUEUED 等）：交接已受理但未就绪；落库 status=PENDING，`commercial_ready=false`；**不是 FAIL**。；**FAILED**：明确失败；抛错并落库 FAILED；与 PENDING 文案区分。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
