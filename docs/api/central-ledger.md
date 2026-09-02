# 中央账本直连（bgssai-website）

本产品与官网 `bgssai-website` 同在一个 MySQL 实例，账户体系口径是「中央账本 + 各产品可自治」：

- **官网授权给本产品使用**：用户用 BGSSAI 账号绑定本产品后，官网 PRO 及以上套餐在本产品视为已开通，
  本产品消耗的用量按官网套餐点数扣在官网的用量钱包上；
- **本产品自己卖订阅**：用户不绑官网账号也能在本产品付费，但每一笔订阅都要让官网的中央账本看得见。

产品线约定**应用之间不互调接口，直接操作数据库**：以上读写全部在本产品自己的 DataSource 上跨库完成
（表名带 `bgssai_website.` 前缀），不再调用官网 `/api/user/oauth/quota`、`/api/user/oauth/usage`。
表契约以官网仓为权威：`docs/api/BILLING.md`（用量与权益）、`docs/api/EXTERNAL-SUBSCRIPTION.md`（自有订阅）。

## 代码落点

| 类 | 职责 |
| --- | --- |
| `com.bgssai.shortai.centralledger.service.CentralLedgerService` | 中央账本读写：`isEntitled` / `consume` / `reportOwnSubscription` / `linkWebsiteUser`，口径与官网 `QuotaWalletService` 对齐 |
| `com.bgssai.shortai.centralledger.dto.OwnSubscriptionReport` | 一笔自有订阅的上报载体 |
| `com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient` | 本产品账号 → 官网 `user_id` 的绑定解析，再委托给 `CentralLedgerService`；对外方法名保持 `isEntitled` / `reportDebit` 不变 |
| 持久层 `Website*` 三件套（Entity / Example / Mapper / XML） | 只读写 `bgssai_website` 库的 `website_user`、`subscription_plan`、`user_subscription`、`user_quota`、`usage_ledger`、`usage_idempotency`、`external_subscription`，仅用 Example 白名单方法 |

产品编码 `bgssai-short` 同时作为中央账本的 `product_code` 与 `client_id`（与官网 `oauth_client` 种子一致）。

## 什么时候写中央账本

| 时机 | 调用 | 写入 |
| --- | --- | --- |
| 套餐订单支付成功（`PaidOrderGrantService.grantIfCreated` 的 subscription 分支） | `reportOwnSubscription` | `external_subscription` 一行：`external_order_no` = `payment_order.order_no`，金额取 `amount_cent`，周期取发放后的 `user_subscription.current_period_start / end` |
| 用户完成 BGSSAI 账号绑定 | `linkWebsiteUser` | 把本产品此前上报、`user_id` 仍为空的订阅一次回填归属 |
| 本地额度扣减成功后 | `reportDebit` → `consume` | 官网 `user_quota` 扣点 + `usage_ledger` 一条流水；未绑定官网账号则跳过 |
| 查询「我的订阅」 | `isEntitled` | 只读官网 `user_subscription`，付费未过期即视为本产品已开通 |

## 失败语义

- `consume`、`reportOwnSubscription`、`linkWebsiteUser` 均使用 `REQUIRES_NEW`；每次中央写入内部独立提交或回滚，
  其中用量扣减、流水和幂等键不会只成功一部分；
- 调用方捕获中央账本异常并记 WARN，**不回滚**本产品自己的订单、订阅与额度；因此本地事务与中央事务
  不是一个分布式原子事务；
- `consume` 是尽力入账：重复幂等键视为成功重放且不会重复扣点；点数不足、钱包缺失、并发重试耗尽
  则不入账，也不回滚本地扣减；
- 若中央提交后本地事务又回滚，或进程在两次提交之间退出，需要按订单号 / 幂等键重放并通过运营对账补偿。

## 运维前提

产品数据库账号需要 `bgssai_website` 库上述七张表的 `SELECT` / `INSERT` / `UPDATE` / `DELETE` 权限
（`DELETE` 仅用于回收未入账的幂等行）。是否需要额外 DCL 由运维按环境核验。
