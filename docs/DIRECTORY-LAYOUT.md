# docs 目录结构规约（单一权威）

> 本文件是本仓库 `docs/` 目录组织的**单一权威**。新增 / 整理任何文档前先读本规约；与其它文档
> （含 `docs/BGSSAI-Standards.md` §9、`CLAUDE.md`）中旧的目录表述不一致处，**一律以本文件为准**。

## 目录拓扑（强制）

```text
docs/
├── api/                      ← 接口文档
├── architecture/             ← 架构文档
├── demo-static/              ← 原型页面
│   ├── android/              ← Android 端原型（仅用户端）
│   ├── ios/                  ← iOS 端原型（仅用户端）
│   └── web/                  ← web 端原型
│       ├── admin/            ← 管理端
│       └── user/             ← 用户端
├── feature/                  ← 功能需求
├── project/                  ← 项目进度
├── review/                   ← 项目检查记录
└── security/                 ← 安全相关文档
```

## 目录职责

| 目录 | 职责 |
| --- | --- |
| `docs/api` | 接口文档（REST / API 契约；离线 Markdown，禁用 Swagger 等运行时注解工具）。「页面 → API」映射主表为 `docs/api/page-api-mapping.md` |
| `docs/architecture` | 架构文档（系统分解 / ADR / 数据模型 / 状态机 / 时序图 / 架构图等通用技术设计） |
| `docs/demo-static` | 原型页面**与页面跳转描述**，纯 HTML（见下「原型目录」） |
| `docs/feature` | 功能需求（每个功能的需求澄清、验收标准、详细设计合并归此） |
| `docs/project` | 项目进度（里程碑与排期、迭代计划、待办与风险、发布记录） |
| `docs/review` | 项目检查记录（CR 结论、规范合规巡检、四层一致性核对、验收检查、坏合并事后审计） |
| `docs/security` | 安全相关文档（安全设计 / 威胁建模 / 安全规范）；**必须与常规功能分开存放**，不混入 `feature` / `architecture` |

## 只有 `demo-static` 按端拆分（强制）

管理端与用户端是两个独立部署的应用，但**共用同一套数据结构与接口契约**，因此除原型外的文档**一律统一
维护、不按端拆目录**：

| 目录 | 是否按端拆分 | 说明 |
| --- | --- | --- |
| `docs/demo-static` | **是** | 原型要直观呈现各端功能界面，故分 `web/admin`、`web/user`、`ios`、`android` |
| `docs/api` | 否 | 一份契约覆盖两端，端差异（路径前缀 `/bgssai/admin` 与 `/bgssai/user`、角色、数据可见范围）在同一篇契约内并列说明 |
| `docs/architecture` · `docs/feature` · `docs/project` · `docs/review` · `docs/security` | 否 | 同一份需求 / 架构 / 进度 / 检查 / 安全文档同时覆盖两端；涉端差异在文内分小节说明 |

**禁止**在上述「否」的目录下新建 `admin/` · `user/` 子目录或 `*-admin.md` · `*-user.md` 双份副本——同一概念
两份文档必然漂移（Standards §0「全产出版本一致」、§11 型六）。

> `docs/` 顶层只设上述 7 类目录（另有本规约与 `BGSSAI-Standards.md`、`README.md` 三份根级文件）。
> 数据库脚本**不在 `docs/` 维护**，统一收敛到仓库根 `sql/`（见 `docs/BGSSAI-Standards.md` §9）；
> 云资源清单 / 外部 API / 运维参考资料归入 `docs/architecture`。

## 原型目录 `docs/demo-static`（强制）

- 存放可运行 HTML 原型 + 内联 mock 数据，**以及页面跳转描述**——两者均为 HTML 形式，不放 `.md` 文档。
- **按端建子目录**：`web/`、`ios/`、`android/`。**web 端再按角色端拆** `web/admin/`（管理端）与
  `web/user/`（用户端）；`ios/` 与 `android/` **只有用户端**，因此直接平铺，不再分 `admin/` / `user/`。
- **每个端目录内必须有一份 `index.html`** 作为该端原型与页面跳转的概览 / 导航入口；跳转真值另存
  `PAGE-FLOW.html`。此处「端目录」指 `web/admin/`、`web/user/`、`ios/`、`android/` 四者。
- `web/index.html` 是 web 端的分流概览（导向 `admin/` 与 `user/`）；`web/assets/` 存放两个角色端共享的
  CSS，各页以 `../assets/theme.css` 引用。
- 本仓库现含：`web/admin/` 与 `web/user/` 各一套 `index.html`（概览）、`PAGE-FLOW.html`（跳转真值）、
  `login.html`、`home.html`、`login-logs.html`；`web/assets/theme.css`；`ios/index.html` 与
  `android/index.html` 为占位页（本仓库暂不含移动端工程，产品支持后在对应目录补齐该端产物）。

## 前端工程不放文档（强制）

- 前端工程（`*-react` 等）一律不放任何项目文档（PRD / 需求 / 架构 / API 契约 / 原型与页面跳转 /
  进度 / 检查记录 / 安全）。
- 唯一文档权威是仓库根 `docs/`；前端仅保留工程自身运行必需的 `README.md`（构建 / 运行）。

## 详细设计文档须含图（强制）

- `docs/feature` 下每个功能的详细设计，以及 `docs/architecture` 下的技术架构设计，必须包含流程图 /
  时序图 / 状态泳道图等可视化图示，不得只有纯文字描述。
- **优先用 HTML 绘制**：自包含 `.html`（内联 SVG / CSS），与本仓库原型统一为 HTML 形态；图随设计文档
  就近存放（功能级图归 `docs/feature`，通用架构图归 `docs/architecture`）。`docs/architecture/_template.html`
  为可复制的图示模板。
