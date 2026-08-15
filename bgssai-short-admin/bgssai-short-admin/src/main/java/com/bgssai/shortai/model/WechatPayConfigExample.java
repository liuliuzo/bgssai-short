package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WechatPayConfigExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    private String[] selectColumns;

    public WechatPayConfigExample() {
        oredCriteria = new ArrayList<>();
    }

    public WechatPayConfigExample selectColumns(String... cols) {
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

        public Criteria andMchIdIsNull() { addCriterion("mch_id is null"); return (Criteria) this; }
        public Criteria andMchIdIsNotNull() { addCriterion("mch_id is not null"); return (Criteria) this; }
        public Criteria andMchIdEqualTo(String value) { addCriterion("mch_id =", value, "mchId"); return (Criteria) this; }
        public Criteria andMchIdNotEqualTo(String value) { addCriterion("mch_id <>", value, "mchId"); return (Criteria) this; }
        public Criteria andMchIdGreaterThan(String value) { addCriterion("mch_id >", value, "mchId"); return (Criteria) this; }
        public Criteria andMchIdGreaterThanOrEqualTo(String value) { addCriterion("mch_id >=", value, "mchId"); return (Criteria) this; }
        public Criteria andMchIdLessThan(String value) { addCriterion("mch_id <", value, "mchId"); return (Criteria) this; }
        public Criteria andMchIdLessThanOrEqualTo(String value) { addCriterion("mch_id <=", value, "mchId"); return (Criteria) this; }
        public Criteria andMchIdIn(List<String> values) { addCriterion("mch_id in", values, "mchId"); return (Criteria) this; }
        public Criteria andMchIdLike(String value) { addCriterion("mch_id like", value, "mchId"); return (Criteria) this; }

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

        public Criteria andMerchantSerialNoIsNull() { addCriterion("merchant_serial_no is null"); return (Criteria) this; }
        public Criteria andMerchantSerialNoIsNotNull() { addCriterion("merchant_serial_no is not null"); return (Criteria) this; }
        public Criteria andMerchantSerialNoEqualTo(String value) { addCriterion("merchant_serial_no =", value, "merchantSerialNo"); return (Criteria) this; }
        public Criteria andMerchantSerialNoNotEqualTo(String value) { addCriterion("merchant_serial_no <>", value, "merchantSerialNo"); return (Criteria) this; }
        public Criteria andMerchantSerialNoGreaterThan(String value) { addCriterion("merchant_serial_no >", value, "merchantSerialNo"); return (Criteria) this; }
        public Criteria andMerchantSerialNoGreaterThanOrEqualTo(String value) { addCriterion("merchant_serial_no >=", value, "merchantSerialNo"); return (Criteria) this; }
        public Criteria andMerchantSerialNoLessThan(String value) { addCriterion("merchant_serial_no <", value, "merchantSerialNo"); return (Criteria) this; }
        public Criteria andMerchantSerialNoLessThanOrEqualTo(String value) { addCriterion("merchant_serial_no <=", value, "merchantSerialNo"); return (Criteria) this; }
        public Criteria andMerchantSerialNoIn(List<String> values) { addCriterion("merchant_serial_no in", values, "merchantSerialNo"); return (Criteria) this; }
        public Criteria andMerchantSerialNoLike(String value) { addCriterion("merchant_serial_no like", value, "merchantSerialNo"); return (Criteria) this; }

        public Criteria andMchPrivateKeyIsNull() { addCriterion("mch_private_key is null"); return (Criteria) this; }
        public Criteria andMchPrivateKeyIsNotNull() { addCriterion("mch_private_key is not null"); return (Criteria) this; }
        public Criteria andMchPrivateKeyEqualTo(String value) { addCriterion("mch_private_key =", value, "mchPrivateKey"); return (Criteria) this; }
        public Criteria andMchPrivateKeyNotEqualTo(String value) { addCriterion("mch_private_key <>", value, "mchPrivateKey"); return (Criteria) this; }
        public Criteria andMchPrivateKeyGreaterThan(String value) { addCriterion("mch_private_key >", value, "mchPrivateKey"); return (Criteria) this; }
        public Criteria andMchPrivateKeyGreaterThanOrEqualTo(String value) { addCriterion("mch_private_key >=", value, "mchPrivateKey"); return (Criteria) this; }
        public Criteria andMchPrivateKeyLessThan(String value) { addCriterion("mch_private_key <", value, "mchPrivateKey"); return (Criteria) this; }
        public Criteria andMchPrivateKeyLessThanOrEqualTo(String value) { addCriterion("mch_private_key <=", value, "mchPrivateKey"); return (Criteria) this; }
        public Criteria andMchPrivateKeyIn(List<String> values) { addCriterion("mch_private_key in", values, "mchPrivateKey"); return (Criteria) this; }
        public Criteria andMchPrivateKeyLike(String value) { addCriterion("mch_private_key like", value, "mchPrivateKey"); return (Criteria) this; }

        public Criteria andApiclientCertIsNull() { addCriterion("apiclient_cert is null"); return (Criteria) this; }
        public Criteria andApiclientCertIsNotNull() { addCriterion("apiclient_cert is not null"); return (Criteria) this; }
        public Criteria andApiclientCertEqualTo(String value) { addCriterion("apiclient_cert =", value, "apiclientCert"); return (Criteria) this; }
        public Criteria andApiclientCertNotEqualTo(String value) { addCriterion("apiclient_cert <>", value, "apiclientCert"); return (Criteria) this; }
        public Criteria andApiclientCertGreaterThan(String value) { addCriterion("apiclient_cert >", value, "apiclientCert"); return (Criteria) this; }
        public Criteria andApiclientCertGreaterThanOrEqualTo(String value) { addCriterion("apiclient_cert >=", value, "apiclientCert"); return (Criteria) this; }
        public Criteria andApiclientCertLessThan(String value) { addCriterion("apiclient_cert <", value, "apiclientCert"); return (Criteria) this; }
        public Criteria andApiclientCertLessThanOrEqualTo(String value) { addCriterion("apiclient_cert <=", value, "apiclientCert"); return (Criteria) this; }
        public Criteria andApiclientCertIn(List<String> values) { addCriterion("apiclient_cert in", values, "apiclientCert"); return (Criteria) this; }
        public Criteria andApiclientCertLike(String value) { addCriterion("apiclient_cert like", value, "apiclientCert"); return (Criteria) this; }

        public Criteria andMchCertP12IsNull() { addCriterion("mch_cert_p12 is null"); return (Criteria) this; }
        public Criteria andMchCertP12IsNotNull() { addCriterion("mch_cert_p12 is not null"); return (Criteria) this; }
        public Criteria andMchCertP12EqualTo(String value) { addCriterion("mch_cert_p12 =", value, "mchCertP12"); return (Criteria) this; }
        public Criteria andMchCertP12NotEqualTo(String value) { addCriterion("mch_cert_p12 <>", value, "mchCertP12"); return (Criteria) this; }
        public Criteria andMchCertP12GreaterThan(String value) { addCriterion("mch_cert_p12 >", value, "mchCertP12"); return (Criteria) this; }
        public Criteria andMchCertP12GreaterThanOrEqualTo(String value) { addCriterion("mch_cert_p12 >=", value, "mchCertP12"); return (Criteria) this; }
        public Criteria andMchCertP12LessThan(String value) { addCriterion("mch_cert_p12 <", value, "mchCertP12"); return (Criteria) this; }
        public Criteria andMchCertP12LessThanOrEqualTo(String value) { addCriterion("mch_cert_p12 <=", value, "mchCertP12"); return (Criteria) this; }
        public Criteria andMchCertP12In(List<String> values) { addCriterion("mch_cert_p12 in", values, "mchCertP12"); return (Criteria) this; }
        public Criteria andMchCertP12Like(String value) { addCriterion("mch_cert_p12 like", value, "mchCertP12"); return (Criteria) this; }

        public Criteria andWechatPublicKeyIdIsNull() { addCriterion("wechat_public_key_id is null"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdIsNotNull() { addCriterion("wechat_public_key_id is not null"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdEqualTo(String value) { addCriterion("wechat_public_key_id =", value, "wechatPublicKeyId"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdNotEqualTo(String value) { addCriterion("wechat_public_key_id <>", value, "wechatPublicKeyId"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdGreaterThan(String value) { addCriterion("wechat_public_key_id >", value, "wechatPublicKeyId"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdGreaterThanOrEqualTo(String value) { addCriterion("wechat_public_key_id >=", value, "wechatPublicKeyId"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdLessThan(String value) { addCriterion("wechat_public_key_id <", value, "wechatPublicKeyId"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdLessThanOrEqualTo(String value) { addCriterion("wechat_public_key_id <=", value, "wechatPublicKeyId"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdIn(List<String> values) { addCriterion("wechat_public_key_id in", values, "wechatPublicKeyId"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIdLike(String value) { addCriterion("wechat_public_key_id like", value, "wechatPublicKeyId"); return (Criteria) this; }

        public Criteria andWechatPublicKeyIsNull() { addCriterion("wechat_public_key is null"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIsNotNull() { addCriterion("wechat_public_key is not null"); return (Criteria) this; }
        public Criteria andWechatPublicKeyEqualTo(String value) { addCriterion("wechat_public_key =", value, "wechatPublicKey"); return (Criteria) this; }
        public Criteria andWechatPublicKeyNotEqualTo(String value) { addCriterion("wechat_public_key <>", value, "wechatPublicKey"); return (Criteria) this; }
        public Criteria andWechatPublicKeyGreaterThan(String value) { addCriterion("wechat_public_key >", value, "wechatPublicKey"); return (Criteria) this; }
        public Criteria andWechatPublicKeyGreaterThanOrEqualTo(String value) { addCriterion("wechat_public_key >=", value, "wechatPublicKey"); return (Criteria) this; }
        public Criteria andWechatPublicKeyLessThan(String value) { addCriterion("wechat_public_key <", value, "wechatPublicKey"); return (Criteria) this; }
        public Criteria andWechatPublicKeyLessThanOrEqualTo(String value) { addCriterion("wechat_public_key <=", value, "wechatPublicKey"); return (Criteria) this; }
        public Criteria andWechatPublicKeyIn(List<String> values) { addCriterion("wechat_public_key in", values, "wechatPublicKey"); return (Criteria) this; }
        public Criteria andWechatPublicKeyLike(String value) { addCriterion("wechat_public_key like", value, "wechatPublicKey"); return (Criteria) this; }

        public Criteria andPlatformCertSerialNoIsNull() { addCriterion("platform_cert_serial_no is null"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoIsNotNull() { addCriterion("platform_cert_serial_no is not null"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoEqualTo(String value) { addCriterion("platform_cert_serial_no =", value, "platformCertSerialNo"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoNotEqualTo(String value) { addCriterion("platform_cert_serial_no <>", value, "platformCertSerialNo"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoGreaterThan(String value) { addCriterion("platform_cert_serial_no >", value, "platformCertSerialNo"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoGreaterThanOrEqualTo(String value) { addCriterion("platform_cert_serial_no >=", value, "platformCertSerialNo"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoLessThan(String value) { addCriterion("platform_cert_serial_no <", value, "platformCertSerialNo"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoLessThanOrEqualTo(String value) { addCriterion("platform_cert_serial_no <=", value, "platformCertSerialNo"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoIn(List<String> values) { addCriterion("platform_cert_serial_no in", values, "platformCertSerialNo"); return (Criteria) this; }
        public Criteria andPlatformCertSerialNoLike(String value) { addCriterion("platform_cert_serial_no like", value, "platformCertSerialNo"); return (Criteria) this; }

        public Criteria andPlatformCertIsNull() { addCriterion("platform_cert is null"); return (Criteria) this; }
        public Criteria andPlatformCertIsNotNull() { addCriterion("platform_cert is not null"); return (Criteria) this; }
        public Criteria andPlatformCertEqualTo(String value) { addCriterion("platform_cert =", value, "platformCert"); return (Criteria) this; }
        public Criteria andPlatformCertNotEqualTo(String value) { addCriterion("platform_cert <>", value, "platformCert"); return (Criteria) this; }
        public Criteria andPlatformCertGreaterThan(String value) { addCriterion("platform_cert >", value, "platformCert"); return (Criteria) this; }
        public Criteria andPlatformCertGreaterThanOrEqualTo(String value) { addCriterion("platform_cert >=", value, "platformCert"); return (Criteria) this; }
        public Criteria andPlatformCertLessThan(String value) { addCriterion("platform_cert <", value, "platformCert"); return (Criteria) this; }
        public Criteria andPlatformCertLessThanOrEqualTo(String value) { addCriterion("platform_cert <=", value, "platformCert"); return (Criteria) this; }
        public Criteria andPlatformCertIn(List<String> values) { addCriterion("platform_cert in", values, "platformCert"); return (Criteria) this; }
        public Criteria andPlatformCertLike(String value) { addCriterion("platform_cert like", value, "platformCert"); return (Criteria) this; }

        public Criteria andApiV3KeyIsNull() { addCriterion("api_v3_key is null"); return (Criteria) this; }
        public Criteria andApiV3KeyIsNotNull() { addCriterion("api_v3_key is not null"); return (Criteria) this; }
        public Criteria andApiV3KeyEqualTo(String value) { addCriterion("api_v3_key =", value, "apiV3Key"); return (Criteria) this; }
        public Criteria andApiV3KeyNotEqualTo(String value) { addCriterion("api_v3_key <>", value, "apiV3Key"); return (Criteria) this; }
        public Criteria andApiV3KeyGreaterThan(String value) { addCriterion("api_v3_key >", value, "apiV3Key"); return (Criteria) this; }
        public Criteria andApiV3KeyGreaterThanOrEqualTo(String value) { addCriterion("api_v3_key >=", value, "apiV3Key"); return (Criteria) this; }
        public Criteria andApiV3KeyLessThan(String value) { addCriterion("api_v3_key <", value, "apiV3Key"); return (Criteria) this; }
        public Criteria andApiV3KeyLessThanOrEqualTo(String value) { addCriterion("api_v3_key <=", value, "apiV3Key"); return (Criteria) this; }
        public Criteria andApiV3KeyIn(List<String> values) { addCriterion("api_v3_key in", values, "apiV3Key"); return (Criteria) this; }
        public Criteria andApiV3KeyLike(String value) { addCriterion("api_v3_key like", value, "apiV3Key"); return (Criteria) this; }

        public Criteria andApiV2KeyIsNull() { addCriterion("api_v2_key is null"); return (Criteria) this; }
        public Criteria andApiV2KeyIsNotNull() { addCriterion("api_v2_key is not null"); return (Criteria) this; }
        public Criteria andApiV2KeyEqualTo(String value) { addCriterion("api_v2_key =", value, "apiV2Key"); return (Criteria) this; }
        public Criteria andApiV2KeyNotEqualTo(String value) { addCriterion("api_v2_key <>", value, "apiV2Key"); return (Criteria) this; }
        public Criteria andApiV2KeyGreaterThan(String value) { addCriterion("api_v2_key >", value, "apiV2Key"); return (Criteria) this; }
        public Criteria andApiV2KeyGreaterThanOrEqualTo(String value) { addCriterion("api_v2_key >=", value, "apiV2Key"); return (Criteria) this; }
        public Criteria andApiV2KeyLessThan(String value) { addCriterion("api_v2_key <", value, "apiV2Key"); return (Criteria) this; }
        public Criteria andApiV2KeyLessThanOrEqualTo(String value) { addCriterion("api_v2_key <=", value, "apiV2Key"); return (Criteria) this; }
        public Criteria andApiV2KeyIn(List<String> values) { addCriterion("api_v2_key in", values, "apiV2Key"); return (Criteria) this; }
        public Criteria andApiV2KeyLike(String value) { addCriterion("api_v2_key like", value, "apiV2Key"); return (Criteria) this; }

        public Criteria andJsapiAuthDirIsNull() { addCriterion("jsapi_auth_dir is null"); return (Criteria) this; }
        public Criteria andJsapiAuthDirIsNotNull() { addCriterion("jsapi_auth_dir is not null"); return (Criteria) this; }
        public Criteria andJsapiAuthDirEqualTo(String value) { addCriterion("jsapi_auth_dir =", value, "jsapiAuthDir"); return (Criteria) this; }
        public Criteria andJsapiAuthDirNotEqualTo(String value) { addCriterion("jsapi_auth_dir <>", value, "jsapiAuthDir"); return (Criteria) this; }
        public Criteria andJsapiAuthDirGreaterThan(String value) { addCriterion("jsapi_auth_dir >", value, "jsapiAuthDir"); return (Criteria) this; }
        public Criteria andJsapiAuthDirGreaterThanOrEqualTo(String value) { addCriterion("jsapi_auth_dir >=", value, "jsapiAuthDir"); return (Criteria) this; }
        public Criteria andJsapiAuthDirLessThan(String value) { addCriterion("jsapi_auth_dir <", value, "jsapiAuthDir"); return (Criteria) this; }
        public Criteria andJsapiAuthDirLessThanOrEqualTo(String value) { addCriterion("jsapi_auth_dir <=", value, "jsapiAuthDir"); return (Criteria) this; }
        public Criteria andJsapiAuthDirIn(List<String> values) { addCriterion("jsapi_auth_dir in", values, "jsapiAuthDir"); return (Criteria) this; }
        public Criteria andJsapiAuthDirLike(String value) { addCriterion("jsapi_auth_dir like", value, "jsapiAuthDir"); return (Criteria) this; }

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

        public Criteria andDeleteIsNull() { addCriterion("is_delete is null"); return (Criteria) this; }
        public Criteria andDeleteIsNotNull() { addCriterion("is_delete is not null"); return (Criteria) this; }
        public Criteria andDeleteEqualTo(Integer value) { addCriterion("is_delete =", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteNotEqualTo(Integer value) { addCriterion("is_delete <>", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteGreaterThan(Integer value) { addCriterion("is_delete >", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteGreaterThanOrEqualTo(Integer value) { addCriterion("is_delete >=", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteLessThan(Integer value) { addCriterion("is_delete <", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteLessThanOrEqualTo(Integer value) { addCriterion("is_delete <=", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteIn(List<Integer> values) { addCriterion("is_delete in", values, "delete"); return (Criteria) this; }

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
