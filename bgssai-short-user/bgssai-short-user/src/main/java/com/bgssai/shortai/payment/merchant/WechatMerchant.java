package com.bgssai.shortai.payment.merchant;

public class WechatMerchant {

    private final String mchId;
    private final String appId;
    private final String merchantSerialNo;
    private final String mchPrivateKey;
    private final String apiV3Key;
    private final String notifyUrl;
    private final String keyMode;
    private final String wechatPublicKeyId;
    private final String wechatPublicKey;

    public WechatMerchant(String mchId, String appId, String merchantSerialNo,
                          String mchPrivateKey, String apiV3Key, String notifyUrl,
                          String keyMode, String wechatPublicKeyId, String wechatPublicKey) {
        this.mchId = mchId;
        this.appId = appId;
        this.merchantSerialNo = merchantSerialNo;
        this.mchPrivateKey = mchPrivateKey;
        this.apiV3Key = apiV3Key;
        this.notifyUrl = notifyUrl;
        this.keyMode = keyMode;
        this.wechatPublicKeyId = wechatPublicKeyId;
        this.wechatPublicKey = wechatPublicKey;
    }

    public String getMchId() { return mchId; }
    public String getAppId() { return appId; }
    public String getMerchantSerialNo() { return merchantSerialNo; }
    public String getMchPrivateKey() { return mchPrivateKey; }
    public String getApiV3Key() { return apiV3Key; }
    public String getNotifyUrl() { return notifyUrl; }
    public String getKeyMode() { return keyMode; }
    public String getWechatPublicKeyId() { return wechatPublicKeyId; }
    public String getWechatPublicKey() { return wechatPublicKey; }

    /** Stored {@code wechat_pay_config.key_mode}. Unset or {@code PLATFORM_CERT} keeps certificate mode. */
    public boolean usesPublicKeyMode() {
        return keyMode != null && "PUBLIC_KEY".equalsIgnoreCase(keyMode.trim());
    }

    public boolean hasCompleteCredentials() {
        if (!isPresent(mchId) || !isPresent(appId) || !isPresent(merchantSerialNo)
                || !isPresent(mchPrivateKey) || !isPresent(apiV3Key) || !isPresent(notifyUrl)) {
            return false;
        }
        if (usesPublicKeyMode()) {
            return isPresent(wechatPublicKeyId) && isPresent(wechatPublicKey);
        }
        return true;
    }

    public String missingFields() {
        StringBuilder missing = new StringBuilder();
        appendIfMissing(missing, "mch_id", mchId);
        appendIfMissing(missing, "app_id", appId);
        appendIfMissing(missing, "merchant_serial_no", merchantSerialNo);
        appendIfMissing(missing, "mch_private_key", mchPrivateKey);
        appendIfMissing(missing, "api_v3_key", apiV3Key);
        appendIfMissing(missing, "notify_url", notifyUrl);
        if (usesPublicKeyMode()) {
            appendIfMissing(missing, "wechat_public_key_id", wechatPublicKeyId);
            appendIfMissing(missing, "wechat_public_key", wechatPublicKey);
        }
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
