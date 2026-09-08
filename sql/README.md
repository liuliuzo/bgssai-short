# sql（共享库权威 schema）

管理端与用户端共用一个 MySQL 库 `bgssai_short`；其 schema 与种子数据统一在本目录维护，不在各后端
模块 `src/` 下重复存放（Standards §9）。

## 文件

| 文件 | 说明 |
| --- | --- |
| `DDL.sql` | 建库 + 建表（登录 / OAuth / MCP / 订阅支付 / 短剧制作闭环）。自顶向下执行一次即建出完整 schema。 |
| `DML.sql` | 种子数据：示例账号、套餐目录、OAuth 密钥、用户端铺底、短剧业务样例。 |
| `dev/DML.sql` | 开发环境专属种子（在共享 DML 之后执行）。 |
| `prod/DML.sql` | 生产环境专属种子（在共享 DML 之后执行）。 |

> **`DDL.sql` 只能建新库。** 它含 `DROP TABLE IF EXISTS`，对已有数据的库执行等于删库重来。
> **禁止增量脚本**：不得新增或保留 `UPGRADE-*.sql`、`PRECHECK-*.sql`、`patch_*.sql`、
> Flyway / Liquibase、`V*__xxx.sql`、`sql/migration/`。结构与种子只改全量 `DDL.sql` / `DML.sql`
>（及 `sql/dev/DML.sql`、`sql/prod/DML.sql`）。落地：Jenkins `clean database` → `init database`
>（`DROP DATABASE` → DDL → 共享 DML → 环境 DML）。重建会丢弃该库既有业务数据。
> 应用启动期不跑任何 DDL。

## 导入

```bash
mysql -h <host> -u <user> -p < sql/DDL.sql
mysql -h <host> -u <user> -p bgssai_short < sql/DML.sql
mysql -h <host> -u <user> -p bgssai_short < sql/dev/DML.sql
```

生产清库后把最后一步换成 `sql/prod/DML.sql`。

## 开发环境种子账号

| 账号 | 密码 | 角色 | 可登录 | 说明 |
| --- | --- | --- | --- | --- |
| `admin` | `admin123` | `PLATFORM_ADMIN` | 管理端 | 审核队列、创作者列表、套餐与商户配置 |
| `demo` | `demo123` | `USER` | 用户端 | 推荐联调账号：PRO 额度 + 6 部覆盖各状态的短剧样例 |
| `seeduser001` ~ `seeduser080` | `demo123` | `USER` | 用户端 | 大批量铺底；各带轻量作品 / 订阅，便于列表与审核联调 |

登录页不得公示口令。生产环境请改密 `admin`，不要沿用开发口令；铺底段（`USER-VOLUME-SEED` /
`SHORT-BIZ-SEED`）禁止在生产执行。

### `demo` 样例作品（登录用户端即可看到）

| work_id | 剧名 | work_status | 用途 |
| --- | --- | --- | --- |
| `w-dm-001` | 退婚三年后她成了首富 | `PRODUCING` | 制作中全链路（角色 / 分镜 / 任务 / 成片） |
| `w-dm-002` | 重生之我在豪门当保姆 | `SCRIPTING` | 剧本中 |
| `w-dm-003` | 甜宠小娇妻养成记 | `DRAFT` | 新建草稿 |
| `w-dm-004` | 穿越之女帝归来 | `COMPLETED` | 已完成并审核通过 |
| `w-dm-005` | 悬疑法医手记 | `REVIEWING` | 待管理端审核 |
| `w-dm-006` | 总裁的替身新娘 | `ARCHIVED` | 已归档 |

## 结构性变更约定

任何加 / 改 / 删列后须同步：更新 `DDL.sql`、手工同步持久层三件套 更新后端 `model/**` / `mapper/**` /
Mapper XML，并保持需求 / 原型 / 设计 / 代码四层一致（Standards §0、§9）。应用启动期不自动执行 DDL，
schema 由部署方手动执行。

短剧业务种子可由 `tmp/gen_short_seed_dml.py` 重新生成后写回 `DML.sql` 的 `SHORT-BIZ-SEED` 段。
