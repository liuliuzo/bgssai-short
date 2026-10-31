# 中文产品 /en 入口

需求：[`../feature/chinese-only-en-alias.md`](../feature/chinese-only-en-alias.md)。
跳转真值：用户端与管理端 `PAGE-FLOW.html`。页面契约：[`../api/page-api-mapping.md`](../api/page-api-mapping.md)。

两端前端各登记 `/en` 与 `/en/*`，组件替换导航到 `/login`，并带上原查询串和 hash。
未匹配到这两条、但路径仍以 `/en/` 开头时，未知页同样替换到 `/login`。
Spring 的 SPA 前缀白名单包含 `/en`，浏览器文档请求直接落到 `index.html`。

没有新接口，没有英文资源包。
