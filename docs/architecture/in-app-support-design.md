# 站内在线客服详设

## 目标

落地全产品线「站内在线客服（留言 / 会话）」标准：用户端悬浮入口 + 会话 UI；管理端处理台；会话 / 消息落库；不做第三方 SaaS / 智能客服。

## 模块落点

| 层 | 路径 |
| --- | --- |
| 用户后端 | `com.bgssai.shortai.support`（`ui` / `service`） |
| 管理后端 | 同包名，管理端 Controllers |
| 表 | `short_cs_session` · `short_cs_message` |
| 用户前端 | `components/SupportWidget`（登录页 + MainLayout） |
| 管理前端 | `pages/support/SupportInboxPage` |

## 鉴权

- 用户公开接口：无 `@NeedAop`，IP 限流；可选解析 `Jwttoken` 绑定 `user_id`；会话归属靠 `guest_token` 或 `user_id`。
- 管理接口：`@NeedAop` + `AdminAuthGuard.isAdmin()`。

## 状态机

```
create(+content) → PENDING
create(无 content) → OPEN
user send → PENDING
admin reply → OPEN
admin close → CLOSED（终态；发消息拒绝）
```

## 安全

- 公开 create / send：`@RateLimit(capacity=10, refillPerMinute=10, keyBy=IP)`。
- content 长度上限 2000 字；联系方式字段 trim 后长度校验。
- 不在前端写死密钥；本切片不做邮件/企微通知。

## 与六端

Web 用户 / 管理端本切片完整实现。可安装六端经 WebView 打开同一用户 / 管理基址即可复用；`CLIENTS.md` 标注 `support=in-app messaging`。
