# 中文产品的 /en 入口

> 原型：`docs/demo-static/web/user/PAGE-FLOW.html`、`docs/demo-static/web/admin/PAGE-FLOW.html`
> 契约：`docs/api/page-api-mapping.md`
> 配图：[`flow-chinese-only-en-alias.html`](./flow-chinese-only-en-alias.html)

## 背景

本产品只有中文界面。客人打开 `/en` 或 `/en/login` 时落到「页面不存在」，进不了已经可用的中文登录页。

## 需求

1. 用户端与管理端：`/en`、`/en/`、`/en/*` 进入已有中文登录页 `/login`，地址栏变为 `/login`。GET 与 HEAD 由服务端 302，`Location` 为 `/login` 并保留查询串；浏览器保留 hash。前端路由仍在，避免页面契约丢掉这个别名。已登录用户仍由登录页守卫进入 `/home`。
2. 不新增英文页面、英文文案或英文登录表单。
3. 不以 `/en` 开头的未知路径（含 `/privacy`、`/privacy-policy`、`/enterprise`）仍显示「页面不存在」。
4. 官网法律页继续外链到 `https://www.bgssai.com`，本站不托管这些正文。

## 验收

1. 未登录打开 `/en`、`/en/` 与 `/en/login`，响应是 302，地址变为 `/login`，可见中文登录页，不再把中文登录停在 `/en`。
2. 未登录打开 `/no-such-page`，仍见「页面不存在」。
