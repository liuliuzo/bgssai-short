package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortSubtitleExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortSubtitleExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andSubtitleIdIsNull() { addCriterion("subtitle_id is null"); return (Criteria) this; }
        public Criteria andSubtitleIdIsNotNull() { addCriterion("subtitle_id is not null"); return (Criteria) this; }
        public Criteria andSubtitleIdEqualTo(String value) { addCriterion("subtitle_id =", value, "subtitleId"); return (Criteria) this; }
        public Criteria andSubtitleIdNotEqualTo(String value) { addCriterion("subtitle_id <>", value, "subtitleId"); return (Criteria) this; }
        public Criteria andSubtitleIdGreaterThan(String value) { addCriterion("subtitle_id >", value, "subtitleId"); return (Criteria) this; }
        public Criteria andSubtitleIdLessThan(String value) { addCriterion("subtitle_id <", value, "subtitleId"); return (Criteria) this; }
        public Criteria andSubtitleIdLike(String value) { addCriterion("subtitle_id like", value, "subtitleId"); return (Criteria) this; }
        public Criteria andSubtitleIdIn(List<String> values) { addCriterion("subtitle_id in", values, "subtitleId"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNull() { addCriterion("episode_id is null"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNotNull() { addCriterion("episode_id is not null"); return (Criteria) this; }
        public Criteria andEpisodeIdEqualTo(String value) { addCriterion("episode_id =", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdNotEqualTo(String value) { addCriterion("episode_id <>", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdGreaterThan(String value) { addCriterion("episode_id >", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLessThan(String value) { addCriterion("episode_id <", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLike(String value) { addCriterion("episode_id like", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdIn(List<String> values) { addCriterion("episode_id in", values, "episodeId"); return (Criteria) this; }
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
        public Criteria andSubtitleNoIsNull() { addCriterion("subtitle_no is null"); return (Criteria) this; }
        public Criteria andSubtitleNoIsNotNull() { addCriterion("subtitle_no is not null"); return (Criteria) this; }
        public Criteria andSubtitleNoEqualTo(Integer value) { addCriterion("subtitle_no =", value, "subtitleNo"); return (Criteria) this; }
        public Criteria andSubtitleNoNotEqualTo(Integer value) { addCriterion("subtitle_no <>", value, "subtitleNo"); return (Criteria) this; }
        public Criteria andSubtitleNoGreaterThan(Integer value) { addCriterion("subtitle_no >", value, "subtitleNo"); return (Criteria) this; }
        public Criteria andSubtitleNoLessThan(Integer value) { addCriterion("subtitle_no <", value, "subtitleNo"); return (Criteria) this; }
        public Criteria andSubtitleNoIn(List<Integer> values) { addCriterion("subtitle_no in", values, "subtitleNo"); return (Criteria) this; }
        public Criteria andStartMsIsNull() { addCriterion("start_ms is null"); return (Criteria) this; }
        public Criteria andStartMsIsNotNull() { addCriterion("start_ms is not null"); return (Criteria) this; }
        public Criteria andStartMsEqualTo(Integer value) { addCriterion("start_ms =", value, "startMs"); return (Criteria) this; }
        public Criteria andStartMsNotEqualTo(Integer value) { addCriterion("start_ms <>", value, "startMs"); return (Criteria) this; }
        public Criteria andStartMsGreaterThan(Integer value) { addCriterion("start_ms >", value, "startMs"); return (Criteria) this; }
        public Criteria andStartMsLessThan(Integer value) { addCriterion("start_ms <", value, "startMs"); return (Criteria) this; }
        public Criteria andStartMsIn(List<Integer> values) { addCriterion("start_ms in", values, "startMs"); return (Criteria) this; }
        public Criteria andEndMsIsNull() { addCriterion("end_ms is null"); return (Criteria) this; }
        public Criteria andEndMsIsNotNull() { addCriterion("end_ms is not null"); return (Criteria) this; }
        public Criteria andEndMsEqualTo(Integer value) { addCriterion("end_ms =", value, "endMs"); return (Criteria) this; }
        public Criteria andEndMsNotEqualTo(Integer value) { addCriterion("end_ms <>", value, "endMs"); return (Criteria) this; }
        public Criteria andEndMsGreaterThan(Integer value) { addCriterion("end_ms >", value, "endMs"); return (Criteria) this; }
        public Criteria andEndMsLessThan(Integer value) { addCriterion("end_ms <", value, "endMs"); return (Criteria) this; }
        public Criteria andEndMsIn(List<Integer> values) { addCriterion("end_ms in", values, "endMs"); return (Criteria) this; }
        public Criteria andSubtitleTextIsNull() { addCriterion("subtitle_text is null"); return (Criteria) this; }
        public Criteria andSubtitleTextIsNotNull() { addCriterion("subtitle_text is not null"); return (Criteria) this; }
        public Criteria andSubtitleTextEqualTo(String value) { addCriterion("subtitle_text =", value, "subtitleText"); return (Criteria) this; }
        public Criteria andSubtitleTextNotEqualTo(String value) { addCriterion("subtitle_text <>", value, "subtitleText"); return (Criteria) this; }
        public Criteria andSubtitleTextGreaterThan(String value) { addCriterion("subtitle_text >", value, "subtitleText"); return (Criteria) this; }
        public Criteria andSubtitleTextLessThan(String value) { addCriterion("subtitle_text <", value, "subtitleText"); return (Criteria) this; }
        public Criteria andSubtitleTextLike(String value) { addCriterion("subtitle_text like", value, "subtitleText"); return (Criteria) this; }
        public Criteria andSubtitleTextIn(List<String> values) { addCriterion("subtitle_text in", values, "subtitleText"); return (Criteria) this; }
        public Criteria andShotIdIsNull() { addCriterion("shot_id is null"); return (Criteria) this; }
        public Criteria andShotIdIsNotNull() { addCriterion("shot_id is not null"); return (Criteria) this; }
        public Criteria andShotIdEqualTo(String value) { addCriterion("shot_id =", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdNotEqualTo(String value) { addCriterion("shot_id <>", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdGreaterThan(String value) { addCriterion("shot_id >", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLessThan(String value) { addCriterion("shot_id <", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLike(String value) { addCriterion("shot_id like", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdIn(List<String> values) { addCriterion("shot_id in", values, "shotId"); return (Criteria) this; }
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

