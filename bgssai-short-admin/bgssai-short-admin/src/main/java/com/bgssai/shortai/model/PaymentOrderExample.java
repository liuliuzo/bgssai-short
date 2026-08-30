package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PaymentOrderExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    private String[] selectColumns;

    public PaymentOrderExample() {
        oredCriteria = new ArrayList<>();
    }

    public PaymentOrderExample selectColumns(String... cols) {
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

        public Criteria andOrderNoIsNull() { addCriterion("order_no is null"); return (Criteria) this; }
        public Criteria andOrderNoIsNotNull() { addCriterion("order_no is not null"); return (Criteria) this; }
        public Criteria andOrderNoEqualTo(String value) { addCriterion("order_no =", value, "orderNo"); return (Criteria) this; }
        public Criteria andOrderNoNotEqualTo(String value) { addCriterion("order_no <>", value, "orderNo"); return (Criteria) this; }
        public Criteria andOrderNoGreaterThan(String value) { addCriterion("order_no >", value, "orderNo"); return (Criteria) this; }
        public Criteria andOrderNoGreaterThanOrEqualTo(String value) { addCriterion("order_no >=", value, "orderNo"); return (Criteria) this; }
        public Criteria andOrderNoLessThan(String value) { addCriterion("order_no <", value, "orderNo"); return (Criteria) this; }
        public Criteria andOrderNoLessThanOrEqualTo(String value) { addCriterion("order_no <=", value, "orderNo"); return (Criteria) this; }
        public Criteria andOrderNoIn(List<String> values) { addCriterion("order_no in", values, "orderNo"); return (Criteria) this; }
        public Criteria andOrderNoLike(String value) { addCriterion("order_no like", value, "orderNo"); return (Criteria) this; }

        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdGreaterThan(String value) { addCriterion("user_id >", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdGreaterThanOrEqualTo(String value) { addCriterion("user_id >=", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLessThan(String value) { addCriterion("user_id <", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLessThanOrEqualTo(String value) { addCriterion("user_id <=", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
        public Criteria andUserIdLike(String value) { addCriterion("user_id like", value, "userId"); return (Criteria) this; }

        public Criteria andOrderTypeIsNull() { addCriterion("order_type is null"); return (Criteria) this; }
        public Criteria andOrderTypeIsNotNull() { addCriterion("order_type is not null"); return (Criteria) this; }
        public Criteria andOrderTypeEqualTo(String value) { addCriterion("order_type =", value, "orderType"); return (Criteria) this; }
        public Criteria andOrderTypeNotEqualTo(String value) { addCriterion("order_type <>", value, "orderType"); return (Criteria) this; }
        public Criteria andOrderTypeGreaterThan(String value) { addCriterion("order_type >", value, "orderType"); return (Criteria) this; }
        public Criteria andOrderTypeGreaterThanOrEqualTo(String value) { addCriterion("order_type >=", value, "orderType"); return (Criteria) this; }
        public Criteria andOrderTypeLessThan(String value) { addCriterion("order_type <", value, "orderType"); return (Criteria) this; }
        public Criteria andOrderTypeLessThanOrEqualTo(String value) { addCriterion("order_type <=", value, "orderType"); return (Criteria) this; }
        public Criteria andOrderTypeIn(List<String> values) { addCriterion("order_type in", values, "orderType"); return (Criteria) this; }
        public Criteria andOrderTypeLike(String value) { addCriterion("order_type like", value, "orderType"); return (Criteria) this; }

        public Criteria andBizCodeIsNull() { addCriterion("biz_code is null"); return (Criteria) this; }
        public Criteria andBizCodeIsNotNull() { addCriterion("biz_code is not null"); return (Criteria) this; }
        public Criteria andBizCodeEqualTo(String value) { addCriterion("biz_code =", value, "bizCode"); return (Criteria) this; }
        public Criteria andBizCodeNotEqualTo(String value) { addCriterion("biz_code <>", value, "bizCode"); return (Criteria) this; }
        public Criteria andBizCodeGreaterThan(String value) { addCriterion("biz_code >", value, "bizCode"); return (Criteria) this; }
        public Criteria andBizCodeGreaterThanOrEqualTo(String value) { addCriterion("biz_code >=", value, "bizCode"); return (Criteria) this; }
        public Criteria andBizCodeLessThan(String value) { addCriterion("biz_code <", value, "bizCode"); return (Criteria) this; }
        public Criteria andBizCodeLessThanOrEqualTo(String value) { addCriterion("biz_code <=", value, "bizCode"); return (Criteria) this; }
        public Criteria andBizCodeIn(List<String> values) { addCriterion("biz_code in", values, "bizCode"); return (Criteria) this; }
        public Criteria andBizCodeLike(String value) { addCriterion("biz_code like", value, "bizCode"); return (Criteria) this; }

        public Criteria andBizNameIsNull() { addCriterion("biz_name is null"); return (Criteria) this; }
        public Criteria andBizNameIsNotNull() { addCriterion("biz_name is not null"); return (Criteria) this; }
        public Criteria andBizNameEqualTo(String value) { addCriterion("biz_name =", value, "bizName"); return (Criteria) this; }
        public Criteria andBizNameNotEqualTo(String value) { addCriterion("biz_name <>", value, "bizName"); return (Criteria) this; }
        public Criteria andBizNameGreaterThan(String value) { addCriterion("biz_name >", value, "bizName"); return (Criteria) this; }
        public Criteria andBizNameGreaterThanOrEqualTo(String value) { addCriterion("biz_name >=", value, "bizName"); return (Criteria) this; }
        public Criteria andBizNameLessThan(String value) { addCriterion("biz_name <", value, "bizName"); return (Criteria) this; }
        public Criteria andBizNameLessThanOrEqualTo(String value) { addCriterion("biz_name <=", value, "bizName"); return (Criteria) this; }
        public Criteria andBizNameIn(List<String> values) { addCriterion("biz_name in", values, "bizName"); return (Criteria) this; }
        public Criteria andBizNameLike(String value) { addCriterion("biz_name like", value, "bizName"); return (Criteria) this; }

        public Criteria andBizRefIdIsNull() { addCriterion("biz_ref_id is null"); return (Criteria) this; }
        public Criteria andBizRefIdIsNotNull() { addCriterion("biz_ref_id is not null"); return (Criteria) this; }
        public Criteria andBizRefIdEqualTo(String value) { addCriterion("biz_ref_id =", value, "bizRefId"); return (Criteria) this; }
        public Criteria andBizRefIdNotEqualTo(String value) { addCriterion("biz_ref_id <>", value, "bizRefId"); return (Criteria) this; }
        public Criteria andBizRefIdGreaterThan(String value) { addCriterion("biz_ref_id >", value, "bizRefId"); return (Criteria) this; }
        public Criteria andBizRefIdGreaterThanOrEqualTo(String value) { addCriterion("biz_ref_id >=", value, "bizRefId"); return (Criteria) this; }
        public Criteria andBizRefIdLessThan(String value) { addCriterion("biz_ref_id <", value, "bizRefId"); return (Criteria) this; }
        public Criteria andBizRefIdLessThanOrEqualTo(String value) { addCriterion("biz_ref_id <=", value, "bizRefId"); return (Criteria) this; }
        public Criteria andBizRefIdIn(List<String> values) { addCriterion("biz_ref_id in", values, "bizRefId"); return (Criteria) this; }
        public Criteria andBizRefIdLike(String value) { addCriterion("biz_ref_id like", value, "bizRefId"); return (Criteria) this; }

        public Criteria andAmountCentIsNull() { addCriterion("amount_cent is null"); return (Criteria) this; }
        public Criteria andAmountCentIsNotNull() { addCriterion("amount_cent is not null"); return (Criteria) this; }
        public Criteria andAmountCentEqualTo(Integer value) { addCriterion("amount_cent =", value, "amountCent"); return (Criteria) this; }
        public Criteria andAmountCentNotEqualTo(Integer value) { addCriterion("amount_cent <>", value, "amountCent"); return (Criteria) this; }
        public Criteria andAmountCentGreaterThan(Integer value) { addCriterion("amount_cent >", value, "amountCent"); return (Criteria) this; }
        public Criteria andAmountCentGreaterThanOrEqualTo(Integer value) { addCriterion("amount_cent >=", value, "amountCent"); return (Criteria) this; }
        public Criteria andAmountCentLessThan(Integer value) { addCriterion("amount_cent <", value, "amountCent"); return (Criteria) this; }
        public Criteria andAmountCentLessThanOrEqualTo(Integer value) { addCriterion("amount_cent <=", value, "amountCent"); return (Criteria) this; }
        public Criteria andAmountCentIn(List<Integer> values) { addCriterion("amount_cent in", values, "amountCent"); return (Criteria) this; }

        public Criteria andGrantCentIsNull() { addCriterion("grant_cent is null"); return (Criteria) this; }
        public Criteria andGrantCentIsNotNull() { addCriterion("grant_cent is not null"); return (Criteria) this; }
        public Criteria andGrantCentEqualTo(Integer value) { addCriterion("grant_cent =", value, "grantCent"); return (Criteria) this; }
        public Criteria andGrantCentNotEqualTo(Integer value) { addCriterion("grant_cent <>", value, "grantCent"); return (Criteria) this; }
        public Criteria andGrantCentGreaterThan(Integer value) { addCriterion("grant_cent >", value, "grantCent"); return (Criteria) this; }
        public Criteria andGrantCentGreaterThanOrEqualTo(Integer value) { addCriterion("grant_cent >=", value, "grantCent"); return (Criteria) this; }
        public Criteria andGrantCentLessThan(Integer value) { addCriterion("grant_cent <", value, "grantCent"); return (Criteria) this; }
        public Criteria andGrantCentLessThanOrEqualTo(Integer value) { addCriterion("grant_cent <=", value, "grantCent"); return (Criteria) this; }
        public Criteria andGrantCentIn(List<Integer> values) { addCriterion("grant_cent in", values, "grantCent"); return (Criteria) this; }

        public Criteria andPaymentChannelIsNull() { addCriterion("payment_channel is null"); return (Criteria) this; }
        public Criteria andPaymentChannelIsNotNull() { addCriterion("payment_channel is not null"); return (Criteria) this; }
        public Criteria andPaymentChannelEqualTo(String value) { addCriterion("payment_channel =", value, "paymentChannel"); return (Criteria) this; }
        public Criteria andPaymentChannelNotEqualTo(String value) { addCriterion("payment_channel <>", value, "paymentChannel"); return (Criteria) this; }
        public Criteria andPaymentChannelGreaterThan(String value) { addCriterion("payment_channel >", value, "paymentChannel"); return (Criteria) this; }
        public Criteria andPaymentChannelGreaterThanOrEqualTo(String value) { addCriterion("payment_channel >=", value, "paymentChannel"); return (Criteria) this; }
        public Criteria andPaymentChannelLessThan(String value) { addCriterion("payment_channel <", value, "paymentChannel"); return (Criteria) this; }
        public Criteria andPaymentChannelLessThanOrEqualTo(String value) { addCriterion("payment_channel <=", value, "paymentChannel"); return (Criteria) this; }
        public Criteria andPaymentChannelIn(List<String> values) { addCriterion("payment_channel in", values, "paymentChannel"); return (Criteria) this; }
        public Criteria andPaymentChannelLike(String value) { addCriterion("payment_channel like", value, "paymentChannel"); return (Criteria) this; }

        public Criteria andPayMerchantNoIsNull() { addCriterion("pay_merchant_no is null"); return (Criteria) this; }
        public Criteria andPayMerchantNoIsNotNull() { addCriterion("pay_merchant_no is not null"); return (Criteria) this; }
        public Criteria andPayMerchantNoEqualTo(String value) { addCriterion("pay_merchant_no =", value, "payMerchantNo"); return (Criteria) this; }
        public Criteria andPayMerchantNoNotEqualTo(String value) { addCriterion("pay_merchant_no <>", value, "payMerchantNo"); return (Criteria) this; }
        public Criteria andPayMerchantNoGreaterThan(String value) { addCriterion("pay_merchant_no >", value, "payMerchantNo"); return (Criteria) this; }
        public Criteria andPayMerchantNoGreaterThanOrEqualTo(String value) { addCriterion("pay_merchant_no >=", value, "payMerchantNo"); return (Criteria) this; }
        public Criteria andPayMerchantNoLessThan(String value) { addCriterion("pay_merchant_no <", value, "payMerchantNo"); return (Criteria) this; }
        public Criteria andPayMerchantNoLessThanOrEqualTo(String value) { addCriterion("pay_merchant_no <=", value, "payMerchantNo"); return (Criteria) this; }
        public Criteria andPayMerchantNoIn(List<String> values) { addCriterion("pay_merchant_no in", values, "payMerchantNo"); return (Criteria) this; }
        public Criteria andPayMerchantNoLike(String value) { addCriterion("pay_merchant_no like", value, "payMerchantNo"); return (Criteria) this; }

        public Criteria andStatusIsNull() { addCriterion("status is null"); return (Criteria) this; }
        public Criteria andStatusIsNotNull() { addCriterion("status is not null"); return (Criteria) this; }
        public Criteria andStatusEqualTo(String value) { addCriterion("status =", value, "status"); return (Criteria) this; }
        public Criteria andStatusNotEqualTo(String value) { addCriterion("status <>", value, "status"); return (Criteria) this; }
        public Criteria andStatusGreaterThan(String value) { addCriterion("status >", value, "status"); return (Criteria) this; }
        public Criteria andStatusGreaterThanOrEqualTo(String value) { addCriterion("status >=", value, "status"); return (Criteria) this; }
        public Criteria andStatusLessThan(String value) { addCriterion("status <", value, "status"); return (Criteria) this; }
        public Criteria andStatusLessThanOrEqualTo(String value) { addCriterion("status <=", value, "status"); return (Criteria) this; }
        public Criteria andStatusIn(List<String> values) { addCriterion("status in", values, "status"); return (Criteria) this; }
        public Criteria andStatusLike(String value) { addCriterion("status like", value, "status"); return (Criteria) this; }

        public Criteria andCodeUrlIsNull() { addCriterion("code_url is null"); return (Criteria) this; }
        public Criteria andCodeUrlIsNotNull() { addCriterion("code_url is not null"); return (Criteria) this; }
        public Criteria andCodeUrlEqualTo(String value) { addCriterion("code_url =", value, "codeUrl"); return (Criteria) this; }
        public Criteria andCodeUrlNotEqualTo(String value) { addCriterion("code_url <>", value, "codeUrl"); return (Criteria) this; }
        public Criteria andCodeUrlGreaterThan(String value) { addCriterion("code_url >", value, "codeUrl"); return (Criteria) this; }
        public Criteria andCodeUrlGreaterThanOrEqualTo(String value) { addCriterion("code_url >=", value, "codeUrl"); return (Criteria) this; }
        public Criteria andCodeUrlLessThan(String value) { addCriterion("code_url <", value, "codeUrl"); return (Criteria) this; }
        public Criteria andCodeUrlLessThanOrEqualTo(String value) { addCriterion("code_url <=", value, "codeUrl"); return (Criteria) this; }
        public Criteria andCodeUrlIn(List<String> values) { addCriterion("code_url in", values, "codeUrl"); return (Criteria) this; }
        public Criteria andCodeUrlLike(String value) { addCriterion("code_url like", value, "codeUrl"); return (Criteria) this; }

        public Criteria andPayFormIsNull() { addCriterion("pay_form is null"); return (Criteria) this; }
        public Criteria andPayFormIsNotNull() { addCriterion("pay_form is not null"); return (Criteria) this; }
        public Criteria andPayFormEqualTo(String value) { addCriterion("pay_form =", value, "payForm"); return (Criteria) this; }
        public Criteria andPayFormNotEqualTo(String value) { addCriterion("pay_form <>", value, "payForm"); return (Criteria) this; }
        public Criteria andPayFormGreaterThan(String value) { addCriterion("pay_form >", value, "payForm"); return (Criteria) this; }
        public Criteria andPayFormGreaterThanOrEqualTo(String value) { addCriterion("pay_form >=", value, "payForm"); return (Criteria) this; }
        public Criteria andPayFormLessThan(String value) { addCriterion("pay_form <", value, "payForm"); return (Criteria) this; }
        public Criteria andPayFormLessThanOrEqualTo(String value) { addCriterion("pay_form <=", value, "payForm"); return (Criteria) this; }
        public Criteria andPayFormIn(List<String> values) { addCriterion("pay_form in", values, "payForm"); return (Criteria) this; }
        public Criteria andPayFormLike(String value) { addCriterion("pay_form like", value, "payForm"); return (Criteria) this; }

        public Criteria andChannelTradeNoIsNull() { addCriterion("channel_trade_no is null"); return (Criteria) this; }
        public Criteria andChannelTradeNoIsNotNull() { addCriterion("channel_trade_no is not null"); return (Criteria) this; }
        public Criteria andChannelTradeNoEqualTo(String value) { addCriterion("channel_trade_no =", value, "channelTradeNo"); return (Criteria) this; }
        public Criteria andChannelTradeNoNotEqualTo(String value) { addCriterion("channel_trade_no <>", value, "channelTradeNo"); return (Criteria) this; }
        public Criteria andChannelTradeNoGreaterThan(String value) { addCriterion("channel_trade_no >", value, "channelTradeNo"); return (Criteria) this; }
        public Criteria andChannelTradeNoGreaterThanOrEqualTo(String value) { addCriterion("channel_trade_no >=", value, "channelTradeNo"); return (Criteria) this; }
        public Criteria andChannelTradeNoLessThan(String value) { addCriterion("channel_trade_no <", value, "channelTradeNo"); return (Criteria) this; }
        public Criteria andChannelTradeNoLessThanOrEqualTo(String value) { addCriterion("channel_trade_no <=", value, "channelTradeNo"); return (Criteria) this; }
        public Criteria andChannelTradeNoIn(List<String> values) { addCriterion("channel_trade_no in", values, "channelTradeNo"); return (Criteria) this; }
        public Criteria andChannelTradeNoLike(String value) { addCriterion("channel_trade_no like", value, "channelTradeNo"); return (Criteria) this; }

        public Criteria andPaidAtIsNull() { addCriterion("paid_at is null"); return (Criteria) this; }
        public Criteria andPaidAtIsNotNull() { addCriterion("paid_at is not null"); return (Criteria) this; }
        public Criteria andPaidAtEqualTo(Date value) { addCriterion("paid_at =", value, "paidAt"); return (Criteria) this; }
        public Criteria andPaidAtNotEqualTo(Date value) { addCriterion("paid_at <>", value, "paidAt"); return (Criteria) this; }
        public Criteria andPaidAtGreaterThan(Date value) { addCriterion("paid_at >", value, "paidAt"); return (Criteria) this; }
        public Criteria andPaidAtGreaterThanOrEqualTo(Date value) { addCriterion("paid_at >=", value, "paidAt"); return (Criteria) this; }
        public Criteria andPaidAtLessThan(Date value) { addCriterion("paid_at <", value, "paidAt"); return (Criteria) this; }
        public Criteria andPaidAtLessThanOrEqualTo(Date value) { addCriterion("paid_at <=", value, "paidAt"); return (Criteria) this; }
        public Criteria andPaidAtIn(List<Date> values) { addCriterion("paid_at in", values, "paidAt"); return (Criteria) this; }

        public Criteria andExpiresAtIsNull() { addCriterion("expires_at is null"); return (Criteria) this; }
        public Criteria andExpiresAtIsNotNull() { addCriterion("expires_at is not null"); return (Criteria) this; }
        public Criteria andExpiresAtEqualTo(Date value) { addCriterion("expires_at =", value, "expiresAt"); return (Criteria) this; }
        public Criteria andExpiresAtNotEqualTo(Date value) { addCriterion("expires_at <>", value, "expiresAt"); return (Criteria) this; }
        public Criteria andExpiresAtGreaterThan(Date value) { addCriterion("expires_at >", value, "expiresAt"); return (Criteria) this; }
        public Criteria andExpiresAtGreaterThanOrEqualTo(Date value) { addCriterion("expires_at >=", value, "expiresAt"); return (Criteria) this; }
        public Criteria andExpiresAtLessThan(Date value) { addCriterion("expires_at <", value, "expiresAt"); return (Criteria) this; }
        public Criteria andExpiresAtLessThanOrEqualTo(Date value) { addCriterion("expires_at <=", value, "expiresAt"); return (Criteria) this; }
        public Criteria andExpiresAtIn(List<Date> values) { addCriterion("expires_at in", values, "expiresAt"); return (Criteria) this; }

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
