# 账号设置详设（注销 / 绑定解绑）

版本：2026-10-17。需求：`REQ-SHORT-COMMON-016`、`REQ-SHORT-COMMON-017`。

## 模块

- `account.service.AccountDeletionService`：预览后果、身份验证、未结事项检查、匿名化与会话失效。
- `account.service.AccountLoginMethodService`：登录方式列表、OAuth/手机/邮箱绑定、解绑与至少一种方式约束。
- 登录入口统一调用 `AccountGuard.rejectIfDeleted`。

## 状态

- `sys_user.deleted_at` 非空且 `del_flag=1` → 已注销。
- `oauth_binding` 软删并改写 `open_id` 释放唯一约束，避免占用第三方身份。

## 安全

敏感变更（解绑、注销）需密码或验证码重新验证；失败时绑定关系不变。
