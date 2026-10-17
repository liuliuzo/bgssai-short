# docs（文档单一事实来源）

本目录是本仓库全部文档的**单一事实来源（SoT）**。前端与后端工程一律依据本目录开发，严禁在
工程内另起需求 / 设计 / 契约文档。

## 导航

| 入口 | 内容 |
| --- | --- |
| [全局开发规范](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/BGSSAI-Standards.md) | 全局开发规范（最高优先级强制约束，自动导入每次会话） |
| `DIRECTORY-LAYOUT.md` | 本 `docs/` 目录组织的单一权威 |
| `feature/` | 功能需求澄清 + 详细设计（需求是版本基准与单一事实来源） |
| `demo-static/` | 可运行 HTML 原型 + 页面跳转真值（接口字段 / 响应结构 / 跳转关系的权威参照）。按端分目录：`web/admin`（管理端）、`web/user`（用户端）、`ios`、`android`（仅用户端） |
| `architecture/` | 通用技术架构设计（系统分解 / 数据模型 / 状态机 / 时序图 / 架构图） |
| `api/` | REST / API 契约（离线 Markdown）；`api/page-api-mapping.md` 为「页面 → API」映射主表 |
| `project/` | 项目进度（里程碑 / 排期 / 迭代计划 / 待办与风险 / 发布记录） |
| `review/` | 项目检查记录（CR 结论 / 合规巡检 / 四层一致性核对 / 坏合并事后审计） |
| `security/` | 安全设计 / 威胁建模 / 安全规范（与常规功能分开存放） |

> 管理端与用户端共用同一套数据结构与契约，因此**只有 `demo-static` 按端拆分**；`api` / `architecture` /
> `feature` / `project` / `review` / `security` 统一维护，端差异在同一篇文档内分小节说明。

## 必经链路

```
PRD (feature) → 原型 (demo-static) → 架构与契约 (architecture / api / security) → 代码 (src/**, 前端) 与数据库 (sql/**)
```

单向依赖：下游一律以上游为准，并最终对齐到需求当前版本。任何需求变更或缺陷修复，四层（需求 / 原型 /
设计 / 代码与数据库）必须在同一次提交内同步完成（Standards §0）。
