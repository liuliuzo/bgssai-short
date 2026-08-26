package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class RechargeApplyExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public RechargeApplyExample() { oredCriteria = new ArrayList<>(); }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
    public boolean isDistinct() { return distinct; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }
    public void or(Criteria criteria) { oredCriteria.add(criteria); }
    public Criteria or() { Criteria criteria = createCriteriaInternal(); oredCriteria.add(criteria); return criteria; }
    public Criteria createCriteria() { Criteria criteria = createCriteriaInternal(); if (oredCriteria.size() == 0) { oredCriteria.add(criteria); } return criteria; }
    protected Criteria createCriteriaInternal() { return new Criteria(); }
    public void clear() { oredCriteria.clear(); orderByClause = null; distinct = false; }
    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;
        protected GeneratedCriteria() { criteria = new ArrayList<>(); }
        public boolean isValid() { return criteria.size() > 0; }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }
        protected void addCriterion(String condition) { if (condition == null) { throw new RuntimeException("Value for condition cannot be null"); } criteria.add(new Criterion(condition)); }
        protected void addCriterion(String condition, Object value, String property) { if (value == null) { throw new RuntimeException("Value for " + property + " cannot be null"); } criteria.add(new Criterion(condition, value)); }
        protected void addCriterion(String condition, Object value1, Object value2, String property) { if (value1 == null || value2 == null) { throw new RuntimeException("Between values for " + property + " cannot be null"); } criteria.add(new Criterion(condition, value1, value2)); }
        public Criteria andIdIsNull() { addCriterion("id is null"); return (Criteria) this; }
        public Criteria andIdIsNotNull() { addCriterion("id is not null"); return (Criteria) this; }
        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return (Criteria) this; }
        public Criteria andIdNotEqualTo(Long value) { addCriterion("id <>", value, "id"); return (Criteria) this; }
        public Criteria andIdGreaterThan(Long value) { addCriterion("id >", value, "id"); return (Criteria) this; }
        public Criteria andIdLessThan(Long value) { addCriterion("id <", value, "id"); return (Criteria) this; }
        public Criteria andIdIn(List<Long> values) { addCriterion("id in", values, "id"); return (Criteria) this; }
        public Criteria andApplyIdIsNull() { addCriterion("apply_id is null"); return (Criteria) this; }
        public Criteria andApplyIdIsNotNull() { addCriterion("apply_id is not null"); return (Criteria) this; }
        public Criteria andApplyIdEqualTo(String value) { addCriterion("apply_id =", value, "applyId"); return (Criteria) this; }
        public Criteria andApplyIdNotEqualTo(String value) { addCriterion("apply_id <>", value, "applyId"); return (Criteria) this; }
        public Criteria andApplyIdGreaterThan(String value) { addCriterion("apply_id >", value, "applyId"); return (Criteria) this; }
        public Criteria andApplyIdLessThan(String value) { addCriterion("apply_id <", value, "applyId"); return (Criteria) this; }
        public Criteria andApplyIdLike(String value) { addCriterion("apply_id like", value, "applyId"); return (Criteria) this; }
        public Criteria andApplyIdIn(List<String> values) { addCriterion("apply_id in", values, "applyId"); return (Criteria) this; }
        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdGreaterThan(String value) { addCriterion("user_id >", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLessThan(String value) { addCriterion("user_id <", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLike(String value) { addCriterion("user_id like", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
        public Criteria andApplyAmountCentIsNull() { addCriterion("apply_amount_cent is null"); return (Criteria) this; }
        public Criteria andApplyAmountCentIsNotNull() { addCriterion("apply_amount_cent is not null"); return (Criteria) this; }
        public Criteria andApplyAmountCentEqualTo(Integer value) { addCriterion("apply_amount_cent =", value, "applyAmountCent"); return (Criteria) this; }
        public Criteria andApplyAmountCentNotEqualTo(Integer value) { addCriterion("apply_amount_cent <>", value, "applyAmountCent"); return (Criteria) this; }
        public Criteria andApplyAmountCentGreaterThan(Integer value) { addCriterion("apply_amount_cent >", value, "applyAmountCent"); return (Criteria) this; }
        public Criteria andApplyAmountCentLessThan(Integer value) { addCriterion("apply_amount_cent <", value, "applyAmountCent"); return (Criteria) this; }
        public Criteria andApplyAmountCentIn(List<Integer> values) { addCriterion("apply_amount_cent in", values, "applyAmountCent"); return (Criteria) this; }
        public Criteria andApplyStatusIsNull() { addCriterion("apply_status is null"); return (Criteria) this; }
        public Criteria andApplyStatusIsNotNull() { addCriterion("apply_status is not null"); return (Criteria) this; }
        public Criteria andApplyStatusEqualTo(String value) { addCriterion("apply_status =", value, "applyStatus"); return (Criteria) this; }
        public Criteria andApplyStatusNotEqualTo(String value) { addCriterion("apply_status <>", value, "applyStatus"); return (Criteria) this; }
        public Criteria andApplyStatusGreaterThan(String value) { addCriterion("apply_status >", value, "applyStatus"); return (Criteria) this; }
        public Criteria andApplyStatusLessThan(String value) { addCriterion("apply_status <", value, "applyStatus"); return (Criteria) this; }
        public Criteria andApplyStatusLike(String value) { addCriterion("apply_status like", value, "applyStatus"); return (Criteria) this; }
        public Criteria andApplyStatusIn(List<String> values) { addCriterion("apply_status in", values, "applyStatus"); return (Criteria) this; }
        public Criteria andUserRemarkIsNull() { addCriterion("user_remark is null"); return (Criteria) this; }
        public Criteria andUserRemarkIsNotNull() { addCriterion("user_remark is not null"); return (Criteria) this; }
        public Criteria andUserRemarkEqualTo(String value) { addCriterion("user_remark =", value, "userRemark"); return (Criteria) this; }
        public Criteria andUserRemarkNotEqualTo(String value) { addCriterion("user_remark <>", value, "userRemark"); return (Criteria) this; }
        public Criteria andUserRemarkGreaterThan(String value) { addCriterion("user_remark >", value, "userRemark"); return (Criteria) this; }
        public Criteria andUserRemarkLessThan(String value) { addCriterion("user_remark <", value, "userRemark"); return (Criteria) this; }
        public Criteria andUserRemarkLike(String value) { addCriterion("user_remark like", value, "userRemark"); return (Criteria) this; }
        public Criteria andUserRemarkIn(List<String> values) { addCriterion("user_remark in", values, "userRemark"); return (Criteria) this; }
        public Criteria andOperatorUserIdIsNull() { addCriterion("operator_user_id is null"); return (Criteria) this; }
        public Criteria andOperatorUserIdIsNotNull() { addCriterion("operator_user_id is not null"); return (Criteria) this; }
        public Criteria andOperatorUserIdEqualTo(String value) { addCriterion("operator_user_id =", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdNotEqualTo(String value) { addCriterion("operator_user_id <>", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdGreaterThan(String value) { addCriterion("operator_user_id >", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdLessThan(String value) { addCriterion("operator_user_id <", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdLike(String value) { addCriterion("operator_user_id like", value, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorUserIdIn(List<String> values) { addCriterion("operator_user_id in", values, "operatorUserId"); return (Criteria) this; }
        public Criteria andOperatorRemarkIsNull() { addCriterion("operator_remark is null"); return (Criteria) this; }
        public Criteria andOperatorRemarkIsNotNull() { addCriterion("operator_remark is not null"); return (Criteria) this; }
        public Criteria andOperatorRemarkEqualTo(String value) { addCriterion("operator_remark =", value, "operatorRemark"); return (Criteria) this; }
        public Criteria andOperatorRemarkNotEqualTo(String value) { addCriterion("operator_remark <>", value, "operatorRemark"); return (Criteria) this; }
        public Criteria andOperatorRemarkGreaterThan(String value) { addCriterion("operator_remark >", value, "operatorRemark"); return (Criteria) this; }
        public Criteria andOperatorRemarkLessThan(String value) { addCriterion("operator_remark <", value, "operatorRemark"); return (Criteria) this; }
        public Criteria andOperatorRemarkLike(String value) { addCriterion("operator_remark like", value, "operatorRemark"); return (Criteria) this; }
        public Criteria andOperatorRemarkIn(List<String> values) { addCriterion("operator_remark in", values, "operatorRemark"); return (Criteria) this; }
        public Criteria andHandledTimeIsNull() { addCriterion("handled_time is null"); return (Criteria) this; }
        public Criteria andHandledTimeIsNotNull() { addCriterion("handled_time is not null"); return (Criteria) this; }
        public Criteria andHandledTimeEqualTo(Date value) { addCriterion("handled_time =", value, "handledTime"); return (Criteria) this; }
        public Criteria andHandledTimeNotEqualTo(Date value) { addCriterion("handled_time <>", value, "handledTime"); return (Criteria) this; }
        public Criteria andHandledTimeGreaterThan(Date value) { addCriterion("handled_time >", value, "handledTime"); return (Criteria) this; }
        public Criteria andHandledTimeLessThan(Date value) { addCriterion("handled_time <", value, "handledTime"); return (Criteria) this; }
        public Criteria andHandledTimeIn(List<Date> values) { addCriterion("handled_time in", values, "handledTime"); return (Criteria) this; }
        public Criteria andCreatedTimeIsNull() { addCriterion("created_time is null"); return (Criteria) this; }
        public Criteria andCreatedTimeIsNotNull() { addCriterion("created_time is not null"); return (Criteria) this; }
        public Criteria andCreatedTimeEqualTo(Date value) { addCriterion("created_time =", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeNotEqualTo(Date value) { addCriterion("created_time <>", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeGreaterThan(Date value) { addCriterion("created_time >", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeLessThan(Date value) { addCriterion("created_time <", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeIn(List<Date> values) { addCriterion("created_time in", values, "createdTime"); return (Criteria) this; }
        public Criteria andUpdatedTimeIsNull() { addCriterion("updated_time is null"); return (Criteria) this; }
        public Criteria andUpdatedTimeIsNotNull() { addCriterion("updated_time is not null"); return (Criteria) this; }
        public Criteria andUpdatedTimeEqualTo(Date value) { addCriterion("updated_time =", value, "updatedTime"); return (Criteria) this; }
        public Criteria andUpdatedTimeNotEqualTo(Date value) { addCriterion("updated_time <>", value, "updatedTime"); return (Criteria) this; }
        public Criteria andUpdatedTimeGreaterThan(Date value) { addCriterion("updated_time >", value, "updatedTime"); return (Criteria) this; }
        public Criteria andUpdatedTimeLessThan(Date value) { addCriterion("updated_time <", value, "updatedTime"); return (Criteria) this; }
        public Criteria andUpdatedTimeIn(List<Date> values) { addCriterion("updated_time in", values, "updatedTime"); return (Criteria) this; }
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
            this.typeHandler = null;
            this.noValue = true;
        }
        protected Criterion(String condition, Object value, String typeHandler) {
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof java.util.List<?>) { this.listValue = true; } else { this.singleValue = true; }
        }
        protected Criterion(String condition, Object value) { this(condition, value, null); }
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

