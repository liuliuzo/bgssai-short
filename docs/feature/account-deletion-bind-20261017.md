# 账号注销与登录方式绑定解绑（2026-10-17）

对应需求：`REQ-SHORT-COMMON-016`、`REQ-SHORT-COMMON-017`。

## 功能

1. 用户在账号设置阅读后果说明，经密码或验证码身份验证后自助注销。
2. 注销后 `sys_user.deleted_at` 写入、`del_flag=1`，手机/邮箱/昵称匿名化，会话清空，第三方绑定软删；密码/验证码/Chat 授权均不可再登录。
3. 存在未结支付订单或进行中任务时默认拒绝注销；用户确认后可强制（不静默清余额）。
4. 账号设置列出密码/手机/邮箱/第三方绑定；可绑定或解绑；同一 `(platform, open_id)` 唯一；至少保留一种登录方式；解绑需重新验证。

## 接口

见 `docs/api/account-settings.md`。

## 数据

- `sys_user.deleted_at`
- `account_deletion_log`
- 升级脚本：`sql/upgrade/20261017_account_deletion.sql`、`sql/UPGRADE-20261017-account-deletion.sql`
