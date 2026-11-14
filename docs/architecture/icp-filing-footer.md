# 网站 ICP 页脚设计

需求：[网站 ICP 备案展示](../feature/icp-filing-footer.md)。原型：[用户端](../demo-static/web/user/icp-filing-footer.html)、[管理端](../demo-static/web/admin/icp-filing-footer.html)。

两端 `src/lib/legalLinks.js` 导出本域名已核准号码 `ICP_FILING_NUMBER = '苏ICP备2024070062号-15'` 与 `ICP_FILING_URL = 'https://beian.miit.gov.cn/'`。`LegalFooter.jsx` 必须渲染该号码。用户端放在同一行浅灰页脚的末段，管理端放在版权行之后的独立备案行。链接使用 `target="_blank"` 和 `rel="noopener noreferrer"`；不经后端代理、不携带应用凭据。

产品页脚同时渲染五条官网法律外链。法律地址只使用 `https://www.bgssai.com/{slug}/` 与 `https://www.bgssai.com/en/{slug}/`。登录同意句仍只使用用户协议和隐私政策，同意组件不渲染备案号。

单元测试校验五条官网路径、页脚源码引用 `ICP_FILING_NUMBER` / `ICP_FILING_URL`，并继续拦截其他后缀和未授权证照编号。不得把 `-1` 至 `-17` 中除 `-15` 以外的号码写进本仓页脚。

本次不改变法律文档目录、登录同意逻辑、业务页面、API、数据库、环境配置或原生客户端。上述层不涉及数据模型及契约变更，无需 SQL 迁移。两端构建通过后经 PR 合入 `develop`，再由已有 Jenkins dev/prod job 各自部署。
