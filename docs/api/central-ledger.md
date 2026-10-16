# 本库账本镜像与权益服务（CentralLedgerService）

本产品账户与额度体系采用「本库 `ledger_*` 自治镜像表 + 独立事务记账」架构，已完全解耦跨库 `bgssai_website.*` SQL 引用：

- **官网授权给本产品使用**：用户用 BGSSAI 账号绑定本产品后，`ledger_user_subscription` 中 PRO 及以上未过期套餐在本产品视为已开通，本产品消耗的用量按双窗口 + 加量包口径扣减本库 `ledger_user_quota` 并记录 `ledger_usage_ledger`；
- **本产品自己卖订阅**：用户不绑官网账号也能在本产品付费，每一笔订阅写入本库 `ledger_external_subscription`，用户绑定 BGSSAI 账号后自动回填归属。

以上读写全部在本产品自己的 `bgssai_short` 数据库 `ledger_*` 表内完成（零跨库 `bgssai_website.*` 引用），不直连外部 schema，也不在业务热路径上同步调用外部 HTTP 接口。

## 代码落点

| 类 | 职责 |
| --- | --- |
| `com.bgssai.shortai.centralledger.service.CentralLedgerService` | 账本读写：`isEntitled` / `consume` / `reportOwnSubscription` / `linkWebsiteUser` |
| `com.bgssai.shortai.centralledger.dto.OwnSubscriptionReport` | 一笔自有订阅的上报载体 |
| `com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient` | 本产品账号到绑定 `open_id` 的解析，再委托给 `CentralLedgerService`；对外方法名保持 `isEntitled` / `reportDebit` 不变 |
| 持久层 `Website*` 三件套（Entity / Example / Mapper / XML） | 只读写本库 `ledger_user`、`ledger_subscription_plan`、`ledger_user_subscription`、`ledger_user_quota`、`ledger_usage_ledger`、`ledger_usage_idempotency`、`ledger_external_subscription`，仅用 Example 白名单方法 |

产品编码 `bgssai-short` 同时作为账本的 `product_code` 与 `client_id`。

## 什么时候写账本

| 时机 | 调用 | 写入 |
| --- | --- | --- |
| 套餐订单支付成功（`PaidOrderGrantService.grantIfCreated` 的 subscription 分支） | `reportOwnSubscription` | `ledger_external_subscription` 一行：`external_order_no` = `payment_order.order_no`，金额取 `amount_cent`，周期取发放后的 `user_subscription.current_period_start / end` |
| 用户完成 BGSSAI 账号绑定 | `linkWebsiteUser` | 把本产品此前上报、`user_id` 仍为空的订阅一次回填归属 |
| 本地额度扣减成功后 | `reportDebit` -> `consume` | 本库 `ledger_user_quota` 扣点 + `ledger_usage_ledger` 一条流水；未绑定账号则跳过 |
| 查询「我的订阅」 | `isEntitled` | 只读本库 `ledger_user_subscription`，付费未过期即视为本产品已开通 |

## 失败语义

- `consume`、`reportOwnSubscription`、`linkWebsiteUser` 均使用 `REQUIRES_NEW`；每次账本写入内部独立提交或回滚，其中用量扣减、流水和幂等键不会只成功一部分；
- 调用方捕获账本异常并记 WARN，**不回滚**本产品自己的订单、订阅与额度；
- `consume` 是尽力入账：重复幂等键视为成功重放且不会重复扣点；点数不足、钱包缺失、并发重试耗尽则不入账，也不回滚本地扣减。

## 运维前提

所有 `ledger_*` 表均在 `sql/DDL.sql` 中随本产品库一起建表，`ledger_subscription_plan` 默认套餐（`FREE` / `PRO` / `MAX`）在 `sql/DML.sql`、`sql/dev/DML.sql`、`sql/prod/DML.sql` 中幂等初始化，无需跨库数据库权限。

