# 官网法律页分发（HOW）

需求权威：[`../feature/legal-fan-out.md`](../feature/legal-fan-out.md)。状态：**需法务审阅**。

- 落点：用户端 / 管理端各一份 `src/lib/legalLinks.js`，五页 URL 与中英标签只在这里登记。
- 展示：`LegalFooter` 用于登录面板与 `MainLayout` 页脚。`target=_blank` + `rel=noopener noreferrer`。
- 用户端：登录 + 登录后页脚。管理端：同样外链，不增加站内 `/legal` 路由。
- 版权行只写公司名。`legalLinks` 测试拒绝 ICP / 许可证号样例。
- 无后端接口，无 JWT / 密钥变更，无 DDL / DML。
- 流程图：[`../feature/flow-legal-fan-out.html`](../feature/flow-legal-fan-out.html)。
