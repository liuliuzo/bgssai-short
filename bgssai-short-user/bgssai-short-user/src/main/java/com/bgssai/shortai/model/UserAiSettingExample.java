package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserAiSettingExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    private String[] selectColumns;

    public UserAiSettingExample() {
        oredCriteria = new ArrayList<>();
    }

    public UserAiSettingExample selectColumns(String... cols) {
        this.selectColumns = cols;
        return this;
    }

    public String[] getSelectColumns() { return selectColumns; }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
    public boolean isDistinct() { return distinct; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }
    public void or(Criteria criteria) { oredCriteria.add(criteria); }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.isEmpty()) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() { return new Criteria(); }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
        selectColumns = null;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() { criteria = new ArrayList<>(); }

        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() { addCriterion("id is null"); return (Criteria) this; }
        public Criteria andIdIsNotNull() { addCriterion("id is not null"); return (Criteria) this; }
        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return (Criteria) this; }
        public Criteria andIdNotEqualTo(Long value) { addCriterion("id <>", value, "id"); return (Criteria) this; }
        public Criteria andIdGreaterThan(Long value) { addCriterion("id >", value, "id"); return (Criteria) this; }
        public Criteria andIdGreaterThanOrEqualTo(Long value) { addCriterion("id >=", value, "id"); return (Criteria) this; }
        public Criteria andIdLessThan(Long value) { addCriterion("id <", value, "id"); return (Criteria) this; }
        public Criteria andIdLessThanOrEqualTo(Long value) { addCriterion("id <=", value, "id"); return (Criteria) this; }
        public Criteria andIdIn(List<Long> values) { addCriterion("id in", values, "id"); return (Criteria) this; }

        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
        public Criteria andUserIdLike(String value) { addCriterion("user_id like", value, "userId"); return (Criteria) this; }

        public Criteria andOnlyUserKeyIsNull() { addCriterion("is_only_user_key is null"); return (Criteria) this; }
        public Criteria andOnlyUserKeyIsNotNull() { addCriterion("is_only_user_key is not null"); return (Criteria) this; }
        public Criteria andOnlyUserKeyEqualTo(Integer value) { addCriterion("is_only_user_key =", value, "onlyUserKey"); return (Criteria) this; }
        public Criteria andOnlyUserKeyNotEqualTo(Integer value) { addCriterion("is_only_user_key <>", value, "onlyUserKey"); return (Criteria) this; }
        public Criteria andOnlyUserKeyIn(List<Integer> values) { addCriterion("is_only_user_key in", values, "onlyUserKey"); return (Criteria) this; }

        public Criteria andDelFlagIsNull() { addCriterion("del_flag is null"); return (Criteria) this; }
        public Criteria andDelFlagIsNotNull() { addCriterion("del_flag is not null"); return (Criteria) this; }
        public Criteria andDelFlagEqualTo(Integer value) { addCriterion("del_flag =", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagNotEqualTo(Integer value) { addCriterion("del_flag <>", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagIn(List<Integer> values) { addCriterion("del_flag in", values, "delFlag"); return (Criteria) this; }

        public Criteria andCreatorIsNull() { addCriterion("creator is null"); return (Criteria) this; }
        public Criteria andCreatorIsNotNull() { addCriterion("creator is not null"); return (Criteria) this; }
        public Criteria andCreatorEqualTo(String value) { addCriterion("creator =", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorNotEqualTo(String value) { addCriterion("creator <>", value, "creator"); return (Criteria) this; }

        public Criteria andModifierIsNull() { addCriterion("modifier is null"); return (Criteria) this; }
        public Criteria andModifierIsNotNull() { addCriterion("modifier is not null"); return (Criteria) this; }
        public Criteria andModifierEqualTo(String value) { addCriterion("modifier =", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierNotEqualTo(String value) { addCriterion("modifier <>", value, "modifier"); return (Criteria) this; }

        public Criteria andGmtCreateIsNull() { addCriterion("gmt_create is null"); return (Criteria) this; }
        public Criteria andGmtCreateIsNotNull() { addCriterion("gmt_create is not null"); return (Criteria) this; }
        public Criteria andGmtCreateEqualTo(Date value) { addCriterion("gmt_create =", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) { addCriterion("gmt_create >=", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateLessThanOrEqualTo(Date value) { addCriterion("gmt_create <=", value, "gmtCreate"); return (Criteria) this; }

        public Criteria andGmtModifiedIsNull() { addCriterion("gmt_modified is null"); return (Criteria) this; }
        public Criteria andGmtModifiedIsNotNull() { addCriterion("gmt_modified is not null"); return (Criteria) this; }
        public Criteria andGmtModifiedEqualTo(Date value) { addCriterion("gmt_modified =", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) { addCriterion("gmt_modified >=", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) { addCriterion("gmt_modified <=", value, "gmtModified"); return (Criteria) this; }
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
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

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
