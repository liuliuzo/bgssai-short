package com.bgssai.shortai.model;

import java.util.Date;

public class AlipayConfig {
    private Long id;
    private String merchantName;
    private String appId;
    private String keyMode;
    private String appPrivateKey;
    private String alipayPublicKey;
    private String appCertPublicKey;
    private String alipayCertPublicKey;
    private String alipayRootCert;
    private String appCertSn;
    private String alipayRootCertSn;
    private String signType;
    private String charset;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String envMode;
    private String lastTestStatus;
    private Date lastTestAt;
    private String lastTestError;
    private Integer enabled;
    private Integer collectionActive;
    private Integer delFlag;
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

    public String getAppPrivateKey() {
        return appPrivateKey;
    }

    public void setAppPrivateKey(String appPrivateKey) {
        this.appPrivateKey = appPrivateKey == null ? null : appPrivateKey.trim();
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey == null ? null : alipayPublicKey.trim();
    }

    public String getAppCertPublicKey() {
        return appCertPublicKey;
    }

    public void setAppCertPublicKey(String appCertPublicKey) {
        this.appCertPublicKey = appCertPublicKey == null ? null : appCertPublicKey.trim();
    }

    public String getAlipayCertPublicKey() {
        return alipayCertPublicKey;
    }

    public void setAlipayCertPublicKey(String alipayCertPublicKey) {
        this.alipayCertPublicKey = alipayCertPublicKey == null ? null : alipayCertPublicKey.trim();
    }

    public String getAlipayRootCert() {
        return alipayRootCert;
    }

    public void setAlipayRootCert(String alipayRootCert) {
        this.alipayRootCert = alipayRootCert == null ? null : alipayRootCert.trim();
    }

    public String getAppCertSn() {
        return appCertSn;
    }

    public void setAppCertSn(String appCertSn) {
        this.appCertSn = appCertSn == null ? null : appCertSn.trim();
    }

    public String getAlipayRootCertSn() {
        return alipayRootCertSn;
    }

    public void setAlipayRootCertSn(String alipayRootCertSn) {
        this.alipayRootCertSn = alipayRootCertSn == null ? null : alipayRootCertSn.trim();
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType == null ? null : signType.trim();
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset == null ? null : charset.trim();
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl == null ? null : gatewayUrl.trim();
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl == null ? null : notifyUrl.trim();
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl == null ? null : returnUrl.trim();
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

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
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
