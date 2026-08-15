# 支付回调验签

对齐 VPN `WechatNotifyController` / `AlipayNotifyController` 与 Standards 支付回调护栏：验签步骤不得删除、不得在业务成功后才补验。

## 威胁

- 伪造异步通知将订单置为 `paid` 并虚增额度。
- 篡改通知金额小于目录价，以低价买高额套餐。
- 重放已成功通知导致重复发放。

## 控制

1. 回调接口不加 `@NeedAop`，但必须走渠道 `verifyAndParseNotify`。
2. 微信生产：APIv3 签名头 + 资源解密；无匹配商户视为验签失败。
3. 支付宝生产：RSA2 `rsaCheckV1`，按 `app_id` 精确路由 `alipay_config`。
4. 沙箱：HMAC-SHA256，密钥来自 `bgssai.payment.*.sign-key`，**不是**商户 APIv3 / 应用私钥。
5. 验签通过后仍用库内 `payment_order.amount_cent` 与通知金额（分）比较，不一致抛 `60005`，不改状态。
6. `status=paid` 直接确认，不再发放（幂等）。
7. 日志只记 `order_no` / 渠道 / 商户号，不记密钥、私钥、APIv3 key。
8. 用户端任何接口不返回商户凭据。

## 应答契约

- 微信成功：HTTP 200 + `{"code":"SUCCESS","message":"OK"}`；验签失败 HTTP 401 + FAIL。
- 支付宝：明文 `success` / `failure`（失败也 HTTP 200，由 body 驱动重试）。
