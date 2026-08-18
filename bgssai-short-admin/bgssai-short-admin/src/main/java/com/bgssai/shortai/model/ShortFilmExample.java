package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortFilmExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortFilmExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andFilmIdIsNull() { addCriterion("film_id is null"); return (Criteria) this; }
        public Criteria andFilmIdIsNotNull() { addCriterion("film_id is not null"); return (Criteria) this; }
        public Criteria andFilmIdEqualTo(String value) { addCriterion("film_id =", value, "filmId"); return (Criteria) this; }
        public Criteria andFilmIdNotEqualTo(String value) { addCriterion("film_id <>", value, "filmId"); return (Criteria) this; }
        public Criteria andFilmIdGreaterThan(String value) { addCriterion("film_id >", value, "filmId"); return (Criteria) this; }
        public Criteria andFilmIdLessThan(String value) { addCriterion("film_id <", value, "filmId"); return (Criteria) this; }
        public Criteria andFilmIdLike(String value) { addCriterion("film_id like", value, "filmId"); return (Criteria) this; }
        public Criteria andFilmIdIn(List<String> values) { addCriterion("film_id in", values, "filmId"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNull() { addCriterion("episode_id is null"); return (Criteria) this; }
        public Criteria andEpisodeIdIsNotNull() { addCriterion("episode_id is not null"); return (Criteria) this; }
        public Criteria andEpisodeIdEqualTo(String value) { addCriterion("episode_id =", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdNotEqualTo(String value) { addCriterion("episode_id <>", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdGreaterThan(String value) { addCriterion("episode_id >", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLessThan(String value) { addCriterion("episode_id <", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdLike(String value) { addCriterion("episode_id like", value, "episodeId"); return (Criteria) this; }
        public Criteria andEpisodeIdIn(List<String> values) { addCriterion("episode_id in", values, "episodeId"); return (Criteria) this; }
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
        public Criteria andFilmAssetIdIsNull() { addCriterion("film_asset_id is null"); return (Criteria) this; }
        public Criteria andFilmAssetIdIsNotNull() { addCriterion("film_asset_id is not null"); return (Criteria) this; }
        public Criteria andFilmAssetIdEqualTo(String value) { addCriterion("film_asset_id =", value, "filmAssetId"); return (Criteria) this; }
        public Criteria andFilmAssetIdNotEqualTo(String value) { addCriterion("film_asset_id <>", value, "filmAssetId"); return (Criteria) this; }
        public Criteria andFilmAssetIdGreaterThan(String value) { addCriterion("film_asset_id >", value, "filmAssetId"); return (Criteria) this; }
        public Criteria andFilmAssetIdLessThan(String value) { addCriterion("film_asset_id <", value, "filmAssetId"); return (Criteria) this; }
        public Criteria andFilmAssetIdLike(String value) { addCriterion("film_asset_id like", value, "filmAssetId"); return (Criteria) this; }
        public Criteria andFilmAssetIdIn(List<String> values) { addCriterion("film_asset_id in", values, "filmAssetId"); return (Criteria) this; }
        public Criteria andFilmDurationSecIsNull() { addCriterion("film_duration_sec is null"); return (Criteria) this; }
        public Criteria andFilmDurationSecIsNotNull() { addCriterion("film_duration_sec is not null"); return (Criteria) this; }
        public Criteria andFilmDurationSecEqualTo(Integer value) { addCriterion("film_duration_sec =", value, "filmDurationSec"); return (Criteria) this; }
        public Criteria andFilmDurationSecNotEqualTo(Integer value) { addCriterion("film_duration_sec <>", value, "filmDurationSec"); return (Criteria) this; }
        public Criteria andFilmDurationSecGreaterThan(Integer value) { addCriterion("film_duration_sec >", value, "filmDurationSec"); return (Criteria) this; }
        public Criteria andFilmDurationSecLessThan(Integer value) { addCriterion("film_duration_sec <", value, "filmDurationSec"); return (Criteria) this; }
        public Criteria andFilmDurationSecIn(List<Integer> values) { addCriterion("film_duration_sec in", values, "filmDurationSec"); return (Criteria) this; }
        public Criteria andComposeStatusIsNull() { addCriterion("compose_status is null"); return (Criteria) this; }
        public Criteria andComposeStatusIsNotNull() { addCriterion("compose_status is not null"); return (Criteria) this; }
        public Criteria andComposeStatusEqualTo(String value) { addCriterion("compose_status =", value, "composeStatus"); return (Criteria) this; }
        public Criteria andComposeStatusNotEqualTo(String value) { addCriterion("compose_status <>", value, "composeStatus"); return (Criteria) this; }
        public Criteria andComposeStatusGreaterThan(String value) { addCriterion("compose_status >", value, "composeStatus"); return (Criteria) this; }
        public Criteria andComposeStatusLessThan(String value) { addCriterion("compose_status <", value, "composeStatus"); return (Criteria) this; }
        public Criteria andComposeStatusLike(String value) { addCriterion("compose_status like", value, "composeStatus"); return (Criteria) this; }
        public Criteria andComposeStatusIn(List<String> values) { addCriterion("compose_status in", values, "composeStatus"); return (Criteria) this; }
        public Criteria andExpiredFlagIsNull() { addCriterion("expired_flag is null"); return (Criteria) this; }
        public Criteria andExpiredFlagIsNotNull() { addCriterion("expired_flag is not null"); return (Criteria) this; }
        public Criteria andExpiredFlagEqualTo(Integer value) { addCriterion("expired_flag =", value, "expiredFlag"); return (Criteria) this; }
        public Criteria andExpiredFlagNotEqualTo(Integer value) { addCriterion("expired_flag <>", value, "expiredFlag"); return (Criteria) this; }
        public Criteria andExpiredFlagGreaterThan(Integer value) { addCriterion("expired_flag >", value, "expiredFlag"); return (Criteria) this; }
        public Criteria andExpiredFlagLessThan(Integer value) { addCriterion("expired_flag <", value, "expiredFlag"); return (Criteria) this; }
        public Criteria andExpiredFlagIn(List<Integer> values) { addCriterion("expired_flag in", values, "expiredFlag"); return (Criteria) this; }
        public Criteria andSubtitleStaleFlagIsNull() { addCriterion("subtitle_stale_flag is null"); return (Criteria) this; }
        public Criteria andSubtitleStaleFlagIsNotNull() { addCriterion("subtitle_stale_flag is not null"); return (Criteria) this; }
        public Criteria andSubtitleStaleFlagEqualTo(Integer value) { addCriterion("subtitle_stale_flag =", value, "subtitleStaleFlag"); return (Criteria) this; }
        public Criteria andSubtitleStaleFlagNotEqualTo(Integer value) { addCriterion("subtitle_stale_flag <>", value, "subtitleStaleFlag"); return (Criteria) this; }
        public Criteria andSubtitleStaleFlagGreaterThan(Integer value) { addCriterion("subtitle_stale_flag >", value, "subtitleStaleFlag"); return (Criteria) this; }
        public Criteria andSubtitleStaleFlagLessThan(Integer value) { addCriterion("subtitle_stale_flag <", value, "subtitleStaleFlag"); return (Criteria) this; }
        public Criteria andSubtitleStaleFlagIn(List<Integer> values) { addCriterion("subtitle_stale_flag in", values, "subtitleStaleFlag"); return (Criteria) this; }
        public Criteria andComposeTaskIdIsNull() { addCriterion("compose_task_id is null"); return (Criteria) this; }
        public Criteria andComposeTaskIdIsNotNull() { addCriterion("compose_task_id is not null"); return (Criteria) this; }
        public Criteria andComposeTaskIdEqualTo(String value) { addCriterion("compose_task_id =", value, "composeTaskId"); return (Criteria) this; }
        public Criteria andComposeTaskIdNotEqualTo(String value) { addCriterion("compose_task_id <>", value, "composeTaskId"); return (Criteria) this; }
        public Criteria andComposeTaskIdGreaterThan(String value) { addCriterion("compose_task_id >", value, "composeTaskId"); return (Criteria) this; }
        public Criteria andComposeTaskIdLessThan(String value) { addCriterion("compose_task_id <", value, "composeTaskId"); return (Criteria) this; }
        public Criteria andComposeTaskIdLike(String value) { addCriterion("compose_task_id like", value, "composeTaskId"); return (Criteria) this; }
        public Criteria andComposeTaskIdIn(List<String> values) { addCriterion("compose_task_id in", values, "composeTaskId"); return (Criteria) this; }
        public Criteria andComposedTimeIsNull() { addCriterion("composed_time is null"); return (Criteria) this; }
        public Criteria andComposedTimeIsNotNull() { addCriterion("composed_time is not null"); return (Criteria) this; }
        public Criteria andComposedTimeEqualTo(Date value) { addCriterion("composed_time =", value, "composedTime"); return (Criteria) this; }
        public Criteria andComposedTimeNotEqualTo(Date value) { addCriterion("composed_time <>", value, "composedTime"); return (Criteria) this; }
        public Criteria andComposedTimeGreaterThan(Date value) { addCriterion("composed_time >", value, "composedTime"); return (Criteria) this; }
        public Criteria andComposedTimeLessThan(Date value) { addCriterion("composed_time <", value, "composedTime"); return (Criteria) this; }
        public Criteria andComposedTimeIn(List<Date> values) { addCriterion("composed_time in", values, "composedTime"); return (Criteria) this; }
        public Criteria andExportJsonIsNull() { addCriterion("export_json is null"); return (Criteria) this; }
        public Criteria andExportJsonIsNotNull() { addCriterion("export_json is not null"); return (Criteria) this; }
        public Criteria andExportJsonEqualTo(String value) { addCriterion("export_json =", value, "exportJson"); return (Criteria) this; }
        public Criteria andExportJsonNotEqualTo(String value) { addCriterion("export_json <>", value, "exportJson"); return (Criteria) this; }
        public Criteria andExportJsonGreaterThan(String value) { addCriterion("export_json >", value, "exportJson"); return (Criteria) this; }
        public Criteria andExportJsonLessThan(String value) { addCriterion("export_json <", value, "exportJson"); return (Criteria) this; }
        public Criteria andExportJsonLike(String value) { addCriterion("export_json like", value, "exportJson"); return (Criteria) this; }
        public Criteria andExportJsonIn(List<String> values) { addCriterion("export_json in", values, "exportJson"); return (Criteria) this; }
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

