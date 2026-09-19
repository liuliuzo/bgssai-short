# BGSSAI Short 客户端矩阵

产品：AI 短剧制作平台（`bgssai-short`）

| 形态 | 交付 | Admin / User | 标注 |
|------|------|--------------|------|
| 浏览器 Web | 打开网址即可 | 分域名（或路径） | `web=separate domains OK` |
| PC 桌面（Windows / macOS / Linux） | 单一安装包 | 应用内双入口 | `installable=single-app dual-entry` |
| Android / iOS | 单一 App | 应用内双入口 | `installable=single-app dual-entry` |
| 微信小程序 | 单一小程序 | 应用内双入口 | `installable=single-app dual-entry` |

**禁止**：desktop-admin + desktop-user、mobile-admin + mobile-user、两个小程序并行交付。

## 浏览器 Web

| 环境 | 用户端 | 管理端 |
|------|--------|--------|
| dev | `https://dev.user.bgssai-short.com` | `https://dev.admin.bgssai-short.com` |
| prod | `https://www.bgssai-short.com` | `https://admin.bgssai-short.com` |

Web 两端仍是独立部署的后端/前端工程（`bgssai-short-user` / `bgssai-short-admin`），浏览器侧换域名即可，不要求用户装两套浏览器。

## 可安装六端目录

```text
clients/desktop/       # 单一 Electron（Win / macOS / Linux），首屏双入口
clients/mobile/        # 单一 Expo（iOS / Android），首屏双入口
clients/miniprogram/   # 单一微信小程序，首屏双入口
```

入口只切换路由、权限与 Web/API 基址，不是第二套安装包。

## 本地启动（壳子）

```bash
# 桌面
cd clients/desktop && npm install && npm start

# 移动（Expo）
cd clients/mobile && npm install && npx expo start

# 小程序：用微信开发者工具打开 clients/miniprogram/
```

环境变量（桌面 / 移动共用语义）：

- `BGSSAI_ENV=dev|prod`（默认 `dev`）
- 可覆盖：`BGSSAI_USER_URL`、`BGSSAI_ADMIN_URL`
