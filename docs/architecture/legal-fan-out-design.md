# 官网法律页分发（HOW）

需求权威：[`../feature/legal-fan-out.md`](../feature/legal-fan-out.md)。状态：**需法务审阅**。
对齐 bgssai-blog feature-088 **v003** 登录精简策略。

- 落点：用户端 / 管理端各一份 `src/lib/legalLinks.js`，服务条款与隐私政策中英 URL、标签及已核准网站备案号只在这里登记，路径对齐官网 `docs/legal/README.md`。
- 基址：`https://www.bgssai.com`。中文路径无前缀、英文路径 `/en/`，均带尾斜杠。
- 五页：`/terms-of-service/`、`/privacy-policy/`、`/personal-information-inventory/`、`/third-party-sharing/`、`/app-permissions/`，以及对应 `/en/*`。
- 可选索引：`/legal/` 与 `/en/legal/`，页脚不强制展示。
- 用户端登录：`LegalFooter variant="consent"`（仅「用户协议」「隐私政策」中文锚点）+ 全局 `LegalFooter`（中英外链 + 版权行 + 已核准备案号 `苏ICP备2024070062号-15`）。
- 用户端与管理端全路由共用 App 根挂载的 `LegalFooter` — 服务条款与隐私政策中英外链 + 版权行 + `苏ICP备2024070062号-15`（链接 `https://beian.miit.gov.cn/`，`target="_blank" rel="noopener noreferrer"`），见 [网站 ICP 页脚设计](icp-filing-footer.md)。
- 页脚排版三层：链接行（可换行）、版权行、备案行；字号与行高保持可读，避免强制单行或固定定位遮挡。
- 管理端：同样外链，不增加站内 `/legal` 路由。
- 版权行写公司名；备案行展示已核准的 `苏ICP备2024070062号-15`。`legalLinks` 测试校验 `苏ICP备2024070062号-15` 与 `https://beian.miit.gov.cn/`，继续拒绝其他编造的 ICP / 许可证号样例，并拒绝 `bgssai.cn`、`/legal/user-agreement`、`children-privacy`、`personal-info-collection`。
- 无后端接口，无 JWT / 密钥变更，无 DDL / DML。
- 流程图：[`../feature/flow-legal-fan-out.html`](../feature/flow-legal-fan-out.html)。
