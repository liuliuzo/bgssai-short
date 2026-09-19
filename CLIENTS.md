# bgssai-short 客户端交付

## 形态

| 形态 | 交付 | Admin / User |
|------|------|----------------|
| 浏览器 Web | 打开网址即可 | `web=separate domains OK`（`user.bgssai-short.com` / `admin.bgssai-short.com`） |
| PC 桌面（Win / macOS / Linux） | 一个安装包 | `installable=single-app dual-entry` |
| Android / iOS | 一个 App | `installable=single-app dual-entry` |
| 微信小程序 | 一个小程序 | `installable=single-app dual-entry` |

可安装端：**一套安装、两个登录入口**（用户入口 / 管理入口）。入口只切换路由、权限与 Web 基址，不是第二套安装包。

禁止：desktop-admin + desktop-user、mobile-admin + mobile-user、两个小程序并行作为产品交付。

## 目录

```text
clients/desktop/       # 单一 Electron，双登录入口（Win / macOS / Linux）
clients/mobile/        # 单一 Expo App，双登录入口（iOS / Android）
clients/miniprogram/   # 单一微信小程序，双登录入口
```

## 入口与基址（占位，域名/服务器稍后接入）

| 入口 | 本地开发 | 开发环境 | 生产 |
|------|----------|----------|------|
| 用户 | `http://localhost:3002` | `https://dev.user.bgssai-short.com` | `https://user.bgssai-short.com` |
| 管理 | `http://localhost:3001` | `https://dev.admin.bgssai-short.com` | `https://admin.bgssai-short.com` |

各端通过环境变量或 `config` 选择 profile（`local` / `dev` / `prod`），默认 `dev`。

## 在线客服

`support=in-app messaging`（站内留言 / 会话；用户端悬浮入口 + 管理端处理台；不强制第三方 SaaS）。
