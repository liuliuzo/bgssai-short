# sql（共享库权威 schema）

管理端与用户端共用一个 MySQL 库 `bgssai_short_drama`；其 schema 与种子数据统一在本目录维护，不在各后端
模块 `src/` 下重复存放（Standards §9）。

## 文件

| 文件 | 说明 |
| --- | --- |
| `DDL.sql` | 建库 + 建表（当前含 `sys_user`、`login_log` 两张基础表，业务表随需求逐个追加）。自顶向下执行一次即建出完整 schema。 |
| `DML.sql` | 种子数据（两个示例账号，BCrypt 密码）。 |

## 导入

```bash
mysql -h <host> -u <user> -p < sql/DDL.sql
mysql -h <host> -u <user> -p bgssai_short_drama < sql/DML.sql
```

## 开发环境种子账号

| 账号 | 密码 | 角色 | 可登录 |
| --- | --- | --- | --- |
| `admin` | `admin123` | `PLATFORM_ADMIN` | 管理端 8080 |

用户端通过注册或第三方授权建号。登录页不得公示口令。生产环境请改密 `admin`，不要沿用开发口令。

## 结构性变更约定

任何加 / 改 / 删列后须同步：更新 `DDL.sql`、重跑 MyBatis Generator 更新后端 `model/**` / `mapper/**` /
Mapper XML，并保持需求 / 原型 / 设计 / 代码四层一致（Standards §0、§9）。应用启动期不自动执行 DDL，
schema 由部署方手动执行。
