package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortSceneCardExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortSceneCardExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andSceneCardIdIsNull() { addCriterion("scene_card_id is null"); return (Criteria) this; }
        public Criteria andSceneCardIdIsNotNull() { addCriterion("scene_card_id is not null"); return (Criteria) this; }
        public Criteria andSceneCardIdEqualTo(String value) { addCriterion("scene_card_id =", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdNotEqualTo(String value) { addCriterion("scene_card_id <>", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdGreaterThan(String value) { addCriterion("scene_card_id >", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdLessThan(String value) { addCriterion("scene_card_id <", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdLike(String value) { addCriterion("scene_card_id like", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdIn(List<String> values) { addCriterion("scene_card_id in", values, "sceneCardId"); return (Criteria) this; }
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
        public Criteria andCardTypeIsNull() { addCriterion("card_type is null"); return (Criteria) this; }
        public Criteria andCardTypeIsNotNull() { addCriterion("card_type is not null"); return (Criteria) this; }
        public Criteria andCardTypeEqualTo(String value) { addCriterion("card_type =", value, "cardType"); return (Criteria) this; }
        public Criteria andCardTypeNotEqualTo(String value) { addCriterion("card_type <>", value, "cardType"); return (Criteria) this; }
        public Criteria andCardTypeGreaterThan(String value) { addCriterion("card_type >", value, "cardType"); return (Criteria) this; }
        public Criteria andCardTypeLessThan(String value) { addCriterion("card_type <", value, "cardType"); return (Criteria) this; }
        public Criteria andCardTypeLike(String value) { addCriterion("card_type like", value, "cardType"); return (Criteria) this; }
        public Criteria andCardTypeIn(List<String> values) { addCriterion("card_type in", values, "cardType"); return (Criteria) this; }
        public Criteria andCardNameIsNull() { addCriterion("card_name is null"); return (Criteria) this; }
        public Criteria andCardNameIsNotNull() { addCriterion("card_name is not null"); return (Criteria) this; }
        public Criteria andCardNameEqualTo(String value) { addCriterion("card_name =", value, "cardName"); return (Criteria) this; }
        public Criteria andCardNameNotEqualTo(String value) { addCriterion("card_name <>", value, "cardName"); return (Criteria) this; }
        public Criteria andCardNameGreaterThan(String value) { addCriterion("card_name >", value, "cardName"); return (Criteria) this; }
        public Criteria andCardNameLessThan(String value) { addCriterion("card_name <", value, "cardName"); return (Criteria) this; }
        public Criteria andCardNameLike(String value) { addCriterion("card_name like", value, "cardName"); return (Criteria) this; }
        public Criteria andCardNameIn(List<String> values) { addCriterion("card_name in", values, "cardName"); return (Criteria) this; }
        public Criteria andCardStatusIsNull() { addCriterion("card_status is null"); return (Criteria) this; }
        public Criteria andCardStatusIsNotNull() { addCriterion("card_status is not null"); return (Criteria) this; }
        public Criteria andCardStatusEqualTo(String value) { addCriterion("card_status =", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusNotEqualTo(String value) { addCriterion("card_status <>", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusGreaterThan(String value) { addCriterion("card_status >", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusLessThan(String value) { addCriterion("card_status <", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusLike(String value) { addCriterion("card_status like", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusIn(List<String> values) { addCriterion("card_status in", values, "cardStatus"); return (Criteria) this; }
        public Criteria andAppearancePromptIsNull() { addCriterion("appearance_prompt is null"); return (Criteria) this; }
        public Criteria andAppearancePromptIsNotNull() { addCriterion("appearance_prompt is not null"); return (Criteria) this; }
        public Criteria andAppearancePromptEqualTo(String value) { addCriterion("appearance_prompt =", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptNotEqualTo(String value) { addCriterion("appearance_prompt <>", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptGreaterThan(String value) { addCriterion("appearance_prompt >", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptLessThan(String value) { addCriterion("appearance_prompt <", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptLike(String value) { addCriterion("appearance_prompt like", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptIn(List<String> values) { addCriterion("appearance_prompt in", values, "appearancePrompt"); return (Criteria) this; }
        public Criteria andRefImageAssetIdIsNull() { addCriterion("ref_image_asset_id is null"); return (Criteria) this; }
        public Criteria andRefImageAssetIdIsNotNull() { addCriterion("ref_image_asset_id is not null"); return (Criteria) this; }
        public Criteria andRefImageAssetIdEqualTo(String value) { addCriterion("ref_image_asset_id =", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdNotEqualTo(String value) { addCriterion("ref_image_asset_id <>", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdGreaterThan(String value) { addCriterion("ref_image_asset_id >", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdLessThan(String value) { addCriterion("ref_image_asset_id <", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdLike(String value) { addCriterion("ref_image_asset_id like", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdIn(List<String> values) { addCriterion("ref_image_asset_id in", values, "refImageAssetId"); return (Criteria) this; }
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

