package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortPromptVersionExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortPromptVersionExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andVersionIdIsNull() { addCriterion("version_id is null"); return (Criteria) this; }
        public Criteria andVersionIdIsNotNull() { addCriterion("version_id is not null"); return (Criteria) this; }
        public Criteria andVersionIdEqualTo(String value) { addCriterion("version_id =", value, "versionId"); return (Criteria) this; }
        public Criteria andVersionIdNotEqualTo(String value) { addCriterion("version_id <>", value, "versionId"); return (Criteria) this; }
        public Criteria andVersionIdGreaterThan(String value) { addCriterion("version_id >", value, "versionId"); return (Criteria) this; }
        public Criteria andVersionIdLessThan(String value) { addCriterion("version_id <", value, "versionId"); return (Criteria) this; }
        public Criteria andVersionIdLike(String value) { addCriterion("version_id like", value, "versionId"); return (Criteria) this; }
        public Criteria andVersionIdIn(List<String> values) { addCriterion("version_id in", values, "versionId"); return (Criteria) this; }
        public Criteria andShotIdIsNull() { addCriterion("shot_id is null"); return (Criteria) this; }
        public Criteria andShotIdIsNotNull() { addCriterion("shot_id is not null"); return (Criteria) this; }
        public Criteria andShotIdEqualTo(String value) { addCriterion("shot_id =", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdNotEqualTo(String value) { addCriterion("shot_id <>", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdGreaterThan(String value) { addCriterion("shot_id >", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLessThan(String value) { addCriterion("shot_id <", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLike(String value) { addCriterion("shot_id like", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdIn(List<String> values) { addCriterion("shot_id in", values, "shotId"); return (Criteria) this; }
        public Criteria andPromptVersionIsNull() { addCriterion("prompt_version is null"); return (Criteria) this; }
        public Criteria andPromptVersionIsNotNull() { addCriterion("prompt_version is not null"); return (Criteria) this; }
        public Criteria andPromptVersionEqualTo(Integer value) { addCriterion("prompt_version =", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionNotEqualTo(Integer value) { addCriterion("prompt_version <>", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionGreaterThan(Integer value) { addCriterion("prompt_version >", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionLessThan(Integer value) { addCriterion("prompt_version <", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionIn(List<Integer> values) { addCriterion("prompt_version in", values, "promptVersion"); return (Criteria) this; }
        public Criteria andImagePromptIsNull() { addCriterion("image_prompt is null"); return (Criteria) this; }
        public Criteria andImagePromptIsNotNull() { addCriterion("image_prompt is not null"); return (Criteria) this; }
        public Criteria andImagePromptEqualTo(String value) { addCriterion("image_prompt =", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptNotEqualTo(String value) { addCriterion("image_prompt <>", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptGreaterThan(String value) { addCriterion("image_prompt >", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptLessThan(String value) { addCriterion("image_prompt <", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptLike(String value) { addCriterion("image_prompt like", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptIn(List<String> values) { addCriterion("image_prompt in", values, "imagePrompt"); return (Criteria) this; }
        public Criteria andChangeSummaryIsNull() { addCriterion("change_summary is null"); return (Criteria) this; }
        public Criteria andChangeSummaryIsNotNull() { addCriterion("change_summary is not null"); return (Criteria) this; }
        public Criteria andChangeSummaryEqualTo(String value) { addCriterion("change_summary =", value, "changeSummary"); return (Criteria) this; }
        public Criteria andChangeSummaryNotEqualTo(String value) { addCriterion("change_summary <>", value, "changeSummary"); return (Criteria) this; }
        public Criteria andChangeSummaryGreaterThan(String value) { addCriterion("change_summary >", value, "changeSummary"); return (Criteria) this; }
        public Criteria andChangeSummaryLessThan(String value) { addCriterion("change_summary <", value, "changeSummary"); return (Criteria) this; }
        public Criteria andChangeSummaryLike(String value) { addCriterion("change_summary like", value, "changeSummary"); return (Criteria) this; }
        public Criteria andChangeSummaryIn(List<String> values) { addCriterion("change_summary in", values, "changeSummary"); return (Criteria) this; }
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

