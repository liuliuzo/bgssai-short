package com.bgssai.shortai.payment.channel;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.merchant.AlipayMerchant;
import com.bgssai.shortai.payment.merchant.PayMerchantReader;
import com.bgssai.shortai.payment.util.AmountCent;

@Component
public class AlipayPaymentChannel implements PaymentChannel {

    private static final Logger log = LoggerFactory.getLogger(AlipayPaymentChannel.class);

    @Autowired
    private PayMerchantReader payMerchantReader;

    @Autowired
    private AlipayGateway alipayGateway;

    @Override
    public String channelCode() {
        return "ALIPAY";
    }

    @Override
    public PrepayResult createPayment(PaymentIntent intent, String tradeNo) {
        log.info("alipay createPayment orderNo={} tradeNo={} amountCent={}",
                intent.getOrderNo(), tradeNo, intent.getAmountCent());
        AlipayMerchant merchant = payMerchantReader.requireActiveAlipayMerchant();
        if (!merchant.hasCompleteCredentials()) {
            log.warn("alipay production payment not configured appId={} missing={}",
                    merchant.getAppId(), merchant.missingFields());
            throw new BizException("60013", "production payment not configured: alipay merchant missing " + merchant.missingFields());
        }
        String subject = buildSubject(intent);
        try {
            String qr = alipayGateway.precreate(
                    merchant, intent.getOrderNo(), AmountCent.toYuan(intent.getAmountCent()), subject);
            log.info("alipay precreate ok orderNo={} appId={} amountFen={}",
                    intent.getOrderNo(), merchant.getAppId(), intent.getAmountCent());
            return new PrepayResult(qr, "ali_" + tradeNo, merchant.getAppId());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("alipay precreate error orderNo={} appId={} msg={}",
                    intent.getOrderNo(), merchant.getAppId(), e.getMessage());
            throw new BizException("60013", "alipay trade.precreate failed: " + e.getMessage(), e);
        }
    }

    @Override
    public NotifyResult verifyAndParseNotify(NotifyContext context) {
        Map<String, String> params = context.getFormParams().isEmpty()
                ? SignSupport.parseForm(context.getRawBody())
                : context.getFormParams();
        AlipayMerchant merchant = payMerchantReader.findAlipayMerchantByAppId(params.get("app_id"));
        if (merchant == null) {
            log.warn("alipay notify rejected: app_id not matched to an enabled merchant outTradeNo={}",
                    params.get("out_trade_no"));
            return NotifyResult.rejected();
        }
        boolean verified = alipayGateway.verifyNotify(merchant, params);
        if (!verified) {
            log.warn("alipay notify rsa2 verify failed appId={} outTradeNo={}",
                    merchant.getAppId(), params.get("out_trade_no"));
            return NotifyResult.rejected();
        }
        return parseAlipayParams(params);
    }

    private NotifyResult parseAlipayParams(Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String channelTradeNo = params.get("trade_no");
        boolean success = "TRADE_SUCCESS".equalsIgnoreCase(params.get("trade_status"))
                || "TRADE_FINISHED".equalsIgnoreCase(params.get("trade_status"));
        Integer amountCent = AmountCent.yuanToCent(params.get("total_amount"));
        log.info("alipay notify parsed orderNo={} success={}", orderNo, success);
        return new NotifyResult(true, success, orderNo, channelTradeNo, amountCent);
    }


    @Override
    public ChannelPaidQuery queryPaidOrder(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return null;
        }
        try {
            List<AlipayMerchant> candidates = payMerchantReader.listVerifiableAlipayMerchants();
            for (AlipayMerchant merchant : candidates) {
                try {
                    ChannelPaidQuery paid = alipayGateway.queryPaidOrder(merchant, outTradeNo);
                    if (paid != null) {
                        log.info("alipay order query paid outTradeNo={} appId={} amountFen={}",
                                outTradeNo, merchant.getAppId(), paid.getAmountCent());
                        return paid;
                    }
                } catch (Exception e) {
                    log.warn("alipay order query failed outTradeNo={} appId={} error={}",
                            outTradeNo, merchant.getAppId(), e.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            log.warn("alipay order query skipped outTradeNo={} error={}",
                    outTradeNo, e.getClass().getSimpleName());
        }
        return null;
    }

    private static String buildSubject(PaymentIntent intent) {
        String name = intent.getSubject();
        if (name == null || name.isBlank()) {
            name = "订单";
        }
        String subject = name + " " + intent.getOrderNo();
        return subject.length() > 256 ? subject.substring(0, 256) : subject;
    }
}
