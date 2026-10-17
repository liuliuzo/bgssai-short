# 官网法律页分发（legal-fan-out）

> 状态：**需法务审阅**。正文只在 `bgssai-website`，本仓只做外链。对照官网
> `docs/legal/README.md`。禁止编造 ICP / 许可证号。

## 一、背景与目标

- 背景：短剧用户端登录与页脚需要指向官网权威法律页；管理端不托管法律正文。
- 目标：用户端登录页与全站页脚展示中英双语外链；管理端仅外链；页脚只写公司名，不写未颁发证照号。
- 关联角色：`USER`（登录 + 页脚）；`PLATFORM_ADMIN`（仅外链）。

## 二、需求澄清（WHAT）

- 用户故事：作为创作者，我希望在登录和页脚用中英文打开官网法律页，而不在短剧产品里读一份另写的条款。
- 业务规则：
  1. 权威正文只在官网，本仓不复制、不改写条款。路径必须与官网 `docs/legal/README.md` **逐字一致**。
  2. 基址只能是 `https://www.bgssai.com`，禁止 `www.bgssai.cn`。
  3. 五页中英路径（中文无前缀，英文一律 `/en/`，均带尾斜杠）：
     - 服务条款 / Terms of Service：`/terms-of-service/` + `/en/terms-of-service/`
     - 隐私政策 / Privacy Policy：`/privacy-policy/` + `/en/privacy-policy/`
     - 个人信息收集清单 / Personal Information Inventory：`/personal-information-inventory/` + `/en/personal-information-inventory/`
     - 第三方信息共享清单 / Third-Party Sharing：`/third-party-sharing/` + `/en/third-party-sharing/`
     - 应用权限 / App Permissions：`/app-permissions/` + `/en/app-permissions/`
  4. 可选索引：`/legal/` + `/en/legal/`。页脚不强制展示。
  5. 禁止旧路径：`/legal/user-agreement`、`children-privacy`、`personal-info-collection`，以及任何 `bgssai.cn` 法律页。
  6. 用户端登录（对齐 blog v003）：登录按钮下同意句仅「用户协议」「隐私政策」中文锚点；登录区页脚仅一行 `© 2026 昆山兵贵神速智能科技有限公司`（不挂五页 fan-out，不编造 ICP）。
  7. 用户端登录后页脚：五条中英外链（中文标签开中文页，英文标签开英文页，条目间用 `·` 分隔）+ 版权行，新标签打开。
  8. 管理端：登录页与页脚同样外链策略（登录精简 / 非登录五页），不新增 `/legal` 路由，不托管正文。
  9. 版权行只写 `© 2026 昆山兵贵神速智能科技有限公司`。禁止编造 ICP 备案号、公安备案号、增值电信业务经营许可证号。
  10. 本切片不改接口、不改 `sql/`、不改鉴权。

## 三、验收标准

1. 用户端 `/login`：同意句（用户协议 + 隐私政策）+ 版权行；登录后页脚可见五条中英外链（`·` 分隔），目标为 `https://www.bgssai.com/<slug>/` 与 `https://www.bgssai.com/en/<slug>/`。
2. 管理端登录与页脚只有外链，无站内法律页。
3. 页脚与登录区不出现证照编号，也不出现已退役路径。
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

## 2026-09-23 收敛

用户端登录、注册只呈现用户协议与隐私政策两份文件的一行同意文案，已有勾选框保持原交互。管理端登录不放同意行。Web 全路由使用统一页脚，年份动态读取，备案号沿用本仓既有号码；无备案号时不展示。中英界面各显示当前语言。原生客户端只收敛已有同意及设置入口。内部审阅备注不进入产品界面，法务正文保持原内容。

标准见 [bgssai-skeleton https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/BGSSAI-Standards.md 第 15 节](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/BGSSAI-Standards.md#15-法务同意行与页脚统一口径全产品线强制)。官网用户端沿用服务条款、隐私政策、法律信息和语言切换的页脚底栏。
