package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortComplianceHitExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortComplianceHitExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andHitIdIsNull() { addCriterion("hit_id is null"); return (Criteria) this; }
        public Criteria andHitIdIsNotNull() { addCriterion("hit_id is not null"); return (Criteria) this; }
        public Criteria andHitIdEqualTo(String value) { addCriterion("hit_id =", value, "hitId"); return (Criteria) this; }
        public Criteria andHitIdNotEqualTo(String value) { addCriterion("hit_id <>", value, "hitId"); return (Criteria) this; }
        public Criteria andHitIdGreaterThan(String value) { addCriterion("hit_id >", value, "hitId"); return (Criteria) this; }
        public Criteria andHitIdLessThan(String value) { addCriterion("hit_id <", value, "hitId"); return (Criteria) this; }
        public Criteria andHitIdLike(String value) { addCriterion("hit_id like", value, "hitId"); return (Criteria) this; }
        public Criteria andHitIdIn(List<String> values) { addCriterion("hit_id in", values, "hitId"); return (Criteria) this; }
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
        public Criteria andCheckScopeIsNull() { addCriterion("check_scope is null"); return (Criteria) this; }
        public Criteria andCheckScopeIsNotNull() { addCriterion("check_scope is not null"); return (Criteria) this; }
        public Criteria andCheckScopeEqualTo(String value) { addCriterion("check_scope =", value, "checkScope"); return (Criteria) this; }
        public Criteria andCheckScopeNotEqualTo(String value) { addCriterion("check_scope <>", value, "checkScope"); return (Criteria) this; }
        public Criteria andCheckScopeGreaterThan(String value) { addCriterion("check_scope >", value, "checkScope"); return (Criteria) this; }
        public Criteria andCheckScopeLessThan(String value) { addCriterion("check_scope <", value, "checkScope"); return (Criteria) this; }
        public Criteria andCheckScopeLike(String value) { addCriterion("check_scope like", value, "checkScope"); return (Criteria) this; }
        public Criteria andCheckScopeIn(List<String> values) { addCriterion("check_scope in", values, "checkScope"); return (Criteria) this; }
        public Criteria andRiskLevelIsNull() { addCriterion("risk_level is null"); return (Criteria) this; }
        public Criteria andRiskLevelIsNotNull() { addCriterion("risk_level is not null"); return (Criteria) this; }
        public Criteria andRiskLevelEqualTo(String value) { addCriterion("risk_level =", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelNotEqualTo(String value) { addCriterion("risk_level <>", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelGreaterThan(String value) { addCriterion("risk_level >", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelLessThan(String value) { addCriterion("risk_level <", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelLike(String value) { addCriterion("risk_level like", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelIn(List<String> values) { addCriterion("risk_level in", values, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskCategoryIsNull() { addCriterion("risk_category is null"); return (Criteria) this; }
        public Criteria andRiskCategoryIsNotNull() { addCriterion("risk_category is not null"); return (Criteria) this; }
        public Criteria andRiskCategoryEqualTo(String value) { addCriterion("risk_category =", value, "riskCategory"); return (Criteria) this; }
        public Criteria andRiskCategoryNotEqualTo(String value) { addCriterion("risk_category <>", value, "riskCategory"); return (Criteria) this; }
        public Criteria andRiskCategoryGreaterThan(String value) { addCriterion("risk_category >", value, "riskCategory"); return (Criteria) this; }
        public Criteria andRiskCategoryLessThan(String value) { addCriterion("risk_category <", value, "riskCategory"); return (Criteria) this; }
        public Criteria andRiskCategoryLike(String value) { addCriterion("risk_category like", value, "riskCategory"); return (Criteria) this; }
        public Criteria andRiskCategoryIn(List<String> values) { addCriterion("risk_category in", values, "riskCategory"); return (Criteria) this; }
        public Criteria andHitTextIsNull() { addCriterion("hit_text is null"); return (Criteria) this; }
        public Criteria andHitTextIsNotNull() { addCriterion("hit_text is not null"); return (Criteria) this; }
        public Criteria andHitTextEqualTo(String value) { addCriterion("hit_text =", value, "hitText"); return (Criteria) this; }
        public Criteria andHitTextNotEqualTo(String value) { addCriterion("hit_text <>", value, "hitText"); return (Criteria) this; }
        public Criteria andHitTextGreaterThan(String value) { addCriterion("hit_text >", value, "hitText"); return (Criteria) this; }
        public Criteria andHitTextLessThan(String value) { addCriterion("hit_text <", value, "hitText"); return (Criteria) this; }
        public Criteria andHitTextLike(String value) { addCriterion("hit_text like", value, "hitText"); return (Criteria) this; }
        public Criteria andHitTextIn(List<String> values) { addCriterion("hit_text in", values, "hitText"); return (Criteria) this; }
        public Criteria andSuggestionIsNull() { addCriterion("suggestion is null"); return (Criteria) this; }
        public Criteria andSuggestionIsNotNull() { addCriterion("suggestion is not null"); return (Criteria) this; }
        public Criteria andSuggestionEqualTo(String value) { addCriterion("suggestion =", value, "suggestion"); return (Criteria) this; }
        public Criteria andSuggestionNotEqualTo(String value) { addCriterion("suggestion <>", value, "suggestion"); return (Criteria) this; }
        public Criteria andSuggestionGreaterThan(String value) { addCriterion("suggestion >", value, "suggestion"); return (Criteria) this; }
        public Criteria andSuggestionLessThan(String value) { addCriterion("suggestion <", value, "suggestion"); return (Criteria) this; }
        public Criteria andSuggestionLike(String value) { addCriterion("suggestion like", value, "suggestion"); return (Criteria) this; }
        public Criteria andSuggestionIn(List<String> values) { addCriterion("suggestion in", values, "suggestion"); return (Criteria) this; }
        public Criteria andSceneNoIsNull() { addCriterion("scene_no is null"); return (Criteria) this; }
        public Criteria andSceneNoIsNotNull() { addCriterion("scene_no is not null"); return (Criteria) this; }
        public Criteria andSceneNoEqualTo(Integer value) { addCriterion("scene_no =", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoNotEqualTo(Integer value) { addCriterion("scene_no <>", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoGreaterThan(Integer value) { addCriterion("scene_no >", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoLessThan(Integer value) { addCriterion("scene_no <", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoIn(List<Integer> values) { addCriterion("scene_no in", values, "sceneNo"); return (Criteria) this; }
        public Criteria andLineNoIsNull() { addCriterion("line_no is null"); return (Criteria) this; }
        public Criteria andLineNoIsNotNull() { addCriterion("line_no is not null"); return (Criteria) this; }
        public Criteria andLineNoEqualTo(Integer value) { addCriterion("line_no =", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoNotEqualTo(Integer value) { addCriterion("line_no <>", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoGreaterThan(Integer value) { addCriterion("line_no >", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoLessThan(Integer value) { addCriterion("line_no <", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoIn(List<Integer> values) { addCriterion("line_no in", values, "lineNo"); return (Criteria) this; }
        public Criteria andShotNoIsNull() { addCriterion("shot_no is null"); return (Criteria) this; }
        public Criteria andShotNoIsNotNull() { addCriterion("shot_no is not null"); return (Criteria) this; }
        public Criteria andShotNoEqualTo(Integer value) { addCriterion("shot_no =", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoNotEqualTo(Integer value) { addCriterion("shot_no <>", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoGreaterThan(Integer value) { addCriterion("shot_no >", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoLessThan(Integer value) { addCriterion("shot_no <", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoIn(List<Integer> values) { addCriterion("shot_no in", values, "shotNo"); return (Criteria) this; }
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

