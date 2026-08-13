# docs/feature（功能需求 + 详细设计）

本目录承载每个功能的**需求澄清（WHAT）+ 详细设计**。需求是版本基准与单一事实来源，一切下游产出
（原型 / 架构 / 契约 / 代码 / 数据库）对齐到需求当前版本（Standards §0）。

## 约定

- 每个功能一份 Markdown（可拆多份），命名用小写连字符，如 `account-registration.md`。
- 详细设计**必须配图**（流程图 / 时序图 / 状态泳道图），优先用自包含 HTML 绘制，随功能就近存放本目录。
- 新增 / 修改功能时，同步更新原型（`docs/demo-static` 下**涉及的每个端目录**）、架构与契约
  （`docs/architecture` / `docs/api`，含 `docs/api/page-api-mapping.md`）、代码与数据库（`src/**`、`sql/**`），
  四层缺一不可。
- 本目录**不按端拆分**：管理端与用户端共用同一套数据结构与契约，同一功能一份需求文档，端差异在文内分
  小节说明；严禁建 `admin/` · `user/` 子目录或双份副本。
- 新建功能文档可复制 `_TEMPLATE-feature.md`。

## 仓库内置的基础功能（自骨架继承）

| 功能 | 说明 | 契约 | 原型 |
| --- | --- | --- | --- |
| 登录鉴权 | 账号密码登录签发 JWT，请求头 `Jwttoken` 携带；管理端限 `PLATFORM_ADMIN` | `../api/auth-login.md` | `../demo-static/web/admin/login.html`、`../demo-static/web/user/login.html` |
| 登录日志（示例分页） | 分页查询登录记录，演示 `@NeedAop` + `PageResponse` + PageHelper；管理端全量、用户端仅本人 | `../api/auth-login.md` | `../demo-static/web/admin/login-logs.html`、`../demo-static/web/user/login-logs.html` |
| 健康检查 | 全产品线统一方案（Standards §13）的落地实现：存活 / 就绪 / 全量三端点 + 组件化探针；供部署脚本与集中巡检平台 `bgssai-healthcheck` 调用 | `../api/health-check.md`（详设 `../architecture/health-check-design.md`） | 无（非功能需求，无用户界面，按 Standards §0 不走原型一步） |
| MCP 服务 | 用户端 Streamable HTTP MCP + PAT；只读包装登录日志与存活/就绪探针，不发明短剧领域工具 | `../api/MCP.md` | `../demo-static/web/user/mcp-tokens.html` |
