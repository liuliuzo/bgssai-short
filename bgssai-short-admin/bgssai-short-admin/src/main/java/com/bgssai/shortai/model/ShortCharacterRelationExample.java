package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortCharacterRelationExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortCharacterRelationExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andRelationIdIsNull() { addCriterion("relation_id is null"); return (Criteria) this; }
        public Criteria andRelationIdIsNotNull() { addCriterion("relation_id is not null"); return (Criteria) this; }
        public Criteria andRelationIdEqualTo(String value) { addCriterion("relation_id =", value, "relationId"); return (Criteria) this; }
        public Criteria andRelationIdNotEqualTo(String value) { addCriterion("relation_id <>", value, "relationId"); return (Criteria) this; }
        public Criteria andRelationIdGreaterThan(String value) { addCriterion("relation_id >", value, "relationId"); return (Criteria) this; }
        public Criteria andRelationIdLessThan(String value) { addCriterion("relation_id <", value, "relationId"); return (Criteria) this; }
        public Criteria andRelationIdLike(String value) { addCriterion("relation_id like", value, "relationId"); return (Criteria) this; }
        public Criteria andRelationIdIn(List<String> values) { addCriterion("relation_id in", values, "relationId"); return (Criteria) this; }
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
        public Criteria andFromCharacterIdIsNull() { addCriterion("from_character_id is null"); return (Criteria) this; }
        public Criteria andFromCharacterIdIsNotNull() { addCriterion("from_character_id is not null"); return (Criteria) this; }
        public Criteria andFromCharacterIdEqualTo(String value) { addCriterion("from_character_id =", value, "fromCharacterId"); return (Criteria) this; }
        public Criteria andFromCharacterIdNotEqualTo(String value) { addCriterion("from_character_id <>", value, "fromCharacterId"); return (Criteria) this; }
        public Criteria andFromCharacterIdGreaterThan(String value) { addCriterion("from_character_id >", value, "fromCharacterId"); return (Criteria) this; }
        public Criteria andFromCharacterIdLessThan(String value) { addCriterion("from_character_id <", value, "fromCharacterId"); return (Criteria) this; }
        public Criteria andFromCharacterIdLike(String value) { addCriterion("from_character_id like", value, "fromCharacterId"); return (Criteria) this; }
        public Criteria andFromCharacterIdIn(List<String> values) { addCriterion("from_character_id in", values, "fromCharacterId"); return (Criteria) this; }
        public Criteria andToCharacterIdIsNull() { addCriterion("to_character_id is null"); return (Criteria) this; }
        public Criteria andToCharacterIdIsNotNull() { addCriterion("to_character_id is not null"); return (Criteria) this; }
        public Criteria andToCharacterIdEqualTo(String value) { addCriterion("to_character_id =", value, "toCharacterId"); return (Criteria) this; }
        public Criteria andToCharacterIdNotEqualTo(String value) { addCriterion("to_character_id <>", value, "toCharacterId"); return (Criteria) this; }
        public Criteria andToCharacterIdGreaterThan(String value) { addCriterion("to_character_id >", value, "toCharacterId"); return (Criteria) this; }
        public Criteria andToCharacterIdLessThan(String value) { addCriterion("to_character_id <", value, "toCharacterId"); return (Criteria) this; }
        public Criteria andToCharacterIdLike(String value) { addCriterion("to_character_id like", value, "toCharacterId"); return (Criteria) this; }
        public Criteria andToCharacterIdIn(List<String> values) { addCriterion("to_character_id in", values, "toCharacterId"); return (Criteria) this; }
        public Criteria andRelationTypeIsNull() { addCriterion("relation_type is null"); return (Criteria) this; }
        public Criteria andRelationTypeIsNotNull() { addCriterion("relation_type is not null"); return (Criteria) this; }
        public Criteria andRelationTypeEqualTo(String value) { addCriterion("relation_type =", value, "relationType"); return (Criteria) this; }
        public Criteria andRelationTypeNotEqualTo(String value) { addCriterion("relation_type <>", value, "relationType"); return (Criteria) this; }
        public Criteria andRelationTypeGreaterThan(String value) { addCriterion("relation_type >", value, "relationType"); return (Criteria) this; }
        public Criteria andRelationTypeLessThan(String value) { addCriterion("relation_type <", value, "relationType"); return (Criteria) this; }
        public Criteria andRelationTypeLike(String value) { addCriterion("relation_type like", value, "relationType"); return (Criteria) this; }
        public Criteria andRelationTypeIn(List<String> values) { addCriterion("relation_type in", values, "relationType"); return (Criteria) this; }
        public Criteria andAsOfEpisodeNoIsNull() { addCriterion("as_of_episode_no is null"); return (Criteria) this; }
        public Criteria andAsOfEpisodeNoIsNotNull() { addCriterion("as_of_episode_no is not null"); return (Criteria) this; }
        public Criteria andAsOfEpisodeNoEqualTo(Integer value) { addCriterion("as_of_episode_no =", value, "asOfEpisodeNo"); return (Criteria) this; }
        public Criteria andAsOfEpisodeNoNotEqualTo(Integer value) { addCriterion("as_of_episode_no <>", value, "asOfEpisodeNo"); return (Criteria) this; }
        public Criteria andAsOfEpisodeNoGreaterThan(Integer value) { addCriterion("as_of_episode_no >", value, "asOfEpisodeNo"); return (Criteria) this; }
        public Criteria andAsOfEpisodeNoLessThan(Integer value) { addCriterion("as_of_episode_no <", value, "asOfEpisodeNo"); return (Criteria) this; }
        public Criteria andAsOfEpisodeNoIn(List<Integer> values) { addCriterion("as_of_episode_no in", values, "asOfEpisodeNo"); return (Criteria) this; }
        public Criteria andRelationDescIsNull() { addCriterion("relation_desc is null"); return (Criteria) this; }
        public Criteria andRelationDescIsNotNull() { addCriterion("relation_desc is not null"); return (Criteria) this; }
        public Criteria andRelationDescEqualTo(String value) { addCriterion("relation_desc =", value, "relationDesc"); return (Criteria) this; }
        public Criteria andRelationDescNotEqualTo(String value) { addCriterion("relation_desc <>", value, "relationDesc"); return (Criteria) this; }
        public Criteria andRelationDescGreaterThan(String value) { addCriterion("relation_desc >", value, "relationDesc"); return (Criteria) this; }
        public Criteria andRelationDescLessThan(String value) { addCriterion("relation_desc <", value, "relationDesc"); return (Criteria) this; }
        public Criteria andRelationDescLike(String value) { addCriterion("relation_desc like", value, "relationDesc"); return (Criteria) this; }
        public Criteria andRelationDescIn(List<String> values) { addCriterion("relation_desc in", values, "relationDesc"); return (Criteria) this; }
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

