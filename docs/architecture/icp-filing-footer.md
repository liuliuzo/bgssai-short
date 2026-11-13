# 网站 ICP 页脚设计

需求：[网站 ICP 备案展示](../feature/icp-filing-footer.md)。原型：[用户端](../demo-static/web/user/icp-filing-footer.html)、[管理端](../demo-static/web/admin/icp-filing-footer.html)。

两端 `src/lib/legalLinks.js` 导出固定常量 `ICP_FILING_NUMBER = '苏ICP备2024070062号-15'` 与 `ICP_FILING_URL = 'https://beian.miit.gov.cn/'`。两端 `src/components/LegalFooter.jsx` 在现有官网法律链接与版权行之后渲染 `苏ICP备2024070062号-15` 链接。外链使用 `target="_blank"` 和 `rel="noopener noreferrer"`；不经后端代理、不携带应用凭据。

两端 App 保持唯一公共页脚挂载。管理端 `.legal-footer` 仍是条款链接、版权、备案分层，备案号在独立末行。用户端登录页与登录后各页改为一行浅灰中文：版权、用户协议、隐私政策、`苏ICP备2024070062号-15`，中间用 `·` 分隔；用户协议打开 `https://www.bgssai.com/terms-of-service/`，隐私政策打开 `https://www.bgssai.com/privacy-policy/`，备案号打开 `https://beian.miit.gov.cn/`。用户端不展示英文副行，链接无亮色下划线，不铺浅色底带。窄屏可以换行，不隐藏备案号，也不遮挡登录主区。中文备案号不翻译。管理端页脚组件不改。

单元测试 `src/lib/legalLinks.test.js` 校验已核准备案号 `苏ICP备2024070062号-15` 与 `https://beian.miit.gov.cn/`，并继续拦截未授权或虚构的证照编号。

本次不改变法律文档目录、登录同意逻辑、业务页面、API、数据库、环境配置或原生客户端。上述层不涉及数据模型及契约变更，无需 SQL 迁移。两端构建通过后经 PR 合入 `develop`，再由已有 Jenkins dev/prod job 各自部署。
