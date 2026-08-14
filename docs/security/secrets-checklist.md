# 密钥与敏感配置清单（secrets-checklist）

生产 / 测试环境的敏感配置**一律写死在各 profile 文件里随 jar 打包**，
服务器上不放任何配置文件（原部署期密钥文件注入机制已停用）。本文件列出这些项与其代价。

## 敏感项清单（均已在各 profile 文件中落为最终字面量）

| key | 说明 | 生成 / 来源 |
| --- | --- | --- |
| `spring.datasource.url` | 数据库连接串（指向 `bgssai_short_drama` 库） | 环境 DBA 提供 |
| `spring.datasource.username` | 数据库账号 | 环境 DBA 提供 |
| `spring.datasource.password` | 数据库密码 | 环境 DBA 提供 |
| `jwt.secret` | JWT 签名密钥，>= 32 字符 | `openssl rand -base64 48`，dev/test/prod 各不复用、两端互不复用 |

## 落位方式

1. `application-dev/-test/-prod.properties` 三档中上述项一律**写死本环境最终字面量**，
   不留空、也不写 `${}` 占位符（Standards §1）。test 档数据源口令仍可为空（不连真实库的单元测试）。
2. 部署期服务器上**不放任何配置文件**：jar 自足，目标机只需 JDK 21，启动命令只带
   `--spring.profiles.active=<档>`。
3. `application-secrets.properties` 注入机制**已停用**。

## 安全约束与代价

- **代价（登记，不隐藏）**：生产数据库口令与 `jwt.secret` 随仓库和 jar 分发——凡能
  读仓库或取到 jar 的人即可直连生产库、伪造任意用户 token；轮换需改代码 + 重新构建部署，
  旧值永久留在 git 历史。这是**开发阶段以排障速度优先的取舍**，上线前应重新评估是否恢复服务器侧注入。
- `jwt.secret` 留空或不足 32 字符时 `JwtUtil` 启动 fail-fast（预期安全行为）。
- 本产品两端与中间件 / Jenkins 共机，当前未上 HTTPS；对外域名见 `deploy/README.md`。
- 泄漏或轮换：轮换 `jwt.secret` 会使存量 token 失效（用户需重新登录），属预期。
