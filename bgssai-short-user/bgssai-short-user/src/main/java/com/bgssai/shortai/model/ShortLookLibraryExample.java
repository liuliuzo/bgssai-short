package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;

public class ShortLookLibraryExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;

    public ShortLookLibraryExample() { oredCriteria = new ArrayList<>(); }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
    public boolean isDistinct() { return distinct; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }
    public Criteria createCriteria() {
        Criteria criteria = new Criteria();
        if (oredCriteria.isEmpty()) oredCriteria.add(criteria);
        return criteria;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria = new ArrayList<>();
        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getCriteria() { return criteria; }
        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) throw new RuntimeException("Value for " + property + " cannot be null");
            criteria.add(new Criterion(condition, value));
        }
        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return (Criteria) this; }
        public Criteria andLibraryIdEqualTo(String value) { addCriterion("library_id =", value, "libraryId"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andStatusEqualTo(String value) { addCriterion("status =", value, "status"); return (Criteria) this; }
        public Criteria andDeletedFlagEqualTo(Integer value) { addCriterion("deleted_flag =", value, "deletedFlag"); return (Criteria) this; }
    }

    public static class Criteria extends GeneratedCriteria { }

    public static class Criterion {
        private final String condition;
        private final Object value;
        private final boolean noValue;
        private final boolean singleValue;
        private final boolean listValue;
        public Criterion(String condition, Object value) {
            this.condition = condition;
            this.value = value;
            this.noValue = false;
            this.listValue = value instanceof List<?>;
            this.singleValue = !this.listValue;
        }
        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public boolean isNoValue() { return noValue; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isBetweenValue() { return false; }
        public boolean isListValue() { return listValue; }
    }
}
