# 网站 ICP 页脚设计

需求：[网站 ICP 备案展示](../feature/icp-filing-footer.md)。原型：[用户端](../demo-static/web/user/icp-filing-footer.html)、[管理端](../demo-static/web/admin/icp-filing-footer.html)。

两端 `src/lib/legalLinks.js` 仍导出已核准号码 `ICP_FILING_NUMBER = '苏ICP备2024070062号-15'`，只用于拒绝其他编造号码。2026-11-14 起 `LegalFooter.jsx` 不再渲染该号码，也不链接工信部。产品页脚是五条官网法律外链。外链使用 `target="_blank"` 和 `rel="noopener noreferrer"`；不经后端代理、不携带应用凭据。

单元测试校验五条官网路径，并确认页脚源码不含备案链接，同时继续拦截未授权或虚构的证照编号。

本次不改变法律文档目录、登录同意逻辑、业务页面、API、数据库、环境配置或原生客户端。上述层不涉及数据模型及契约变更，无需 SQL 迁移。两端构建通过后经 PR 合入 `develop`，再由已有 Jenkins dev/prod job 各自部署。
