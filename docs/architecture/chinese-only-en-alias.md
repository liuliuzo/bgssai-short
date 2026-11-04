# 中文产品 /en 入口

需求：[`../feature/chinese-only-en-alias.md`](../feature/chinese-only-en-alias.md)。
跳转真值：用户端与管理端 `PAGE-FLOW.html`。页面契约：[`../api/page-api-mapping.md`](../api/page-api-mapping.md)。

两端前端仍登记 `/en` 与 `/en/*`，页面契约继续把它们记在中文登录页上。
GET 与 HEAD 由 `EnglishEntryRedirectFilter` 返回 302，`Location` 为 `/login`，查询串原样附上。浏览器保留 hash。
SPA 前缀白名单仍包含 `/en`。若文档仍被返回，前端用 `location.replace` 离开 `/en`，不再把中文登录停在这个地址上。
未匹配到这两条、但路径仍以 `/en/` 开头时，同样离开到 `/login`。

没有新接口，没有英文资源包。
