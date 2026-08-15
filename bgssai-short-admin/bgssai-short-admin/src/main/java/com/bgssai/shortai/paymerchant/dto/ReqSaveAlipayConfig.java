package com.bgssai.shortai.paymerchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Create-or-update request body for one alipay_config row.
 *
 * <p>No {@code id} means create; an {@code id} means update. Every field is written as supplied;
 * a blank credential field leaves the stored value untouched, so clearing a value is an explicit
 * action rather than a side effect of editing a neighbouring field.</p>
 */
public class ReqSaveAlipayConfig {

    private Long id;

    private String merchantName;

    private String appId;

    private String keyMode;

    @JsonProperty("is_enabled")
    private Boolean enabled;

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

    public String getAppPrivateKey() {
        return appPrivateKey;
    }

    public void setAppPrivateKey(String appPrivateKey) {
        this.appPrivateKey = appPrivateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public String getAppCertPublicKey() {
        return appCertPublicKey;
    }

    public void setAppCertPublicKey(String appCertPublicKey) {
        this.appCertPublicKey = appCertPublicKey;
    }

    public String getAlipayCertPublicKey() {
        return alipayCertPublicKey;
    }

    public void setAlipayCertPublicKey(String alipayCertPublicKey) {
        this.alipayCertPublicKey = alipayCertPublicKey;
    }

    public String getAlipayRootCert() {
        return alipayRootCert;
    }

    public void setAlipayRootCert(String alipayRootCert) {
        this.alipayRootCert = alipayRootCert;
    }

    public String getAppCertSn() {
        return appCertSn;
    }

    public void setAppCertSn(String appCertSn) {
        this.appCertSn = appCertSn;
    }

    public String getAlipayRootCertSn() {
        return alipayRootCertSn;
    }

    public void setAlipayRootCertSn(String alipayRootCertSn) {
        this.alipayRootCertSn = alipayRootCertSn;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getEnvMode() {
        return envMode;
    }

    public void setEnvMode(String envMode) {
        this.envMode = envMode;
    }
}
