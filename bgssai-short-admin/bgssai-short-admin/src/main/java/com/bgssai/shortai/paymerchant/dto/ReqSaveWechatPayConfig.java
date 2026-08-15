package com.bgssai.shortai.paymerchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Create-or-update request body for one wechat_pay_config row.
 *
 * <p>No {@code id} means create; an {@code id} means update. Every field is written as supplied;
 * a blank credential field leaves the stored value untouched, so clearing a value is an explicit
 * action rather than a side effect of editing a neighbouring field.</p>
 */
public class ReqSaveWechatPayConfig {

    private Long id;

    private String merchantName;

    private String mchId;

    private String appId;

    private String keyMode;

    @JsonProperty("is_enabled")
    private Boolean enabled;

    private String merchantSerialNo;

    private String mchPrivateKey;

    private String apiclientCert;

    private String mchCertP12;

    private String wechatPublicKeyId;

    private String wechatPublicKey;

    private String platformCertSerialNo;

    private String platformCert;

    private String apiV3Key;

    private String apiV2Key;

    private String jsapiAuthDir;

    private String notifyUrl;

    private String envMode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getKeyMode() {
        return keyMode;
    }

    public void setKeyMode(String keyMode) {
        this.keyMode = keyMode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getMerchantSerialNo() {
        return merchantSerialNo;
    }

    public void setMerchantSerialNo(String merchantSerialNo) {
        this.merchantSerialNo = merchantSerialNo;
    }

    public String getMchPrivateKey() {
        return mchPrivateKey;
    }

    public void setMchPrivateKey(String mchPrivateKey) {
        this.mchPrivateKey = mchPrivateKey;
    }

    public String getApiclientCert() {
        return apiclientCert;
    }

    public void setApiclientCert(String apiclientCert) {
        this.apiclientCert = apiclientCert;
    }

    public String getMchCertP12() {
        return mchCertP12;
    }

    public void setMchCertP12(String mchCertP12) {
        this.mchCertP12 = mchCertP12;
    }

    public String getWechatPublicKeyId() {
        return wechatPublicKeyId;
    }

    public void setWechatPublicKeyId(String wechatPublicKeyId) {
        this.wechatPublicKeyId = wechatPublicKeyId;
    }

    public String getWechatPublicKey() {
        return wechatPublicKey;
    }

    public void setWechatPublicKey(String wechatPublicKey) {
        this.wechatPublicKey = wechatPublicKey;
    }

    public String getPlatformCertSerialNo() {
        return platformCertSerialNo;
    }

    public void setPlatformCertSerialNo(String platformCertSerialNo) {
        this.platformCertSerialNo = platformCertSerialNo;
    }

    public String getPlatformCert() {
        return platformCert;
    }

    public void setPlatformCert(String platformCert) {
        this.platformCert = platformCert;
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public void setApiV3Key(String apiV3Key) {
        this.apiV3Key = apiV3Key;
    }

    public String getApiV2Key() {
        return apiV2Key;
    }

    public void setApiV2Key(String apiV2Key) {
        this.apiV2Key = apiV2Key;
    }

    public String getJsapiAuthDir() {
        return jsapiAuthDir;
    }

    public void setJsapiAuthDir(String jsapiAuthDir) {
        this.jsapiAuthDir = jsapiAuthDir;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getEnvMode() {
        return envMode;
    }

    public void setEnvMode(String envMode) {
        this.envMode = envMode;
    }
}
