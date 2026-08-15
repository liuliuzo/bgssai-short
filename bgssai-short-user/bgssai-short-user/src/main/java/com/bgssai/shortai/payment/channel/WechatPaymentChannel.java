package com.bgssai.shortai.payment.channel;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.config.PaymentProperties;
import com.bgssai.shortai.payment.merchant.PayMerchantReader;
import com.bgssai.shortai.payment.merchant.WechatMerchant;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.service.payments.model.Transaction;

@Component
public class WechatPaymentChannel implements PaymentChannel {

    private static final Logger log = LoggerFactory.getLogger(WechatPaymentChannel.class);

    @Autowired
    private PaymentProperties paymentProperties;

    @Autowired
    private PayMerchantReader payMerchantReader;

    @Autowired
    private WechatPayGateway wechatPayGateway;

    @Override
    public String channelCode() {
        return "WECHAT";
    }

    @Override
    public PrepayResult createPayment(PaymentIntent intent, String tradeNo) {
        log.info("wechat createPayment orderNo={} tradeNo={} amountCent={}",
                intent.getOrderNo(), tradeNo, intent.getAmountCent());
        if (paymentProperties.isSandboxEnabled()) {
            String codeUrl = "weixin://wxpay/bizpayurl?pr=SANDBOX-" + tradeNo;
            return new PrepayResult(codeUrl, "wx_" + tradeNo, null);
        }
        WechatMerchant merchant = payMerchantReader.requireActiveWechatMerchant();
        if (!merchant.hasCompleteCredentials()) {
            log.warn("wechat production payment not configured mchId={} missing={}",
                    merchant.getMchId(), merchant.missingFields());
            throw new BizException("60013", "production payment not configured: wechat merchant missing " + merchant.missingFields());
        }
        try {
            String codeUrl = wechatPayGateway.nativePrepay(
                    merchant, buildDescription(intent), intent.getOrderNo(), intent.getAmountCent());
            log.info("wechat native prepay ok orderNo={} mchId={} amountFen={}",
                    intent.getOrderNo(), merchant.getMchId(), intent.getAmountCent());
            return new PrepayResult(codeUrl, "wx_" + tradeNo, merchant.getMchId());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("wechat native prepay error orderNo={} mchId={} msg={}",
                    intent.getOrderNo(), merchant.getMchId(), e.getMessage());
            throw new BizException("60013", "wechat native prepay failed: " + e.getMessage(), e);
        }
    }

    @Override
    public NotifyResult verifyAndParseNotify(NotifyContext context) {
        if (paymentProperties.isSandboxEnabled()) {
            return verifySandbox(context);
        }
        return verifyProduction(context);
    }

    private NotifyResult verifySandbox(NotifyContext context) {
        Map<String, String> params = context.getFormParams().isEmpty()
                ? SignSupport.parseForm(context.getRawBody())
                : context.getFormParams();
        boolean verified = SignSupport.verify(params, paymentProperties.getWechatSignKey());
        if (!verified) {
            log.warn("wechat sandbox notify verify failed outTradeNo={}", params.get("out_trade_no"));
            return NotifyResult.rejected();
        }
        String orderNo = params.get("out_trade_no");
        String channelTradeNo = params.get("transaction_id");
        boolean success = "SUCCESS".equalsIgnoreCase(params.get("trade_state"));
        Integer amountCent = null;
        if (params.containsKey("amount")) {
            try {
                amountCent = Integer.valueOf(params.get("amount"));
            } catch (NumberFormatException e) {
                log.warn("wechat sandbox notify amount parse error orderNo={}", orderNo);
            }
        }
        log.info("wechat sandbox notify parsed orderNo={} success={}", orderNo, success);
        return new NotifyResult(true, success, orderNo, channelTradeNo, amountCent);
    }

    private NotifyResult verifyProduction(NotifyContext context) {
        List<WechatMerchant> candidates;
        try {
            candidates = payMerchantReader.listVerifiableWechatMerchants();
        } catch (Exception e) {
            log.warn("wechat notify merchant lookup failed: {}", e.getMessage());
            return NotifyResult.rejected();
        }
        if (candidates.isEmpty()) {
            log.warn("wechat notify rejected: no enabled merchant with complete credentials");
            return NotifyResult.rejected();
        }
        for (WechatMerchant merchant : candidates) {
            try {
                Transaction tx = wechatPayGateway.parseTransactionNotify(merchant, context);
                if (tx == null) {
                    continue;
                }
                String orderNo = tx.getOutTradeNo();
                String channelTradeNo = tx.getTransactionId();
                boolean success = tx.getTradeState() == Transaction.TradeStateEnum.SUCCESS;
                Integer amountCent = null;
                if (tx.getAmount() != null && tx.getAmount().getTotal() != null) {
                    amountCent = tx.getAmount().getTotal();
                }
                log.info("wechat notify parsed mchId={} orderNo={} success={}",
                        merchant.getMchId(), orderNo, success);
                return new NotifyResult(true, success, orderNo, channelTradeNo, amountCent);
            } catch (ValidationException e) {
                log.warn("wechat notify verify failed for mchId={}, trying next candidate: {}",
                        merchant.getMchId(), e.getMessage());
            } catch (Exception e) {
                log.warn("wechat notify parse error mchId={}: {}", merchant.getMchId(), e.getMessage());
            }
        }
        log.warn("wechat notify rejected: no candidate merchant verified the signature candidates={}",
                candidates.size());
        return NotifyResult.rejected();
    }

    private static String buildDescription(PaymentIntent intent) {
        String name = intent.getSubject();
        if (name == null || name.isBlank()) {
            name = "订单";
        }
        String description = name + " " + intent.getOrderNo();
        return description.length() > 127 ? description.substring(0, 127) : description;
    }
}
