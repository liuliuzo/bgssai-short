package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortReviewRecordExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortReviewRecordExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andReviewIdIsNull() { addCriterion("review_id is null"); return (Criteria) this; }
        public Criteria andReviewIdIsNotNull() { addCriterion("review_id is not null"); return (Criteria) this; }
        public Criteria andReviewIdEqualTo(String value) { addCriterion("review_id =", value, "reviewId"); return (Criteria) this; }
        public Criteria andReviewIdNotEqualTo(String value) { addCriterion("review_id <>", value, "reviewId"); return (Criteria) this; }
        public Criteria andReviewIdGreaterThan(String value) { addCriterion("review_id >", value, "reviewId"); return (Criteria) this; }
        public Criteria andReviewIdLessThan(String value) { addCriterion("review_id <", value, "reviewId"); return (Criteria) this; }
        public Criteria andReviewIdLike(String value) { addCriterion("review_id like", value, "reviewId"); return (Criteria) this; }
        public Criteria andReviewIdIn(List<String> values) { addCriterion("review_id in", values, "reviewId"); return (Criteria) this; }
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
        public Criteria andCreatorUserIdIsNull() { addCriterion("creator_user_id is null"); return (Criteria) this; }
        public Criteria andCreatorUserIdIsNotNull() { addCriterion("creator_user_id is not null"); return (Criteria) this; }
        public Criteria andCreatorUserIdEqualTo(String value) { addCriterion("creator_user_id =", value, "creatorUserId"); return (Criteria) this; }
        public Criteria andCreatorUserIdNotEqualTo(String value) { addCriterion("creator_user_id <>", value, "creatorUserId"); return (Criteria) this; }
        public Criteria andCreatorUserIdGreaterThan(String value) { addCriterion("creator_user_id >", value, "creatorUserId"); return (Criteria) this; }
        public Criteria andCreatorUserIdLessThan(String value) { addCriterion("creator_user_id <", value, "creatorUserId"); return (Criteria) this; }
        public Criteria andCreatorUserIdLike(String value) { addCriterion("creator_user_id like", value, "creatorUserId"); return (Criteria) this; }
        public Criteria andCreatorUserIdIn(List<String> values) { addCriterion("creator_user_id in", values, "creatorUserId"); return (Criteria) this; }
        public Criteria andReviewStatusIsNull() { addCriterion("review_status is null"); return (Criteria) this; }
        public Criteria andReviewStatusIsNotNull() { addCriterion("review_status is not null"); return (Criteria) this; }
        public Criteria andReviewStatusEqualTo(String value) { addCriterion("review_status =", value, "reviewStatus"); return (Criteria) this; }
        public Criteria andReviewStatusNotEqualTo(String value) { addCriterion("review_status <>", value, "reviewStatus"); return (Criteria) this; }
        public Criteria andReviewStatusGreaterThan(String value) { addCriterion("review_status >", value, "reviewStatus"); return (Criteria) this; }
        public Criteria andReviewStatusLessThan(String value) { addCriterion("review_status <", value, "reviewStatus"); return (Criteria) this; }
        public Criteria andReviewStatusLike(String value) { addCriterion("review_status like", value, "reviewStatus"); return (Criteria) this; }
        public Criteria andReviewStatusIn(List<String> values) { addCriterion("review_status in", values, "reviewStatus"); return (Criteria) this; }
        public Criteria andRiskLevelIsNull() { addCriterion("risk_level is null"); return (Criteria) this; }
        public Criteria andRiskLevelIsNotNull() { addCriterion("risk_level is not null"); return (Criteria) this; }
        public Criteria andRiskLevelEqualTo(String value) { addCriterion("risk_level =", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelNotEqualTo(String value) { addCriterion("risk_level <>", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelGreaterThan(String value) { addCriterion("risk_level >", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelLessThan(String value) { addCriterion("risk_level <", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelLike(String value) { addCriterion("risk_level like", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelIn(List<String> values) { addCriterion("risk_level in", values, "riskLevel"); return (Criteria) this; }
        public Criteria andReviewerUserIdIsNull() { addCriterion("reviewer_user_id is null"); return (Criteria) this; }
        public Criteria andReviewerUserIdIsNotNull() { addCriterion("reviewer_user_id is not null"); return (Criteria) this; }
        public Criteria andReviewerUserIdEqualTo(String value) { addCriterion("reviewer_user_id =", value, "reviewerUserId"); return (Criteria) this; }
        public Criteria andReviewerUserIdNotEqualTo(String value) { addCriterion("reviewer_user_id <>", value, "reviewerUserId"); return (Criteria) this; }
        public Criteria andReviewerUserIdGreaterThan(String value) { addCriterion("reviewer_user_id >", value, "reviewerUserId"); return (Criteria) this; }
        public Criteria andReviewerUserIdLessThan(String value) { addCriterion("reviewer_user_id <", value, "reviewerUserId"); return (Criteria) this; }
        public Criteria andReviewerUserIdLike(String value) { addCriterion("reviewer_user_id like", value, "reviewerUserId"); return (Criteria) this; }
        public Criteria andReviewerUserIdIn(List<String> values) { addCriterion("reviewer_user_id in", values, "reviewerUserId"); return (Criteria) this; }
        public Criteria andReviewCommentIsNull() { addCriterion("review_comment is null"); return (Criteria) this; }
        public Criteria andReviewCommentIsNotNull() { addCriterion("review_comment is not null"); return (Criteria) this; }
        public Criteria andReviewCommentEqualTo(String value) { addCriterion("review_comment =", value, "reviewComment"); return (Criteria) this; }
        public Criteria andReviewCommentNotEqualTo(String value) { addCriterion("review_comment <>", value, "reviewComment"); return (Criteria) this; }
        public Criteria andReviewCommentGreaterThan(String value) { addCriterion("review_comment >", value, "reviewComment"); return (Criteria) this; }
        public Criteria andReviewCommentLessThan(String value) { addCriterion("review_comment <", value, "reviewComment"); return (Criteria) this; }
        public Criteria andReviewCommentLike(String value) { addCriterion("review_comment like", value, "reviewComment"); return (Criteria) this; }
        public Criteria andReviewCommentIn(List<String> values) { addCriterion("review_comment in", values, "reviewComment"); return (Criteria) this; }
        public Criteria andRejectItemJsonIsNull() { addCriterion("reject_item_json is null"); return (Criteria) this; }
        public Criteria andRejectItemJsonIsNotNull() { addCriterion("reject_item_json is not null"); return (Criteria) this; }
        public Criteria andRejectItemJsonEqualTo(String value) { addCriterion("reject_item_json =", value, "rejectItemJson"); return (Criteria) this; }
        public Criteria andRejectItemJsonNotEqualTo(String value) { addCriterion("reject_item_json <>", value, "rejectItemJson"); return (Criteria) this; }
        public Criteria andRejectItemJsonGreaterThan(String value) { addCriterion("reject_item_json >", value, "rejectItemJson"); return (Criteria) this; }
        public Criteria andRejectItemJsonLessThan(String value) { addCriterion("reject_item_json <", value, "rejectItemJson"); return (Criteria) this; }
        public Criteria andRejectItemJsonLike(String value) { addCriterion("reject_item_json like", value, "rejectItemJson"); return (Criteria) this; }
        public Criteria andRejectItemJsonIn(List<String> values) { addCriterion("reject_item_json in", values, "rejectItemJson"); return (Criteria) this; }
        public Criteria andSubmittedTimeIsNull() { addCriterion("submitted_time is null"); return (Criteria) this; }
        public Criteria andSubmittedTimeIsNotNull() { addCriterion("submitted_time is not null"); return (Criteria) this; }
        public Criteria andSubmittedTimeEqualTo(Date value) { addCriterion("submitted_time =", value, "submittedTime"); return (Criteria) this; }
        public Criteria andSubmittedTimeNotEqualTo(Date value) { addCriterion("submitted_time <>", value, "submittedTime"); return (Criteria) this; }
        public Criteria andSubmittedTimeGreaterThan(Date value) { addCriterion("submitted_time >", value, "submittedTime"); return (Criteria) this; }
        public Criteria andSubmittedTimeLessThan(Date value) { addCriterion("submitted_time <", value, "submittedTime"); return (Criteria) this; }
        public Criteria andSubmittedTimeIn(List<Date> values) { addCriterion("submitted_time in", values, "submittedTime"); return (Criteria) this; }
        public Criteria andReviewedTimeIsNull() { addCriterion("reviewed_time is null"); return (Criteria) this; }
        public Criteria andReviewedTimeIsNotNull() { addCriterion("reviewed_time is not null"); return (Criteria) this; }
        public Criteria andReviewedTimeEqualTo(Date value) { addCriterion("reviewed_time =", value, "reviewedTime"); return (Criteria) this; }
        public Criteria andReviewedTimeNotEqualTo(Date value) { addCriterion("reviewed_time <>", value, "reviewedTime"); return (Criteria) this; }
        public Criteria andReviewedTimeGreaterThan(Date value) { addCriterion("reviewed_time >", value, "reviewedTime"); return (Criteria) this; }
        public Criteria andReviewedTimeLessThan(Date value) { addCriterion("reviewed_time <", value, "reviewedTime"); return (Criteria) this; }
        public Criteria andReviewedTimeIn(List<Date> values) { addCriterion("reviewed_time in", values, "reviewedTime"); return (Criteria) this; }
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

