package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortQualityGateExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortQualityGateExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andGateIdIsNull() { addCriterion("gate_id is null"); return (Criteria) this; }
        public Criteria andGateIdIsNotNull() { addCriterion("gate_id is not null"); return (Criteria) this; }
        public Criteria andGateIdEqualTo(String value) { addCriterion("gate_id =", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdNotEqualTo(String value) { addCriterion("gate_id <>", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdGreaterThan(String value) { addCriterion("gate_id >", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdLessThan(String value) { addCriterion("gate_id <", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdLike(String value) { addCriterion("gate_id like", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdIn(List<String> values) { addCriterion("gate_id in", values, "gateId"); return (Criteria) this; }
        public Criteria andWorkIdIsNull() { addCriterion("work_id is null"); return (Criteria) this; }
        public Criteria andWorkIdIsNotNull() { addCriterion("work_id is not null"); return (Criteria) this; }
        public Criteria andWorkIdEqualTo(String value) { addCriterion("work_id =", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdNotEqualTo(String value) { addCriterion("work_id <>", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdGreaterThan(String value) { addCriterion("work_id >", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdLessThan(String value) { addCriterion("work_id <", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdLike(String value) { addCriterion("work_id like", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdIn(List<String> values) { addCriterion("work_id in", values, "workId"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNull() { addCriterion("episode_id is null"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNotNull() { addCriterion("episode_id is not null"); return (Criteria) this; }
        public Criteria andEpisodeIdEqualTo(String value) { addCriterion("episode_id =", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdNotEqualTo(String value) { addCriterion("episode_id <>", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdGreaterThan(String value) { addCriterion("episode_id >", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLessThan(String value) { addCriterion("episode_id <", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLike(String value) { addCriterion("episode_id like", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdIn(List<String> values) { addCriterion("episode_id in", values, "episodeId"); return (Criteria) this; }
        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdGreaterThan(String value) { addCriterion("user_id >", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLessThan(String value) { addCriterion("user_id <", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLike(String value) { addCriterion("user_id like", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
        public Criteria andGateCodeIsNull() { addCriterion("gate_code is null"); return (Criteria) this; }
        public Criteria andGateCodeIsNotNull() { addCriterion("gate_code is not null"); return (Criteria) this; }
        public Criteria andGateCodeEqualTo(String value) { addCriterion("gate_code =", value, "gateCode"); return (Criteria) this; }
        public Criteria andGateCodeNotEqualTo(String value) { addCriterion("gate_code <>", value, "gateCode"); return (Criteria) this; }
        public Criteria andGateCodeGreaterThan(String value) { addCriterion("gate_code >", value, "gateCode"); return (Criteria) this; }
        public Criteria andGateCodeLessThan(String value) { addCriterion("gate_code <", value, "gateCode"); return (Criteria) this; }
        public Criteria andGateCodeLike(String value) { addCriterion("gate_code like", value, "gateCode"); return (Criteria) this; }
        public Criteria andGateCodeIn(List<String> values) { addCriterion("gate_code in", values, "gateCode"); return (Criteria) this; }
        public Criteria andGateStatusIsNull() { addCriterion("gate_status is null"); return (Criteria) this; }
        public Criteria andGateStatusIsNotNull() { addCriterion("gate_status is not null"); return (Criteria) this; }
        public Criteria andGateStatusEqualTo(String value) { addCriterion("gate_status =", value, "gateStatus"); return (Criteria) this; }
        public Criteria andGateStatusNotEqualTo(String value) { addCriterion("gate_status <>", value, "gateStatus"); return (Criteria) this; }
        public Criteria andGateStatusGreaterThan(String value) { addCriterion("gate_status >", value, "gateStatus"); return (Criteria) this; }
        public Criteria andGateStatusLessThan(String value) { addCriterion("gate_status <", value, "gateStatus"); return (Criteria) this; }
        public Criteria andGateStatusLike(String value) { addCriterion("gate_status like", value, "gateStatus"); return (Criteria) this; }
        public Criteria andGateStatusIn(List<String> values) { addCriterion("gate_status in", values, "gateStatus"); return (Criteria) this; }
        public Criteria andScoreIsNull() { addCriterion("score is null"); return (Criteria) this; }
        public Criteria andScoreIsNotNull() { addCriterion("score is not null"); return (Criteria) this; }
        public Criteria andScoreEqualTo(Integer value) { addCriterion("score =", value, "score"); return (Criteria) this; }
        public Criteria andScoreNotEqualTo(Integer value) { addCriterion("score <>", value, "score"); return (Criteria) this; }
        public Criteria andScoreGreaterThan(Integer value) { addCriterion("score >", value, "score"); return (Criteria) this; }
        public Criteria andScoreLessThan(Integer value) { addCriterion("score <", value, "score"); return (Criteria) this; }
        public Criteria andScoreIn(List<Integer> values) { addCriterion("score in", values, "score"); return (Criteria) this; }
        public Criteria andThresholdScoreIsNull() { addCriterion("threshold_score is null"); return (Criteria) this; }
        public Criteria andThresholdScoreIsNotNull() { addCriterion("threshold_score is not null"); return (Criteria) this; }
        public Criteria andThresholdScoreEqualTo(Integer value) { addCriterion("threshold_score =", value, "thresholdScore"); return (Criteria) this; }
        public Criteria andThresholdScoreNotEqualTo(Integer value) { addCriterion("threshold_score <>", value, "thresholdScore"); return (Criteria) this; }
        public Criteria andThresholdScoreGreaterThan(Integer value) { addCriterion("threshold_score >", value, "thresholdScore"); return (Criteria) this; }
        public Criteria andThresholdScoreLessThan(Integer value) { addCriterion("threshold_score <", value, "thresholdScore"); return (Criteria) this; }
        public Criteria andThresholdScoreIn(List<Integer> values) { addCriterion("threshold_score in", values, "thresholdScore"); return (Criteria) this; }
        public Criteria andForcePassFlagIsNull() { addCriterion("force_pass_flag is null"); return (Criteria) this; }
        public Criteria andForcePassFlagIsNotNull() { addCriterion("force_pass_flag is not null"); return (Criteria) this; }
        public Criteria andForcePassFlagEqualTo(Integer value) { addCriterion("force_pass_flag =", value, "forcePassFlag"); return (Criteria) this; }
        public Criteria andForcePassFlagNotEqualTo(Integer value) { addCriterion("force_pass_flag <>", value, "forcePassFlag"); return (Criteria) this; }
        public Criteria andForcePassFlagGreaterThan(Integer value) { addCriterion("force_pass_flag >", value, "forcePassFlag"); return (Criteria) this; }
        public Criteria andForcePassFlagLessThan(Integer value) { addCriterion("force_pass_flag <", value, "forcePassFlag"); return (Criteria) this; }
        public Criteria andForcePassFlagIn(List<Integer> values) { addCriterion("force_pass_flag in", values, "forcePassFlag"); return (Criteria) this; }
        public Criteria andForcePassReasonIsNull() { addCriterion("force_pass_reason is null"); return (Criteria) this; }
        public Criteria andForcePassReasonIsNotNull() { addCriterion("force_pass_reason is not null"); return (Criteria) this; }
        public Criteria andForcePassReasonEqualTo(String value) { addCriterion("force_pass_reason =", value, "forcePassReason"); return (Criteria) this; }
        public Criteria andForcePassReasonNotEqualTo(String value) { addCriterion("force_pass_reason <>", value, "forcePassReason"); return (Criteria) this; }
        public Criteria andForcePassReasonGreaterThan(String value) { addCriterion("force_pass_reason >", value, "forcePassReason"); return (Criteria) this; }
        public Criteria andForcePassReasonLessThan(String value) { addCriterion("force_pass_reason <", value, "forcePassReason"); return (Criteria) this; }
        public Criteria andForcePassReasonLike(String value) { addCriterion("force_pass_reason like", value, "forcePassReason"); return (Criteria) this; }
        public Criteria andForcePassReasonIn(List<String> values) { addCriterion("force_pass_reason in", values, "forcePassReason"); return (Criteria) this; }
        public Criteria andResultJsonIsNull() { addCriterion("result_json is null"); return (Criteria) this; }
        public Criteria andResultJsonIsNotNull() { addCriterion("result_json is not null"); return (Criteria) this; }
        public Criteria andResultJsonEqualTo(String value) { addCriterion("result_json =", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonNotEqualTo(String value) { addCriterion("result_json <>", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonGreaterThan(String value) { addCriterion("result_json >", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonLessThan(String value) { addCriterion("result_json <", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonLike(String value) { addCriterion("result_json like", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonIn(List<String> values) { addCriterion("result_json in", values, "resultJson"); return (Criteria) this; }
        public Criteria andDeletedFlagIsNull() { addCriterion("deleted_flag is null"); return (Criteria) this; }
        public Criteria andDeletedFlagIsNotNull() { addCriterion("deleted_flag is not null"); return (Criteria) this; }
        public Criteria andDeletedFlagEqualTo(Integer value) { addCriterion("deleted_flag =", value, "deletedFlag"); return (Criteria) this; }
        public Criteria andDeletedFlagNotEqualTo(Integer value) { addCriterion("deleted_flag <>", value, "deletedFlag"); return (Criteria) this; }
        public Criteria andDeletedFlagGreaterThan(Integer value) { addCriterion("deleted_flag >", value, "deletedFlag"); return (Criteria) this; }
        public Criteria andDeletedFlagLessThan(Integer value) { addCriterion("deleted_flag <", value, "deletedFlag"); return (Criteria) this; }
        public Criteria andDeletedFlagIn(List<Integer> values) { addCriterion("deleted_flag in", values, "deletedFlag"); return (Criteria) this; }
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

