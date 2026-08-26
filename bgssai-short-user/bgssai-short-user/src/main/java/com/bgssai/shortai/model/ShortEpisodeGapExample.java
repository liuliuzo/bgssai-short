package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortEpisodeGapExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortEpisodeGapExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andGapIdIsNull() { addCriterion("gap_id is null"); return (Criteria) this; }
        public Criteria andGapIdIsNotNull() { addCriterion("gap_id is not null"); return (Criteria) this; }
        public Criteria andGapIdEqualTo(String value) { addCriterion("gap_id =", value, "gapId"); return (Criteria) this; }
        public Criteria andGapIdNotEqualTo(String value) { addCriterion("gap_id <>", value, "gapId"); return (Criteria) this; }
        public Criteria andGapIdGreaterThan(String value) { addCriterion("gap_id >", value, "gapId"); return (Criteria) this; }
        public Criteria andGapIdLessThan(String value) { addCriterion("gap_id <", value, "gapId"); return (Criteria) this; }
        public Criteria andGapIdLike(String value) { addCriterion("gap_id like", value, "gapId"); return (Criteria) this; }
        public Criteria andGapIdIn(List<String> values) { addCriterion("gap_id in", values, "gapId"); return (Criteria) this; }
        public Criteria andWorkIdIsNull() { addCriterion("work_id is null"); return (Criteria) this; }
        public Criteria andWorkIdIsNotNull() { addCriterion("work_id is not null"); return (Criteria) this; }
        public Criteria andWorkIdEqualTo(String value) { addCriterion("work_id =", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdNotEqualTo(String value) { addCriterion("work_id <>", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdGreaterThan(String value) { addCriterion("work_id >", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdLessThan(String value) { addCriterion("work_id <", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdLike(String value) { addCriterion("work_id like", value, "workId"); return (Criteria) this; }
        public Criteria andWorkIdIn(List<String> values) { addCriterion("work_id in", values, "workId"); return (Criteria) this; }
        public Criteria andFromEpisodeIdIsNull() { addCriterion("from_episode_id is null"); return (Criteria) this; }
        public Criteria andFromEpisodeIdIsNotNull() { addCriterion("from_episode_id is not null"); return (Criteria) this; }
        public Criteria andFromEpisodeIdEqualTo(String value) { addCriterion("from_episode_id =", value, "fromEpisodeId"); return (Criteria) this; }
        public Criteria andFromEpisodeIdNotEqualTo(String value) { addCriterion("from_episode_id <>", value, "fromEpisodeId"); return (Criteria) this; }
        public Criteria andFromEpisodeIdGreaterThan(String value) { addCriterion("from_episode_id >", value, "fromEpisodeId"); return (Criteria) this; }
        public Criteria andFromEpisodeIdLessThan(String value) { addCriterion("from_episode_id <", value, "fromEpisodeId"); return (Criteria) this; }
        public Criteria andFromEpisodeIdLike(String value) { addCriterion("from_episode_id like", value, "fromEpisodeId"); return (Criteria) this; }
        public Criteria andFromEpisodeIdIn(List<String> values) { addCriterion("from_episode_id in", values, "fromEpisodeId"); return (Criteria) this; }
        public Criteria andToEpisodeIdIsNull() { addCriterion("to_episode_id is null"); return (Criteria) this; }
        public Criteria andToEpisodeIdIsNotNull() { addCriterion("to_episode_id is not null"); return (Criteria) this; }
        public Criteria andToEpisodeIdEqualTo(String value) { addCriterion("to_episode_id =", value, "toEpisodeId"); return (Criteria) this; }
        public Criteria andToEpisodeIdNotEqualTo(String value) { addCriterion("to_episode_id <>", value, "toEpisodeId"); return (Criteria) this; }
        public Criteria andToEpisodeIdGreaterThan(String value) { addCriterion("to_episode_id >", value, "toEpisodeId"); return (Criteria) this; }
        public Criteria andToEpisodeIdLessThan(String value) { addCriterion("to_episode_id <", value, "toEpisodeId"); return (Criteria) this; }
        public Criteria andToEpisodeIdLike(String value) { addCriterion("to_episode_id like", value, "toEpisodeId"); return (Criteria) this; }
        public Criteria andToEpisodeIdIn(List<String> values) { addCriterion("to_episode_id in", values, "toEpisodeId"); return (Criteria) this; }
        public Criteria andGapDescIsNull() { addCriterion("gap_desc is null"); return (Criteria) this; }
        public Criteria andGapDescIsNotNull() { addCriterion("gap_desc is not null"); return (Criteria) this; }
        public Criteria andGapDescEqualTo(String value) { addCriterion("gap_desc =", value, "gapDesc"); return (Criteria) this; }
        public Criteria andGapDescNotEqualTo(String value) { addCriterion("gap_desc <>", value, "gapDesc"); return (Criteria) this; }
        public Criteria andGapDescGreaterThan(String value) { addCriterion("gap_desc >", value, "gapDesc"); return (Criteria) this; }
        public Criteria andGapDescLessThan(String value) { addCriterion("gap_desc <", value, "gapDesc"); return (Criteria) this; }
        public Criteria andGapDescLike(String value) { addCriterion("gap_desc like", value, "gapDesc"); return (Criteria) this; }
        public Criteria andGapDescIn(List<String> values) { addCriterion("gap_desc in", values, "gapDesc"); return (Criteria) this; }
        public Criteria andSeverityIsNull() { addCriterion("severity is null"); return (Criteria) this; }
        public Criteria andSeverityIsNotNull() { addCriterion("severity is not null"); return (Criteria) this; }
        public Criteria andSeverityEqualTo(String value) { addCriterion("severity =", value, "severity"); return (Criteria) this; }
        public Criteria andSeverityNotEqualTo(String value) { addCriterion("severity <>", value, "severity"); return (Criteria) this; }
        public Criteria andSeverityGreaterThan(String value) { addCriterion("severity >", value, "severity"); return (Criteria) this; }
        public Criteria andSeverityLessThan(String value) { addCriterion("severity <", value, "severity"); return (Criteria) this; }
        public Criteria andSeverityLike(String value) { addCriterion("severity like", value, "severity"); return (Criteria) this; }
        public Criteria andSeverityIn(List<String> values) { addCriterion("severity in", values, "severity"); return (Criteria) this; }
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

