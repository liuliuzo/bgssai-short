# 契约：健康检查（health-check）

字段一律 snake_case；统一响应封装 `{ code, message, success, result }`。

需求基线见 [`../feature/health-check.md`](../feature/health-check.md)，详细设计见
[`../architecture/health-check-design.md`](../architecture/health-check-design.md)，
全产品线强制口径见 [`../BGSSAI-Standards.md`](../BGSSAI-Standards.md) §13。

> **本契约是全产品线共用的**：9 个产品仓 18 个后端的健康端点路径、字段集、枚举值与本文逐字一致，
> 差异只允许出现在「组件清单」一节（各产品依赖不同）。改本文等于改 9 个仓，须按 Standards §13.8
> 先改 `bgssai-short-drama` 的 Standards §13，再各仓同步。

## 0. 通用约定

- **两端同构**：管理端 `bgssai-short-drama-admin` 与用户端 `bgssai-short-drama-user` 各自暴露下列 3 个端点，
  **路径、请求、响应结构逐字一致**，无 `/admin` · `/user` 前缀之分——探针不区分业务角色。
- **公开端点**：三个端点均**不需要** `Jwttoken` 请求头，也不做角色校验（HC-07）。调用方是部署脚本、
  进程守护与集中巡检平台，无法持有业务令牌。对应地，三个 Controller 都不带 `@NeedAop`。
- **`code` / `success` 恒定**：`code` 恒为 `"0"`、`success` 恒为 `true`，表达「接口调用成功」。
  **健康结论只看 `result.status` 与 HTTP 状态码**，调用方不得用 `success` 判健康。
- **HTTP 状态码**：`result.status` 为 `DOWN` 时返回 **503**；`UP` / `DEGRADED` 返回 **200**。
  存活探针恒定返回 200。
- **无请求参数**：三个端点均为 `GET`，不接受任何查询参数或请求体。
- **只读**：健康检查不写库、不改任何业务状态、不发起任何外部网络调用。
- **缓存**：组件检查结果按 2000ms TTL 缓存（`bgssai.health.cache-ttl-ms`），因此同一秒内的多次调用可能
  返回同一份 `checked_at` 与同一批组件读数。存活探针不走缓存。就绪批次与全量批次**各自独立缓存**。
- **单检查硬超时**：每个组件的检查有 2000ms 硬超时（`bgssai.health.probe-timeout-ms`），超时的组件记为
  `status=DOWN`、`message="check timed out"`，端点仍正常返回，其余组件照常检查。
- **响应耗时**：依赖健康时三个端点均为毫秒级。数据库不可达时，就绪约 4 秒（2 个 critical 组件各烧一次
  超时）、全量约 6 秒（3 个触达数据库或连接池的组件），存活始终瞬时返回。

### 状态枚举 `status`

| 值 | 含义 |
| --- | --- |
| `UP` | 正常 |
| `DEGRADED` | 可用但劣化，需关注（不阻断部署） |
| `DOWN` | 不可用 |

**只有这三个值。** 历史上出现过的 `WARN` / `DISABLED` 一律禁止（Standards §13.2）。

### 组件类型枚举 `type`

| 值 | 含义 |
| --- | --- |
| `DATABASE` | 数据库与持久层 |
| `MIDDLEWARE` | 中间件（连接池 / 线程池 / 调度 / 缓存 / 消息队列） |
| `RUNTIME` | 运行时资源（内存 / 磁盘） |
| `EXTERNAL` | 业务外部依赖的本地就绪度 |

## 1. 存活探针

`GET /bgssai/health/liveness`

只证明进程与 HTTP 端口可服务，**不访问数据库或任何依赖**。HTTP 状态码恒为 200。

### 成功响应

```json
{
  "code": "0",
  "message": null,
  "success": true,
  "result": {
    "status": "UP",
    "app": "bgssai-short-drama-admin",
    "profile": "dev",
    "uptime_seconds": 3675,
    "checked_at": "2026-07-31 10:24:05"
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `status` | string | 恒为 `UP` |
| `app` | string | 应用名（`spring.application.name`） |
| `profile` | string | 当前生效的启动档；多档时以逗号连接，未设置时为 `default` |
| `uptime_seconds` | long | JVM 运行时长（**秒**，不是毫秒） |
| `checked_at` | string | 检查时刻，`yyyy-MM-dd HH:mm:ss` |

## 2. 就绪探针

`GET /bgssai/health/readiness`

只检查 **critical 组件**（`db`、`mybatis`），也只由它们决定结论。整体 `DOWN` 时返回 HTTP 503，
其余返回 200。响应只含组件名与状态，**不含任何指标明细**——探针不需要，公开端点上更小的响应体也是
更小的暴露面。

非 critical 组件既不参与判定，**也不在本端点被检查**——这样数据库不可达时就绪仍能在部署脚本的
`curl --max-time 5` 窗口内给出 503，而不是拖成一次探测超时。

### 成功响应（HTTP 200）

```json
{
  "code": "0",
  "message": null,
  "success": true,
  "result": {
    "status": "UP",
    "app": "bgssai-short-drama-admin",
    "checked_at": "2026-07-31 10:24:05",
    "components": [
      { "name": "db", "status": "UP" },
      { "name": "mybatis", "status": "UP" }
    ]
  }
}
```

### 不可用响应（HTTP 503）

```json
{
  "code": "0",
  "message": null,
  "success": true,
  "result": {
    "status": "DOWN",
    "app": "bgssai-short-drama-admin",
    "checked_at": "2026-07-31 10:24:05",
    "components": [
      { "name": "db", "status": "DOWN" },
      { "name": "mybatis", "status": "DOWN" }
    ]
  }
}
```

部署脚本 `deploy/remote-deploy.sh` 的探测判定是「2xx/3xx/401/403/404 视为健康，5xx 视为不健康」，
因此就绪 `DOWN` 的 503 会直接命中「不健康 → 自动回滚上一个可用 jar」分支。

## 3. 全量健康报告

`GET /bgssai/health`

返回全部组件的状态、类型、是否关键、检查耗时、说明与关键指标。整体 `DOWN` 时返回 HTTP 503，其余 200。

### 成功响应

```json
{
  "code": "0",
  "message": null,
  "success": true,
  "result": {
    "status": "DEGRADED",
    "app": "bgssai-short-drama-admin",
    "profile": "dev",
    "uptime_seconds": 3675,
    "checked_at": "2026-07-31 10:24:05",
    "component_total": 5,
    "up_count": 4,
    "degraded_count": 1,
    "down_count": 0,
    "components": [
      {
        "name": "db",
        "type": "DATABASE",
        "critical": true,
        "status": "UP",
        "latency_ms": 3,
        "message": "connection validated",
        "metrics": { "validation_timeout_seconds": "2" }
      },
      {
        "name": "mybatis",
        "type": "DATABASE",
        "critical": true,
        "status": "UP",
        "latency_ms": 5,
        "message": "example count executed",
        "metrics": { "sys_user_row_count": "12" }
      },
      {
        "name": "datasource_pool",
        "type": "MIDDLEWARE",
        "critical": false,
        "status": "UP",
        "latency_ms": 0,
        "message": "pool healthy",
        "metrics": {
          "active_count": "1",
          "pooling_count": "7",
          "max_active": "20",
          "wait_thread_count": "0",
          "active_used_percent": "5",
          "active_used_threshold_percent": "90"
        }
      },
      {
        "name": "disk_space",
        "type": "RUNTIME",
        "critical": false,
        "status": "DEGRADED",
        "latency_ms": 1,
        "message": "free disk space below threshold",
        "metrics": { "free_mb": "310", "total_mb": "40960", "free_threshold_mb": "512" }
      },
      {
        "name": "jvm_memory",
        "type": "RUNTIME",
        "critical": false,
        "status": "UP",
        "latency_ms": 0,
        "message": "heap usage normal",
        "metrics": {
          "heap_used_mb": "268",
          "heap_max_mb": "2048",
          "heap_used_percent": "13",
          "heap_used_threshold_percent": "90",
          "thread_count": "42"
        }
      }
    ]
  }
}
```

### 顶层字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `status` | string | 整体状态（聚合规则见下） |
| `app` | string | 应用名 |
| `profile` | string | 当前生效启动档 |
| `uptime_seconds` | long | JVM 运行时长（秒） |
| `checked_at` | string | 本批组件检查时刻（缓存命中时为上次检查时刻） |
| `component_total` | int | 组件总数 |
| `up_count` / `degraded_count` / `down_count` | int | 各状态组件计数 |
| `components` | array | 组件明细，按 `type` 声明序（DATABASE → MIDDLEWARE → RUNTIME → EXTERNAL）再按 `name` 字典序 |

### `components[]` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | string | 组件名（snake_case，见第 5 节清单） |
| `type` | string | 组件类型枚举 |
| `critical` | bool | 是否参与就绪判定 |
| `status` | string | 组件状态枚举 |
| `latency_ms` | long | 本次检查耗时毫秒 |
| `message` | string | 人可读的简短说明；异常时**只写异常类型名**（如 `SQLException`），检查超时为 `check timed out`。**不含异常原文、不含堆栈**——JDBC 异常文案常带连接串与主机名 |
| `metrics` | object | 键值均为字符串的非敏感指标；键为 snake_case，内容随组件而定，见第 5 节 |

### 整体状态聚合规则

```text
任一 critical 组件 = DOWN                          -> DOWN
否则 任一组件 = DEGRADED 或 非 critical 组件 = DOWN  -> DEGRADED
否则                                               -> UP
```

## 4. 错误响应

三个端点在正常路径下不返回业务错误码。单个组件检查失败或超时被隔离在该组件的 `status=DOWN` +
`message` 中，**不会**让整个端点失败或挂起（HC-10）。仅当发生框架级未捕获异常时，才由全局异常处理器
按通用错误码返回：

```json
{ "code": "999999", "message": "system error", "success": false }
```

## 5. 组件清单与指标（基线）

下表是**每个 BGSSAI 后端都必须具备**的基线组件；各产品仓在此基础上按本产品依赖追加组件，追加的组件
一律 `critical: false`。

| 组件 `name` | `type` | `critical` | 指标键 | 端 |
| --- | --- | --- | --- | --- |
| `db` | `DATABASE` | true | `validation_timeout_seconds` | 两端 |
| `mybatis` | `DATABASE` | true | `sys_user_row_count`（产品仓换成本产品核心表的计数键） | 两端 |
| `datasource_pool` | `MIDDLEWARE` | false | `active_count` `pooling_count` `max_active` `wait_thread_count` `active_used_percent` `active_used_threshold_percent` | 两端 |
| `jvm_memory` | `RUNTIME` | false | `heap_used_mb` `heap_max_mb` `heap_used_percent` `heap_used_threshold_percent` `thread_count` | 两端 |
| `disk_space` | `RUNTIME` | false | `free_mb` `total_mb` `free_threshold_mb` | 两端 |

## 6. 响应字段安全边界

响应体**只允许**出现上表列出的字段与指标键。以下内容**一律不出现**（HC-08，与 Standards §6.1.6
「白名单显式字段」同一口径）：数据库连接串 / 主机名 / IP / 端口 / 数据库账号与口令 / 数据库产品与版本 /
JWT 与各类签名密钥 / 第三方商户凭证 / 业务账号口令 / 绝对文件路径 / 异常堆栈。

## 7. 调用示例

```bash
# 存活（恒 200）
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/bgssai/health/liveness

# 就绪（数据库不可达时 503）
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/bgssai/health/readiness

# 全量报告
curl -s http://127.0.0.1:8080/bgssai/health
```
