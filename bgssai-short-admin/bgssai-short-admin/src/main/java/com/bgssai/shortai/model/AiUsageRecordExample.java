package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class AiUsageRecordExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public AiUsageRecordExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andRecordIdIsNull() { addCriterion("record_id is null"); return (Criteria) this; }
        public Criteria andRecordIdIsNotNull() { addCriterion("record_id is not null"); return (Criteria) this; }
        public Criteria andRecordIdEqualTo(String value) { addCriterion("record_id =", value, "recordId"); return (Criteria) this; }
        public Criteria andRecordIdNotEqualTo(String value) { addCriterion("record_id <>", value, "recordId"); return (Criteria) this; }
        public Criteria andRecordIdGreaterThan(String value) { addCriterion("record_id >", value, "recordId"); return (Criteria) this; }
        public Criteria andRecordIdLessThan(String value) { addCriterion("record_id <", value, "recordId"); return (Criteria) this; }
        public Criteria andRecordIdLike(String value) { addCriterion("record_id like", value, "recordId"); return (Criteria) this; }
        public Criteria andRecordIdIn(List<String> values) { addCriterion("record_id in", values, "recordId"); return (Criteria) this; }
        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdGreaterThan(String value) { addCriterion("user_id >", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLessThan(String value) { addCriterion("user_id <", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLike(String value) { addCriterion("user_id like", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
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
        public Criteria andShotIdIsNull() { addCriterion("shot_id is null"); return (Criteria) this; }
        public Criteria andShotIdIsNotNull() { addCriterion("shot_id is not null"); return (Criteria) this; }
        public Criteria andShotIdEqualTo(String value) { addCriterion("shot_id =", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdNotEqualTo(String value) { addCriterion("shot_id <>", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdGreaterThan(String value) { addCriterion("shot_id >", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLessThan(String value) { addCriterion("shot_id <", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLike(String value) { addCriterion("shot_id like", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdIn(List<String> values) { addCriterion("shot_id in", values, "shotId"); return (Criteria) this; }
        public Criteria andTaskIdIsNull() { addCriterion("task_id is null"); return (Criteria) this; }
        public Criteria andTaskIdIsNotNull() { addCriterion("task_id is not null"); return (Criteria) this; }
        public Criteria andTaskIdEqualTo(String value) { addCriterion("task_id =", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdNotEqualTo(String value) { addCriterion("task_id <>", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdGreaterThan(String value) { addCriterion("task_id >", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdLessThan(String value) { addCriterion("task_id <", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdLike(String value) { addCriterion("task_id like", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdIn(List<String> values) { addCriterion("task_id in", values, "taskId"); return (Criteria) this; }
        public Criteria andAbilityTypeIsNull() { addCriterion("ability_type is null"); return (Criteria) this; }
        public Criteria andAbilityTypeIsNotNull() { addCriterion("ability_type is not null"); return (Criteria) this; }
        public Criteria andAbilityTypeEqualTo(String value) { addCriterion("ability_type =", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeNotEqualTo(String value) { addCriterion("ability_type <>", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeGreaterThan(String value) { addCriterion("ability_type >", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeLessThan(String value) { addCriterion("ability_type <", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeLike(String value) { addCriterion("ability_type like", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeIn(List<String> values) { addCriterion("ability_type in", values, "abilityType"); return (Criteria) this; }
        public Criteria andModelCodeIsNull() { addCriterion("model_code is null"); return (Criteria) this; }
        public Criteria andModelCodeIsNotNull() { addCriterion("model_code is not null"); return (Criteria) this; }
        public Criteria andModelCodeEqualTo(String value) { addCriterion("model_code =", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeNotEqualTo(String value) { addCriterion("model_code <>", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeGreaterThan(String value) { addCriterion("model_code >", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeLessThan(String value) { addCriterion("model_code <", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeLike(String value) { addCriterion("model_code like", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeIn(List<String> values) { addCriterion("model_code in", values, "modelCode"); return (Criteria) this; }
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
        public Criteria andUsageAmountIsNull() { addCriterion("usage_amount is null"); return (Criteria) this; }
        public Criteria andUsageAmountIsNotNull() { addCriterion("usage_amount is not null"); return (Criteria) this; }
        public Criteria andUsageAmountEqualTo(Integer value) { addCriterion("usage_amount =", value, "usageAmount"); return (Criteria) this; }
        public Criteria andUsageAmountNotEqualTo(Integer value) { addCriterion("usage_amount <>", value, "usageAmount"); return (Criteria) this; }
        public Criteria andUsageAmountGreaterThan(Integer value) { addCriterion("usage_amount >", value, "usageAmount"); return (Criteria) this; }
        public Criteria andUsageAmountLessThan(Integer value) { addCriterion("usage_amount <", value, "usageAmount"); return (Criteria) this; }
        public Criteria andUsageAmountIn(List<Integer> values) { addCriterion("usage_amount in", values, "usageAmount"); return (Criteria) this; }
        public Criteria andUnitPriceCentIsNull() { addCriterion("unit_price_cent is null"); return (Criteria) this; }
        public Criteria andUnitPriceCentIsNotNull() { addCriterion("unit_price_cent is not null"); return (Criteria) this; }
        public Criteria andUnitPriceCentEqualTo(Integer value) { addCriterion("unit_price_cent =", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentNotEqualTo(Integer value) { addCriterion("unit_price_cent <>", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentGreaterThan(Integer value) { addCriterion("unit_price_cent >", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentLessThan(Integer value) { addCriterion("unit_price_cent <", value, "unitPriceCent"); return (Criteria) this; }
        public Criteria andUnitPriceCentIn(List<Integer> values) { addCriterion("unit_price_cent in", values, "unitPriceCent"); return (Criteria) this; }
        public Criteria andCostCentIsNull() { addCriterion("cost_cent is null"); return (Criteria) this; }
        public Criteria andCostCentIsNotNull() { addCriterion("cost_cent is not null"); return (Criteria) this; }
        public Criteria andCostCentEqualTo(Integer value) { addCriterion("cost_cent =", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentNotEqualTo(Integer value) { addCriterion("cost_cent <>", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentGreaterThan(Integer value) { addCriterion("cost_cent >", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentLessThan(Integer value) { addCriterion("cost_cent <", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentIn(List<Integer> values) { addCriterion("cost_cent in", values, "costCent"); return (Criteria) this; }
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

