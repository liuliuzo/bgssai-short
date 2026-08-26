package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortVariantExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortVariantExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andVariantIdIsNull() { addCriterion("variant_id is null"); return (Criteria) this; }
        public Criteria andVariantIdIsNotNull() { addCriterion("variant_id is not null"); return (Criteria) this; }
        public Criteria andVariantIdEqualTo(String value) { addCriterion("variant_id =", value, "variantId"); return (Criteria) this; }
        public Criteria andVariantIdNotEqualTo(String value) { addCriterion("variant_id <>", value, "variantId"); return (Criteria) this; }
        public Criteria andVariantIdGreaterThan(String value) { addCriterion("variant_id >", value, "variantId"); return (Criteria) this; }
        public Criteria andVariantIdLessThan(String value) { addCriterion("variant_id <", value, "variantId"); return (Criteria) this; }
        public Criteria andVariantIdLike(String value) { addCriterion("variant_id like", value, "variantId"); return (Criteria) this; }
        public Criteria andVariantIdIn(List<String> values) { addCriterion("variant_id in", values, "variantId"); return (Criteria) this; }
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
        public Criteria andVariantNodeIsNull() { addCriterion("variant_node is null"); return (Criteria) this; }
        public Criteria andVariantNodeIsNotNull() { addCriterion("variant_node is not null"); return (Criteria) this; }
        public Criteria andVariantNodeEqualTo(String value) { addCriterion("variant_node =", value, "variantNode"); return (Criteria) this; }
        public Criteria andVariantNodeNotEqualTo(String value) { addCriterion("variant_node <>", value, "variantNode"); return (Criteria) this; }
        public Criteria andVariantNodeGreaterThan(String value) { addCriterion("variant_node >", value, "variantNode"); return (Criteria) this; }
        public Criteria andVariantNodeLessThan(String value) { addCriterion("variant_node <", value, "variantNode"); return (Criteria) this; }
        public Criteria andVariantNodeLike(String value) { addCriterion("variant_node like", value, "variantNode"); return (Criteria) this; }
        public Criteria andVariantNodeIn(List<String> values) { addCriterion("variant_node in", values, "variantNode"); return (Criteria) this; }
        public Criteria andTargetIdIsNull() { addCriterion("target_id is null"); return (Criteria) this; }
        public Criteria andTargetIdIsNotNull() { addCriterion("target_id is not null"); return (Criteria) this; }
        public Criteria andTargetIdEqualTo(String value) { addCriterion("target_id =", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdNotEqualTo(String value) { addCriterion("target_id <>", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdGreaterThan(String value) { addCriterion("target_id >", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdLessThan(String value) { addCriterion("target_id <", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdLike(String value) { addCriterion("target_id like", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdIn(List<String> values) { addCriterion("target_id in", values, "targetId"); return (Criteria) this; }
        public Criteria andVariantNoIsNull() { addCriterion("variant_no is null"); return (Criteria) this; }
        public Criteria andVariantNoIsNotNull() { addCriterion("variant_no is not null"); return (Criteria) this; }
        public Criteria andVariantNoEqualTo(Integer value) { addCriterion("variant_no =", value, "variantNo"); return (Criteria) this; }
        public Criteria andVariantNoNotEqualTo(Integer value) { addCriterion("variant_no <>", value, "variantNo"); return (Criteria) this; }
        public Criteria andVariantNoGreaterThan(Integer value) { addCriterion("variant_no >", value, "variantNo"); return (Criteria) this; }
        public Criteria andVariantNoLessThan(Integer value) { addCriterion("variant_no <", value, "variantNo"); return (Criteria) this; }
        public Criteria andVariantNoIn(List<Integer> values) { addCriterion("variant_no in", values, "variantNo"); return (Criteria) this; }
        public Criteria andContentJsonIsNull() { addCriterion("content_json is null"); return (Criteria) this; }
        public Criteria andContentJsonIsNotNull() { addCriterion("content_json is not null"); return (Criteria) this; }
        public Criteria andContentJsonEqualTo(String value) { addCriterion("content_json =", value, "contentJson"); return (Criteria) this; }
        public Criteria andContentJsonNotEqualTo(String value) { addCriterion("content_json <>", value, "contentJson"); return (Criteria) this; }
        public Criteria andContentJsonGreaterThan(String value) { addCriterion("content_json >", value, "contentJson"); return (Criteria) this; }
        public Criteria andContentJsonLessThan(String value) { addCriterion("content_json <", value, "contentJson"); return (Criteria) this; }
        public Criteria andContentJsonLike(String value) { addCriterion("content_json like", value, "contentJson"); return (Criteria) this; }
        public Criteria andContentJsonIn(List<String> values) { addCriterion("content_json in", values, "contentJson"); return (Criteria) this; }
        public Criteria andScoreIsNull() { addCriterion("score is null"); return (Criteria) this; }
        public Criteria andScoreIsNotNull() { addCriterion("score is not null"); return (Criteria) this; }
        public Criteria andScoreEqualTo(Integer value) { addCriterion("score =", value, "score"); return (Criteria) this; }
        public Criteria andScoreNotEqualTo(Integer value) { addCriterion("score <>", value, "score"); return (Criteria) this; }
        public Criteria andScoreGreaterThan(Integer value) { addCriterion("score >", value, "score"); return (Criteria) this; }
        public Criteria andScoreLessThan(Integer value) { addCriterion("score <", value, "score"); return (Criteria) this; }
        public Criteria andScoreIn(List<Integer> values) { addCriterion("score in", values, "score"); return (Criteria) this; }
        public Criteria andSelectedFlagIsNull() { addCriterion("selected_flag is null"); return (Criteria) this; }
        public Criteria andSelectedFlagIsNotNull() { addCriterion("selected_flag is not null"); return (Criteria) this; }
        public Criteria andSelectedFlagEqualTo(Integer value) { addCriterion("selected_flag =", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagNotEqualTo(Integer value) { addCriterion("selected_flag <>", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagGreaterThan(Integer value) { addCriterion("selected_flag >", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagLessThan(Integer value) { addCriterion("selected_flag <", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagIn(List<Integer> values) { addCriterion("selected_flag in", values, "selectedFlag"); return (Criteria) this; }
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

