package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortSourceChapterExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortSourceChapterExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andChapterIdIsNull() { addCriterion("chapter_id is null"); return (Criteria) this; }
        public Criteria andChapterIdIsNotNull() { addCriterion("chapter_id is not null"); return (Criteria) this; }
        public Criteria andChapterIdEqualTo(String value) { addCriterion("chapter_id =", value, "chapterId"); return (Criteria) this; }
        public Criteria andChapterIdNotEqualTo(String value) { addCriterion("chapter_id <>", value, "chapterId"); return (Criteria) this; }
        public Criteria andChapterIdGreaterThan(String value) { addCriterion("chapter_id >", value, "chapterId"); return (Criteria) this; }
        public Criteria andChapterIdLessThan(String value) { addCriterion("chapter_id <", value, "chapterId"); return (Criteria) this; }
        public Criteria andChapterIdLike(String value) { addCriterion("chapter_id like", value, "chapterId"); return (Criteria) this; }
        public Criteria andChapterIdIn(List<String> values) { addCriterion("chapter_id in", values, "chapterId"); return (Criteria) this; }
        public Criteria andSourceIdIsNull() { addCriterion("source_id is null"); return (Criteria) this; }
        public Criteria andSourceIdIsNotNull() { addCriterion("source_id is not null"); return (Criteria) this; }
        public Criteria andSourceIdEqualTo(String value) { addCriterion("source_id =", value, "sourceId"); return (Criteria) this; }
        public Criteria andSourceIdNotEqualTo(String value) { addCriterion("source_id <>", value, "sourceId"); return (Criteria) this; }
        public Criteria andSourceIdGreaterThan(String value) { addCriterion("source_id >", value, "sourceId"); return (Criteria) this; }
        public Criteria andSourceIdLessThan(String value) { addCriterion("source_id <", value, "sourceId"); return (Criteria) this; }
        public Criteria andSourceIdLike(String value) { addCriterion("source_id like", value, "sourceId"); return (Criteria) this; }
        public Criteria andSourceIdIn(List<String> values) { addCriterion("source_id in", values, "sourceId"); return (Criteria) this; }
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
        public Criteria andChapterNoIsNull() { addCriterion("chapter_no is null"); return (Criteria) this; }
        public Criteria andChapterNoIsNotNull() { addCriterion("chapter_no is not null"); return (Criteria) this; }
        public Criteria andChapterNoEqualTo(Integer value) { addCriterion("chapter_no =", value, "chapterNo"); return (Criteria) this; }
        public Criteria andChapterNoNotEqualTo(Integer value) { addCriterion("chapter_no <>", value, "chapterNo"); return (Criteria) this; }
        public Criteria andChapterNoGreaterThan(Integer value) { addCriterion("chapter_no >", value, "chapterNo"); return (Criteria) this; }
        public Criteria andChapterNoLessThan(Integer value) { addCriterion("chapter_no <", value, "chapterNo"); return (Criteria) this; }
        public Criteria andChapterNoIn(List<Integer> values) { addCriterion("chapter_no in", values, "chapterNo"); return (Criteria) this; }
        public Criteria andChapterNoGreaterThanOrEqualTo(Integer value) { addCriterion("chapter_no >=", value, "chapterNo"); return (Criteria) this; }
        public Criteria andChapterNoLessThanOrEqualTo(Integer value) { addCriterion("chapter_no <=", value, "chapterNo"); return (Criteria) this; }
        public Criteria andChapterTitleIsNull() { addCriterion("chapter_title is null"); return (Criteria) this; }
        public Criteria andChapterTitleIsNotNull() { addCriterion("chapter_title is not null"); return (Criteria) this; }
        public Criteria andChapterTitleEqualTo(String value) { addCriterion("chapter_title =", value, "chapterTitle"); return (Criteria) this; }
        public Criteria andChapterTitleNotEqualTo(String value) { addCriterion("chapter_title <>", value, "chapterTitle"); return (Criteria) this; }
        public Criteria andChapterTitleGreaterThan(String value) { addCriterion("chapter_title >", value, "chapterTitle"); return (Criteria) this; }
        public Criteria andChapterTitleLessThan(String value) { addCriterion("chapter_title <", value, "chapterTitle"); return (Criteria) this; }
        public Criteria andChapterTitleLike(String value) { addCriterion("chapter_title like", value, "chapterTitle"); return (Criteria) this; }
        public Criteria andChapterTitleIn(List<String> values) { addCriterion("chapter_title in", values, "chapterTitle"); return (Criteria) this; }
        public Criteria andChapterTextIsNull() { addCriterion("chapter_text is null"); return (Criteria) this; }
        public Criteria andChapterTextIsNotNull() { addCriterion("chapter_text is not null"); return (Criteria) this; }
        public Criteria andChapterTextEqualTo(String value) { addCriterion("chapter_text =", value, "chapterText"); return (Criteria) this; }
        public Criteria andChapterTextNotEqualTo(String value) { addCriterion("chapter_text <>", value, "chapterText"); return (Criteria) this; }
        public Criteria andChapterTextGreaterThan(String value) { addCriterion("chapter_text >", value, "chapterText"); return (Criteria) this; }
        public Criteria andChapterTextLessThan(String value) { addCriterion("chapter_text <", value, "chapterText"); return (Criteria) this; }
        public Criteria andChapterTextLike(String value) { addCriterion("chapter_text like", value, "chapterText"); return (Criteria) this; }
        public Criteria andChapterTextIn(List<String> values) { addCriterion("chapter_text in", values, "chapterText"); return (Criteria) this; }
        public Criteria andCharCountIsNull() { addCriterion("char_count is null"); return (Criteria) this; }
        public Criteria andCharCountIsNotNull() { addCriterion("char_count is not null"); return (Criteria) this; }
        public Criteria andCharCountEqualTo(Integer value) { addCriterion("char_count =", value, "charCount"); return (Criteria) this; }
        public Criteria andCharCountNotEqualTo(Integer value) { addCriterion("char_count <>", value, "charCount"); return (Criteria) this; }
        public Criteria andCharCountGreaterThan(Integer value) { addCriterion("char_count >", value, "charCount"); return (Criteria) this; }
        public Criteria andCharCountLessThan(Integer value) { addCriterion("char_count <", value, "charCount"); return (Criteria) this; }
        public Criteria andCharCountIn(List<Integer> values) { addCriterion("char_count in", values, "charCount"); return (Criteria) this; }
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

