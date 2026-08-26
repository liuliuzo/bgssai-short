package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortForeshadowExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortForeshadowExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andForeshadowIdIsNull() { addCriterion("foreshadow_id is null"); return (Criteria) this; }
        public Criteria andForeshadowIdIsNotNull() { addCriterion("foreshadow_id is not null"); return (Criteria) this; }
        public Criteria andForeshadowIdEqualTo(String value) { addCriterion("foreshadow_id =", value, "foreshadowId"); return (Criteria) this; }
        public Criteria andForeshadowIdNotEqualTo(String value) { addCriterion("foreshadow_id <>", value, "foreshadowId"); return (Criteria) this; }
        public Criteria andForeshadowIdGreaterThan(String value) { addCriterion("foreshadow_id >", value, "foreshadowId"); return (Criteria) this; }
        public Criteria andForeshadowIdLessThan(String value) { addCriterion("foreshadow_id <", value, "foreshadowId"); return (Criteria) this; }
        public Criteria andForeshadowIdLike(String value) { addCriterion("foreshadow_id like", value, "foreshadowId"); return (Criteria) this; }
        public Criteria andForeshadowIdIn(List<String> values) { addCriterion("foreshadow_id in", values, "foreshadowId"); return (Criteria) this; }
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
        public Criteria andPlantEpisodeNoIsNull() { addCriterion("plant_episode_no is null"); return (Criteria) this; }
        public Criteria andPlantEpisodeNoIsNotNull() { addCriterion("plant_episode_no is not null"); return (Criteria) this; }
        public Criteria andPlantEpisodeNoEqualTo(Integer value) { addCriterion("plant_episode_no =", value, "plantEpisodeNo"); return (Criteria) this; }
        public Criteria andPlantEpisodeNoNotEqualTo(Integer value) { addCriterion("plant_episode_no <>", value, "plantEpisodeNo"); return (Criteria) this; }
        public Criteria andPlantEpisodeNoGreaterThan(Integer value) { addCriterion("plant_episode_no >", value, "plantEpisodeNo"); return (Criteria) this; }
        public Criteria andPlantEpisodeNoLessThan(Integer value) { addCriterion("plant_episode_no <", value, "plantEpisodeNo"); return (Criteria) this; }
        public Criteria andPlantEpisodeNoIn(List<Integer> values) { addCriterion("plant_episode_no in", values, "plantEpisodeNo"); return (Criteria) this; }
        public Criteria andPayoffEpisodeNoIsNull() { addCriterion("payoff_episode_no is null"); return (Criteria) this; }
        public Criteria andPayoffEpisodeNoIsNotNull() { addCriterion("payoff_episode_no is not null"); return (Criteria) this; }
        public Criteria andPayoffEpisodeNoEqualTo(Integer value) { addCriterion("payoff_episode_no =", value, "payoffEpisodeNo"); return (Criteria) this; }
        public Criteria andPayoffEpisodeNoNotEqualTo(Integer value) { addCriterion("payoff_episode_no <>", value, "payoffEpisodeNo"); return (Criteria) this; }
        public Criteria andPayoffEpisodeNoGreaterThan(Integer value) { addCriterion("payoff_episode_no >", value, "payoffEpisodeNo"); return (Criteria) this; }
        public Criteria andPayoffEpisodeNoLessThan(Integer value) { addCriterion("payoff_episode_no <", value, "payoffEpisodeNo"); return (Criteria) this; }
        public Criteria andPayoffEpisodeNoIn(List<Integer> values) { addCriterion("payoff_episode_no in", values, "payoffEpisodeNo"); return (Criteria) this; }
        public Criteria andForeshadowDescIsNull() { addCriterion("foreshadow_desc is null"); return (Criteria) this; }
        public Criteria andForeshadowDescIsNotNull() { addCriterion("foreshadow_desc is not null"); return (Criteria) this; }
        public Criteria andForeshadowDescEqualTo(String value) { addCriterion("foreshadow_desc =", value, "foreshadowDesc"); return (Criteria) this; }
        public Criteria andForeshadowDescNotEqualTo(String value) { addCriterion("foreshadow_desc <>", value, "foreshadowDesc"); return (Criteria) this; }
        public Criteria andForeshadowDescGreaterThan(String value) { addCriterion("foreshadow_desc >", value, "foreshadowDesc"); return (Criteria) this; }
        public Criteria andForeshadowDescLessThan(String value) { addCriterion("foreshadow_desc <", value, "foreshadowDesc"); return (Criteria) this; }
        public Criteria andForeshadowDescLike(String value) { addCriterion("foreshadow_desc like", value, "foreshadowDesc"); return (Criteria) this; }
        public Criteria andForeshadowDescIn(List<String> values) { addCriterion("foreshadow_desc in", values, "foreshadowDesc"); return (Criteria) this; }
        public Criteria andStatusIsNull() { addCriterion("status is null"); return (Criteria) this; }
        public Criteria andStatusIsNotNull() { addCriterion("status is not null"); return (Criteria) this; }
        public Criteria andStatusEqualTo(String value) { addCriterion("status =", value, "status"); return (Criteria) this; }
        public Criteria andStatusNotEqualTo(String value) { addCriterion("status <>", value, "status"); return (Criteria) this; }
        public Criteria andStatusGreaterThan(String value) { addCriterion("status >", value, "status"); return (Criteria) this; }
        public Criteria andStatusLessThan(String value) { addCriterion("status <", value, "status"); return (Criteria) this; }
        public Criteria andStatusLike(String value) { addCriterion("status like", value, "status"); return (Criteria) this; }
        public Criteria andStatusIn(List<String> values) { addCriterion("status in", values, "status"); return (Criteria) this; }
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

