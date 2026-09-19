# 站内在线客服详设

## 目标

落地全产品线「站内在线客服（留言 / 会话）」标准：用户端悬浮入口 + **对话气泡**会话 UI；聊天面板内可 **提工单**；管理端处理台（会话 + 工单状态）；会话 / 消息 / 工单落库；不做第三方 SaaS / 智能客服。

## 访客 UX（对齐 bot #87 / note #36 + CS 标准 2026-09-19）

- 打开面板 → 气泡列表 + 底部 composer；**无**联系方式 / 主题前置表单。
- 首条留言调用 `sessions/create`（带 `content`）建会话；后续走 `messages/send`。
- 工具栏「提工单」：挂当前会话；`subject` 可选（缺省最近留言）；提单后仍可继续聊。
- API 可选 `contact_*` 仅兼容 / 管理端展示；访客 UI 不采集。管理端 inbox 展示工单状态并可更新。

## 模块落点

| 层 | 路径 |
| --- | --- |
| 用户后端 | `com.bgssai.shortai.support`（`ui` / `service`） |
| 管理后端 | 同包名，管理端 Controllers |
| 表 | `short_cs_session` · `short_cs_message` · `short_cs_ticket` |
| 用户前端 | `components/SupportWidget`（登录页 + MainLayout；对话气泡 + 提工单） |
| 管理前端 | `pages/support/SupportInboxPage`（会话 + 工单状态） |

## 鉴权

- 用户公开接口：无 `@NeedAop`，IP 限流；可选解析 `Jwttoken` 绑定 `user_id`；会话归属靠 `guest_token` 或 `user_id`。
- 管理接口：`@NeedAop` + `AdminAuthGuard.isAdmin()`。

## 状态机

```
会话：
  create(+content) → PENDING
  create(无 content) → OPEN
  user send / 提工单 → PENDING
  admin reply → OPEN
  admin close → CLOSED（终态；发消息 / 提工单拒绝）

工单：
  create → OPEN
  admin update_status → OPEN | IN_PROGRESS | RESOLVED | CLOSED
  同一会话同时最多一个未 CLOSED 工单（再建幂等返回）
```

## 安全

- 公开 create / send / tickets/create：`@RateLimit(capacity=10, refillPerMinute=10, keyBy=IP)`。
- content / description 长度上限 2000 字；subject 上限 200；联系方式字段 trim 后长度校验。
- 不在前端写死密钥；本切片不做邮件/企微通知。

## 与六端

Web 用户 / 管理端本切片完整实现。可安装六端经 WebView 打开同一用户 / 管理基址即可复用；`CLIENTS.md` 标注 `support=in-app messaging`。
