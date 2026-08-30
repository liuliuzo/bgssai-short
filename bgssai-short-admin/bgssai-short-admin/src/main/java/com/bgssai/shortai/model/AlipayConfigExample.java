package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AlipayConfigExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    private String[] selectColumns;

    public AlipayConfigExample() {
        oredCriteria = new ArrayList<>();
    }

    public AlipayConfigExample selectColumns(String... cols) {
        this.selectColumns = cols;
        return this;
    }

    public String[] getSelectColumns() { return selectColumns; }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
    public boolean isDistinct() { return distinct; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }
    public void or(Criteria criteria) { oredCriteria.add(criteria); }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.isEmpty()) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() { return new Criteria(); }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
        selectColumns = null;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() { criteria = new ArrayList<>(); }

        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() { addCriterion("id is null"); return (Criteria) this; }
        public Criteria andIdIsNotNull() { addCriterion("id is not null"); return (Criteria) this; }
        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return (Criteria) this; }
        public Criteria andIdNotEqualTo(Long value) { addCriterion("id <>", value, "id"); return (Criteria) this; }
        public Criteria andIdGreaterThan(Long value) { addCriterion("id >", value, "id"); return (Criteria) this; }
        public Criteria andIdGreaterThanOrEqualTo(Long value) { addCriterion("id >=", value, "id"); return (Criteria) this; }
        public Criteria andIdLessThan(Long value) { addCriterion("id <", value, "id"); return (Criteria) this; }
        public Criteria andIdLessThanOrEqualTo(Long value) { addCriterion("id <=", value, "id"); return (Criteria) this; }
        public Criteria andIdIn(List<Long> values) { addCriterion("id in", values, "id"); return (Criteria) this; }

        public Criteria andMerchantNameIsNull() { addCriterion("merchant_name is null"); return (Criteria) this; }
        public Criteria andMerchantNameIsNotNull() { addCriterion("merchant_name is not null"); return (Criteria) this; }
        public Criteria andMerchantNameEqualTo(String value) { addCriterion("merchant_name =", value, "merchantName"); return (Criteria) this; }
        public Criteria andMerchantNameNotEqualTo(String value) { addCriterion("merchant_name <>", value, "merchantName"); return (Criteria) this; }
        public Criteria andMerchantNameGreaterThan(String value) { addCriterion("merchant_name >", value, "merchantName"); return (Criteria) this; }
        public Criteria andMerchantNameGreaterThanOrEqualTo(String value) { addCriterion("merchant_name >=", value, "merchantName"); return (Criteria) this; }
        public Criteria andMerchantNameLessThan(String value) { addCriterion("merchant_name <", value, "merchantName"); return (Criteria) this; }
        public Criteria andMerchantNameLessThanOrEqualTo(String value) { addCriterion("merchant_name <=", value, "merchantName"); return (Criteria) this; }
        public Criteria andMerchantNameIn(List<String> values) { addCriterion("merchant_name in", values, "merchantName"); return (Criteria) this; }
        public Criteria andMerchantNameLike(String value) { addCriterion("merchant_name like", value, "merchantName"); return (Criteria) this; }

        public Criteria andAppIdIsNull() { addCriterion("app_id is null"); return (Criteria) this; }
        public Criteria andAppIdIsNotNull() { addCriterion("app_id is not null"); return (Criteria) this; }
        public Criteria andAppIdEqualTo(String value) { addCriterion("app_id =", value, "appId"); return (Criteria) this; }
        public Criteria andAppIdNotEqualTo(String value) { addCriterion("app_id <>", value, "appId"); return (Criteria) this; }
        public Criteria andAppIdGreaterThan(String value) { addCriterion("app_id >", value, "appId"); return (Criteria) this; }
        public Criteria andAppIdGreaterThanOrEqualTo(String value) { addCriterion("app_id >=", value, "appId"); return (Criteria) this; }
        public Criteria andAppIdLessThan(String value) { addCriterion("app_id <", value, "appId"); return (Criteria) this; }
        public Criteria andAppIdLessThanOrEqualTo(String value) { addCriterion("app_id <=", value, "appId"); return (Criteria) this; }
        public Criteria andAppIdIn(List<String> values) { addCriterion("app_id in", values, "appId"); return (Criteria) this; }
        public Criteria andAppIdLike(String value) { addCriterion("app_id like", value, "appId"); return (Criteria) this; }

        public Criteria andKeyModeIsNull() { addCriterion("key_mode is null"); return (Criteria) this; }
        public Criteria andKeyModeIsNotNull() { addCriterion("key_mode is not null"); return (Criteria) this; }
        public Criteria andKeyModeEqualTo(String value) { addCriterion("key_mode =", value, "keyMode"); return (Criteria) this; }
        public Criteria andKeyModeNotEqualTo(String value) { addCriterion("key_mode <>", value, "keyMode"); return (Criteria) this; }
        public Criteria andKeyModeGreaterThan(String value) { addCriterion("key_mode >", value, "keyMode"); return (Criteria) this; }
        public Criteria andKeyModeGreaterThanOrEqualTo(String value) { addCriterion("key_mode >=", value, "keyMode"); return (Criteria) this; }
        public Criteria andKeyModeLessThan(String value) { addCriterion("key_mode <", value, "keyMode"); return (Criteria) this; }
        public Criteria andKeyModeLessThanOrEqualTo(String value) { addCriterion("key_mode <=", value, "keyMode"); return (Criteria) this; }
        public Criteria andKeyModeIn(List<String> values) { addCriterion("key_mode in", values, "keyMode"); return (Criteria) this; }
        public Criteria andKeyModeLike(String value) { addCriterion("key_mode like", value, "keyMode"); return (Criteria) this; }

        public Criteria andAppPrivateKeyIsNull() { addCriterion("app_private_key is null"); return (Criteria) this; }
        public Criteria andAppPrivateKeyIsNotNull() { addCriterion("app_private_key is not null"); return (Criteria) this; }
        public Criteria andAppPrivateKeyEqualTo(String value) { addCriterion("app_private_key =", value, "appPrivateKey"); return (Criteria) this; }
        public Criteria andAppPrivateKeyNotEqualTo(String value) { addCriterion("app_private_key <>", value, "appPrivateKey"); return (Criteria) this; }
        public Criteria andAppPrivateKeyGreaterThan(String value) { addCriterion("app_private_key >", value, "appPrivateKey"); return (Criteria) this; }
        public Criteria andAppPrivateKeyGreaterThanOrEqualTo(String value) { addCriterion("app_private_key >=", value, "appPrivateKey"); return (Criteria) this; }
        public Criteria andAppPrivateKeyLessThan(String value) { addCriterion("app_private_key <", value, "appPrivateKey"); return (Criteria) this; }
        public Criteria andAppPrivateKeyLessThanOrEqualTo(String value) { addCriterion("app_private_key <=", value, "appPrivateKey"); return (Criteria) this; }
        public Criteria andAppPrivateKeyIn(List<String> values) { addCriterion("app_private_key in", values, "appPrivateKey"); return (Criteria) this; }
        public Criteria andAppPrivateKeyLike(String value) { addCriterion("app_private_key like", value, "appPrivateKey"); return (Criteria) this; }

        public Criteria andAlipayPublicKeyIsNull() { addCriterion("alipay_public_key is null"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyIsNotNull() { addCriterion("alipay_public_key is not null"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyEqualTo(String value) { addCriterion("alipay_public_key =", value, "alipayPublicKey"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyNotEqualTo(String value) { addCriterion("alipay_public_key <>", value, "alipayPublicKey"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyGreaterThan(String value) { addCriterion("alipay_public_key >", value, "alipayPublicKey"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyGreaterThanOrEqualTo(String value) { addCriterion("alipay_public_key >=", value, "alipayPublicKey"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyLessThan(String value) { addCriterion("alipay_public_key <", value, "alipayPublicKey"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyLessThanOrEqualTo(String value) { addCriterion("alipay_public_key <=", value, "alipayPublicKey"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyIn(List<String> values) { addCriterion("alipay_public_key in", values, "alipayPublicKey"); return (Criteria) this; }
        public Criteria andAlipayPublicKeyLike(String value) { addCriterion("alipay_public_key like", value, "alipayPublicKey"); return (Criteria) this; }

        public Criteria andAppCertPublicKeyIsNull() { addCriterion("app_cert_public_key is null"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyIsNotNull() { addCriterion("app_cert_public_key is not null"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyEqualTo(String value) { addCriterion("app_cert_public_key =", value, "appCertPublicKey"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyNotEqualTo(String value) { addCriterion("app_cert_public_key <>", value, "appCertPublicKey"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyGreaterThan(String value) { addCriterion("app_cert_public_key >", value, "appCertPublicKey"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyGreaterThanOrEqualTo(String value) { addCriterion("app_cert_public_key >=", value, "appCertPublicKey"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyLessThan(String value) { addCriterion("app_cert_public_key <", value, "appCertPublicKey"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyLessThanOrEqualTo(String value) { addCriterion("app_cert_public_key <=", value, "appCertPublicKey"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyIn(List<String> values) { addCriterion("app_cert_public_key in", values, "appCertPublicKey"); return (Criteria) this; }
        public Criteria andAppCertPublicKeyLike(String value) { addCriterion("app_cert_public_key like", value, "appCertPublicKey"); return (Criteria) this; }

        public Criteria andAlipayCertPublicKeyIsNull() { addCriterion("alipay_cert_public_key is null"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyIsNotNull() { addCriterion("alipay_cert_public_key is not null"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyEqualTo(String value) { addCriterion("alipay_cert_public_key =", value, "alipayCertPublicKey"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyNotEqualTo(String value) { addCriterion("alipay_cert_public_key <>", value, "alipayCertPublicKey"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyGreaterThan(String value) { addCriterion("alipay_cert_public_key >", value, "alipayCertPublicKey"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyGreaterThanOrEqualTo(String value) { addCriterion("alipay_cert_public_key >=", value, "alipayCertPublicKey"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyLessThan(String value) { addCriterion("alipay_cert_public_key <", value, "alipayCertPublicKey"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyLessThanOrEqualTo(String value) { addCriterion("alipay_cert_public_key <=", value, "alipayCertPublicKey"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyIn(List<String> values) { addCriterion("alipay_cert_public_key in", values, "alipayCertPublicKey"); return (Criteria) this; }
        public Criteria andAlipayCertPublicKeyLike(String value) { addCriterion("alipay_cert_public_key like", value, "alipayCertPublicKey"); return (Criteria) this; }

        public Criteria andAlipayRootCertIsNull() { addCriterion("alipay_root_cert is null"); return (Criteria) this; }
        public Criteria andAlipayRootCertIsNotNull() { addCriterion("alipay_root_cert is not null"); return (Criteria) this; }
        public Criteria andAlipayRootCertEqualTo(String value) { addCriterion("alipay_root_cert =", value, "alipayRootCert"); return (Criteria) this; }
        public Criteria andAlipayRootCertNotEqualTo(String value) { addCriterion("alipay_root_cert <>", value, "alipayRootCert"); return (Criteria) this; }
        public Criteria andAlipayRootCertGreaterThan(String value) { addCriterion("alipay_root_cert >", value, "alipayRootCert"); return (Criteria) this; }
        public Criteria andAlipayRootCertGreaterThanOrEqualTo(String value) { addCriterion("alipay_root_cert >=", value, "alipayRootCert"); return (Criteria) this; }
        public Criteria andAlipayRootCertLessThan(String value) { addCriterion("alipay_root_cert <", value, "alipayRootCert"); return (Criteria) this; }
        public Criteria andAlipayRootCertLessThanOrEqualTo(String value) { addCriterion("alipay_root_cert <=", value, "alipayRootCert"); return (Criteria) this; }
        public Criteria andAlipayRootCertIn(List<String> values) { addCriterion("alipay_root_cert in", values, "alipayRootCert"); return (Criteria) this; }
        public Criteria andAlipayRootCertLike(String value) { addCriterion("alipay_root_cert like", value, "alipayRootCert"); return (Criteria) this; }

        public Criteria andAppCertSnIsNull() { addCriterion("app_cert_sn is null"); return (Criteria) this; }
        public Criteria andAppCertSnIsNotNull() { addCriterion("app_cert_sn is not null"); return (Criteria) this; }
        public Criteria andAppCertSnEqualTo(String value) { addCriterion("app_cert_sn =", value, "appCertSn"); return (Criteria) this; }
        public Criteria andAppCertSnNotEqualTo(String value) { addCriterion("app_cert_sn <>", value, "appCertSn"); return (Criteria) this; }
        public Criteria andAppCertSnGreaterThan(String value) { addCriterion("app_cert_sn >", value, "appCertSn"); return (Criteria) this; }
        public Criteria andAppCertSnGreaterThanOrEqualTo(String value) { addCriterion("app_cert_sn >=", value, "appCertSn"); return (Criteria) this; }
        public Criteria andAppCertSnLessThan(String value) { addCriterion("app_cert_sn <", value, "appCertSn"); return (Criteria) this; }
        public Criteria andAppCertSnLessThanOrEqualTo(String value) { addCriterion("app_cert_sn <=", value, "appCertSn"); return (Criteria) this; }
        public Criteria andAppCertSnIn(List<String> values) { addCriterion("app_cert_sn in", values, "appCertSn"); return (Criteria) this; }
        public Criteria andAppCertSnLike(String value) { addCriterion("app_cert_sn like", value, "appCertSn"); return (Criteria) this; }

        public Criteria andAlipayRootCertSnIsNull() { addCriterion("alipay_root_cert_sn is null"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnIsNotNull() { addCriterion("alipay_root_cert_sn is not null"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnEqualTo(String value) { addCriterion("alipay_root_cert_sn =", value, "alipayRootCertSn"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnNotEqualTo(String value) { addCriterion("alipay_root_cert_sn <>", value, "alipayRootCertSn"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnGreaterThan(String value) { addCriterion("alipay_root_cert_sn >", value, "alipayRootCertSn"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnGreaterThanOrEqualTo(String value) { addCriterion("alipay_root_cert_sn >=", value, "alipayRootCertSn"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnLessThan(String value) { addCriterion("alipay_root_cert_sn <", value, "alipayRootCertSn"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnLessThanOrEqualTo(String value) { addCriterion("alipay_root_cert_sn <=", value, "alipayRootCertSn"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnIn(List<String> values) { addCriterion("alipay_root_cert_sn in", values, "alipayRootCertSn"); return (Criteria) this; }
        public Criteria andAlipayRootCertSnLike(String value) { addCriterion("alipay_root_cert_sn like", value, "alipayRootCertSn"); return (Criteria) this; }

        public Criteria andSignTypeIsNull() { addCriterion("sign_type is null"); return (Criteria) this; }
        public Criteria andSignTypeIsNotNull() { addCriterion("sign_type is not null"); return (Criteria) this; }
        public Criteria andSignTypeEqualTo(String value) { addCriterion("sign_type =", value, "signType"); return (Criteria) this; }
        public Criteria andSignTypeNotEqualTo(String value) { addCriterion("sign_type <>", value, "signType"); return (Criteria) this; }
        public Criteria andSignTypeGreaterThan(String value) { addCriterion("sign_type >", value, "signType"); return (Criteria) this; }
        public Criteria andSignTypeGreaterThanOrEqualTo(String value) { addCriterion("sign_type >=", value, "signType"); return (Criteria) this; }
        public Criteria andSignTypeLessThan(String value) { addCriterion("sign_type <", value, "signType"); return (Criteria) this; }
        public Criteria andSignTypeLessThanOrEqualTo(String value) { addCriterion("sign_type <=", value, "signType"); return (Criteria) this; }
        public Criteria andSignTypeIn(List<String> values) { addCriterion("sign_type in", values, "signType"); return (Criteria) this; }
        public Criteria andSignTypeLike(String value) { addCriterion("sign_type like", value, "signType"); return (Criteria) this; }

        public Criteria andCharsetIsNull() { addCriterion("charset is null"); return (Criteria) this; }
        public Criteria andCharsetIsNotNull() { addCriterion("charset is not null"); return (Criteria) this; }
        public Criteria andCharsetEqualTo(String value) { addCriterion("charset =", value, "charset"); return (Criteria) this; }
        public Criteria andCharsetNotEqualTo(String value) { addCriterion("charset <>", value, "charset"); return (Criteria) this; }
        public Criteria andCharsetGreaterThan(String value) { addCriterion("charset >", value, "charset"); return (Criteria) this; }
        public Criteria andCharsetGreaterThanOrEqualTo(String value) { addCriterion("charset >=", value, "charset"); return (Criteria) this; }
        public Criteria andCharsetLessThan(String value) { addCriterion("charset <", value, "charset"); return (Criteria) this; }
        public Criteria andCharsetLessThanOrEqualTo(String value) { addCriterion("charset <=", value, "charset"); return (Criteria) this; }
        public Criteria andCharsetIn(List<String> values) { addCriterion("charset in", values, "charset"); return (Criteria) this; }
        public Criteria andCharsetLike(String value) { addCriterion("charset like", value, "charset"); return (Criteria) this; }

        public Criteria andGatewayUrlIsNull() { addCriterion("gateway_url is null"); return (Criteria) this; }
        public Criteria andGatewayUrlIsNotNull() { addCriterion("gateway_url is not null"); return (Criteria) this; }
        public Criteria andGatewayUrlEqualTo(String value) { addCriterion("gateway_url =", value, "gatewayUrl"); return (Criteria) this; }
        public Criteria andGatewayUrlNotEqualTo(String value) { addCriterion("gateway_url <>", value, "gatewayUrl"); return (Criteria) this; }
        public Criteria andGatewayUrlGreaterThan(String value) { addCriterion("gateway_url >", value, "gatewayUrl"); return (Criteria) this; }
        public Criteria andGatewayUrlGreaterThanOrEqualTo(String value) { addCriterion("gateway_url >=", value, "gatewayUrl"); return (Criteria) this; }
        public Criteria andGatewayUrlLessThan(String value) { addCriterion("gateway_url <", value, "gatewayUrl"); return (Criteria) this; }
        public Criteria andGatewayUrlLessThanOrEqualTo(String value) { addCriterion("gateway_url <=", value, "gatewayUrl"); return (Criteria) this; }
        public Criteria andGatewayUrlIn(List<String> values) { addCriterion("gateway_url in", values, "gatewayUrl"); return (Criteria) this; }
        public Criteria andGatewayUrlLike(String value) { addCriterion("gateway_url like", value, "gatewayUrl"); return (Criteria) this; }

        public Criteria andNotifyUrlIsNull() { addCriterion("notify_url is null"); return (Criteria) this; }
        public Criteria andNotifyUrlIsNotNull() { addCriterion("notify_url is not null"); return (Criteria) this; }
        public Criteria andNotifyUrlEqualTo(String value) { addCriterion("notify_url =", value, "notifyUrl"); return (Criteria) this; }
        public Criteria andNotifyUrlNotEqualTo(String value) { addCriterion("notify_url <>", value, "notifyUrl"); return (Criteria) this; }
        public Criteria andNotifyUrlGreaterThan(String value) { addCriterion("notify_url >", value, "notifyUrl"); return (Criteria) this; }
        public Criteria andNotifyUrlGreaterThanOrEqualTo(String value) { addCriterion("notify_url >=", value, "notifyUrl"); return (Criteria) this; }
        public Criteria andNotifyUrlLessThan(String value) { addCriterion("notify_url <", value, "notifyUrl"); return (Criteria) this; }
        public Criteria andNotifyUrlLessThanOrEqualTo(String value) { addCriterion("notify_url <=", value, "notifyUrl"); return (Criteria) this; }
        public Criteria andNotifyUrlIn(List<String> values) { addCriterion("notify_url in", values, "notifyUrl"); return (Criteria) this; }
        public Criteria andNotifyUrlLike(String value) { addCriterion("notify_url like", value, "notifyUrl"); return (Criteria) this; }

        public Criteria andReturnUrlIsNull() { addCriterion("return_url is null"); return (Criteria) this; }
        public Criteria andReturnUrlIsNotNull() { addCriterion("return_url is not null"); return (Criteria) this; }
        public Criteria andReturnUrlEqualTo(String value) { addCriterion("return_url =", value, "returnUrl"); return (Criteria) this; }
        public Criteria andReturnUrlNotEqualTo(String value) { addCriterion("return_url <>", value, "returnUrl"); return (Criteria) this; }
        public Criteria andReturnUrlGreaterThan(String value) { addCriterion("return_url >", value, "returnUrl"); return (Criteria) this; }
        public Criteria andReturnUrlGreaterThanOrEqualTo(String value) { addCriterion("return_url >=", value, "returnUrl"); return (Criteria) this; }
        public Criteria andReturnUrlLessThan(String value) { addCriterion("return_url <", value, "returnUrl"); return (Criteria) this; }
        public Criteria andReturnUrlLessThanOrEqualTo(String value) { addCriterion("return_url <=", value, "returnUrl"); return (Criteria) this; }
        public Criteria andReturnUrlIn(List<String> values) { addCriterion("return_url in", values, "returnUrl"); return (Criteria) this; }
        public Criteria andReturnUrlLike(String value) { addCriterion("return_url like", value, "returnUrl"); return (Criteria) this; }

        public Criteria andEnvModeIsNull() { addCriterion("env_mode is null"); return (Criteria) this; }
        public Criteria andEnvModeIsNotNull() { addCriterion("env_mode is not null"); return (Criteria) this; }
        public Criteria andEnvModeEqualTo(String value) { addCriterion("env_mode =", value, "envMode"); return (Criteria) this; }
        public Criteria andEnvModeNotEqualTo(String value) { addCriterion("env_mode <>", value, "envMode"); return (Criteria) this; }
        public Criteria andEnvModeGreaterThan(String value) { addCriterion("env_mode >", value, "envMode"); return (Criteria) this; }
        public Criteria andEnvModeGreaterThanOrEqualTo(String value) { addCriterion("env_mode >=", value, "envMode"); return (Criteria) this; }
        public Criteria andEnvModeLessThan(String value) { addCriterion("env_mode <", value, "envMode"); return (Criteria) this; }
        public Criteria andEnvModeLessThanOrEqualTo(String value) { addCriterion("env_mode <=", value, "envMode"); return (Criteria) this; }
        public Criteria andEnvModeIn(List<String> values) { addCriterion("env_mode in", values, "envMode"); return (Criteria) this; }
        public Criteria andEnvModeLike(String value) { addCriterion("env_mode like", value, "envMode"); return (Criteria) this; }

        public Criteria andLastTestStatusIsNull() { addCriterion("last_test_status is null"); return (Criteria) this; }
        public Criteria andLastTestStatusIsNotNull() { addCriterion("last_test_status is not null"); return (Criteria) this; }
        public Criteria andLastTestStatusEqualTo(String value) { addCriterion("last_test_status =", value, "lastTestStatus"); return (Criteria) this; }
        public Criteria andLastTestStatusNotEqualTo(String value) { addCriterion("last_test_status <>", value, "lastTestStatus"); return (Criteria) this; }
        public Criteria andLastTestStatusGreaterThan(String value) { addCriterion("last_test_status >", value, "lastTestStatus"); return (Criteria) this; }
        public Criteria andLastTestStatusGreaterThanOrEqualTo(String value) { addCriterion("last_test_status >=", value, "lastTestStatus"); return (Criteria) this; }
        public Criteria andLastTestStatusLessThan(String value) { addCriterion("last_test_status <", value, "lastTestStatus"); return (Criteria) this; }
        public Criteria andLastTestStatusLessThanOrEqualTo(String value) { addCriterion("last_test_status <=", value, "lastTestStatus"); return (Criteria) this; }
        public Criteria andLastTestStatusIn(List<String> values) { addCriterion("last_test_status in", values, "lastTestStatus"); return (Criteria) this; }
        public Criteria andLastTestStatusLike(String value) { addCriterion("last_test_status like", value, "lastTestStatus"); return (Criteria) this; }

        public Criteria andLastTestAtIsNull() { addCriterion("last_test_at is null"); return (Criteria) this; }
        public Criteria andLastTestAtIsNotNull() { addCriterion("last_test_at is not null"); return (Criteria) this; }
        public Criteria andLastTestAtEqualTo(Date value) { addCriterion("last_test_at =", value, "lastTestAt"); return (Criteria) this; }
        public Criteria andLastTestAtNotEqualTo(Date value) { addCriterion("last_test_at <>", value, "lastTestAt"); return (Criteria) this; }
        public Criteria andLastTestAtGreaterThan(Date value) { addCriterion("last_test_at >", value, "lastTestAt"); return (Criteria) this; }
        public Criteria andLastTestAtGreaterThanOrEqualTo(Date value) { addCriterion("last_test_at >=", value, "lastTestAt"); return (Criteria) this; }
        public Criteria andLastTestAtLessThan(Date value) { addCriterion("last_test_at <", value, "lastTestAt"); return (Criteria) this; }
        public Criteria andLastTestAtLessThanOrEqualTo(Date value) { addCriterion("last_test_at <=", value, "lastTestAt"); return (Criteria) this; }
        public Criteria andLastTestAtIn(List<Date> values) { addCriterion("last_test_at in", values, "lastTestAt"); return (Criteria) this; }

        public Criteria andLastTestErrorIsNull() { addCriterion("last_test_error is null"); return (Criteria) this; }
        public Criteria andLastTestErrorIsNotNull() { addCriterion("last_test_error is not null"); return (Criteria) this; }
        public Criteria andLastTestErrorEqualTo(String value) { addCriterion("last_test_error =", value, "lastTestError"); return (Criteria) this; }
        public Criteria andLastTestErrorNotEqualTo(String value) { addCriterion("last_test_error <>", value, "lastTestError"); return (Criteria) this; }
        public Criteria andLastTestErrorGreaterThan(String value) { addCriterion("last_test_error >", value, "lastTestError"); return (Criteria) this; }
        public Criteria andLastTestErrorGreaterThanOrEqualTo(String value) { addCriterion("last_test_error >=", value, "lastTestError"); return (Criteria) this; }
        public Criteria andLastTestErrorLessThan(String value) { addCriterion("last_test_error <", value, "lastTestError"); return (Criteria) this; }
        public Criteria andLastTestErrorLessThanOrEqualTo(String value) { addCriterion("last_test_error <=", value, "lastTestError"); return (Criteria) this; }
        public Criteria andLastTestErrorIn(List<String> values) { addCriterion("last_test_error in", values, "lastTestError"); return (Criteria) this; }
        public Criteria andLastTestErrorLike(String value) { addCriterion("last_test_error like", value, "lastTestError"); return (Criteria) this; }

        public Criteria andEnabledIsNull() { addCriterion("is_enabled is null"); return (Criteria) this; }
        public Criteria andEnabledIsNotNull() { addCriterion("is_enabled is not null"); return (Criteria) this; }
        public Criteria andEnabledEqualTo(Integer value) { addCriterion("is_enabled =", value, "enabled"); return (Criteria) this; }
        public Criteria andEnabledNotEqualTo(Integer value) { addCriterion("is_enabled <>", value, "enabled"); return (Criteria) this; }
        public Criteria andEnabledGreaterThan(Integer value) { addCriterion("is_enabled >", value, "enabled"); return (Criteria) this; }
        public Criteria andEnabledGreaterThanOrEqualTo(Integer value) { addCriterion("is_enabled >=", value, "enabled"); return (Criteria) this; }
        public Criteria andEnabledLessThan(Integer value) { addCriterion("is_enabled <", value, "enabled"); return (Criteria) this; }
        public Criteria andEnabledLessThanOrEqualTo(Integer value) { addCriterion("is_enabled <=", value, "enabled"); return (Criteria) this; }
        public Criteria andEnabledIn(List<Integer> values) { addCriterion("is_enabled in", values, "enabled"); return (Criteria) this; }

        public Criteria andCollectionActiveIsNull() { addCriterion("is_collection_active is null"); return (Criteria) this; }
        public Criteria andCollectionActiveIsNotNull() { addCriterion("is_collection_active is not null"); return (Criteria) this; }
        public Criteria andCollectionActiveEqualTo(Integer value) { addCriterion("is_collection_active =", value, "collectionActive"); return (Criteria) this; }
        public Criteria andCollectionActiveNotEqualTo(Integer value) { addCriterion("is_collection_active <>", value, "collectionActive"); return (Criteria) this; }
        public Criteria andCollectionActiveGreaterThan(Integer value) { addCriterion("is_collection_active >", value, "collectionActive"); return (Criteria) this; }
        public Criteria andCollectionActiveGreaterThanOrEqualTo(Integer value) { addCriterion("is_collection_active >=", value, "collectionActive"); return (Criteria) this; }
        public Criteria andCollectionActiveLessThan(Integer value) { addCriterion("is_collection_active <", value, "collectionActive"); return (Criteria) this; }
        public Criteria andCollectionActiveLessThanOrEqualTo(Integer value) { addCriterion("is_collection_active <=", value, "collectionActive"); return (Criteria) this; }
        public Criteria andCollectionActiveIn(List<Integer> values) { addCriterion("is_collection_active in", values, "collectionActive"); return (Criteria) this; }

        public Criteria andDelFlagIsNull() { addCriterion("del_flag is null"); return (Criteria) this; }
        public Criteria andDelFlagIsNotNull() { addCriterion("del_flag is not null"); return (Criteria) this; }
        public Criteria andDelFlagEqualTo(Integer value) { addCriterion("del_flag =", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagNotEqualTo(Integer value) { addCriterion("del_flag <>", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagGreaterThan(Integer value) { addCriterion("del_flag >", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagGreaterThanOrEqualTo(Integer value) { addCriterion("del_flag >=", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagLessThan(Integer value) { addCriterion("del_flag <", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagLessThanOrEqualTo(Integer value) { addCriterion("del_flag <=", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagIn(List<Integer> values) { addCriterion("del_flag in", values, "delFlag"); return (Criteria) this; }

        public Criteria andCreatorIsNull() { addCriterion("creator is null"); return (Criteria) this; }
        public Criteria andCreatorIsNotNull() { addCriterion("creator is not null"); return (Criteria) this; }
        public Criteria andCreatorEqualTo(String value) { addCriterion("creator =", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorNotEqualTo(String value) { addCriterion("creator <>", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorGreaterThan(String value) { addCriterion("creator >", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorGreaterThanOrEqualTo(String value) { addCriterion("creator >=", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorLessThan(String value) { addCriterion("creator <", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorLessThanOrEqualTo(String value) { addCriterion("creator <=", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorIn(List<String> values) { addCriterion("creator in", values, "creator"); return (Criteria) this; }
        public Criteria andCreatorLike(String value) { addCriterion("creator like", value, "creator"); return (Criteria) this; }

        public Criteria andModifierIsNull() { addCriterion("modifier is null"); return (Criteria) this; }
        public Criteria andModifierIsNotNull() { addCriterion("modifier is not null"); return (Criteria) this; }
        public Criteria andModifierEqualTo(String value) { addCriterion("modifier =", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierNotEqualTo(String value) { addCriterion("modifier <>", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierGreaterThan(String value) { addCriterion("modifier >", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierGreaterThanOrEqualTo(String value) { addCriterion("modifier >=", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierLessThan(String value) { addCriterion("modifier <", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierLessThanOrEqualTo(String value) { addCriterion("modifier <=", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierIn(List<String> values) { addCriterion("modifier in", values, "modifier"); return (Criteria) this; }
        public Criteria andModifierLike(String value) { addCriterion("modifier like", value, "modifier"); return (Criteria) this; }

        public Criteria andGmtCreateIsNull() { addCriterion("gmt_create is null"); return (Criteria) this; }
        public Criteria andGmtCreateIsNotNull() { addCriterion("gmt_create is not null"); return (Criteria) this; }
        public Criteria andGmtCreateEqualTo(Date value) { addCriterion("gmt_create =", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateNotEqualTo(Date value) { addCriterion("gmt_create <>", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateGreaterThan(Date value) { addCriterion("gmt_create >", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) { addCriterion("gmt_create >=", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateLessThan(Date value) { addCriterion("gmt_create <", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateLessThanOrEqualTo(Date value) { addCriterion("gmt_create <=", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateIn(List<Date> values) { addCriterion("gmt_create in", values, "gmtCreate"); return (Criteria) this; }

        public Criteria andGmtModifiedIsNull() { addCriterion("gmt_modified is null"); return (Criteria) this; }
        public Criteria andGmtModifiedIsNotNull() { addCriterion("gmt_modified is not null"); return (Criteria) this; }
        public Criteria andGmtModifiedEqualTo(Date value) { addCriterion("gmt_modified =", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedNotEqualTo(Date value) { addCriterion("gmt_modified <>", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedGreaterThan(Date value) { addCriterion("gmt_modified >", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) { addCriterion("gmt_modified >=", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedLessThan(Date value) { addCriterion("gmt_modified <", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) { addCriterion("gmt_modified <=", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedIn(List<Date> values) { addCriterion("gmt_modified in", values, "gmtModified"); return (Criteria) this; }

    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() { super(); }
    }

    public static class Criterion {
        private String condition;
        private Object value;
        private Object secondValue;
        private boolean noValue;
        private boolean singleValue;
        private boolean betweenValue;
        private boolean listValue;
        private String typeHandler;

        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public Object getSecondValue() { return secondValue; }
        public boolean isNoValue() { return noValue; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isBetweenValue() { return betweenValue; }
        public boolean isListValue() { return listValue; }
        public String getTypeHandler() { return typeHandler; }

        protected Criterion(String condition) {
            this.condition = condition;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}
