# 站内在线客服（留言 / 会话）

## 一、背景与目标

- 背景：产品线统一要求各应用平台提供站内在线客服（留言 / 会话），不强制接第三方 SaaS 或智能客服；访客与登录用户可留言成会话，管理端可回复与关闭。
- 目标：短剧用户端具备悬浮「在线客服」入口与会话 UI；管理端具备客服处理台；会话与消息落库，状态含 open / pending / closed；关闭后可再开新会话。
- 关联角色：`USER`（含登录前访客）· `PLATFORM_ADMIN`

## 二、需求澄清（WHAT）

- 用户故事：
  - 作为访客或登录用户，我希望在页面内打开在线客服、留下联系方式并发送文本留言，以便获得人工回复。
  - 作为平台管理员，我希望在客服处理台看到会话列表、查看详情、回复并关闭会话，以便处理咨询。
- 业务规则：
  1. 入口：用户端登录页与登录后主壳均提供悬浮「在线客服」；管理端侧栏提供「在线客服」处理台。
  2. 可匿名建会话：须尽量采集联系方式（姓名 / 手机 / 邮箱至少一项建议填写，不强制）；登录用户建会话写入 `user_id`。
  3. 会话能力：创建会话、发送文本消息、用户侧看历史；管理侧列表 / 详情 / 回复 / 关闭。
  4. 状态：`OPEN`（进行中）· `PENDING`（待客服回复）· `CLOSED`（已关闭）。用户发消息 → `PENDING`；客服回复 → `OPEN`；客服关闭 → `CLOSED`。关闭后不可再发消息，用户须新建会话。
  5. 未读：会话维护 `unread_admin` / `unread_user`；对方发消息时递增，己方打开详情时清零。
  6. 公开建会话 / 发消息接口按 IP 限流；管理端全量读写须 `@NeedAop` + `PLATFORM_ADMIN`。
  7. 本阶段不做：图片附件、第三方 SaaS、Bot 智能客服；可选企微/电话二维码不替代站内会话（本切片不强制展示）。
  8. `CLIENTS.md` 标注 `support=in-app messaging`。

## 三、验收标准

1. 用户侧：打开悬浮入口 → 发一条留言 → 看到会话与己方消息。
2. 管理侧：列表可见该会话 → 回复 → 用户侧可见回复。
3. 关闭会话后用户不可再往该会话发消息，可再开新会话。
4. 访客（未登录）凭 `guest_token` 可建会话并回看；登录用户会话归属本人。

## 四、接口契约（snake_case）

详见 [`../api/in-app-support.md`](../api/in-app-support.md)。

**用户 / 访客（公开，IP 限流；登录用户可带 `Jwttoken` 绑定 `user_id`）**

- `POST /bgssai/user/support/sessions/create`
- `POST /bgssai/user/support/sessions/mine`
- `POST /bgssai/user/support/sessions/detail`
- `POST /bgssai/user/support/messages/send`

**管理端（需 `Jwttoken` + `PLATFORM_ADMIN`）**

- `POST /bgssai/admin/support/sessions/list`
- `POST /bgssai/admin/support/sessions/detail`
- `POST /bgssai/admin/support/messages/reply`
- `POST /bgssai/admin/support/sessions/close`

## 五、流程 / 状态机

```
访客/用户 打开悬浮入口
  → create（可选联系方式 + 首条 content）→ status=PENDING
  → send 文本 → PENDING，unread_admin++
管理端 list → detail（清 unread_admin）→ reply → OPEN，unread_user++
用户 detail（清 unread_user）可见回复
管理端 close → CLOSED；用户须 create 新会话
```

图示见 [`flow-in-app-support.html`](./flow-in-app-support.html)。

## 六、数据模型影响

- 表 `short_cs_session`：会话（`session_id`、`guest_token`、`user_id`、联系方式、`status`、未读计数、`last_message_at`）。
- 表 `short_cs_message`：消息（`message_id`、`session_id`、`sender_type` USER|ADMIN|GUEST、`content`）。
- 同步 `sql/DDL.sql` 与双端 Entity / Example / Mapper XML。

## 七、原型

- 涉及端：web 用户端 · web 管理端
- 页面：`../demo-static/web/user/support-chat.html`、`../demo-static/web/admin/support-inbox.html`
- 跳转：两端 `PAGE-FLOW.html`；映射：`../api/page-api-mapping.md`

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型
- [x] 设计与契约（`docs/api/in-app-support.md`、`docs/architecture/in-app-support-design.md`）
- [x] 代码与数据库（`sql/**`、双端 support 模块与前端）
