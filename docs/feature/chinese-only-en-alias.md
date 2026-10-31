# 中文产品的 /en 入口

> 原型：`docs/demo-static/web/user/PAGE-FLOW.html`、`docs/demo-static/web/admin/PAGE-FLOW.html`
> 契约：`docs/api/page-api-mapping.md`
> 配图：[`flow-chinese-only-en-alias.html`](./flow-chinese-only-en-alias.html)

## 背景

本产品只有中文界面。客人打开 `/en` 或 `/en/login` 时落到「页面不存在」，进不了已经可用的中文登录页。

## 需求

1. 用户端与管理端：`/en`、`/en/`、`/en/*` 替换进入已有中文登录页 `/login`。保留原查询串和 hash。已登录用户仍由登录页守卫进入 `/home`。
2. 不新增英文页面、英文文案或英文登录表单。
3. 不以 `/en` 开头的未知路径（含 `/privacy`、`/privacy-policy`、`/enterprise`）仍显示「页面不存在」。
4. 官网法律页继续外链到 `https://www.bgssai.com`，本站不托管这些正文。

## 验收

1. 未登录打开 `/en` 与 `/en/login`，地址变为 `/login`，可见中文登录页。
2. 未登录打开 `/no-such-page`，仍见「页面不存在」。
