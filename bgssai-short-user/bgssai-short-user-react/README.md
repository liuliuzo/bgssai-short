# bgssai-short-user-react

AI 短剧制作平台 — 用户端（User App）的 React 前端工程。本工程是统一单仓的子项目（位于
`bgssai-short-user/bgssai-short-user-react/`），与对应后端 `bgssai-short-user/bgssai-short-user/`
同处一仓；单仓总览见仓库根 [`README.md`](../../README.md)。

## 技术栈

React 18 + Vite + JavaScript（JSX） + React Router v6 + Ant Design 5 + Zustand + Axios

## 开发起步

```bash
npm install
npm run dev
```

开发服务器运行在 `http://localhost:3002`，`/bgssai` 代理到 `http://localhost:8080`（user 后端）。

## 构建与部署

```bash
# 构建并同步到 user 后端 static 目录
npm run build:deploy
```

构建产物同步到 `../bgssai-short-user/src/main/resources/static/`（通过 `scripts/sync-static.mjs`）。
可用 `BGSS_STATIC_DIR` 环境变量覆盖目标路径。

## 页面结构

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录页 | 用户账号密码登录，`role_code=USER` |
| `/home` | 首页 | 示例首页，展示当前登录身份 |
| `/login-logs` | 登录日志 | 示例分页列表，消费 PageResponse 封装，仅本人记录 |

## 接口约定

- 接口字段全部 snake_case
- POST 业务参数走 JSON Body，不拼在 URL 中
- 用户身份走 JWT（请求头 `Jwttoken`）
- 统一响应封装：`{ code, message, success, result }`；`code "0"` 或 `"200"` 为成功

## 设计真值来源

原型、API 契约、需求 PRD 的唯一权威来源是单仓根 `docs/`：

- 原型：`../../docs/demo-static/web/user/index.html`（跳转真值 `../../docs/demo-static/web/user/PAGE-FLOW.html`）
- API 契约：`../../docs/api/`
- 需求 PRD：`../../docs/feature/`
- 架构设计：`../../docs/architecture/`
- 安全设计：`../../docs/security/`
- 规范：[全局开发规范](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/BGSSAI-Standards.md)
