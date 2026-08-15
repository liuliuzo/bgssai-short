package com.bgssai.shortai.payment.merchant;

public class WechatMerchant {

    private final String mchId;
    private final String appId;
    private final String merchantSerialNo;
    private final String mchPrivateKey;
    private final String apiV3Key;
    private final String notifyUrl;

    public WechatMerchant(String mchId, String appId, String merchantSerialNo,
                          String mchPrivateKey, String apiV3Key, String notifyUrl) {
        this.mchId = mchId;
        this.appId = appId;
        this.merchantSerialNo = merchantSerialNo;
        this.mchPrivateKey = mchPrivateKey;
        this.apiV3Key = apiV3Key;
        this.notifyUrl = notifyUrl;
    }

    public String getMchId() { return mchId; }
    public String getAppId() { return appId; }
    public String getMerchantSerialNo() { return merchantSerialNo; }
    public String getMchPrivateKey() { return mchPrivateKey; }
    public String getApiV3Key() { return apiV3Key; }
    public String getNotifyUrl() { return notifyUrl; }

    public boolean hasCompleteCredentials() {
        return isPresent(mchId) && isPresent(appId) && isPresent(merchantSerialNo)
                && isPresent(mchPrivateKey) && isPresent(apiV3Key) && isPresent(notifyUrl);
    }

    public String missingFields() {
        StringBuilder missing = new StringBuilder();
        appendIfMissing(missing, "mch_id", mchId);
        appendIfMissing(missing, "app_id", appId);
        appendIfMissing(missing, "merchant_serial_no", merchantSerialNo);
        appendIfMissing(missing, "mch_private_key", mchPrivateKey);
        appendIfMissing(missing, "api_v3_key", apiV3Key);
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
