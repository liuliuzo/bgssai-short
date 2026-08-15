package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubscriptionPlanExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    private String[] selectColumns;

    public SubscriptionPlanExample() {
        oredCriteria = new ArrayList<>();
    }

    public SubscriptionPlanExample selectColumns(String... cols) {
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

        public Criteria andPlanCodeIsNull() { addCriterion("plan_code is null"); return (Criteria) this; }
        public Criteria andPlanCodeIsNotNull() { addCriterion("plan_code is not null"); return (Criteria) this; }
        public Criteria andPlanCodeEqualTo(String value) { addCriterion("plan_code =", value, "planCode"); return (Criteria) this; }
        public Criteria andPlanCodeNotEqualTo(String value) { addCriterion("plan_code <>", value, "planCode"); return (Criteria) this; }
        public Criteria andPlanCodeGreaterThan(String value) { addCriterion("plan_code >", value, "planCode"); return (Criteria) this; }
        public Criteria andPlanCodeGreaterThanOrEqualTo(String value) { addCriterion("plan_code >=", value, "planCode"); return (Criteria) this; }
        public Criteria andPlanCodeLessThan(String value) { addCriterion("plan_code <", value, "planCode"); return (Criteria) this; }
        public Criteria andPlanCodeLessThanOrEqualTo(String value) { addCriterion("plan_code <=", value, "planCode"); return (Criteria) this; }
        public Criteria andPlanCodeIn(List<String> values) { addCriterion("plan_code in", values, "planCode"); return (Criteria) this; }
        public Criteria andPlanCodeLike(String value) { addCriterion("plan_code like", value, "planCode"); return (Criteria) this; }

        public Criteria andPlanNameIsNull() { addCriterion("plan_name is null"); return (Criteria) this; }
        public Criteria andPlanNameIsNotNull() { addCriterion("plan_name is not null"); return (Criteria) this; }
        public Criteria andPlanNameEqualTo(String value) { addCriterion("plan_name =", value, "planName"); return (Criteria) this; }
        public Criteria andPlanNameNotEqualTo(String value) { addCriterion("plan_name <>", value, "planName"); return (Criteria) this; }
        public Criteria andPlanNameGreaterThan(String value) { addCriterion("plan_name >", value, "planName"); return (Criteria) this; }
        public Criteria andPlanNameGreaterThanOrEqualTo(String value) { addCriterion("plan_name >=", value, "planName"); return (Criteria) this; }
        public Criteria andPlanNameLessThan(String value) { addCriterion("plan_name <", value, "planName"); return (Criteria) this; }
        public Criteria andPlanNameLessThanOrEqualTo(String value) { addCriterion("plan_name <=", value, "planName"); return (Criteria) this; }
        public Criteria andPlanNameIn(List<String> values) { addCriterion("plan_name in", values, "planName"); return (Criteria) this; }
        public Criteria andPlanNameLike(String value) { addCriterion("plan_name like", value, "planName"); return (Criteria) this; }

        public Criteria andPriceCentIsNull() { addCriterion("price_cent is null"); return (Criteria) this; }
        public Criteria andPriceCentIsNotNull() { addCriterion("price_cent is not null"); return (Criteria) this; }
        public Criteria andPriceCentEqualTo(Integer value) { addCriterion("price_cent =", value, "priceCent"); return (Criteria) this; }
        public Criteria andPriceCentNotEqualTo(Integer value) { addCriterion("price_cent <>", value, "priceCent"); return (Criteria) this; }
        public Criteria andPriceCentGreaterThan(Integer value) { addCriterion("price_cent >", value, "priceCent"); return (Criteria) this; }
        public Criteria andPriceCentGreaterThanOrEqualTo(Integer value) { addCriterion("price_cent >=", value, "priceCent"); return (Criteria) this; }
        public Criteria andPriceCentLessThan(Integer value) { addCriterion("price_cent <", value, "priceCent"); return (Criteria) this; }
        public Criteria andPriceCentLessThanOrEqualTo(Integer value) { addCriterion("price_cent <=", value, "priceCent"); return (Criteria) this; }
        public Criteria andPriceCentIn(List<Integer> values) { addCriterion("price_cent in", values, "priceCent"); return (Criteria) this; }

        public Criteria andMonthlyBalanceCentIsNull() { addCriterion("monthly_balance_cent is null"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentIsNotNull() { addCriterion("monthly_balance_cent is not null"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentEqualTo(Integer value) { addCriterion("monthly_balance_cent =", value, "monthlyBalanceCent"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentNotEqualTo(Integer value) { addCriterion("monthly_balance_cent <>", value, "monthlyBalanceCent"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentGreaterThan(Integer value) { addCriterion("monthly_balance_cent >", value, "monthlyBalanceCent"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentGreaterThanOrEqualTo(Integer value) { addCriterion("monthly_balance_cent >=", value, "monthlyBalanceCent"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentLessThan(Integer value) { addCriterion("monthly_balance_cent <", value, "monthlyBalanceCent"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentLessThanOrEqualTo(Integer value) { addCriterion("monthly_balance_cent <=", value, "monthlyBalanceCent"); return (Criteria) this; }
        public Criteria andMonthlyBalanceCentIn(List<Integer> values) { addCriterion("monthly_balance_cent in", values, "monthlyBalanceCent"); return (Criteria) this; }

        public Criteria andMonthlyLimitCentIsNull() { addCriterion("monthly_limit_cent is null"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentIsNotNull() { addCriterion("monthly_limit_cent is not null"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentEqualTo(Integer value) { addCriterion("monthly_limit_cent =", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentNotEqualTo(Integer value) { addCriterion("monthly_limit_cent <>", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentGreaterThan(Integer value) { addCriterion("monthly_limit_cent >", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentGreaterThanOrEqualTo(Integer value) { addCriterion("monthly_limit_cent >=", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentLessThan(Integer value) { addCriterion("monthly_limit_cent <", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentLessThanOrEqualTo(Integer value) { addCriterion("monthly_limit_cent <=", value, "monthlyLimitCent"); return (Criteria) this; }
        public Criteria andMonthlyLimitCentIn(List<Integer> values) { addCriterion("monthly_limit_cent in", values, "monthlyLimitCent"); return (Criteria) this; }

        public Criteria andActiveIsNull() { addCriterion("is_active is null"); return (Criteria) this; }
        public Criteria andActiveIsNotNull() { addCriterion("is_active is not null"); return (Criteria) this; }
        public Criteria andActiveEqualTo(Integer value) { addCriterion("is_active =", value, "active"); return (Criteria) this; }
        public Criteria andActiveNotEqualTo(Integer value) { addCriterion("is_active <>", value, "active"); return (Criteria) this; }
        public Criteria andActiveGreaterThan(Integer value) { addCriterion("is_active >", value, "active"); return (Criteria) this; }
        public Criteria andActiveGreaterThanOrEqualTo(Integer value) { addCriterion("is_active >=", value, "active"); return (Criteria) this; }
        public Criteria andActiveLessThan(Integer value) { addCriterion("is_active <", value, "active"); return (Criteria) this; }
        public Criteria andActiveLessThanOrEqualTo(Integer value) { addCriterion("is_active <=", value, "active"); return (Criteria) this; }
        public Criteria andActiveIn(List<Integer> values) { addCriterion("is_active in", values, "active"); return (Criteria) this; }

        public Criteria andSortOrderIsNull() { addCriterion("sort_order is null"); return (Criteria) this; }
        public Criteria andSortOrderIsNotNull() { addCriterion("sort_order is not null"); return (Criteria) this; }
        public Criteria andSortOrderEqualTo(Integer value) { addCriterion("sort_order =", value, "sortOrder"); return (Criteria) this; }
        public Criteria andSortOrderNotEqualTo(Integer value) { addCriterion("sort_order <>", value, "sortOrder"); return (Criteria) this; }
        public Criteria andSortOrderGreaterThan(Integer value) { addCriterion("sort_order >", value, "sortOrder"); return (Criteria) this; }
        public Criteria andSortOrderGreaterThanOrEqualTo(Integer value) { addCriterion("sort_order >=", value, "sortOrder"); return (Criteria) this; }
        public Criteria andSortOrderLessThan(Integer value) { addCriterion("sort_order <", value, "sortOrder"); return (Criteria) this; }
        public Criteria andSortOrderLessThanOrEqualTo(Integer value) { addCriterion("sort_order <=", value, "sortOrder"); return (Criteria) this; }
        public Criteria andSortOrderIn(List<Integer> values) { addCriterion("sort_order in", values, "sortOrder"); return (Criteria) this; }

        public Criteria andDeleteIsNull() { addCriterion("is_delete is null"); return (Criteria) this; }
        public Criteria andDeleteIsNotNull() { addCriterion("is_delete is not null"); return (Criteria) this; }
        public Criteria andDeleteEqualTo(Integer value) { addCriterion("is_delete =", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteNotEqualTo(Integer value) { addCriterion("is_delete <>", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteGreaterThan(Integer value) { addCriterion("is_delete >", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteGreaterThanOrEqualTo(Integer value) { addCriterion("is_delete >=", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteLessThan(Integer value) { addCriterion("is_delete <", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteLessThanOrEqualTo(Integer value) { addCriterion("is_delete <=", value, "delete"); return (Criteria) this; }
        public Criteria andDeleteIn(List<Integer> values) { addCriterion("is_delete in", values, "delete"); return (Criteria) this; }

        public Criteria andCreatorIsNull() { addCriterion("creator is null"); return (Criteria) this; }
        public Criteria andCreatorIsNotNull() { addCriterion("creator is not null"); return (Criteria) this; }
        public Criteria andCreatorEqualTo(String value) { addCriterion("creator =", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorNotEqualTo(String value) { addCriterion("creator <>", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorGreaterThan(String value) { addCriterion("creator >", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorGreaterThanOrEqualTo(String value) { addCriterion("creator >=", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorLessThan(String value) { addCriterion("creator <", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorLessThanOrEqualTo(String value) { addCriterion("creator <=", value, "creator"); return (Criteria) this; }
        public Criteria andCreatorIn(List<String> values) { addCriterion("creator in", values, "creator"); return (Criteria) this; }
        public Criteria andCreatorLike(String value) { addCriterion("creator like", value, "creator"); return (Criteria) this; }

        public Criteria andModifierIsNull() { addCriterion("modifier is null"); return (Criteria) this; }
        public Criteria andModifierIsNotNull() { addCriterion("modifier is not null"); return (Criteria) this; }
        public Criteria andModifierEqualTo(String value) { addCriterion("modifier =", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierNotEqualTo(String value) { addCriterion("modifier <>", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierGreaterThan(String value) { addCriterion("modifier >", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierGreaterThanOrEqualTo(String value) { addCriterion("modifier >=", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierLessThan(String value) { addCriterion("modifier <", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierLessThanOrEqualTo(String value) { addCriterion("modifier <=", value, "modifier"); return (Criteria) this; }
        public Criteria andModifierIn(List<String> values) { addCriterion("modifier in", values, "modifier"); return (Criteria) this; }
        public Criteria andModifierLike(String value) { addCriterion("modifier like", value, "modifier"); return (Criteria) this; }

        public Criteria andGmtCreateIsNull() { addCriterion("gmt_create is null"); return (Criteria) this; }
        public Criteria andGmtCreateIsNotNull() { addCriterion("gmt_create is not null"); return (Criteria) this; }
        public Criteria andGmtCreateEqualTo(Date value) { addCriterion("gmt_create =", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateNotEqualTo(Date value) { addCriterion("gmt_create <>", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateGreaterThan(Date value) { addCriterion("gmt_create >", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) { addCriterion("gmt_create >=", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateLessThan(Date value) { addCriterion("gmt_create <", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateLessThanOrEqualTo(Date value) { addCriterion("gmt_create <=", value, "gmtCreate"); return (Criteria) this; }
        public Criteria andGmtCreateIn(List<Date> values) { addCriterion("gmt_create in", values, "gmtCreate"); return (Criteria) this; }

        public Criteria andGmtModifiedIsNull() { addCriterion("gmt_modified is null"); return (Criteria) this; }
        public Criteria andGmtModifiedIsNotNull() { addCriterion("gmt_modified is not null"); return (Criteria) this; }
        public Criteria andGmtModifiedEqualTo(Date value) { addCriterion("gmt_modified =", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedNotEqualTo(Date value) { addCriterion("gmt_modified <>", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedGreaterThan(Date value) { addCriterion("gmt_modified >", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) { addCriterion("gmt_modified >=", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedLessThan(Date value) { addCriterion("gmt_modified <", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) { addCriterion("gmt_modified <=", value, "gmtModified"); return (Criteria) this; }
        public Criteria andGmtModifiedIn(List<Date> values) { addCriterion("gmt_modified in", values, "gmtModified"); return (Criteria) this; }

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
