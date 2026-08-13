# 需求：健康检查（health-check）

本文件是健康检查功能的**需求基线与单一事实来源**。契约见 [`../api/health-check.md`](../api/health-check.md)，
详细设计见 [`../architecture/health-check-design.md`](../architecture/health-check-design.md)，
全产品线强制口径见 [`../BGSSAI-Standards.md`](../BGSSAI-Standards.md) §13。

> 本功能自骨架仓 `bgssai-skeleton` 继承而来，是**全产品线统一口径**的落地实现：口径权威在
> Standards §13，本产品只做「对齐」不做「发明」。骨架里的实现改了，本仓要跟；本仓自行发明的
> 端点路径 / 状态枚举 / 字段名变体，一律按本文与 Standards §13 打回。

## 1. 背景与要解决的问题

BGSSAI 产品线有 9 个应用仓、18 个可部署后端，另有集中巡检平台 `bgssai-healthcheck` 按接口逐个巡检
它们的健康状态。在统一之前，各仓各自实现了一套健康检查——端点路径 6 种、状态枚举 3 套、时间与运行
时长字段各 2~3 种、配置前缀 4 种。造成的实际损失：

1. 巡检平台无法用一套归一化规则理解各应用的自报状态，要么逐仓写适配，要么只能退化成「HTTP 通不通」；
2. 部署脚本的 `HEALTH_PATH` 必须逐仓记忆，配错一个仓就会把一次成功的发布误判为失败并回滚；
3. 运维在 A 仓积累的排障经验换到 B 仓即刻作废，值班交接成本被无谓放大。

本功能的目标不是「让应用有健康检查」（各仓都已经有了），而是**让 18 个后端的健康检查是同一个东西**。

## 2. 范围

**在范围内**：进程存活判定、依赖就绪判定、组件级健康明细、与部署脚本 / 集中巡检平台的对接口径。

**不在范围内**：

- 指标采集与时序存储（Prometheus / Micrometer 那一层）——健康检查回答「现在能不能用」，不回答「过去一小时怎么样」；
- 告警与通知——归集中巡检平台 `bgssai-healthcheck`，本功能只负责如实自报；
- 业务数据正确性校验——健康检查只读、廉价、有界，不做业务巡检。

## 3. 业务规则

| 编号 | 规则 |
| --- | --- |
| HC-01 | 存活探针只证明进程与 HTTP 端口可服务，**不触碰任何依赖**；HTTP 恒 200 |
| HC-02 | 就绪探针只检查、也只由 **critical 组件**决定结论；整体 `DOWN` 返回 HTTP 503 |
| HC-03 | 全量报告返回每个组件的状态、类型、是否关键、耗时、说明与白名单指标；整体 `DOWN` 返回 HTTP 503 |
| HC-04 | 健康结论由组件聚合而成；组件只读、廉价、有界，不写库、不开事务、不发外部网络调用 |
| HC-05 | 状态三值：`UP` / `DEGRADED` / `DOWN`。`DEGRADED` 表示可用但劣化，**永不阻断部署** |
| HC-06 | 聚合规则：任一 critical 组件 `DOWN` → `DOWN`；否则任一组件 `DEGRADED` 或非 critical 组件 `DOWN` → `DEGRADED`；否则 `UP` |
| HC-07 | 三个端点公开，不需要令牌、不做角色校验——调用方是部署脚本与进程守护，无法持有业务令牌 |
| HC-08 | 响应体只允许出现契约列出的字段与白名单指标；连接串 / 主机 / 端口 / 账号口令 / 路径 / 堆栈一律不得出现 |
| HC-09 | 整批组件结果按 TTL 缓存，使密集探测共享一次组件遍历；**不得**改用限流保护 |
| HC-10 | 单个组件失败、超时或被线程池拒绝，只影响该组件的状态，不得让整个端点失败或挂起 |

### 为什么 `DEGRADED` 必须留在 HTTP 200

部署脚本 `deploy/remote-deploy.sh` 的探测口径是「2xx/3xx/401/403/404 视为健康，5xx 视为不健康 →
回滚上一个可用 jar」。如果磁盘偏紧、连接池偏忙这类劣化也返回 5xx，那么一次本来成功的发布会被回滚，
而回滚并不能腾出磁盘——防御变成了故障。因此只有 `DOWN` 映射 503。

### 为什么只有 `db` 与 `mybatis` 是 critical

critical 的含义是「它不行，这次发布就该回滚」。只有「没有它应用根本无法提供任何业务能力」的依赖
才配得上这个含义，实践中就是数据库连通性（`db`）与持久化链路可用性（`mybatis`）。把 Redis、对象存储、
短信这类可选依赖标成 critical，会让一次第三方抖动触发部署回滚——回滚同样修不好第三方。

## 4. 验收标准

| 编号 | 场景 | 期望 |
| --- | --- | --- |
| AC-01 | 依赖全部正常 | 三个端点均 HTTP 200，`result.status` 均为 `UP` |
| AC-02 | 数据库不可达 | 存活仍 200 且 `UP`；就绪 503 且 `DOWN`，`components` 中 `db` / `mybatis` 为 `DOWN`；全量 503 |
| AC-03 | 数据库不可达时的耗时 | 就绪在部署脚本 `curl --max-time 5` 窗口内返回，不拖成探测超时 |
| AC-04 | 磁盘剩余低于阈值 | 全量报告整体 `DEGRADED` 且 **HTTP 200**；就绪不受影响仍 `UP` + 200 |
| AC-05 | `sys_user` 无有效行 | `mybatis` 组件 `DEGRADED`，整体 `DEGRADED`，HTTP 200 |
| AC-06 | 单个组件抛异常 | 该组件 `DOWN` 且 `message` 只含异常类型名；其余组件照常检查，端点正常返回 |
| AC-07 | TTL 窗口内连续调用 | 返回同一份 `checked_at`，组件不被重复检查 |
| AC-08 | 无令牌调用三个端点 | 均正常返回，不出现 `2002` |
| AC-09 | 响应体审查 | 不含连接串 / 主机 / 端口 / 账号口令 / 绝对路径 / 堆栈 |
| AC-10 | 聚合与状态映射 | `HealthProbeRunnerTest` 全绿（8 条，钉死聚合规则与 HTTP 映射） |

## 5. 当前内置的组件清单

本产品当前只有基础表（`sys_user` / `login_log`），因此只落基线组件。后续接入的依赖（对象存储、
AI 生成服务、消息队列等）按详设「扩展点」逐个追加组件。

| 组件 `name` | `type` | `critical` | 指标键 |
| --- | --- | --- | --- |
| `db` | `DATABASE` | true | `validation_timeout_seconds` |
| `mybatis` | `DATABASE` | true | `sys_user_row_count` |
| `datasource_pool` | `MIDDLEWARE` | false | `active_count` `pooling_count` `max_active` `wait_thread_count` `active_used_percent` `active_used_threshold_percent` |
| `jvm_memory` | `RUNTIME` | false | `heap_used_mb` `heap_max_mb` `heap_used_percent` `heap_used_threshold_percent` `thread_count` |
| `disk_space` | `RUNTIME` | false | `free_mb` `total_mb` `free_threshold_mb` |

管理端 `bgssai-short-drama-admin` 与用户端 `bgssai-short-drama-user` 的组件清单**完全相同**：探针不区分业务
角色，两端同构。

## 6. 未做与理由

| 未做的事 | 理由 |
| --- | --- |
| 用 Spring Boot Actuator | Actuator 的 `/actuator/**` 暴露面、鉴权模型与响应结构自成一套，与本仓统一响应封装不兼容；且它把「暴露什么」交给配置，跨 9 个仓极易漂移。统一方案要的是一份逐字固定的契约（Standards §13.1） |
| 保留旧路径作兼容别名 | 两个路径同时存在就是两份契约，正是本功能要消灭的东西。迁移一次性改名 |
| 健康检查页面原型 | 本功能是非功能需求（可观测性），无用户界面；调用方是脚本与巡检平台。按 Standards §0，非功能需求不强制走原型设计 HTML 一步 |
| 组件并发检查 | 组件数量是个位数且每个都有硬超时，串行总耗时可控；并发会让线程池模型复杂化，收益不抵成本 |
| 缓存加锁去重 | 缓存过期瞬间并发的两个请求可能都跑一遍批次。检查只读且幂等，加锁反而让一个慢探针阻塞所有调用方 |
