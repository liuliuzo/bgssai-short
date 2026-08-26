package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortQualityThresholdLogExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortQualityThresholdLogExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andLogIdIsNull() { addCriterion("log_id is null"); return (Criteria) this; }
        public Criteria andLogIdIsNotNull() { addCriterion("log_id is not null"); return (Criteria) this; }
        public Criteria andLogIdEqualTo(String value) { addCriterion("log_id =", value, "logId"); return (Criteria) this; }
        public Criteria andLogIdNotEqualTo(String value) { addCriterion("log_id <>", value, "logId"); return (Criteria) this; }
        public Criteria andLogIdGreaterThan(String value) { addCriterion("log_id >", value, "logId"); return (Criteria) this; }
        public Criteria andLogIdLessThan(String value) { addCriterion("log_id <", value, "logId"); return (Criteria) this; }
        public Criteria andLogIdLike(String value) { addCriterion("log_id like", value, "logId"); return (Criteria) this; }
        public Criteria andLogIdIn(List<String> values) { addCriterion("log_id in", values, "logId"); return (Criteria) this; }
        public Criteria andWorkIdIsNull() { addCriterion("work_id is null"); return (Criteria) this; }
        public Criteria andWorkIdIsNotNull() { addCriterion("work_id is not null"); return (Criteria) this; }
        public Criteria andWorkIdEqualTo(String value) { addCriterion("work_id =", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdNotEqualTo(String value) { addCriterion("work_id <>", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdGreaterThan(String value) { addCriterion("work_id >", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdLessThan(String value) { addCriterion("work_id <", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdLike(String value) { addCriterion("work_id like", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdIn(List<String> values) { addCriterion("work_id in", values, "workId"); return (Criteria) this; }
        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdGreaterThan(String value) { addCriterion("user_id >", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLessThan(String value) { addCriterion("user_id <", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLike(String value) { addCriterion("user_id like", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
        public Criteria andOldThresholdScoreIsNull() { addCriterion("old_threshold_score is null"); return (Criteria) this; }
        public Criteria andOldThresholdScoreIsNotNull() { addCriterion("old_threshold_score is not null"); return (Criteria) this; }
        public Criteria andOldThresholdScoreEqualTo(Integer value) { addCriterion("old_threshold_score =", value, "oldThresholdScore"); return (Criteria) this; }
        public Criteria andOldThresholdScoreNotEqualTo(Integer value) { addCriterion("old_threshold_score <>", value, "oldThresholdScore"); return (Criteria) this; }
        public Criteria andOldThresholdScoreGreaterThan(Integer value) { addCriterion("old_threshold_score >", value, "oldThresholdScore"); return (Criteria) this; }
        public Criteria andOldThresholdScoreLessThan(Integer value) { addCriterion("old_threshold_score <", value, "oldThresholdScore"); return (Criteria) this; }
        public Criteria andOldThresholdScoreIn(List<Integer> values) { addCriterion("old_threshold_score in", values, "oldThresholdScore"); return (Criteria) this; }
        public Criteria andNewThresholdScoreIsNull() { addCriterion("new_threshold_score is null"); return (Criteria) this; }
        public Criteria andNewThresholdScoreIsNotNull() { addCriterion("new_threshold_score is not null"); return (Criteria) this; }
        public Criteria andNewThresholdScoreEqualTo(Integer value) { addCriterion("new_threshold_score =", value, "newThresholdScore"); return (Criteria) this; }
        public Criteria andNewThresholdScoreNotEqualTo(Integer value) { addCriterion("new_threshold_score <>", value, "newThresholdScore"); return (Criteria) this; }
        public Criteria andNewThresholdScoreGreaterThan(Integer value) { addCriterion("new_threshold_score >", value, "newThresholdScore"); return (Criteria) this; }
        public Criteria andNewThresholdScoreLessThan(Integer value) { addCriterion("new_threshold_score <", value, "newThresholdScore"); return (Criteria) this; }
        public Criteria andNewThresholdScoreIn(List<Integer> values) { addCriterion("new_threshold_score in", values, "newThresholdScore"); return (Criteria) this; }
        public Criteria andAdjustReasonIsNull() { addCriterion("adjust_reason is null"); return (Criteria) this; }
        public Criteria andAdjustReasonIsNotNull() { addCriterion("adjust_reason is not null"); return (Criteria) this; }
        public Criteria andAdjustReasonEqualTo(String value) { addCriterion("adjust_reason =", value, "adjustReason"); return (Criteria) this; }
        public Criteria andAdjustReasonNotEqualTo(String value) { addCriterion("adjust_reason <>", value, "adjustReason"); return (Criteria) this; }
        public Criteria andAdjustReasonGreaterThan(String value) { addCriterion("adjust_reason >", value, "adjustReason"); return (Criteria) this; }
        public Criteria andAdjustReasonLessThan(String value) { addCriterion("adjust_reason <", value, "adjustReason"); return (Criteria) this; }
        public Criteria andAdjustReasonLike(String value) { addCriterion("adjust_reason like", value, "adjustReason"); return (Criteria) this; }
        public Criteria andAdjustReasonIn(List<String> values) { addCriterion("adjust_reason in", values, "adjustReason"); return (Criteria) this; }
        public Criteria andCreatedTimeIsNull() { addCriterion("created_time is null"); return (Criteria) this; }
        public Criteria andCreatedTimeIsNotNull() { addCriterion("created_time is not null"); return (Criteria) this; }
        public Criteria andCreatedTimeEqualTo(Date value) { addCriterion("created_time =", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeNotEqualTo(Date value) { addCriterion("created_time <>", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeGreaterThan(Date value) { addCriterion("created_time >", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeLessThan(Date value) { addCriterion("created_time <", value, "createdTime"); return (Criteria) this; }
        public Criteria andCreatedTimeIn(List<Date> values) { addCriterion("created_time in", values, "createdTime"); return (Criteria) this; }
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

