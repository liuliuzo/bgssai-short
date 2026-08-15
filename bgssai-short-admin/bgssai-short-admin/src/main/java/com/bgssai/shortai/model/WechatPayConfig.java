package com.bgssai.shortai.model;

import java.util.Date;

public class WechatPayConfig {
    private Long id;
    private String merchantName;
    private String mchId;
    private String appId;
    private String keyMode;
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
    private Date lastTestAt;
    private String lastTestError;
    private Integer enabled;
    private Integer collectionActive;
    private Integer delete;
    private String creator;
    private String modifier;
    private Date gmtCreate;
    private Date gmtModified;

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
        this.merchantName = merchantName == null ? null : merchantName.trim();
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId == null ? null : mchId.trim();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId == null ? null : appId.trim();
    }

    public String getKeyMode() {
        return keyMode;
    }

    public void setKeyMode(String keyMode) {
        this.keyMode = keyMode == null ? null : keyMode.trim();
    }

    public String getMerchantSerialNo() {
        return merchantSerialNo;
    }

    public void setMerchantSerialNo(String merchantSerialNo) {
        this.merchantSerialNo = merchantSerialNo == null ? null : merchantSerialNo.trim();
    }

    public String getMchPrivateKey() {
        return mchPrivateKey;
    }

    public void setMchPrivateKey(String mchPrivateKey) {
        this.mchPrivateKey = mchPrivateKey == null ? null : mchPrivateKey.trim();
    }

    public String getApiclientCert() {
        return apiclientCert;
    }

    public void setApiclientCert(String apiclientCert) {
        this.apiclientCert = apiclientCert == null ? null : apiclientCert.trim();
    }

    public String getMchCertP12() {
        return mchCertP12;
    }

    public void setMchCertP12(String mchCertP12) {
        this.mchCertP12 = mchCertP12 == null ? null : mchCertP12.trim();
    }

    public String getWechatPublicKeyId() {
        return wechatPublicKeyId;
    }

    public void setWechatPublicKeyId(String wechatPublicKeyId) {
        this.wechatPublicKeyId = wechatPublicKeyId == null ? null : wechatPublicKeyId.trim();
    }

    public String getWechatPublicKey() {
        return wechatPublicKey;
    }

    public void setWechatPublicKey(String wechatPublicKey) {
        this.wechatPublicKey = wechatPublicKey == null ? null : wechatPublicKey.trim();
    }

    public String getPlatformCertSerialNo() {
        return platformCertSerialNo;
    }

    public void setPlatformCertSerialNo(String platformCertSerialNo) {
        this.platformCertSerialNo = platformCertSerialNo == null ? null : platformCertSerialNo.trim();
    }

    public String getPlatformCert() {
        return platformCert;
    }

    public void setPlatformCert(String platformCert) {
        this.platformCert = platformCert == null ? null : platformCert.trim();
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public void setApiV3Key(String apiV3Key) {
        this.apiV3Key = apiV3Key == null ? null : apiV3Key.trim();
    }

    public String getApiV2Key() {
        return apiV2Key;
    }

    public void setApiV2Key(String apiV2Key) {
        this.apiV2Key = apiV2Key == null ? null : apiV2Key.trim();
    }

    public String getJsapiAuthDir() {
        return jsapiAuthDir;
    }

    public void setJsapiAuthDir(String jsapiAuthDir) {
        this.jsapiAuthDir = jsapiAuthDir == null ? null : jsapiAuthDir.trim();
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl == null ? null : notifyUrl.trim();
    }

    public String getEnvMode() {
        return envMode;
    }

    public void setEnvMode(String envMode) {
        this.envMode = envMode == null ? null : envMode.trim();
    }

    public String getLastTestStatus() {
        return lastTestStatus;
    }

    public void setLastTestStatus(String lastTestStatus) {
        this.lastTestStatus = lastTestStatus == null ? null : lastTestStatus.trim();
    }

    public Date getLastTestAt() {
        return lastTestAt;
    }

    public void setLastTestAt(Date lastTestAt) {
        this.lastTestAt = lastTestAt;
    }

    public String getLastTestError() {
        return lastTestError;
    }

    public void setLastTestError(String lastTestError) {
        this.lastTestError = lastTestError == null ? null : lastTestError.trim();
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getCollectionActive() {
        return collectionActive;
    }

    public void setCollectionActive(Integer collectionActive) {
        this.collectionActive = collectionActive;
    }

    public Integer getDelete() {
        return delete;
    }

    public void setDelete(Integer delete) {
        this.delete = delete;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator == null ? null : creator.trim();
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier == null ? null : modifier.trim();
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

}
