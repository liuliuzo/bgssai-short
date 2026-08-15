package com.bgssai.shortai.payment.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.merchant.WechatMerchant;
import com.bgssai.shortai.payment.util.PemSupport;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;

@Component
public class WechatPayGateway {

    private static final Logger log = LoggerFactory.getLogger(WechatPayGateway.class);

    private final Map<String, String> fingerprints = new ConcurrentHashMap<>();
    private final Map<String, Config> configs = new ConcurrentHashMap<>();

    public String nativePrepay(WechatMerchant merchant, String description,
                               String outTradeNo, long amountFen) {
        Config config = requireConfig(merchant);
        NativePayService service = new NativePayService.Builder().config(config).build();
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal((int) amountFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);
        request.setAppid(merchant.getAppId());
        request.setMchid(merchant.getMchId());
        request.setDescription(description);
        request.setNotifyUrl(merchant.getNotifyUrl());
        request.setOutTradeNo(outTradeNo);
        log.info("wechat native prepay request mchId={} outTradeNo={} amountFen={}",
                merchant.getMchId(), outTradeNo, amountFen);
        PrepayResponse response = service.prepay(request);
        if (response == null || response.getCodeUrl() == null || response.getCodeUrl().isBlank()) {
            throw new BizException("60013", "wechat native prepay returned empty code_url");
        }
        return response.getCodeUrl();
    }

    public Transaction parseTransactionNotify(WechatMerchant merchant, NotifyContext ctx) {
        Config config = requireConfig(merchant);
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(ctx.header("Wechatpay-Serial"))
                .nonce(ctx.header("Wechatpay-Nonce"))
                .signature(ctx.header("Wechatpay-Signature"))
                .timestamp(ctx.header("Wechatpay-Timestamp"))
                .signType(ctx.header("Wechatpay-Signature-Type"))
                .body(ctx.getRawBody())
                .build();
        NotificationParser parser = new NotificationParser((RSAAutoCertificateConfig) config);
        try {
            return parser.parse(requestParam, Transaction.class);
        } catch (ValidationException e) {
            log.warn("wechat notify signature validation failed mchId={}: {}",
                    merchant.getMchId(), e.getMessage());
            throw e;
        }
    }

    public void invalidate() {
        fingerprints.clear();
        configs.clear();
        log.info("wechat pay config cache invalidated");
    }

    private Config requireConfig(WechatMerchant merchant) {
        if (!merchant.hasCompleteCredentials()) {
            log.warn("wechat merchant credentials incomplete mchId={} missing={}",
                    merchant.getMchId(), merchant.missingFields());
            throw new BizException("60013", "production payment not configured: wechat merchant missing " + merchant.missingFields());
        }
        String key = merchant.getMchId();
        String fingerprint = fingerprint(merchant);
        Config cached = configs.get(key);
        if (cached != null && fingerprint.equals(fingerprints.get(key))) {
            return cached;
        }
        synchronized (this) {
            cached = configs.get(key);
            if (cached != null && fingerprint.equals(fingerprints.get(key))) {
                return cached;
            }
            String privateKeyPem = PemSupport.toPkcs8Pem(merchant.getMchPrivateKey());
            if (privateKeyPem.isEmpty()) {
                throw new BizException("60013", "production payment not configured: wechat merchant mch_private_key unreadable");
            }
            log.info("wechat pay config building mchId={} serialPresent=true", merchant.getMchId());
            Config built = new RSAAutoCertificateConfig.Builder()
                    .merchantId(merchant.getMchId())
                    .privateKey(privateKeyPem)
                    .merchantSerialNumber(merchant.getMerchantSerialNo())
                    .apiV3Key(merchant.getApiV3Key())
                    .build();
            configs.put(key, built);
            fingerprints.put(key, fingerprint);
            return built;
        }
    }

    private static String fingerprint(WechatMerchant merchant) {
        return String.valueOf(merchant.getMchId()) + '|'
                + String.valueOf(merchant.getMerchantSerialNo()) + '|'
                + String.valueOf(merchant.getApiV3Key()) + '|'
                + String.valueOf(merchant.getMchPrivateKey());
    }
}
