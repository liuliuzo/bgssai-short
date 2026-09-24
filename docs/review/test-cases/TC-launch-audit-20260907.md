# 测试用例：视频生成结果下载修复

- **功能文档**: `docs/feature/launch-audit-20260907.md`
- **范围**: in-scope
- **验收标准数**: 10
- **生成用例数**: 10
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`
- **备注**: 原文无编号验收标准；按可验证需求句拆为 AC。

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-LAUNCH-AUDIT-20260907-01 | AC-01 | P0 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：Veo 异步生成完成后，Google API 文件下载必须携带对应 API Key。可下载视频不能因为下载阶段缺少鉴权而丢失。 | Veo 异步生成完成后，Google API 文件下载必须携带对应 API Key。可下载视频不能因为下载阶段缺少鉴权而丢失。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-02 | AC-02 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：供应商文件重定向必须跟随有限次数并验证最终为成功状态；不能把 302 页面当作视频保存。跨来源跳转不转发供应商凭据。 | 供应商文件重定向必须跟随有限次数并验证最终为成功状态；不能把 302 页面当作视频保存。跨来源跳转不转发供应商凭据。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-03 | AC-03 | P0 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：生成任务轮询只访问配置供应商的同来源地址，禁止将鉴权头发送到响应中任意外部地址。 | 生成任务轮询只访问配置供应商的同来源地址，禁止将鉴权头发送到响应中任意外部地址。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-04 | AC-04 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：验收：上述失败场景应有明确的失败反馈 | 验收：上述失败场景应有明确的失败反馈 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-05 | AC-05 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：成功结果必须来自真实有效的状态 | 成功结果必须来自真实有效的状态 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-06 | AC-06 | P0 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：已有登录、授权和业务权限规则仍适用 | 已有登录、授权和业务权限规则仍适用 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-07 | AC-07 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：未知任务类型必须失败，不能不生成内容就扣费 | 未知任务类型必须失败，不能不生成内容就扣费 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-08 | AC-08 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：模型网络调用完成后，任务成功状态、本地钱包扣减、用量流水和作品成本必须在一个数据库事务中提交 | 模型网络调用完成后，任务成功状态、本地钱包扣减、用量流水和作品成本必须在一个数据库事务中提交 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-09 | AC-09 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：中央账本同步使用同事务写入的持久待发送记录，并以任务唯一标识幂等重试，避免本地回滚而中央已扣款 | 中央账本同步使用同事务写入的持久待发送记录，并以任务唯一标识幂等重试，避免本地回滚而中央已扣款 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
| TC-SHORT-LAUNCH-AUDIT-20260907-10 | AC-10 | P1 | user | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 打开用户端 https://dev.user.bgssai-short.com<br>2. 按验收标准执行：此修复覆盖结算事务，媒体生成过程中提前写入的素材和并发取消清理仍需后续审查 | 此修复覆盖结算事务，媒体生成过程中提前写入的素材和并发取消清理仍需后续审查 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 | 推导自原文 |
