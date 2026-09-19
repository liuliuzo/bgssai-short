# BGSSAI Short 六端客户端状态

标准依据：全产品六端（Windows / macOS / Linux / iOS / Android / 微信小程序）。Web 仍是共享 UI，不能替代六端壳。

## 强制口径（单应用双入口）

**六端全部是「一个安装包 / 一个小程序 + 两个登录入口」**（对标 Boss直聘）：

| 入口 | 进入 |
|------|------|
| 创作者登录 | 用户端 Web（制作） |
| 运营登录 | 管理端 Web（审核 / 运营） |

禁止拆成独立的 admin 安装包与 user 安装包，禁止拆成两个小程序 AppID 安装物。桌面 Electron 同样遵守：一个应用，启动先选入口。

目录约定：

```text
clients/desktop/      Electron（Win / macOS / Linux）— 双入口选择页
clients/mobile/       Expo（iOS / Android）— 双入口选择页
clients/miniprogram/  微信原生小程序 — 双入口选择页
```

产品 URL 环境可配：

| 端 | 用户端变量 | 管理端变量 |
|----|------------|------------|
| Desktop | `USER_PRODUCT_URL` | `ADMIN_PRODUCT_URL` |
| Mobile | `EXPO_PUBLIC_USER_PRODUCT_URL` | `EXPO_PUBLIC_ADMIN_PRODUCT_URL` |
| 小程序 | `config.js` → `userProductUrl` | `config.js` → `adminProductUrl` |

默认（dev）：`https://dev.user.bgssai-short.com` / `https://dev.admin.bgssai-short.com`。本阶段不提交 AppID、签名证书、DNS 或部署流水线。

## 状态表

| 端 | 形态 | 目录 | 状态 | 说明 |
|----|------|------|------|------|
| Windows | Electron 单应用双入口 | `clients/desktop/` | scaffold | 选择页 → 用户端或管理端；可本地 `pack:win` |
| macOS | 同上 | `clients/desktop/` | scaffold | 同上；可本地 `pack:mac` |
| Linux | 同上 | `clients/desktop/` | scaffold | 同上；可本地 `pack:linux`（AppImage） |
| iOS | Expo 单应用双入口 | `clients/mobile/` | scaffold | 选择页 → WebView；包名 `com.bgssai.short` |
| Android | 同上 | `clients/mobile/` | scaffold | 同上；同一 `applicationId` |
| 微信小程序 | 单小程序双入口 | `clients/miniprogram/` | scaffold | 选择页 → web-view；AppID / 业务域名后置 |

状态含义：`scaffold` = 代码壳已就绪，本地可启动预览；登录与核心路径依赖已部署的对应 Web；上架 / 提审未做。
