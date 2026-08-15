package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuotaChangeLogExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    private String[] selectColumns;

    public QuotaChangeLogExample() {
        oredCriteria = new ArrayList<>();
    }

    public QuotaChangeLogExample selectColumns(String... cols) {
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

        public Criteria andOperatorUserIdIsNull() { addCriterion("operator_user_id is null"); return (Criteria) this; }
        public Criteria andOperatorUserIdIsNotNull() { addCriterion("operator_user_id is not null"); return (Criteria) this; }
        public Criteria andOperatorUserIdEqualTo(String value) { addCriterion("operator_user_id =", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdNotEqualTo(String value) { addCriterion("operator_user_id <>", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdGreaterThan(String value) { addCriterion("operator_user_id >", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdGreaterThanOrEqualTo(String value) { addCriterion("operator_user_id >=", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdLessThan(String value) { addCriterion("operator_user_id <", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdLessThanOrEqualTo(String value) { addCriterion("operator_user_id <=", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdIn(List<String> values) { addCriterion("operator_user_id in", values, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdLike(String value) { addCriterion("operator_user_id like", value, "operatorUserId"); return (Criteria) this; }

        public Criteria andChangeTypeIsNull() { addCriterion("change_type is null"); return (Criteria) this; }
        public Criteria andChangeTypeIsNotNull() { addCriterion("change_type is not null"); return (Criteria) this; }
        public Criteria andChangeTypeEqualTo(String value) { addCriterion("change_type =", value, "changeType"); return (Criteria) this; }
        public Criteria andChangeTypeNotEqualTo(String value) { addCriterion("change_type <>", value, "changeType"); return (Criteria) this; }
        public Criteria andChangeTypeGreaterThan(String value) { addCriterion("change_type >", value, "changeType"); return (Criteria) this; }
        public Criteria andChangeTypeGreaterThanOrEqualTo(String value) { addCriterion("change_type >=", value, "changeType"); return (Criteria) this; }
        public Criteria andChangeTypeLessThan(String value) { addCriterion("change_type <", value, "changeType"); return (Criteria) this; }
        public Criteria andChangeTypeLessThanOrEqualTo(String value) { addCriterion("change_type <=", value, "changeType"); return (Criteria) this; }
        public Criteria andChangeTypeIn(List<String> values) { addCriterion("change_type in", values, "changeType"); return (Criteria) this; }
        public Criteria andChangeTypeLike(String value) { addCriterion("change_type like", value, "changeType"); return (Criteria) this; }

        public Criteria andBalanceDeltaCentIsNull() { addCriterion("balance_delta_cent is null"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentIsNotNull() { addCriterion("balance_delta_cent is not null"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentEqualTo(Integer value) { addCriterion("balance_delta_cent =", value, "balanceDeltaCent"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentNotEqualTo(Integer value) { addCriterion("balance_delta_cent <>", value, "balanceDeltaCent"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentGreaterThan(Integer value) { addCriterion("balance_delta_cent >", value, "balanceDeltaCent"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentGreaterThanOrEqualTo(Integer value) { addCriterion("balance_delta_cent >=", value, "balanceDeltaCent"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentLessThan(Integer value) { addCriterion("balance_delta_cent <", value, "balanceDeltaCent"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentLessThanOrEqualTo(Integer value) { addCriterion("balance_delta_cent <=", value, "balanceDeltaCent"); return (Criteria) this; }
        public Criteria andBalanceDeltaCentIn(List<Integer> values) { addCriterion("balance_delta_cent in", values, "balanceDeltaCent"); return (Criteria) this; }

        public Criteria andExtraDeltaCentIsNull() { addCriterion("extra_delta_cent is null"); return (Criteria) this; }
        public Criteria andExtraDeltaCentIsNotNull() { addCriterion("extra_delta_cent is not null"); return (Criteria) this; }
        public Criteria andExtraDeltaCentEqualTo(Integer value) { addCriterion("extra_delta_cent =", value, "extraDeltaCent"); return (Criteria) this; }
        public Criteria andExtraDeltaCentNotEqualTo(Integer value) { addCriterion("extra_delta_cent <>", value, "extraDeltaCent"); return (Criteria) this; }
        public Criteria andExtraDeltaCentGreaterThan(Integer value) { addCriterion("extra_delta_cent >", value, "extraDeltaCent"); return (Criteria) this; }
        public Criteria andExtraDeltaCentGreaterThanOrEqualTo(Integer value) { addCriterion("extra_delta_cent >=", value, "extraDeltaCent"); return (Criteria) this; }
        public Criteria andExtraDeltaCentLessThan(Integer value) { addCriterion("extra_delta_cent <", value, "extraDeltaCent"); return (Criteria) this; }
        public Criteria andExtraDeltaCentLessThanOrEqualTo(Integer value) { addCriterion("extra_delta_cent <=", value, "extraDeltaCent"); return (Criteria) this; }
        public Criteria andExtraDeltaCentIn(List<Integer> values) { addCriterion("extra_delta_cent in", values, "extraDeltaCent"); return (Criteria) this; }

        public Criteria andBalanceAfterCentIsNull() { addCriterion("balance_after_cent is null"); return (Criteria) this; }
        public Criteria andBalanceAfterCentIsNotNull() { addCriterion("balance_after_cent is not null"); return (Criteria) this; }
        public Criteria andBalanceAfterCentEqualTo(Integer value) { addCriterion("balance_after_cent =", value, "balanceAfterCent"); return (Criteria) this; }
        public Criteria andBalanceAfterCentNotEqualTo(Integer value) { addCriterion("balance_after_cent <>", value, "balanceAfterCent"); return (Criteria) this; }
        public Criteria andBalanceAfterCentGreaterThan(Integer value) { addCriterion("balance_after_cent >", value, "balanceAfterCent"); return (Criteria) this; }
        public Criteria andBalanceAfterCentGreaterThanOrEqualTo(Integer value) { addCriterion("balance_after_cent >=", value, "balanceAfterCent"); return (Criteria) this; }
        public Criteria andBalanceAfterCentLessThan(Integer value) { addCriterion("balance_after_cent <", value, "balanceAfterCent"); return (Criteria) this; }
        public Criteria andBalanceAfterCentLessThanOrEqualTo(Integer value) { addCriterion("balance_after_cent <=", value, "balanceAfterCent"); return (Criteria) this; }
        public Criteria andBalanceAfterCentIn(List<Integer> values) { addCriterion("balance_after_cent in", values, "balanceAfterCent"); return (Criteria) this; }

        public Criteria andExtraAfterCentIsNull() { addCriterion("extra_after_cent is null"); return (Criteria) this; }
        public Criteria andExtraAfterCentIsNotNull() { addCriterion("extra_after_cent is not null"); return (Criteria) this; }
        public Criteria andExtraAfterCentEqualTo(Integer value) { addCriterion("extra_after_cent =", value, "extraAfterCent"); return (Criteria) this; }
        public Criteria andExtraAfterCentNotEqualTo(Integer value) { addCriterion("extra_after_cent <>", value, "extraAfterCent"); return (Criteria) this; }
        public Criteria andExtraAfterCentGreaterThan(Integer value) { addCriterion("extra_after_cent >", value, "extraAfterCent"); return (Criteria) this; }
        public Criteria andExtraAfterCentGreaterThanOrEqualTo(Integer value) { addCriterion("extra_after_cent >=", value, "extraAfterCent"); return (Criteria) this; }
        public Criteria andExtraAfterCentLessThan(Integer value) { addCriterion("extra_after_cent <", value, "extraAfterCent"); return (Criteria) this; }
        public Criteria andExtraAfterCentLessThanOrEqualTo(Integer value) { addCriterion("extra_after_cent <=", value, "extraAfterCent"); return (Criteria) this; }
        public Criteria andExtraAfterCentIn(List<Integer> values) { addCriterion("extra_after_cent in", values, "extraAfterCent"); return (Criteria) this; }

        public Criteria andMonthlyLimitCentIsNull() { addCriterion("monthly_limit_cent is null"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentIsNotNull() { addCriterion("monthly_limit_cent is not null"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentEqualTo(Integer value) { addCriterion("monthly_limit_cent =", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentNotEqualTo(Integer value) { addCriterion("monthly_limit_cent <>", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentGreaterThan(Integer value) { addCriterion("monthly_limit_cent >", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentGreaterThanOrEqualTo(Integer value) { addCriterion("monthly_limit_cent >=", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentLessThan(Integer value) { addCriterion("monthly_limit_cent <", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentLessThanOrEqualTo(Integer value) { addCriterion("monthly_limit_cent <=", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentIn(List<Integer> values) { addCriterion("monthly_limit_cent in", values, "monthlyLimitCent"); return (Criteria) this; }

        public Criteria andRemarkIsNull() { addCriterion("remark is null"); return (Criteria) this; }
        public Criteria andRemarkIsNotNull() { addCriterion("remark is not null"); return (Criteria) this; }
        public Criteria andRemarkEqualTo(String value) { addCriterion("remark =", value, "remark"); return (Criteria) this; }
        public Criteria andRemarkNotEqualTo(String value) { addCriterion("remark <>", value, "remark"); return (Criteria) this; }
        public Criteria andRemarkGreaterThan(String value) { addCriterion("remark >", value, "remark"); return (Criteria) this; }
        public Criteria andRemarkGreaterThanOrEqualTo(String value) { addCriterion("remark >=", value, "remark"); return (Criteria) this; }
        public Criteria andRemarkLessThan(String value) { addCriterion("remark <", value, "remark"); return (Criteria) this; }
        public Criteria andRemarkLessThanOrEqualTo(String value) { addCriterion("remark <=", value, "remark"); return (Criteria) this; }
        public Criteria andRemarkIn(List<String> values) { addCriterion("remark in", values, "remark"); return (Criteria) this; }
        public Criteria andRemarkLike(String value) { addCriterion("remark like", value, "remark"); return (Criteria) this; }

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
