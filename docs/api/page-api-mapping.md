# 页面 → 后端 API 映射主表

本表是「原型页面 → 后端接口」映射的**主表**（Standards §9 / §10）。后端实现端点时按本表回溯原型字段；
新增 / 调整原型页后必须同步更新本表，**禁止**只改原型不刷映射表。

- 原型权威：`docs/demo-static/<端>/`（web 端再分 `admin/` 与 `user/`）。
- 契约正文：本目录下对应的接口契约 Markdown。
- 字段命名一律 snake_case；鉴权走请求头 `Jwttoken`。

## web 管理端（`docs/demo-static/web/admin/`，前端 3001 / 后端 8080，`role_code=PLATFORM_ADMIN`）

| 原型页 | 前端路由 | 后端接口 | 鉴权 | 契约 |
| --- | --- | --- | --- | --- |
| `login.html` | `/login` | `POST /bgssai/admin/login` | 公开（IP 限流） | [`auth-login.md`](./auth-login.md) |
| `home.html` | `/home` | -（消费登录响应缓存的身份字段） | 需 `Jwttoken` | - |
| `login-logs.html` | `/login-logs` | `GET /bgssai/admin/login-logs?page_num&page_size` | 需 `Jwttoken` + `PLATFORM_ADMIN` | [`auth-login.md`](./auth-login.md) |

## web 用户端（`docs/demo-static/web/user/`，前端 3002 / 后端 8081，`role_code=USER`）

| 原型页 | 前端路由 | 后端接口 | 鉴权 | 契约 |
| --- | --- | --- | --- | --- |
| `login.html` | `/login` | `POST /bgssai/user/login` | 公开（IP 限流） | [`auth-login.md`](./auth-login.md) |
| `home.html` | `/home` | -（消费登录响应缓存的身份字段） | 需 `Jwttoken` | - |
| `login-logs.html` | `/login-logs` | `GET /bgssai/user/login-logs?page_num&page_size`（仅本人记录，`user_id` 取自 JWT） | 需 `Jwttoken` | [`auth-login.md`](./auth-login.md) |

## iOS 端（`docs/demo-static/ios/`）

本产品暂不含 iOS 工程，原型为占位页，暂无映射条目；支持 iOS 端后在此补齐。

## Android 端（`docs/demo-static/android/`）

本产品暂不含 Android 工程，原型为占位页，暂无映射条目；支持 Android 端后在此补齐。

## 维护规约

1. **新增页面**：先在对应端 `PAGE-FLOW.html` 登记入口 / 出口，再在本表补行，最后写实现代码。
2. **改接口路径 / 字段**：同步改契约 Markdown、原型内联 mock 与本表，三处口径一致。
3. **删页面**：删除本表对应行，并核对对应端 `PAGE-FLOW.html` 无悬空节点。
4. **CR 校验**：涉及原型或前端路由的 PR，CR 阶段核对本表是否已同步（Standards §9 / §10）。
