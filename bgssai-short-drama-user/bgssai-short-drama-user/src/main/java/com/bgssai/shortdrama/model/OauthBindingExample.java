package com.bgssai.shortdrama.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class OauthBindingExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    protected String selectColumns;
    public OauthBindingExample() { oredCriteria = new ArrayList<>(); }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
    public boolean isDistinct() { return distinct; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }
    public void or(Criteria criteria) { oredCriteria.add(criteria); }
    public Criteria or() { Criteria c = createCriteriaInternal(); oredCriteria.add(c); return c; }
    public Criteria createCriteria() { Criteria c = createCriteriaInternal(); if (oredCriteria.isEmpty()) oredCriteria.add(c); return c; }
    protected Criteria createCriteriaInternal() { return new Criteria(); }
    public void clear() { oredCriteria.clear(); orderByClause = null; distinct = false; selectColumns = null; }
    public void selectColumns(String... cols) { if (cols == null || cols.length == 0) { selectColumns = null; return; } selectColumns = String.join(",", cols); }
    public String getSelectColumns() { return selectColumns; }
    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria = new ArrayList<>();
        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }
        protected void addCriterion(String condition) { if (condition == null) throw new RuntimeException("Value for condition cannot be null"); criteria.add(new Criterion(condition)); }
        protected void addCriterion(String condition, Object value, String property) { if (value == null) throw new RuntimeException("Value for " + property + " cannot be null"); criteria.add(new Criterion(condition, value)); }
        protected void addCriterion(String condition, Object value1, Object value2, String property) { if (value1 == null || value2 == null) throw new RuntimeException("Between values for " + property + " cannot be null"); criteria.add(new Criterion(condition, value1, value2)); }
        public Criteria andIdIsNull() { addCriterion("id is null"); return (Criteria) this; }
        public Criteria andIdIsNotNull() { addCriterion("id is not null"); return (Criteria) this; }
        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return (Criteria) this; }
        public Criteria andIdNotEqualTo(Long value) { addCriterion("id <>", value, "id"); return (Criteria) this; }
        public Criteria andIdIn(List<Long> values) { addCriterion("id in", values, "id"); return (Criteria) this; }
        public Criteria andBindingIdIsNull() { addCriterion("binding_id is null"); return (Criteria) this; }
        public Criteria andBindingIdIsNotNull() { addCriterion("binding_id is not null"); return (Criteria) this; }
        public Criteria andBindingIdEqualTo(String value) { addCriterion("binding_id =", value, "bindingId"); return (Criteria) this; }
        public Criteria andBindingIdNotEqualTo(String value) { addCriterion("binding_id <>", value, "bindingId"); return (Criteria) this; }
        public Criteria andBindingIdIn(List<String> values) { addCriterion("binding_id in", values, "bindingId"); return (Criteria) this; }
        public Criteria andPlatformIsNull() { addCriterion("platform is null"); return (Criteria) this; }
        public Criteria andPlatformIsNotNull() { addCriterion("platform is not null"); return (Criteria) this; }
        public Criteria andPlatformEqualTo(String value) { addCriterion("platform =", value, "platform"); return (Criteria) this; }
        public Criteria andPlatformNotEqualTo(String value) { addCriterion("platform <>", value, "platform"); return (Criteria) this; }
        public Criteria andPlatformIn(List<String> values) { addCriterion("platform in", values, "platform"); return (Criteria) this; }
        public Criteria andOpenIdIsNull() { addCriterion("open_id is null"); return (Criteria) this; }
        public Criteria andOpenIdIsNotNull() { addCriterion("open_id is not null"); return (Criteria) this; }
        public Criteria andOpenIdEqualTo(String value) { addCriterion("open_id =", value, "openId"); return (Criteria) this; }
        public Criteria andOpenIdNotEqualTo(String value) { addCriterion("open_id <>", value, "openId"); return (Criteria) this; }
        public Criteria andOpenIdIn(List<String> values) { addCriterion("open_id in", values, "openId"); return (Criteria) this; }
        public Criteria andUnionIdIsNull() { addCriterion("union_id is null"); return (Criteria) this; }
        public Criteria andUnionIdIsNotNull() { addCriterion("union_id is not null"); return (Criteria) this; }
        public Criteria andUnionIdEqualTo(String value) { addCriterion("union_id =", value, "unionId"); return (Criteria) this; }
        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
        public Criteria andNicknameIsNull() { addCriterion("nickname is null"); return (Criteria) this; }
        public Criteria andNicknameIsNotNull() { addCriterion("nickname is not null"); return (Criteria) this; }
        public Criteria andNicknameEqualTo(String value) { addCriterion("nickname =", value, "nickname"); return (Criteria) this; }
        public Criteria andAvatarUrlIsNull() { addCriterion("avatar_url is null"); return (Criteria) this; }
        public Criteria andAvatarUrlIsNotNull() { addCriterion("avatar_url is not null"); return (Criteria) this; }
        public Criteria andAvatarUrlEqualTo(String value) { addCriterion("avatar_url =", value, "avatarUrl"); return (Criteria) this; }
        public Criteria andDeleteIsNull() { addCriterion("is_delete is null"); return (Criteria) this; }
        public Criteria andDeleteIsNotNull() { addCriterion("is_delete is not null"); return (Criteria) this; }
        public Criteria andDeleteEqualTo(Boolean value) { addCriterion("is_delete =", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteNotEqualTo(Boolean value) { addCriterion("is_delete <>", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteIn(List<Boolean> values) { addCriterion("is_delete in", values, "delete"); return (Criteria) this; }
        public Criteria andCreatorIsNull() { addCriterion("creator is null"); return (Criteria) this; }
        public Criteria andCreatorIsNotNull() { addCriterion("creator is not null"); return (Criteria) this; }
        public Criteria andCreatorEqualTo(String value) { addCriterion("creator =", value, "creator"); return (Criteria) this; }
        public Criteria andModifierIsNull() { addCriterion("modifier is null"); return (Criteria) this; }
        public Criteria andModifierIsNotNull() { addCriterion("modifier is not null"); return (Criteria) this; }
        public Criteria andModifierEqualTo(String value) { addCriterion("modifier =", value, "modifier"); return (Criteria) this; }
        public Criteria andGmtCreateIsNull() { addCriterion("gmt_create is null"); return (Criteria) this; }
        public Criteria andGmtCreateIsNotNull() { addCriterion("gmt_create is not null"); return (Criteria) this; }
        public Criteria andGmtCreateEqualTo(Date value) { addCriterion("gmt_create =", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateNotEqualTo(Date value) { addCriterion("gmt_create <>", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateIn(List<Date> values) { addCriterion("gmt_create in", values, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtModifiedIsNull() { addCriterion("gmt_modified is null"); return (Criteria) this; }
        public Criteria andGmtModifiedIsNotNull() { addCriterion("gmt_modified is not null"); return (Criteria) this; }
        public Criteria andGmtModifiedEqualTo(Date value) { addCriterion("gmt_modified =", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedNotEqualTo(Date value) { addCriterion("gmt_modified <>", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedIn(List<Date> values) { addCriterion("gmt_modified in", values, "gmtModified"); return (Criteria) this; }
    }
    public static class Criteria extends GeneratedCriteria { protected Criteria() {} }
    public static class Criterion {
        private String condition; private Object value; private Object secondValue;
        private boolean noValue; private boolean singleValue; private boolean betweenValue; private boolean listValue;
        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public Object getSecondValue() { return secondValue; }
        public boolean isNoValue() { return noValue; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isBetweenValue() { return betweenValue; }
        public boolean isListValue() { return listValue; }
        public String getTypeHandler() { return null; }
        protected Criterion(String condition) { this.condition = condition; this.noValue = true; }
        protected Criterion(String condition, Object value) { this.condition = condition; this.value = value; if (value instanceof List<?>) this.listValue = true; else this.singleValue = true; }
        protected Criterion(String condition, Object value1, Object value2) { this.condition = condition; this.value = value1; this.secondValue = value2; this.betweenValue = true; }
    }
}
