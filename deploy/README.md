# 部署说明（deploy）

两个后端均为 Java 21 + Spring Boot 4，以可执行 jar 运行（`java -jar`），不使用 Docker。前端为静态产物，
由对应后端作为 SPA 提供。管理端与用户端**分机独立部署**，共享同一 MySQL 库 `bgssai_short`。

两端走 HTTPS 443 + HTTP 80 跳转（SslConfig），与其它产品一致。两端与中间件 / Jenkins 共机：
Jenkins 占 8080、Redis 占 6379、EMQX 占 1883/8883 等，均不占 80/443。
证书为 classpath 内置 JKS，文件名（不含 `.jks`）= 应用域名。

`deploy/scripts/ship.sh` 与 `deploy/remote-deploy.sh` 须与中央仓 `bgssai-workflows` **逐字节一致**。

## 开发 / 生产的部署方式：「目标机自建」

dev 与 prod 共用同一条通道（选择规则收在共享库 `bgssaiDeployEnd`）：

| | 目标机自建（dev / prod） |
| --- | --- |
| 源码 | **目标机自己** `git fetch` 本仓 `develop` |
| 构建 | **目标机上**只建本端（本端前端 + 本端 Maven 模块） |
| jar 怎么到位 | 就地产出，落 `app.jar.incoming` |
| 之后的步骤 | `deploy/remote-deploy.sh` 原子替换 → 重启 → 健康检查 → 失败回滚 |

完整说明见中央仓
[`jenkins/README.md`](https://github.com/liuliuzo/bgssai-workflows/blob/develop/jenkins/README.md)。

## 部署通道（先读）

| 通道 | 状态 | 入口 |
| --- | --- | --- |
| **Jenkins** | 主用 | Jenkins -> `bgssai` 文件夹 -> `bgssai-short deploy(dev)` / `bgssai-short deploy(prod)`（关停为 `bgssai-short stop(dev)` / `bgssai-short stop(prod)`）-> Build with Parameters |

**环境由 Job 名决定，不是下拉框**：名字末尾是 `(dev)` 就是开发环境，是 `(prod)` 就是生产
环境。所有任务点 Build 即执行，无需填写任何内容（可选 `target` 参数，不改就是两端都做）。

Jenkins 侧实现：本仓 `Jenkinsfile`（部署）与 `jenkins/Jenkinsfile.stop`（关停）。构建与 ship
逻辑收敛在中央仓 `bgssai-workflows` 的 Jenkins 共享库。
**搭建、凭据、Job 清单与排障见 [`bgssai-workflows/jenkins/README.md`](https://github.com/liuliuzo/bgssai-workflows/blob/develop/jenkins/README.md)。**

| 配置项 | Jenkins 落点 |
| --- | --- |
| SSH 用户名 / 密码 | 凭据 `bgssai-<env>-ssh`（Username with password） |
| 目标主机 | 凭据 `bgssai-<env>-hosts`（Secret file）里的 `SHORT_USER_HOST` / `SHORT_ADMIN_HOST` |
| 端口 / 协议 / 探针 | 同一文件里的 `SHORT_<端>_PORT` / `_HEALTH_SCHEME` / `_HEALTH_PATH` |

部署失败时：不重跑流水线、不推空提交促成重部署；定位原因后由**人工手动**重新触发。
健康检查失败时 `remote-deploy.sh` 已自动回滚到上一个可用 jar，服务应仍在跑旧版本。

首次部署前请先跑 `bgssai-short init database(dev|prod)`：就绪探针会探数据库，库不存在时发布会回滚。

## 一、Jenkins 部署

| 环境 | 部署 | 关停 |
| --- | --- | --- |
| `dev` | `bgssai-short deploy(dev)` | `bgssai-short stop(dev)` |
| `prod` | `bgssai-short deploy(prod)` | `bgssai-short stop(prod)` |

### 目标主机与端口

| Environment | `SHORT_ADMIN_HOST` | admin 端口 | `SHORT_USER_HOST` | user 端口 | 共机 |
| --- | --- | --- | --- | --- | --- |
| `dev` | `101.44.189.190` | **443** HTTPS | `101.44.26.229` | 443 HTTPS | admin 与 Jenkins 共机（Jenkins 占 8080，不占 443）；user 与应用网关-境外 / 注册中心-境外共机 |
| `prod` | `121.37.142.152` | 443 HTTPS | `121.37.158.8` | 443 HTTPS | admin 与 EMQX-MQTT-境内共机；user 与 REDIS-境内共机 |

域名：`dev.admin.bgssai-short.com` / `dev.user.bgssai-short.com` / `admin.bgssai-short.com` / `www.bgssai-short.com`。
生产落点以这组域名角色为准（用户端 www、管理端 admin）。

健康检查：`https://127.0.0.1:443/bgssai/health/readiness`。部署与关停只作用于对应 systemd 服务，
**不得**对共机执行整机级关停或重装。

共享库：dev `192.168.0.217`（公网 `101.44.27.60`）/ prod 境内 `172.31.4.181`（公网 `121.36.230.185`）。

目标机需要构建工具链。一次性准备：在目标机上以 root 跑一次中央仓的
[`jenkins/install/provision-build-host.sh`](https://github.com/liuliuzo/bgssai-workflows/blob/develop/jenkins/install/provision-build-host.sh)。
与 Jenkins 共机的开发管理端同样要装，但不得动 Jenkins 自身进程与 8080 端口。

## 二、构建（本地）

```bash
# 前端（各自构建并同步产物到对应后端 static/）
cd bgssai-short-admin/bgssai-short-admin-react && npm ci && npm run build:deploy
cd bgssai-short-user/bgssai-short-user-react   && npm ci && npm run build:deploy

# 后端（根聚合工程一次构建两个模块，产出可执行 jar）
mvn -q -DskipTests clean package
```

前端 `build:deploy` 通过 `scripts/sync-static.mjs` 将 `dist/index.html` + `dist/assets/` 同步到
`../<对应后端>/src/main/resources/static/`。

## 三、环境与 profile

三环境用 `spring.profiles.active` / `SPRING_PROFILES_ACTIVE` 选择：`dev` / `test` / `prod`。
环境差异分 profile 硬编码真实字面量，**禁用 `${}` 占位符**（Standards §1）。

- `dev` / `prod`：连接串、`jwt.secret`、SSL keystore、监听端口均写死在 `application-<profile>.properties`，随 jar 打包。HTTPS 443 + HTTP 80 跳转。
- `test`：本地 / CI 集成测试用，走 8080/8081 明文。

## 四、服务器上不需要放任何配置文件

本仓**已停用** `application-secrets.properties` 注入机制：三档配置都在各自的
`application-<profile>.properties` 里写死最终字面量、随 jar 打包。
`remote-deploy.sh` 生成的 systemd 单元里不含 `--spring.config.import`。

## 五、启动（手动备查）

```bash
# 管理端
java -jar bgssai-short-admin.jar --spring.profiles.active=prod

# 用户端
java -jar bgssai-short-user.jar --spring.profiles.active=prod
```

配置全部来自 jar 内的 `application-prod.properties`。日志默认落 `/opt/bgssai/log`。
