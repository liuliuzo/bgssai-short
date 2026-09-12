# 官网法律页分发（legal-fan-out）

> 状态：**需法务审阅**。正文只在 `bgssai-website`，本仓只做外链。对照官网
> `docs/feature/legal-fan-out-checklist.md`。禁止编造 ICP / 许可证号。

## 一、背景与目标

- 背景：短剧用户端登录与页脚需要指向官网五份权威法律页；管理端不托管法律正文。
- 目标：用户端登录页与全站页脚展示中英双语外链；管理端仅外链；页脚只写公司名，不写未颁发证照号。
- 关联角色：`USER`（登录 + 页脚）；`PLATFORM_ADMIN`（仅外链）。

## 二、需求澄清（WHAT）

- 用户故事：作为创作者，我希望在登录和页脚用中英文打开官网法律页，而不在短剧产品里读一份另写的条款。
- 业务规则：
  1. 权威正文只在官网五页，本仓不复制、不改写条款。
  2. 五页（中文 / English，同一 URL 双语页）：用户协议 / User Agreement、隐私政策 / Privacy Policy、儿童个人信息保护规则 / Children's Privacy、第三方信息共享清单 / Third-Party Sharing、个人信息收集清单 / Personal Information Collection。
  3. 用户端：登录面板底部 + 登录后页脚，五条双语外链，新标签打开。
  4. 管理端：登录页与页脚同样五条外链，不新增 `/legal` 路由，不托管正文。
  5. 版权行只写 `© 2026 昆山兵贵神速智能科技有限公司`。禁止编造 ICP 备案号、公安备案号、增值电信业务经营许可证号。
  6. 本切片不改接口、不改 `sql/`、不改鉴权。

## 三、验收标准

1. 用户端 `/login` 与登录后页脚可见五条双语外链，目标为 `https://www.bgssai.cn/legal/*`。
2. 管理端登录与页脚只有外链，无站内法律页。
3. 页脚与登录区不出现证照编号。
4. 需求 / 原型 / 设计标注 **需法务审阅**。
5. 无新后端接口，无 DDL / DML。

## 四、接口契约（snake_case）

无新接口。页面仍走既有登录契约 [`../api/auth-login.md`](../api/auth-login.md)。外链不是 API 字段。

## 五、流程 / 状态机

见 [`flow-legal-fan-out.html`](./flow-legal-fan-out.html)。

## 六、数据模型影响

无。

## 七、原型

- 涉及端：web 用户端、web 管理端
- 页面：[`../demo-static/web/user/login.html`](../demo-static/web/user/login.html)、用户端各页页脚；[`../demo-static/web/admin/login.html`](../demo-static/web/admin/login.html)、管理端各页页脚（仅外链）
- 跳转：登记两端 `PAGE-FLOW.html`（外链，不是新路由）
- 映射：[`../api/page-api-mapping.md`](../api/page-api-mapping.md)

## 八、四层同步检查清单

- [x] 需求（本文件）
- [x] 原型（登录页 + 页脚 + `PAGE-FLOW.html`）
- [x] 设计与契约（详设 + 映射表 + 安全说明）
- [x] 代码（用户端 / 管理端 React；无 SQL）
