package com.bgssai.shortai.payment.merchant;

public class AlipayMerchant {

    private final String appId;
    private final String appPrivateKey;
    private final String alipayPublicKey;
    private final String signType;
    private final String charset;
    private final String gatewayUrl;
    private final String notifyUrl;
    private final String returnUrl;

    public AlipayMerchant(String appId, String appPrivateKey, String alipayPublicKey, String signType,
                          String charset, String gatewayUrl, String notifyUrl, String returnUrl) {
        this.appId = appId;
        this.appPrivateKey = appPrivateKey;
        this.alipayPublicKey = alipayPublicKey;
        this.signType = signType;
        this.charset = charset;
        this.gatewayUrl = gatewayUrl;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
    }

    public String getAppId() { return appId; }
    public String getAppPrivateKey() { return appPrivateKey; }
    public String getAlipayPublicKey() { return alipayPublicKey; }
    public String getSignType() { return signType; }
    public String getCharset() { return charset; }
    public String getGatewayUrl() { return gatewayUrl; }
    public String getNotifyUrl() { return notifyUrl; }
    public String getReturnUrl() { return returnUrl; }

    public boolean hasCompleteCredentials() {
        return isPresent(appId) && isPresent(appPrivateKey) && isPresent(alipayPublicKey)
                && isPresent(gatewayUrl) && isPresent(notifyUrl);
    }

    public String missingFields() {
        StringBuilder missing = new StringBuilder();
        appendIfMissing(missing, "app_id", appId);
        appendIfMissing(missing, "app_private_key", appPrivateKey);
        appendIfMissing(missing, "alipay_public_key", alipayPublicKey);
        appendIfMissing(missing, "gateway_url", gatewayUrl);
        appendIfMissing(missing, "notify_url", notifyUrl);
        return missing.length() == 0 ? "none" : missing.toString();
    }

    private static void appendIfMissing(StringBuilder sb, String field, String value) {
        if (isPresent(value)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('/');
        }
        sb.append(field);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
