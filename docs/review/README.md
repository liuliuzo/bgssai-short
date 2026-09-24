# docs/review（项目检查记录）

本目录承载**项目检查记录**：Code Review 结论、规范合规巡检、需求 / 原型 / 设计 / 代码四层一致性核对、
验收检查、坏合并（bad merge）事后审计等留痕。

## 与其它目录的边界

| 内容 | 归属 |
| --- | --- |
| 检查所依据的规范条款 | `docs/BGSSAI-Standards.md` |
| 被检查的需求 / 设计 / 契约 / 原型正文 | `docs/feature` / `docs/architecture` / `docs/api` / `docs/demo-static` |
| 进度 / 排期 / 发布记录 | `docs/project` |
| **某次检查「查了什么、发现什么、怎么处置」** | **本目录** |

本目录只放**检查记录**，不放规范正文——规范的单一权威是 `docs/BGSSAI-Standards.md`；检查中发现的规范缺口
应改规范本体，不在检查记录里另立一套口径。

## 约定

- 一次检查一份 Markdown，命名 `<日期>-<主题>.md`，如 `2026-07-30-docs-layout-audit.md`。
- 每份记录须含：检查范围、依据条款（Standards 章节号）、逐条结论（通过 / 打回 + 证据）、处置与责任人。
- 结论须有证据（文件路径 + 行号 / 命令输出 / PR 链接），不写无出处的判断。
- 坏合并审计记录按 Standards §11.5 归此目录，与修复 PR 描述口径一致。
- 仓库初始化时不预置具体检查记录；每完成一次检查即在此目录留痕。

## 功能测试用例

面向 `docs/feature/**` 验收标准的功能测试用例套件落在 [`test-cases/`](./test-cases/)：

| 文件 | 说明 |
| --- | --- |
| [`test-cases/README.md`](./test-cases/README.md) | 索引（功能清单、AC/TC 统计、范围说明） |
| [`test-cases/TEMPLATE.md`](./test-cases/TEMPLATE.md) | 用例表模板 |
| [`test-cases/STATUS.md`](./test-cases/STATUS.md) | 生成状态摘要 |
| `test-cases/TC-*.md` | 按功能 slug 展开的用例表 |

约定：用例不含口令 / JWT / PAT / 中间件密钥；真机或真实三方未就绪项在备注标 **PENDING**。
