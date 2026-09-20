# 官网法律页分发（HOW）

需求权威：[`../feature/legal-fan-out.md`](../feature/legal-fan-out.md)。状态：**需法务审阅**。
对齐 bgssai-blog feature-088 **v003** 登录精简策略。

- 落点：用户端 / 管理端各一份 `src/lib/legalLinks.js`，五页中英 URL 与标签只在这里登记，路径对齐官网 `docs/legal/README.md`。
- 基址：`https://www.bgssai.com`。中文路径无前缀、英文路径 `/en/`，均带尾斜杠。
- 五页：`/terms-of-service/`、`/privacy-policy/`、`/personal-information-inventory/`、`/third-party-sharing/`、`/app-permissions/`，以及对应 `/en/*`。
- 可选索引：`/legal/` 与 `/en/legal/`，页脚不强制展示。
- 用户端登录：`LegalFooter variant="consent"`（仅「用户协议」「隐私政策」中文锚点）+ `variant="login"`（仅 `COPYRIGHT_LINE`）。不挂五页 fan-out，不编造 ICP。
- 用户端登录后：`LegalFooter variant="app"` — 五条中英外链（条目间 `·`）+ 版权行，挂在 `MainLayout`。`target=_blank` + `rel=noopener noreferrer`。
- 管理端：同样外链，不增加站内 `/legal` 路由。
- 版权行只写公司名。`legalLinks` 测试拒绝 ICP / 许可证号样例，并拒绝 `bgssai.cn`、`/legal/user-agreement`、`children-privacy`、`personal-info-collection`。
- 无后端接口，无 JWT / 密钥变更，无 DDL / DML。
- 流程图：[`../feature/flow-legal-fan-out.html`](../feature/flow-legal-fan-out.html)。
