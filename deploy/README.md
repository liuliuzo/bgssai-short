# 部署说明（deploy）

两个后端均为 Java 21 + Spring Boot 4，以可执行 jar 运行（`java -jar`），不使用 Docker。前端为静态产物，
由对应后端作为 SPA 提供。管理端与用户端**分机独立部署**，共享同一 MySQL 库 `bgssai_short`。

两端均监听 HTTP 8080。
**冲突：** 开发管理端与 Jenkins 控制器共机（`101.44.189.190`），Jenkins 已占用 8080。在迁走 short-admin 或改 Jenkins 监听端口之前，不要把 short-admin 部署到这台机。
keystore 文件仍在 classpath，但 `server.ssl.enabled=false`，应用不再自己终止 TLS。

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
| `dev` | `101.44.189.190` | **8080 HTTP（与 Jenkins 冲突，部署前须迁机）** | `101.44.26.229` | 8080 HTTP | admin 与 Jenkins 共机；user 与应用网关-境外 / 注册中心-境外共机 |
| `prod` | `121.37.158.8` | 8080 HTTP | `121.37.142.152` | 8080 HTTP | admin 与 REDIS-境内共机；user 与 EMQX-MQTT-境内共机 |

域名：`dev.admin.bgssai-short.com` / `dev.user.bgssai-short.com` / `admin.bgssai-short.com` / `www.bgssai-short.com`。
生产落点以这组域名角色为准（用户端 www、管理端 admin）。

健康检查：`http://127.0.0.1:8080/bgssai/health/readiness`。部署与关停只作用于对应 systemd 服务，
**不得**对共机执行整机级关停或重装。

开发中间件：MySQL `122.8.178.143` / Redis `122.8.185.32` / ES `101.44.26.151`。生产库境内 `172.31.4.181`（公网 `121.36.230.185`）。

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

四环境用 `spring.profiles.active` / `SPRING_PROFILES_ACTIVE` 选择：`local` / `dev` / `test` / `prod`。
环境差异分 profile 硬编码真实字面量，**禁用 `${}` 占位符**（Standards §1）。

- `dev` / `prod`：连接串、`jwt.secret`、监听端口均写死在 `application-<profile>.properties`，随 jar 打包。两端 HTTP 8080。
- `test`：本地 / CI 集成测试用，走 8080 明文 HTTP。

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

## OBS dev/prod 环境与运行时凭据（2026-10-26）

本节是本仓 dev/prod OBS 配置的现行约定，覆盖旧文档中把该两环境 OBS AK/SK 随仓库分发的表述。dev 与 prod 均部署 `develop`，只切换对应环境 profile。

| 环境 / 端 | 应用配置 | 桶 |
| --- | --- | --- |
| `dev` / `user` | `bgssai-short-user/bgssai-short-user/src/main/resources/application-dev.properties` | `dev-bgssai-short` |
| `prod` / `user` | `bgssai-short-user/bgssai-short-user/src/main/resources/application-prod.properties` | `prod-bgssai-short` |

区域为 `cn-east-3`，OBS 服务 endpoint 为 `https://obs.cn-east-3.myhuaweicloud.com`。应用配置沿用实际客户端读取的原属性名；不需要区域属性的客户端由 endpoint 确定区域。

Git 中的 OBS AK/SK 字段有意留空，不能用主账号密钥回填。部署前通过 Jenkins 将本产品的环境专用访问密钥写入目标服务已有的 `/etc/bgssai/<SERVICE_NAME>.env`，权限保持 `600`；不要将实际凭据写入提交、PR、日志或前端产物。systemd 的 `EnvironmentFile` 已由现行部署脚本加载，环境变量优先于应用 profile；不恢复 `application-secrets.properties` 导入。

dev/prod 分别使用不同的 AK/SK，但同一产品两把密钥的 IAM 权限均覆盖该产品的 dev/prod 两个桶；不授权其他产品桶，也不构成环境级 IAM 隔离。运维 profile 与凭据元数据校验用于防止配置串用，不能据此声称另一环境桶在 IAM 层拒绝访问。

运行时必须覆盖的精确 Spring 属性：

| Access Key 属性 | Secret Key 属性 |
| --- | --- |
| `bgssai.obs.access-key` | `bgssai.obs.secret-key` |

可在服务环境文件中使用 `SPRING_APPLICATION_JSON` 提供上述精确属性名；如文件已有该项，必须合并 JSON 内的既有配置，不能整项覆盖掉其他设置。profile 本身不增加环境变量占位符。服务名按 Jenkins 主机清单的 `<PRODUCT>_<END>_SERVICE` 覆盖项解析，未覆盖时采用 `bgssai-short-<end>`。只为表中实际使用或已有存储配置的端提供凭据；没有存储消费者的端不因同仓而新增上传功能。

未完成运行时凭据配送前，不应发布依赖 OBS 的服务；仓库配置存在不代表环境已可用。保留既有上传实现、对象键、访问域名、存储模式和本地回退行为；此次不迁移或删除已有文件，不修改业务上传接口。

### 运维校验配置

`deploy/obs/application-dev.properties` 与 `deploy/obs/application-prod.properties` 是无凭据的运维 profile，由中央仓 `bgssai-workflows/deploy/obs/verify.py` 读取，指定校验的产品、环境、桶、区域和业务接入状态。它们不由 Spring Boot 自动加载，不替代上表应用配置，也不承载 AK/SK。应用 profile 负责业务客户端绑定，运维 profile 负责统一验证目标。

通过 Jenkins 在对应环境使用专用凭据校验临时对象写入、读回与清理，并记录桶名、环境及成功/失败，不记录凭据。应用上线后再验证已有上传/读取入口；运维校验成功不能替代业务验收。没有本次实际执行的环境应记录为“未验证”，不能因为两个配置文件已提交而记为上线完成。
