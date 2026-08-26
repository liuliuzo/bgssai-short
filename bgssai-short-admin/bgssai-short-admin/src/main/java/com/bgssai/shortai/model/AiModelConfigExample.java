package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class AiModelConfigExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public AiModelConfigExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andModelIdIsNull() { addCriterion("model_id is null"); return (Criteria) this; }
        public Criteria andModelIdIsNotNull() { addCriterion("model_id is not null"); return (Criteria) this; }
        public Criteria andModelIdEqualTo(String value) { addCriterion("model_id =", value, "modelId"); return (Criteria) this; }
        public Criteria andModelIdNotEqualTo(String value) { addCriterion("model_id <>", value, "modelId"); return (Criteria) this; }
        public Criteria andModelIdGreaterThan(String value) { addCriterion("model_id >", value, "modelId"); return (Criteria) this; }
        public Criteria andModelIdLessThan(String value) { addCriterion("model_id <", value, "modelId"); return (Criteria) this; }
        public Criteria andModelIdLike(String value) { addCriterion("model_id like", value, "modelId"); return (Criteria) this; }
        public Criteria andModelIdIn(List<String> values) { addCriterion("model_id in", values, "modelId"); return (Criteria) this; }
        public Criteria andAbilityTypeIsNull() { addCriterion("ability_type is null"); return (Criteria) this; }
        public Criteria andAbilityTypeIsNotNull() { addCriterion("ability_type is not null"); return (Criteria) this; }
        public Criteria andAbilityTypeEqualTo(String value) { addCriterion("ability_type =", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeNotEqualTo(String value) { addCriterion("ability_type <>", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeGreaterThan(String value) { addCriterion("ability_type >", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeLessThan(String value) { addCriterion("ability_type <", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeLike(String value) { addCriterion("ability_type like", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeIn(List<String> values) { addCriterion("ability_type in", values, "abilityType"); return (Criteria) this; }
        public Criteria andProviderCodeIsNull() { addCriterion("provider_code is null"); return (Criteria) this; }
        public Criteria andProviderCodeIsNotNull() { addCriterion("provider_code is not null"); return (Criteria) this; }
        public Criteria andProviderCodeEqualTo(String value) { addCriterion("provider_code =", value, "providerCode"); return (Criteria) this; }
        public Criteria andProviderCodeNotEqualTo(String value) { addCriterion("provider_code <>", value, "providerCode"); return (Criteria) this; }
        public Criteria andProviderCodeGreaterThan(String value) { addCriterion("provider_code >", value, "providerCode"); return (Criteria) this; }
        public Criteria andProviderCodeLessThan(String value) { addCriterion("provider_code <", value, "providerCode"); return (Criteria) this; }
        public Criteria andProviderCodeLike(String value) { addCriterion("provider_code like", value, "providerCode"); return (Criteria) this; }
        public Criteria andProviderCodeIn(List<String> values) { addCriterion("provider_code in", values, "providerCode"); return (Criteria) this; }
        public Criteria andModelCodeIsNull() { addCriterion("model_code is null"); return (Criteria) this; }
        public Criteria andModelCodeIsNotNull() { addCriterion("model_code is not null"); return (Criteria) this; }
        public Criteria andModelCodeEqualTo(String value) { addCriterion("model_code =", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeNotEqualTo(String value) { addCriterion("model_code <>", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeGreaterThan(String value) { addCriterion("model_code >", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeLessThan(String value) { addCriterion("model_code <", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeLike(String value) { addCriterion("model_code like", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeIn(List<String> values) { addCriterion("model_code in", values, "modelCode"); return (Criteria) this; }
        public Criteria andModelNameIsNull() { addCriterion("model_name is null"); return (Criteria) this; }
        public Criteria andModelNameIsNotNull() { addCriterion("model_name is not null"); return (Criteria) this; }
        public Criteria andModelNameEqualTo(String value) { addCriterion("model_name =", value, "modelName"); return (Criteria) this; }
        public Criteria andModelNameNotEqualTo(String value) { addCriterion("model_name <>", value, "modelName"); return (Criteria) this; }
        public Criteria andModelNameGreaterThan(String value) { addCriterion("model_name >", value, "modelName"); return (Criteria) this; }
        public Criteria andModelNameLessThan(String value) { addCriterion("model_name <", value, "modelName"); return (Criteria) this; }
        public Criteria andModelNameLike(String value) { addCriterion("model_name like", value, "modelName"); return (Criteria) this; }
        public Criteria andModelNameIn(List<String> values) { addCriterion("model_name in", values, "modelName"); return (Criteria) this; }
        public Criteria andQualityLevelIsNull() { addCriterion("quality_level is null"); return (Criteria) this; }
        public Criteria andQualityLevelIsNotNull() { addCriterion("quality_level is not null"); return (Criteria) this; }
        public Criteria andQualityLevelEqualTo(String value) { addCriterion("quality_level =", value, "qualityLevel"); return (Criteria) this; }
        public Criteria andQualityLevelNotEqualTo(String value) { addCriterion("quality_level <>", value, "qualityLevel"); return (Criteria) this; }
        public Criteria andQualityLevelGreaterThan(String value) { addCriterion("quality_level >", value, "qualityLevel"); return (Criteria) this; }
        public Criteria andQualityLevelLessThan(String value) { addCriterion("quality_level <", value, "qualityLevel"); return (Criteria) this; }
        public Criteria andQualityLevelLike(String value) { addCriterion("quality_level like", value, "qualityLevel"); return (Criteria) this; }
        public Criteria andQualityLevelIn(List<String> values) { addCriterion("quality_level in", values, "qualityLevel"); return (Criteria) this; }
        public Criteria andPriceUnitIsNull() { addCriterion("price_unit is null"); return (Criteria) this; }
        public Criteria andPriceUnitIsNotNull() { addCriterion("price_unit is not null"); return (Criteria) this; }
        public Criteria andPriceUnitEqualTo(String value) { addCriterion("price_unit =", value, "priceUnit"); return (Criteria) this; }
        public Criteria andPriceUnitNotEqualTo(String value) { addCriterion("price_unit <>", value, "priceUnit"); return (Criteria) this; }
        public Criteria andPriceUnitGreaterThan(String value) { addCriterion("price_unit >", value, "priceUnit"); return (Criteria) this; }
        public Criteria andPriceUnitLessThan(String value) { addCriterion("price_unit <", value, "priceUnit"); return (Criteria) this; }
        public Criteria andPriceUnitLike(String value) { addCriterion("price_unit like", value, "priceUnit"); return (Criteria) this; }
        public Criteria andPriceUnitIn(List<String> values) { addCriterion("price_unit in", values, "priceUnit"); return (Criteria) this; }
        public Criteria andUnitPriceCentIsNull() { addCriterion("unit_price_cent is null"); return (Criteria) this; }
        public Criteria andUnitPriceCentIsNotNull() { addCriterion("unit_price_cent is not null"); return (Criteria) this; }
        public Criteria andUnitPriceCentEqualTo(Integer value) { addCriterion("unit_price_cent =", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentNotEqualTo(Integer value) { addCriterion("unit_price_cent <>", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentGreaterThan(Integer value) { addCriterion("unit_price_cent >", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentLessThan(Integer value) { addCriterion("unit_price_cent <", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentIn(List<Integer> values) { addCriterion("unit_price_cent in", values, "unitPriceCent"); return (Criteria) this; }
        public Criteria andDefaultFlagIsNull() { addCriterion("default_flag is null"); return (Criteria) this; }
        public Criteria andDefaultFlagIsNotNull() { addCriterion("default_flag is not null"); return (Criteria) this; }
        public Criteria andDefaultFlagEqualTo(Integer value) { addCriterion("default_flag =", value, "defaultFlag"); return (Criteria) this; }
        public Criteria andDefaultFlagNotEqualTo(Integer value) { addCriterion("default_flag <>", value, "defaultFlag"); return (Criteria) this; }
        public Criteria andDefaultFlagGreaterThan(Integer value) { addCriterion("default_flag >", value, "defaultFlag"); return (Criteria) this; }
        public Criteria andDefaultFlagLessThan(Integer value) { addCriterion("default_flag <", value, "defaultFlag"); return (Criteria) this; }
        public Criteria andDefaultFlagIn(List<Integer> values) { addCriterion("default_flag in", values, "defaultFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagIsNull() { addCriterion("enabled_flag is null"); return (Criteria) this; }
        public Criteria andEnabledFlagIsNotNull() { addCriterion("enabled_flag is not null"); return (Criteria) this; }
        public Criteria andEnabledFlagEqualTo(Integer value) { addCriterion("enabled_flag =", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagNotEqualTo(Integer value) { addCriterion("enabled_flag <>", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagGreaterThan(Integer value) { addCriterion("enabled_flag >", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagLessThan(Integer value) { addCriterion("enabled_flag <", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagIn(List<Integer> values) { addCriterion("enabled_flag in", values, "enabledFlag"); return (Criteria) this; }
        public Criteria andTimeoutSecIsNull() { addCriterion("timeout_sec is null"); return (Criteria) this; }
        public Criteria andTimeoutSecIsNotNull() { addCriterion("timeout_sec is not null"); return (Criteria) this; }
        public Criteria andTimeoutSecEqualTo(Integer value) { addCriterion("timeout_sec =", value, "timeoutSec"); return (Criteria) this; }
        public Criteria andTimeoutSecNotEqualTo(Integer value) { addCriterion("timeout_sec <>", value, "timeoutSec"); return (Criteria) this; }
        public Criteria andTimeoutSecGreaterThan(Integer value) { addCriterion("timeout_sec >", value, "timeoutSec"); return (Criteria) this; }
        public Criteria andTimeoutSecLessThan(Integer value) { addCriterion("timeout_sec <", value, "timeoutSec"); return (Criteria) this; }
        public Criteria andTimeoutSecIn(List<Integer> values) { addCriterion("timeout_sec in", values, "timeoutSec"); return (Criteria) this; }
        public Criteria andMaxRetryIsNull() { addCriterion("max_retry is null"); return (Criteria) this; }
        public Criteria andMaxRetryIsNotNull() { addCriterion("max_retry is not null"); return (Criteria) this; }
        public Criteria andMaxRetryEqualTo(Integer value) { addCriterion("max_retry =", value, "maxRetry"); return (Criteria) this; }
        public Criteria andMaxRetryNotEqualTo(Integer value) { addCriterion("max_retry <>", value, "maxRetry"); return (Criteria) this; }
        public Criteria andMaxRetryGreaterThan(Integer value) { addCriterion("max_retry >", value, "maxRetry"); return (Criteria) this; }
        public Criteria andMaxRetryLessThan(Integer value) { addCriterion("max_retry <", value, "maxRetry"); return (Criteria) this; }
        public Criteria andMaxRetryIn(List<Integer> values) { addCriterion("max_retry in", values, "maxRetry"); return (Criteria) this; }
        public Criteria andConfigJsonIsNull() { addCriterion("config_json is null"); return (Criteria) this; }
        public Criteria andConfigJsonIsNotNull() { addCriterion("config_json is not null"); return (Criteria) this; }
        public Criteria andConfigJsonEqualTo(String value) { addCriterion("config_json =", value, "configJson"); return (Criteria) this; }
        public Criteria andConfigJsonNotEqualTo(String value) { addCriterion("config_json <>", value, "configJson"); return (Criteria) this; }
        public Criteria andConfigJsonGreaterThan(String value) { addCriterion("config_json >", value, "configJson"); return (Criteria) this; }
        public Criteria andConfigJsonLessThan(String value) { addCriterion("config_json <", value, "configJson"); return (Criteria) this; }
        public Criteria andConfigJsonLike(String value) { addCriterion("config_json like", value, "configJson"); return (Criteria) this; }
        public Criteria andConfigJsonIn(List<String> values) { addCriterion("config_json in", values, "configJson"); return (Criteria) this; }
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

