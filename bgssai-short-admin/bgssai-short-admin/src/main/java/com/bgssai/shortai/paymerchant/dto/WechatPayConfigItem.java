package com.bgssai.shortai.paymerchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One wechat_pay_config row returned to the admin console.
 *
 * <p>Credential columns are stored and returned in plaintext so the console can display the
 * current value and edit it in place. Timestamps are pre-formatted as
 * {@code yyyy-MM-dd HH:mm:ss} strings so the console renders them without client-side
 * conversion.</p>
 */
public class WechatPayConfigItem {

    private Long id;

    private String merchantName;

    private String mchId;

    private String appId;

    private String keyMode;

    @JsonProperty("is_enabled")
    private Boolean enabled;

    @JsonProperty("is_collection_active")
    private Boolean collectionActive;

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

    private String lastTestStatus;

    private String lastTestAt;

    private String lastTestError;

    private String gmtCreate;

    private String gmtModified;

    private String modifier;

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

    public Boolean getCollectionActive() {
        return collectionActive;
    }

    public void setCollectionActive(Boolean collectionActive) {
        this.collectionActive = collectionActive;
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

    public String getLastTestStatus() {
        return lastTestStatus;
    }

    public void setLastTestStatus(String lastTestStatus) {
        this.lastTestStatus = lastTestStatus;
    }

    public String getLastTestAt() {
        return lastTestAt;
    }

    public void setLastTestAt(String lastTestAt) {
        this.lastTestAt = lastTestAt;
    }

    public String getLastTestError() {
        return lastTestError;
    }

    public void setLastTestError(String lastTestError) {
        this.lastTestError = lastTestError;
    }

    public String getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(String gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public String getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(String gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }
}
