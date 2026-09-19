# 站内在线客服 API

字段一律 snake_case。统一响应 `{ code, message, success, result }`；分页另含 `page_num` / `page_size` / `total_size`。

## 状态与发送方

| 枚举 | 值 |
| --- | --- |
| 会话 `status` | `OPEN` · `PENDING` · `CLOSED` |
| 工单 `status` | `OPEN` · `IN_PROGRESS` · `RESOLVED` · `CLOSED` |
| 工单 `priority`（可选） | `LOW` · `NORMAL` · `HIGH` |
| `sender_type` | `USER` · `GUEST` · `ADMIN` |

## 用户 / 访客

公开接口（无 `@NeedAop`），按 IP `@RateLimit`。登录用户可选请求头 `Jwttoken`：后端若能解析则写入 / 过滤 `user_id`；访客凭 `guest_token` 拥有会话。

### POST `/bgssai/user/support/sessions/create`

请求：

```json
{
  "guest_token": "string|optional",
  "contact_name": "string|optional 兼容字段，访客 UI 不采集",
  "contact_phone": "string|optional 兼容字段，访客 UI 不采集",
  "contact_email": "string|optional 兼容字段，访客 UI 不采集",
  "content": "string|recommended 首条留言；对话气泡 UX 下由首发触发 create"
}
```

响应 `result`：

```json
{
  "session_id": "cs-...",
  "guest_token": "gt-...",
  "status": "PENDING",
  "unread_user": 0,
  "unread_admin": 1,
  "last_message_at": "2026-09-19 10:00:00",
  "message": { "message_id": "cm-...", "sender_type": "GUEST", "content": "...", "create_time": "..." }
}
```

规则：无 `guest_token` 时服务端签发并返回；`content` 非空则写入首条消息且 `status=PENDING`，否则 `OPEN`。访客端对话气泡 UX：打开即气泡 + 底部输入，首条消息带 `content` 调用本接口建会话；不要求联系方式 / 主题表单。

### POST `/bgssai/user/support/sessions/mine`

请求：`{ "guest_token": "string|optional", "page_num": 1, "page_size": 20 }`

响应：分页，`result` 为会话摘要列表（含 `session_id` / `status` / `unread_user` / `last_message_at` / `contact_*`）。登录用户返回本人会话；访客仅返回该 `guest_token` 下会话。

### POST `/bgssai/user/support/sessions/detail`

请求：`{ "session_id": "cs-...", "guest_token": "string|optional" }`

响应 `result`：`session` + `messages[]` + `ticket`（当前会话最新未删工单，无则 `null`）。校验归属（`user_id` 或 `guest_token`）；成功后 `unread_user=0`。

### POST `/bgssai/user/support/messages/send`

请求：`{ "session_id": "cs-...", "guest_token": "string|optional", "content": "文本" }`

响应 `result`：新建消息对象。`CLOSED` 返回 `4006`；成功后 `status=PENDING`，`unread_admin++`。

### POST `/bgssai/user/support/tickets/create`

请求：

```json
{
  "session_id": "cs-...",
  "guest_token": "string|optional",
  "subject": "string|optional 缺省取最近用户留言",
  "category": "string|optional",
  "priority": "LOW|NORMAL|HIGH|optional 默认 NORMAL",
  "description": "string|optional"
}
```

响应 `result`：工单对象（`ticket_id` / `session_id` / `subject` / `category` / `priority` / `status` / `description` / `create_time`）。

规则：会话须存在且未 `CLOSED`；归属校验同 detail。若本会话已有未 `CLOSED` 工单，直接返回该工单（幂等）。新建时 `status=OPEN`，写入一条用户侧提示消息「【已提工单】{subject}」，会话 `PENDING` 且 `unread_admin++`。

## 管理端

均需 `Jwttoken` + `PLATFORM_ADMIN`。

### POST `/bgssai/admin/support/sessions/list`

请求：`{ "status": "OPEN|PENDING|CLOSED|optional", "keyword": "optional", "page_num": 1, "page_size": 20 }`

响应：分页会话列表（含联系方式、未读、`user_id`、`last_message_at`、`ticket_id`、`ticket_status`、`ticket_subject`；无工单时后三者为 null）。

### POST `/bgssai/admin/support/sessions/detail`

请求：`{ "session_id": "cs-..." }`

响应：`session` + `messages[]` + `ticket`；清零 `unread_admin`。

### POST `/bgssai/admin/support/messages/reply`

请求：`{ "session_id": "cs-...", "content": "文本" }`

响应：新建消息；`status=OPEN`，`unread_user++`。`CLOSED` → `4006`。

### POST `/bgssai/admin/support/sessions/close`

请求：`{ "session_id": "cs-..." }`

响应：`{ "session_id", "status": "CLOSED" }`。

### POST `/bgssai/admin/support/tickets/update_status`

请求：`{ "ticket_id": "ct-...", "status": "OPEN|IN_PROGRESS|RESOLVED|CLOSED" }`

响应：更新后的工单对象。非法状态 → `50000001`；不存在 → `404000`。

## 错误码

| code | 含义 |
| --- | --- |
| `4003` | 管理端非管理员 / 用户越权看他人会话 |
| `4006` | 会话已关闭或规则拒绝 |
| `404000` | 会话 / 工单不存在 |
| `429000` | 限流 |
| `50000001` | 参数非法（如 content 为空） |
