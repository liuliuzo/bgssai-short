# 网站 ICP 页脚设计

需求：[网站 ICP 备案展示](../feature/icp-filing-footer.md)。原型：[用户端](../demo-static/web/user/icp-filing-footer.html)、[管理端](../demo-static/web/admin/icp-filing-footer.html)。

两端 `src/lib/legalLinks.js` 导出固定常量 `ICP_FILING_NUMBER = '苏ICP备2024070062号-15'` 与 `ICP_FILING_URL = 'https://beian.miit.gov.cn/'`。两端 `src/components/LegalFooter.jsx` 在现有官网法律链接与版权行之后渲染 `苏ICP备2024070062号-15` 链接。外链使用 `target="_blank"` 和 `rel="noopener noreferrer"`；不经后端代理、不携带应用凭据。

两端 App 保持唯一公共页脚挂载。`.legal-footer` 分三层：条款链接、版权、备案；居中、自然换行，备案项 `.legal-footer-filing` 位于独立末行，窄屏可访问且不遮挡登录主区。用户端条款层按语言分行（中文一行、英文一行，组内 `·` 分隔），不使用「中文 / English」斜杠对，也不铺整条浅色底带。中文与英文界面均展示同一中文备案号，不翻译备案号。管理端页脚组件本次不改。

单元测试 `src/lib/legalLinks.test.js` 校验已核准备案号 `苏ICP备2024070062号-15` 与 `https://beian.miit.gov.cn/`，并继续拦截未授权或虚构的证照编号。

本次不改变法律文档目录、登录同意逻辑、业务页面、API、数据库、环境配置或原生客户端。上述层不涉及数据模型及契约变更，无需 SQL 迁移。两端构建通过后经 PR 合入 `develop`，再由已有 Jenkins dev/prod job 各自部署。
