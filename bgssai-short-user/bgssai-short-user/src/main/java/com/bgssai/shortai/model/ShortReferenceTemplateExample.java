package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortReferenceTemplateExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortReferenceTemplateExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andTemplateIdIsNull() { addCriterion("template_id is null"); return (Criteria) this; }
        public Criteria andTemplateIdIsNotNull() { addCriterion("template_id is not null"); return (Criteria) this; }
        public Criteria andTemplateIdEqualTo(String value) { addCriterion("template_id =", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdNotEqualTo(String value) { addCriterion("template_id <>", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdGreaterThan(String value) { addCriterion("template_id >", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdLessThan(String value) { addCriterion("template_id <", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdLike(String value) { addCriterion("template_id like", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdIn(List<String> values) { addCriterion("template_id in", values, "templateId"); return (Criteria) this; }
        public Criteria andTemplateNameIsNull() { addCriterion("template_name is null"); return (Criteria) this; }
        public Criteria andTemplateNameIsNotNull() { addCriterion("template_name is not null"); return (Criteria) this; }
        public Criteria andTemplateNameEqualTo(String value) { addCriterion("template_name =", value, "templateName"); return (Criteria) this; }
        public Criteria andTemplateNameNotEqualTo(String value) { addCriterion("template_name <>", value, "templateName"); return (Criteria) this; }
        public Criteria andTemplateNameGreaterThan(String value) { addCriterion("template_name >", value, "templateName"); return (Criteria) this; }
        public Criteria andTemplateNameLessThan(String value) { addCriterion("template_name <", value, "templateName"); return (Criteria) this; }
        public Criteria andTemplateNameLike(String value) { addCriterion("template_name like", value, "templateName"); return (Criteria) this; }
        public Criteria andTemplateNameIn(List<String> values) { addCriterion("template_name in", values, "templateName"); return (Criteria) this; }
        public Criteria andGenreIsNull() { addCriterion("genre is null"); return (Criteria) this; }
        public Criteria andGenreIsNotNull() { addCriterion("genre is not null"); return (Criteria) this; }
        public Criteria andGenreEqualTo(String value) { addCriterion("genre =", value, "genre"); return (Criteria) this; }
        public Criteria andGenreNotEqualTo(String value) { addCriterion("genre <>", value, "genre"); return (Criteria) this; }
        public Criteria andGenreGreaterThan(String value) { addCriterion("genre >", value, "genre"); return (Criteria) this; }
        public Criteria andGenreLessThan(String value) { addCriterion("genre <", value, "genre"); return (Criteria) this; }
        public Criteria andGenreLike(String value) { addCriterion("genre like", value, "genre"); return (Criteria) this; }
        public Criteria andGenreIn(List<String> values) { addCriterion("genre in", values, "genre"); return (Criteria) this; }
        public Criteria andHookTypeJsonIsNull() { addCriterion("hook_type_json is null"); return (Criteria) this; }
        public Criteria andHookTypeJsonIsNotNull() { addCriterion("hook_type_json is not null"); return (Criteria) this; }
        public Criteria andHookTypeJsonEqualTo(String value) { addCriterion("hook_type_json =", value, "hookTypeJson"); return (Criteria) this; }
        public Criteria andHookTypeJsonNotEqualTo(String value) { addCriterion("hook_type_json <>", value, "hookTypeJson"); return (Criteria) this; }
        public Criteria andHookTypeJsonGreaterThan(String value) { addCriterion("hook_type_json >", value, "hookTypeJson"); return (Criteria) this; }
        public Criteria andHookTypeJsonLessThan(String value) { addCriterion("hook_type_json <", value, "hookTypeJson"); return (Criteria) this; }
        public Criteria andHookTypeJsonLike(String value) { addCriterion("hook_type_json like", value, "hookTypeJson"); return (Criteria) this; }
        public Criteria andHookTypeJsonIn(List<String> values) { addCriterion("hook_type_json in", values, "hookTypeJson"); return (Criteria) this; }
        public Criteria andBeatSheetJsonIsNull() { addCriterion("beat_sheet_json is null"); return (Criteria) this; }
        public Criteria andBeatSheetJsonIsNotNull() { addCriterion("beat_sheet_json is not null"); return (Criteria) this; }
        public Criteria andBeatSheetJsonEqualTo(String value) { addCriterion("beat_sheet_json =", value, "beatSheetJson"); return (Criteria) this; }
        public Criteria andBeatSheetJsonNotEqualTo(String value) { addCriterion("beat_sheet_json <>", value, "beatSheetJson"); return (Criteria) this; }
        public Criteria andBeatSheetJsonGreaterThan(String value) { addCriterion("beat_sheet_json >", value, "beatSheetJson"); return (Criteria) this; }
        public Criteria andBeatSheetJsonLessThan(String value) { addCriterion("beat_sheet_json <", value, "beatSheetJson"); return (Criteria) this; }
        public Criteria andBeatSheetJsonLike(String value) { addCriterion("beat_sheet_json like", value, "beatSheetJson"); return (Criteria) this; }
        public Criteria andBeatSheetJsonIn(List<String> values) { addCriterion("beat_sheet_json in", values, "beatSheetJson"); return (Criteria) this; }
        public Criteria andTurningPointJsonIsNull() { addCriterion("turning_point_json is null"); return (Criteria) this; }
        public Criteria andTurningPointJsonIsNotNull() { addCriterion("turning_point_json is not null"); return (Criteria) this; }
        public Criteria andTurningPointJsonEqualTo(String value) { addCriterion("turning_point_json =", value, "turningPointJson"); return (Criteria) this; }
        public Criteria andTurningPointJsonNotEqualTo(String value) { addCriterion("turning_point_json <>", value, "turningPointJson"); return (Criteria) this; }
        public Criteria andTurningPointJsonGreaterThan(String value) { addCriterion("turning_point_json >", value, "turningPointJson"); return (Criteria) this; }
        public Criteria andTurningPointJsonLessThan(String value) { addCriterion("turning_point_json <", value, "turningPointJson"); return (Criteria) this; }
        public Criteria andTurningPointJsonLike(String value) { addCriterion("turning_point_json like", value, "turningPointJson"); return (Criteria) this; }
        public Criteria andTurningPointJsonIn(List<String> values) { addCriterion("turning_point_json in", values, "turningPointJson"); return (Criteria) this; }
        public Criteria andTabooListJsonIsNull() { addCriterion("taboo_list_json is null"); return (Criteria) this; }
        public Criteria andTabooListJsonIsNotNull() { addCriterion("taboo_list_json is not null"); return (Criteria) this; }
        public Criteria andTabooListJsonEqualTo(String value) { addCriterion("taboo_list_json =", value, "tabooListJson"); return (Criteria) this; }
        public Criteria andTabooListJsonNotEqualTo(String value) { addCriterion("taboo_list_json <>", value, "tabooListJson"); return (Criteria) this; }
        public Criteria andTabooListJsonGreaterThan(String value) { addCriterion("taboo_list_json >", value, "tabooListJson"); return (Criteria) this; }
        public Criteria andTabooListJsonLessThan(String value) { addCriterion("taboo_list_json <", value, "tabooListJson"); return (Criteria) this; }
        public Criteria andTabooListJsonLike(String value) { addCriterion("taboo_list_json like", value, "tabooListJson"); return (Criteria) this; }
        public Criteria andTabooListJsonIn(List<String> values) { addCriterion("taboo_list_json in", values, "tabooListJson"); return (Criteria) this; }
        public Criteria andEnabledFlagIsNull() { addCriterion("enabled_flag is null"); return (Criteria) this; }
        public Criteria andEnabledFlagIsNotNull() { addCriterion("enabled_flag is not null"); return (Criteria) this; }
        public Criteria andEnabledFlagEqualTo(Integer value) { addCriterion("enabled_flag =", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagNotEqualTo(Integer value) { addCriterion("enabled_flag <>", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagGreaterThan(Integer value) { addCriterion("enabled_flag >", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagLessThan(Integer value) { addCriterion("enabled_flag <", value, "enabledFlag"); return (Criteria) this; }
        public Criteria andEnabledFlagIn(List<Integer> values) { addCriterion("enabled_flag in", values, "enabledFlag"); return (Criteria) this; }
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

