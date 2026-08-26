package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortQualityIssueExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortQualityIssueExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andIssueIdIsNull() { addCriterion("issue_id is null"); return (Criteria) this; }
        public Criteria andIssueIdIsNotNull() { addCriterion("issue_id is not null"); return (Criteria) this; }
        public Criteria andIssueIdEqualTo(String value) { addCriterion("issue_id =", value, "issueId"); return (Criteria) this; }
        public Criteria andIssueIdNotEqualTo(String value) { addCriterion("issue_id <>", value, "issueId"); return (Criteria) this; }
        public Criteria andIssueIdGreaterThan(String value) { addCriterion("issue_id >", value, "issueId"); return (Criteria) this; }
        public Criteria andIssueIdLessThan(String value) { addCriterion("issue_id <", value, "issueId"); return (Criteria) this; }
        public Criteria andIssueIdLike(String value) { addCriterion("issue_id like", value, "issueId"); return (Criteria) this; }
        public Criteria andIssueIdIn(List<String> values) { addCriterion("issue_id in", values, "issueId"); return (Criteria) this; }
        public Criteria andGateIdIsNull() { addCriterion("gate_id is null"); return (Criteria) this; }
        public Criteria andGateIdIsNotNull() { addCriterion("gate_id is not null"); return (Criteria) this; }
        public Criteria andGateIdEqualTo(String value) { addCriterion("gate_id =", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdNotEqualTo(String value) { addCriterion("gate_id <>", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdGreaterThan(String value) { addCriterion("gate_id >", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdLessThan(String value) { addCriterion("gate_id <", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdLike(String value) { addCriterion("gate_id like", value, "gateId"); return (Criteria) this; }
        public Criteria andGateIdIn(List<String> values) { addCriterion("gate_id in", values, "gateId"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNull() { addCriterion("episode_id is null"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNotNull() { addCriterion("episode_id is not null"); return (Criteria) this; }
        public Criteria andEpisodeIdEqualTo(String value) { addCriterion("episode_id =", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdNotEqualTo(String value) { addCriterion("episode_id <>", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdGreaterThan(String value) { addCriterion("episode_id >", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLessThan(String value) { addCriterion("episode_id <", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLike(String value) { addCriterion("episode_id like", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdIn(List<String> values) { addCriterion("episode_id in", values, "episodeId"); return (Criteria) this; }
        public Criteria andDimensionIsNull() { addCriterion("dimension is null"); return (Criteria) this; }
        public Criteria andDimensionIsNotNull() { addCriterion("dimension is not null"); return (Criteria) this; }
        public Criteria andDimensionEqualTo(String value) { addCriterion("dimension =", value, "dimension"); return (Criteria) this; }
        public Criteria andDimensionNotEqualTo(String value) { addCriterion("dimension <>", value, "dimension"); return (Criteria) this; }
        public Criteria andDimensionGreaterThan(String value) { addCriterion("dimension >", value, "dimension"); return (Criteria) this; }
        public Criteria andDimensionLessThan(String value) { addCriterion("dimension <", value, "dimension"); return (Criteria) this; }
        public Criteria andDimensionLike(String value) { addCriterion("dimension like", value, "dimension"); return (Criteria) this; }
        public Criteria andDimensionIn(List<String> values) { addCriterion("dimension in", values, "dimension"); return (Criteria) this; }
        public Criteria andIssueDescIsNull() { addCriterion("issue_desc is null"); return (Criteria) this; }
        public Criteria andIssueDescIsNotNull() { addCriterion("issue_desc is not null"); return (Criteria) this; }
        public Criteria andIssueDescEqualTo(String value) { addCriterion("issue_desc =", value, "issueDesc"); return (Criteria) this; }
        public Criteria andIssueDescNotEqualTo(String value) { addCriterion("issue_desc <>", value, "issueDesc"); return (Criteria) this; }
        public Criteria andIssueDescGreaterThan(String value) { addCriterion("issue_desc >", value, "issueDesc"); return (Criteria) this; }
        public Criteria andIssueDescLessThan(String value) { addCriterion("issue_desc <", value, "issueDesc"); return (Criteria) this; }
        public Criteria andIssueDescLike(String value) { addCriterion("issue_desc like", value, "issueDesc"); return (Criteria) this; }
        public Criteria andIssueDescIn(List<String> values) { addCriterion("issue_desc in", values, "issueDesc"); return (Criteria) this; }
        public Criteria andFixHintIsNull() { addCriterion("fix_hint is null"); return (Criteria) this; }
        public Criteria andFixHintIsNotNull() { addCriterion("fix_hint is not null"); return (Criteria) this; }
        public Criteria andFixHintEqualTo(String value) { addCriterion("fix_hint =", value, "fixHint"); return (Criteria) this; }
        public Criteria andFixHintNotEqualTo(String value) { addCriterion("fix_hint <>", value, "fixHint"); return (Criteria) this; }
        public Criteria andFixHintGreaterThan(String value) { addCriterion("fix_hint >", value, "fixHint"); return (Criteria) this; }
        public Criteria andFixHintLessThan(String value) { addCriterion("fix_hint <", value, "fixHint"); return (Criteria) this; }
        public Criteria andFixHintLike(String value) { addCriterion("fix_hint like", value, "fixHint"); return (Criteria) this; }
        public Criteria andFixHintIn(List<String> values) { addCriterion("fix_hint in", values, "fixHint"); return (Criteria) this; }
        public Criteria andTargetTypeIsNull() { addCriterion("target_type is null"); return (Criteria) this; }
        public Criteria andTargetTypeIsNotNull() { addCriterion("target_type is not null"); return (Criteria) this; }
        public Criteria andTargetTypeEqualTo(String value) { addCriterion("target_type =", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeNotEqualTo(String value) { addCriterion("target_type <>", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeGreaterThan(String value) { addCriterion("target_type >", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeLessThan(String value) { addCriterion("target_type <", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeLike(String value) { addCriterion("target_type like", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeIn(List<String> values) { addCriterion("target_type in", values, "targetType"); return (Criteria) this; }
        public Criteria andTargetIdIsNull() { addCriterion("target_id is null"); return (Criteria) this; }
        public Criteria andTargetIdIsNotNull() { addCriterion("target_id is not null"); return (Criteria) this; }
        public Criteria andTargetIdEqualTo(String value) { addCriterion("target_id =", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdNotEqualTo(String value) { addCriterion("target_id <>", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdGreaterThan(String value) { addCriterion("target_id >", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdLessThan(String value) { addCriterion("target_id <", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdLike(String value) { addCriterion("target_id like", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdIn(List<String> values) { addCriterion("target_id in", values, "targetId"); return (Criteria) this; }
        public Criteria andHandleStatusIsNull() { addCriterion("handle_status is null"); return (Criteria) this; }
        public Criteria andHandleStatusIsNotNull() { addCriterion("handle_status is not null"); return (Criteria) this; }
        public Criteria andHandleStatusEqualTo(String value) { addCriterion("handle_status =", value, "handleStatus"); return (Criteria) this; }
        public Criteria andHandleStatusNotEqualTo(String value) { addCriterion("handle_status <>", value, "handleStatus"); return (Criteria) this; }
        public Criteria andHandleStatusGreaterThan(String value) { addCriterion("handle_status >", value, "handleStatus"); return (Criteria) this; }
        public Criteria andHandleStatusLessThan(String value) { addCriterion("handle_status <", value, "handleStatus"); return (Criteria) this; }
        public Criteria andHandleStatusLike(String value) { addCriterion("handle_status like", value, "handleStatus"); return (Criteria) this; }
        public Criteria andHandleStatusIn(List<String> values) { addCriterion("handle_status in", values, "handleStatus"); return (Criteria) this; }
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

