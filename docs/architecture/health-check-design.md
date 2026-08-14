# 详细设计：健康检查（health-check）

需求基线见 [`../feature/health-check.md`](../feature/health-check.md)，对外契约见
[`../api/health-check.md`](../api/health-check.md)，全产品线强制口径见
[`../BGSSAI-Standards.md`](../BGSSAI-Standards.md) §13。

本文描述本产品健康检查实现的结构与关键取舍。结构自骨架仓 `bgssai-skeleton` 继承，本产品只在
「扩展点」一节说明的位置追加自己的组件，不改端点、状态枚举与字段口径。

## 1. 组件拓扑

```text
com.bgssai.shortai.health
├── HealthStatus                 枚举 UP / DEGRADED / DOWN
├── HealthComponentType          枚举 DATABASE / MIDDLEWARE / RUNTIME / EXTERNAL（声明序即报告序）
├── HealthHttpStatus             DOWN -> 503，其余 -> 200
├── HealthMessages               Throwable -> 异常类型名（永不回显原文与堆栈）
├── HealthContext                应用名 / 启动档 / JVM 运行时长 / 时间格式化
├── dto
│   ├── ComponentHealth          单组件结果（name/type/critical/status/latency_ms/message/metrics）
│   ├── HealthSnapshot           一批组件结果 + 产生时刻（内部类型，不出网）
│   ├── LivenessReport           存活响应 result
│   ├── ReadinessComponent       就绪响应里的单组件（仅 name + status）
│   ├── ReadinessReport          就绪响应 result
│   └── HealthReport             全量响应 result
├── indicator
│   ├── HealthIndicator          扩展点接口：name / type / critical / check
│   ├── DatabaseHealthIndicator          db              DATABASE   critical
│   ├── MybatisHealthIndicator           mybatis         DATABASE   critical
│   ├── ConnectionPoolHealthIndicator    datasource_pool MIDDLEWARE
│   ├── JvmMemoryHealthIndicator         jvm_memory      RUNTIME
│   └── DiskSpaceHealthIndicator         disk_space      RUNTIME
├── service
│   ├── HealthProbeRunner        执行 + 超时隔离 + 计时 + 排序 + TTL 缓存 + 聚合（静态方法）
│   ├── QueryLivenessService     零依赖
│   ├── QueryReadinessService    只跑 critical 批次
│   └── QueryHealthReportService 跑全量批次
└── ui
    ├── QueryLivenessController      GET /bgssai/health/liveness
    ├── QueryReadinessController     GET /bgssai/health/readiness
    └── QueryHealthReportController  GET /bgssai/health

com.bgssai.shortai.configuration.AsyncConfig
└── healthProbeExecutor          健康探针专用线程池（Standards §4：线程池集中声明并命名）
```

三个 Controller 各自一个文件、各含一个 `handle()`，只做「调 Service → 包 ApiResponse → 按状态定 HTTP 码」
（Standards §1 / §3 单接口单 Controller）。业务全部在 Service 与 indicator 里。

## 2. 请求处理链

```text
Client（部署脚本 / systemd / bgssai-healthcheck）
  → RequestTraceFilter（注入 traceId 到 MDC）
  → DispatcherServlet → Query*Controller（无 @NeedAop，公开）
  → Query*Service（打一行 INFO；非 UP 时补一行 WARN）
  → HealthProbeRunner
        ├── TTL 缓存命中 → 直接返回上一批 HealthSnapshot
        └── 未命中 → 逐个 indicator 提交到 healthProbeExecutor，
                     future.get(probeTimeoutMs) 硬超时，异常 / 超时 / 拒绝各自隔离为该组件 DOWN，
                     计时、排序、装入新 HealthSnapshot 并写回缓存
  → 聚合状态 → HealthHttpStatus 定 HTTP 码
  → BgssConfig Jackson2 snake_case 序列化
  → Client
```

存活链路更短：`QueryLivenessService` 只读 `HealthContext`，**完全不进 runner**。

## 3. 关键设计取舍

### 3.1 为什么检查跑在独立线程池上，而不是内联在请求线程上

Druid 的 `maxWait` 默认是「无限等待」。数据库不可达时，裸调 `getConnection()` 永不返回。若检查内联在
请求线程上执行，反复探测会把 Tomcat 线程一条条钉死，直到连存活端点都不再应答——**健康检查自己制造了
它本该报告的故障**。把检查提交到 `healthProbeExecutor` 并 `future.get(timeout)`，才能给每次检查一个
真正的硬截止时间。

`DatabaseHealthIndicator` 另外在借连接处也加了界（Druid 的 `getConnection(maxWaitMillis)` 重载）：
runner 的超时依赖中断探针线程，而这里的界能让等待自己结束，即使连接池哪天不再响应中断也能释放线程。
两道界是冗余的，冗余是故意的。

线程池刻意做得很小（core 2 / max 4 / queue 16）：每个请求逐个提交检查，TTL 缓存又把批次频率压得很低。
允许核心线程超时回收，空闲进程不常驻健康线程；关停时不等待卡住的探针。

### 3.2 为什么用 TTL 缓存而不是限流

健康端点是公开的，天然会被高频调用（多个巡检源 + systemd + 部署脚本）。若每次调用都真去连一次数据库，
公开端点就成了对自己数据库的流量放大器。

限流是更直觉的答案，但它是错的：429 在部署脚本与 systemd 眼里读作「不健康」，于是防御本身触发回滚与
重启——**防御变成了故障**。TTL 缓存没有这个问题：被缓存的调用方拿到的是一份稍旧但真实的结论。

缓存过期瞬间并发的两个请求可能都跑一遍批次。这是接受的：检查只读且幂等，加锁反而让一个慢探针阻塞
所有其他调用方。

### 3.3 为什么就绪批次与全量批次分开缓存

数据库不可达时，每个触达数据库的组件都会烧满单检查超时。全量报告有 3 个这样的组件（`db`、`mybatis`、
`datasource_pool` 的池指标虽不连库但仍在同一批次里排队），最坏约 6 秒；而部署脚本用
`curl --max-time 5` 探测。若就绪复用全量批次，一次失败的发布会被报成「探测超时」——运维还得自己判断
是应用挂了还是探测慢了。

只跑 2 个 critical 组件的独立批次把就绪压在约 4 秒，稳稳落在 5 秒窗口内，于是失败的发布被报成一个
干净的 503，直接命中回滚分支。

### 3.4 为什么 `message` 只写异常类型名

端点是公开的（HC-07），响应体等同于公开内容。JDBC 异常文案通常内嵌 JDBC URL、主机与端口，
直接回显等于把 Standards §6.1.6 明令不得入日志的内容搬上公网。完整信息留在 indicator 自己写的那行
服务端 WARN 日志里（无栈；堆栈仍归全局异常处理器）。

### 3.5 为什么不用 Actuator

- Actuator 的响应结构（`{status, components:{...}}`）与本仓统一响应封装 `{code, message, success, result}`
  是两套东西，混用会让前端与巡检平台面对两种封装；
- 它把「暴露什么端点、给谁看多少明细」交给 `management.*` 配置，跨 9 个仓极易漂移——本方案要的恰恰是
  一份逐字固定、无配置空间的契约；
- 它的健康贡献者与本仓 MyBatis Example 白名单、Druid 指标读取方式并不天然贴合，包一层反而更绕。

代价是要自己写 runner 与 indicator，约 900 行。这些代码在骨架仓写一次，本产品直接继承。

## 4. 扩展点：本产品如何加自己的组件

**唯一动作**：新增一个类实现 `HealthIndicator`，标 `@Component`。Spring 会把它注入 runner 的 List，
端点、缓存、超时隔离、排序、聚合规则、HTTP 映射全部自动生效，别处一行不改（Standards §7 策略隔离）。

```java
@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Override public String name() { return "redis"; }

    @Override public HealthComponentType type() { return HealthComponentType.MIDDLEWARE; }

    // 产品自有依赖一律非 critical：一次第三方抖动不该触发部署回滚
    @Override public boolean critical() { return false; }

    @Override
    public ComponentHealth check() {
        Map<String, String> metrics = new LinkedHashMap<>();
        try {
            // 只读、廉价、有界；不写库、不开事务
            metrics.put("ping_timeout_ms", "1000");
            return ComponentHealth.of(name(), type(), critical(), HealthStatus.UP, "ping ok", metrics);
        } catch (Exception e) {
            log.warn("health check redis failed exception={} message={}",
                    e.getClass().getSimpleName(), e.getMessage());
            // 只写异常类型名，不回显原文
            return ComponentHealth.of(name(), type(), critical(), HealthStatus.DOWN,
                    HealthMessages.of(e), metrics);
        }
    }
}
```

新增组件时必须同步做的三件事：

1. 在 `docs/api/health-check.md` 第 5 节组件清单里加一行（组件名、类型、critical、指标键）；
2. 在 `docs/feature/health-check.md` 第 5 节同步；
3. 阈值配置加在 `bgssai.health.*` 前缀下、写进 `application.properties` 主配置（Standards §13.6）。

**不允许**的扩展方式：改端点路径、改 `result` 字段集、给状态枚举加值、把产品组件标成 critical、
另起一个配置前缀。这些都会破坏跨仓一致性，CR 一律打回（Standards §13.9）。

## 5. 配置

三环境不变量，按 Standards §1 归入 `application.properties` 主配置，不进 profile 文件、不用 `${}` 占位符：

| 键 | 默认值 | 作用 |
| --- | --- | --- |
| `bgssai.health.cache-ttl-ms` | 2000 | 整批组件结果的缓存存活时间 |
| `bgssai.health.probe-timeout-ms` | 2000 | 单个组件检查的硬超时 |
| `bgssai.health.db-validation-timeout-seconds` | 2 | `db` 借连接与校验连接的超时 |
| `bgssai.health.pool-active-threshold-percent` | 90 | `datasource_pool` 判 DEGRADED 的活跃连接占比阈值 |
| `bgssai.health.heap-used-threshold-percent` | 90 | `jvm_memory` 判 DEGRADED 的堆占比阈值 |
| `bgssai.health.disk-free-threshold-mb` | 512 | `disk_space` 判 DEGRADED 的剩余空间阈值 |

## 6. 测试

`src/test/java/com/bgssai/shortai/health/HealthProbeRunnerTest`（两端各一份，8 条）钉死本方案里最容易被
「顺手改一下」的两条规则：

- **聚合规则**：全 UP → UP；critical DOWN → DOWN；非 critical DOWN → 仅 DEGRADED；任一 DEGRADED → DEGRADED；
  就绪判定忽略非 critical 组件；
- **HTTP 映射**：只有 DOWN → 503，`UP` 与 `DEGRADED` 都留在 200。

改动这两条会同时让部署脚本的回滚判定与集中巡检平台的归一化失真，因此用测试而不是注释来守。

## 7. 与部署 / 巡检的对接

- `deploy/remote-deploy.sh` 与 `deploy/scripts/ship.sh` 的 `HEALTH_PATH` 默认值为
  `/bgssai/health/readiness`；探测口径「2xx/3xx/401/403/404 健康，5xx 不健康 → 回滚」。
- 集中巡检平台 `bgssai-healthcheck` 以 `/bgssai/health/readiness` 巡检各应用。平台能从响应体解析
  `status` 并以它为准，所以统一后的三值枚举可被直接归一化，无需逐仓适配。
