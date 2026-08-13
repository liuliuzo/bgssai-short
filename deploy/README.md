# 部署说明（deploy）

两个后端均为 Java 21 + Spring Boot 4，以可执行 jar 运行（`java -jar`），不使用 Docker。前端为静态产物，
由对应后端作为 SPA 提供。管理端与用户端独立部署，共享同一 MySQL 库 `bgssai_short_drama`。

## 一、构建

```bash
# 后端（根聚合工程一次构建两个模块，产出可执行 jar）
mvn -q -DskipTests clean package

# 前端（各自构建并同步产物到对应后端 static/）
cd bgssai-short-drama-admin/bgssai-short-drama-admin-react && npm ci && npm run build:deploy
cd bgssai-short-drama-user/bgssai-short-drama-user-react   && npm ci && npm run build:deploy
```

前端 `build:deploy` 通过 `scripts/sync-static.mjs` 将 `dist/index.html` + `dist/assets/` 同步到
`../<对应后端>/src/main/resources/static/`（可用环境变量 `BGSS_STATIC_DIR` 覆盖目标路径），随后端 jar
一起打包发布。

## 二、环境与 profile

三环境用 `spring.profiles.active` / `SPRING_PROFILES_ACTIVE` 选择：`dev`（本地）/ `test` / `prod`。
环境差异分 profile 硬编码真实字面量，**禁用 `${}` 占位符**（Standards §1）。

- `dev`：`application-dev.properties` 自带本地占位值，开箱即用。
- `test` / `prod`：连接串、账号密码、`jwt.secret` 在 profile 文件中**留空**，由部署期密钥文件注入。

## 三、密钥注入（test / prod）

在服务器放置密钥文件（权限 600，绝不入库），从本目录模板复制填写：

- 管理端：`/opt/bgssai/bgssai-short-drama-admin/application-secrets.properties`
  （模板 `application-secrets.admin.properties.example`）
- 用户端：`/opt/bgssai/bgssai-short-drama-user/application-secrets.properties`
  （模板 `application-secrets.user.properties.example`）

详见 `docs/security/secrets-checklist.md`。

## 四、启动

```bash
# 管理端（8080）
java -jar bgssai-short-drama-admin.jar \
  --spring.profiles.active=prod \
  --spring.config.import=optional:file:/opt/bgssai/bgssai-short-drama-admin/application-secrets.properties

# 用户端（8081）
java -jar bgssai-short-drama-user.jar \
  --spring.profiles.active=prod \
  --spring.config.import=optional:file:/opt/bgssai/bgssai-short-drama-user/application-secrets.properties
```

`jwt.secret` 未注入时 `JwtUtil` 启动 fail-fast（预期安全行为）。日志默认落 `/opt/bgssai/log`
（logback file appender 首次写入时自动建目录）。
