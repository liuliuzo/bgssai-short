# 密钥与敏感配置清单（secrets-checklist）

生产 / 测试环境的敏感配置**绝不入 git**，一律由部署期密钥文件注入。本文件列出需注入项与操作口径。

## 需注入的敏感项

| key | 说明 | 生成 / 来源 |
| --- | --- | --- |
| `spring.datasource.url` | 数据库连接串（指向 `bgssai_short_drama` 库） | 环境 DBA 提供 |
| `spring.datasource.username` | 数据库账号 | 环境 DBA 提供 |
| `spring.datasource.password` | 数据库密码 | 环境 DBA 提供 |
| `jwt.secret` | JWT 签名密钥，>= 32 字符 | `openssl rand -base64 48`，dev/test/prod 各不复用 |

## 注入方式

1. 各 profile 文件（`application-test.properties` / `application-prod.properties`）中上述项**留空**，
   仓库内不写真实值，也不写 `${}` 占位符（Standards §1）。
2. 部署期在服务器放置密钥文件，给出本环境真实字面量：
   - 管理端：`/opt/bgssai/bgssai-short-drama-admin/application-secrets.properties`
   - 用户端：`/opt/bgssai/bgssai-short-drama-user/application-secrets.properties`
   - 模板见 `deploy/application-secrets.admin.properties.example` / `...user...`。
3. 启动时用 `--spring.config.import` 导入密钥文件覆盖留空项，例如：
   ```bash
   java -jar bgssai-short-drama-admin.jar \
     --spring.profiles.active=prod \
     --spring.config.import=optional:file:/opt/bgssai/bgssai-short-drama-admin/application-secrets.properties
   ```

## 安全约束

- 密钥文件权限 600，仅部署账号可读；文件名 `application-secrets.properties` 已被根 `.gitignore` 排除。
- `jwt.secret` 未注入时 `JwtUtil` 启动 fail-fast（预期安全行为，杜绝弱密钥带病运行）。
- 泄漏或轮换：轮换 `jwt.secret` 会使存量 token 失效（用户需重新登录），属预期。
- 仓库自带的 `application-dev.properties` 仅含**本地占位值**，切勿用于任何联网环境。
