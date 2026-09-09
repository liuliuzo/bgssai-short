package com.bgssai.shortai.payment.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.merchant.AlipayMerchant;
import com.bgssai.shortai.payment.util.AmountCent;
import com.bgssai.shortai.payment.util.PemSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;

@Component
public class AlipayGateway {

    private static final Logger log = LoggerFactory.getLogger(AlipayGateway.class);
    private static final String FORMAT = "json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> fingerprints = new ConcurrentHashMap<>();
    private final Map<String, AlipayClient> clients = new ConcurrentHashMap<>();

    public String precreate(AlipayMerchant merchant, String outTradeNo, String totalAmountYuan, String subject) {
        AlipayClient client = requireClient(merchant);
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(merchant.getNotifyUrl());
        try {
            ObjectNode biz = objectMapper.createObjectNode();
            biz.put("out_trade_no", outTradeNo);
            biz.put("total_amount", totalAmountYuan);
            biz.put("subject", subject);
            request.setBizContent(objectMapper.writeValueAsString(biz));
            log.info("alipay precreate request appId={} outTradeNo={}", merchant.getAppId(), outTradeNo);
            AlipayTradePrecreateResponse response = client.execute(request);
            if (response == null || !response.isSuccess()
                    || response.getQrCode() == null || response.getQrCode().isBlank()) {
                throw new BizException("60013", "alipay trade.precreate returned empty qr_code");
            }
            return response.getQrCode();
        } catch (AlipayApiException e) {
            log.warn("alipay precreate failed appId={} outTradeNo={} err={}",
                    merchant.getAppId(), outTradeNo, e.getErrMsg());
            throw new BizException("60013", "alipay trade.precreate failed: " + safeMsg(e), e);
        } catch (Exception e) {
            throw new BizException("60013", "alipay trade.precreate failed: " + e.getMessage(), e);
        }
    }

    public boolean verifyNotify(AlipayMerchant merchant, Map<String, String> params) {
        String publicKey = PemSupport.toAlipayKey(merchant.getAlipayPublicKey());
        if (publicKey.isEmpty()) {
            log.warn("alipay notify verify skipped appId={}: public key missing", merchant.getAppId());
            return false;
        }
        try {
            return AlipaySignature.rsaCheckV1(params, publicKey,
                    merchant.getCharset(), merchant.getSignType());
        } catch (AlipayApiException e) {
            log.warn("alipay notify rsaCheckV1 error appId={}: {}", merchant.getAppId(), e.getErrMsg());
            return false;
        }
    }


    public ChannelPaidQuery queryPaidOrder(AlipayMerchant merchant, String outTradeNo) {
        AlipayClient client = requireClient(merchant);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        try {
            ObjectNode biz = objectMapper.createObjectNode();
            biz.put("out_trade_no", outTradeNo);
            request.setBizContent(objectMapper.writeValueAsString(biz));
            AlipayTradeQueryResponse response = client.execute(request);
            if (response != null && response.isSuccess()
                    && ("TRADE_SUCCESS".equals(response.getTradeStatus())
                    || "TRADE_FINISHED".equals(response.getTradeStatus()))) {
                Integer amountCent = AmountCent.yuanToCent(response.getTotalAmount());
                return ChannelPaidQuery.of(response.getTradeNo(), amountCent);
            }
            return null;
        } catch (Exception e) {
            log.warn("alipay trade.query failed appId={} outTradeNo={} err={}",
                    merchant.getAppId(), outTradeNo, e.getClass().getSimpleName());
            return null;
        }
    }

    public String queryPaidTradeNo(AlipayMerchant merchant, String outTradeNo) {
        ChannelPaidQuery paid = queryPaidOrder(merchant, outTradeNo);
        return paid == null ? null : paid.getChannelTradeNo();
    }

    public void invalidate() {
        fingerprints.clear();
        clients.clear();
        log.info("alipay client cache invalidated");
    }

    private AlipayClient requireClient(AlipayMerchant merchant) {
        if (!merchant.hasCompleteCredentials()) {
            log.warn("alipay merchant credentials incomplete appId={} missing={}",
                    merchant.getAppId(), merchant.missingFields());
            throw new BizException("60013", "production payment not configured: alipay merchant missing " + merchant.missingFields());
        }
        String key = merchant.getAppId();
        String fingerprint = fingerprint(merchant);
        AlipayClient cached = clients.get(key);
        if (cached != null && fingerprint.equals(fingerprints.get(key))) {
            return cached;
        }
        synchronized (this) {
            cached = clients.get(key);
            if (cached != null && fingerprint.equals(fingerprints.get(key))) {
                return cached;
            }
            String privateKey = PemSupport.toAlipayKey(merchant.getAppPrivateKey());
            String alipayPublicKey = PemSupport.toAlipayKey(merchant.getAlipayPublicKey());
            if (privateKey.isEmpty() || alipayPublicKey.isEmpty()) {
                throw new BizException("60013", "production payment not configured: alipay merchant key material unreadable");
            }
            log.info("alipay client building appId={}", merchant.getAppId());
            AlipayClient built = new DefaultAlipayClient(
                    merchant.getGatewayUrl(), merchant.getAppId(), privateKey, FORMAT,
                    merchant.getCharset(), alipayPublicKey, merchant.getSignType());
            clients.put(key, built);
            fingerprints.put(key, fingerprint);
            return built;
        }
    }

    private static String fingerprint(AlipayMerchant merchant) {
        return String.valueOf(merchant.getAppId()) + '|'
                + String.valueOf(merchant.getGatewayUrl()) + '|'
                + String.valueOf(merchant.getCharset()) + '|'
                + String.valueOf(merchant.getSignType()) + '|'
                + String.valueOf(merchant.getAppPrivateKey()) + '|'
                + String.valueOf(merchant.getAlipayPublicKey());
    }

    private static String safeMsg(AlipayApiException e) {
        if (e.getErrMsg() != null && !e.getErrMsg().isBlank()) {
            return e.getErrMsg();
        }
        return e.getMessage() == null ? "unknown" : e.getMessage();
    }
}
