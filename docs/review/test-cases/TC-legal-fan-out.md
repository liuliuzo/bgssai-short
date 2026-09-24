# 测试用例：官网法律页分发（legal-fan-out）

- **功能文档**: `docs/feature/legal-fan-out.md`
- **范围**: in-scope
- **验收标准数**: 5
- **生成用例数**: 5
- **环境**: User `https://dev.user.bgssai-short.com` · Admin `https://dev.admin.bgssai-short.com`

| 用例ID | 关联需求/AC | 优先级 | 端(user/admin) | 前置条件 | 测试步骤 | 期望结果 | 测试数据 | 环境 | 结果(待测) | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-SHORT-LEGAL-FAN-OUT-01 | AC-01 | P0 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：用户端 `/login`：同意句（用户协议 + 隐私政策）+ 版权行；登录后页脚可见五条中英外链（`·` 分隔），目标为 `https://www.bgssai.com/<slug>/` 与 `https://www.bgssai.com/en/<slug>/`。 | 用户端 `/login`：同意句（用户协议 + 隐私政策）+ 版权行；登录后页脚可见五条中英外链（`·` 分隔），目标为 `https://www.bgssai.com/<slug>/` 与 `https://www.bgssai.com/en/<slug>/`。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-LEGAL-FAN-OUT-02 | AC-02 | P0 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：管理端登录与页脚只有外链，无站内法律页。 | 管理端登录与页脚只有外链，无站内法律页。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-LEGAL-FAN-OUT-03 | AC-03 | P0 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：页脚与登录区不出现证照编号，也不出现已退役路径。 | 页脚与登录区不出现证照编号，也不出现已退役路径。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-LEGAL-FAN-OUT-04 | AC-04 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：需求 / 原型 / 设计标注 **需法务审阅**。 | 需求 / 原型 / 设计标注 **需法务审阅**。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
| TC-SHORT-LEGAL-FAN-OUT-05 | AC-05 | P1 | both | 已登录对应用户端或管理端账号（占位账号，无口令写入） | 1. 按需打开用户端 https://dev.user.bgssai-short.com 或管理端 https://dev.admin.bgssai-short.com<br>2. 按验收标准执行：无新后端接口，无 DDL / DML。 | 无新后端接口，无 DDL / DML。 | 占位账号 / 样例数据（无口令） | User:https://dev.user.bgssai-short.com / Admin:https://dev.admin.bgssai-short.com | 待测 |  |
