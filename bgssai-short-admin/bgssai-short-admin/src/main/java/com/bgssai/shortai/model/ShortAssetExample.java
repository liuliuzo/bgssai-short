package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortAssetExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortAssetExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andAssetIdIsNull() { addCriterion("asset_id is null"); return (Criteria) this; }
        public Criteria andAssetIdIsNotNull() { addCriterion("asset_id is not null"); return (Criteria) this; }
        public Criteria andAssetIdEqualTo(String value) { addCriterion("asset_id =", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdNotEqualTo(String value) { addCriterion("asset_id <>", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdGreaterThan(String value) { addCriterion("asset_id >", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdLessThan(String value) { addCriterion("asset_id <", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdLike(String value) { addCriterion("asset_id like", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdIn(List<String> values) { addCriterion("asset_id in", values, "assetId"); return (Criteria) this; }
        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdGreaterThan(String value) { addCriterion("user_id >", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLessThan(String value) { addCriterion("user_id <", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdLike(String value) { addCriterion("user_id like", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }
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
        public Criteria andAssetTypeIsNull() { addCriterion("asset_type is null"); return (Criteria) this; }
        public Criteria andAssetTypeIsNotNull() { addCriterion("asset_type is not null"); return (Criteria) this; }
        public Criteria andAssetTypeEqualTo(String value) { addCriterion("asset_type =", value, "assetType"); return (Criteria) this; }
        public Criteria andAssetTypeNotEqualTo(String value) { addCriterion("asset_type <>", value, "assetType"); return (Criteria) this; }
        public Criteria andAssetTypeGreaterThan(String value) { addCriterion("asset_type >", value, "assetType"); return (Criteria) this; }
        public Criteria andAssetTypeLessThan(String value) { addCriterion("asset_type <", value, "assetType"); return (Criteria) this; }
        public Criteria andAssetTypeLike(String value) { addCriterion("asset_type like", value, "assetType"); return (Criteria) this; }
        public Criteria andAssetTypeIn(List<String> values) { addCriterion("asset_type in", values, "assetType"); return (Criteria) this; }
        public Criteria andStorageKeyIsNull() { addCriterion("storage_key is null"); return (Criteria) this; }
        public Criteria andStorageKeyIsNotNull() { addCriterion("storage_key is not null"); return (Criteria) this; }
        public Criteria andStorageKeyEqualTo(String value) { addCriterion("storage_key =", value, "storageKey"); return (Criteria) this; }
        public Criteria andStorageKeyNotEqualTo(String value) { addCriterion("storage_key <>", value, "storageKey"); return (Criteria) this; }
        public Criteria andStorageKeyGreaterThan(String value) { addCriterion("storage_key >", value, "storageKey"); return (Criteria) this; }
        public Criteria andStorageKeyLessThan(String value) { addCriterion("storage_key <", value, "storageKey"); return (Criteria) this; }
        public Criteria andStorageKeyLike(String value) { addCriterion("storage_key like", value, "storageKey"); return (Criteria) this; }
        public Criteria andStorageKeyIn(List<String> values) { addCriterion("storage_key in", values, "storageKey"); return (Criteria) this; }
        public Criteria andFileUrlIsNull() { addCriterion("file_url is null"); return (Criteria) this; }
        public Criteria andFileUrlIsNotNull() { addCriterion("file_url is not null"); return (Criteria) this; }
        public Criteria andFileUrlEqualTo(String value) { addCriterion("file_url =", value, "fileUrl"); return (Criteria) this; }
        public Criteria andFileUrlNotEqualTo(String value) { addCriterion("file_url <>", value, "fileUrl"); return (Criteria) this; }
        public Criteria andFileUrlGreaterThan(String value) { addCriterion("file_url >", value, "fileUrl"); return (Criteria) this; }
        public Criteria andFileUrlLessThan(String value) { addCriterion("file_url <", value, "fileUrl"); return (Criteria) this; }
        public Criteria andFileUrlLike(String value) { addCriterion("file_url like", value, "fileUrl"); return (Criteria) this; }
        public Criteria andFileUrlIn(List<String> values) { addCriterion("file_url in", values, "fileUrl"); return (Criteria) this; }
        public Criteria andFileSizeByteIsNull() { addCriterion("file_size_byte is null"); return (Criteria) this; }
        public Criteria andFileSizeByteIsNotNull() { addCriterion("file_size_byte is not null"); return (Criteria) this; }
        public Criteria andFileSizeByteEqualTo(Integer value) { addCriterion("file_size_byte =", value, "fileSizeByte"); return (Criteria) this; }
        public Criteria andFileSizeByteNotEqualTo(Integer value) { addCriterion("file_size_byte <>", value, "fileSizeByte"); return (Criteria) this; }
        public Criteria andFileSizeByteGreaterThan(Integer value) { addCriterion("file_size_byte >", value, "fileSizeByte"); return (Criteria) this; }
        public Criteria andFileSizeByteLessThan(Integer value) { addCriterion("file_size_byte <", value, "fileSizeByte"); return (Criteria) this; }
        public Criteria andFileSizeByteIn(List<Integer> values) { addCriterion("file_size_byte in", values, "fileSizeByte"); return (Criteria) this; }
        public Criteria andDurationSecIsNull() { addCriterion("duration_sec is null"); return (Criteria) this; }
        public Criteria andDurationSecIsNotNull() { addCriterion("duration_sec is not null"); return (Criteria) this; }
        public Criteria andDurationSecEqualTo(Integer value) { addCriterion("duration_sec =", value, "durationSec"); return (Criteria) this; }
        public Criteria andDurationSecNotEqualTo(Integer value) { addCriterion("duration_sec <>", value, "durationSec"); return (Criteria) this; }
        public Criteria andDurationSecGreaterThan(Integer value) { addCriterion("duration_sec >", value, "durationSec"); return (Criteria) this; }
        public Criteria andDurationSecLessThan(Integer value) { addCriterion("duration_sec <", value, "durationSec"); return (Criteria) this; }
        public Criteria andDurationSecIn(List<Integer> values) { addCriterion("duration_sec in", values, "durationSec"); return (Criteria) this; }
        public Criteria andWidthIsNull() { addCriterion("width is null"); return (Criteria) this; }
        public Criteria andWidthIsNotNull() { addCriterion("width is not null"); return (Criteria) this; }
        public Criteria andWidthEqualTo(Integer value) { addCriterion("width =", value, "width"); return (Criteria) this; }
        public Criteria andWidthNotEqualTo(Integer value) { addCriterion("width <>", value, "width"); return (Criteria) this; }
        public Criteria andWidthGreaterThan(Integer value) { addCriterion("width >", value, "width"); return (Criteria) this; }
        public Criteria andWidthLessThan(Integer value) { addCriterion("width <", value, "width"); return (Criteria) this; }
        public Criteria andWidthIn(List<Integer> values) { addCriterion("width in", values, "width"); return (Criteria) this; }
        public Criteria andHeightIsNull() { addCriterion("height is null"); return (Criteria) this; }
        public Criteria andHeightIsNotNull() { addCriterion("height is not null"); return (Criteria) this; }
        public Criteria andHeightEqualTo(Integer value) { addCriterion("height =", value, "height"); return (Criteria) this; }
        public Criteria andHeightNotEqualTo(Integer value) { addCriterion("height <>", value, "height"); return (Criteria) this; }
        public Criteria andHeightGreaterThan(Integer value) { addCriterion("height >", value, "height"); return (Criteria) this; }
        public Criteria andHeightLessThan(Integer value) { addCriterion("height <", value, "height"); return (Criteria) this; }
        public Criteria andHeightIn(List<Integer> values) { addCriterion("height in", values, "height"); return (Criteria) this; }
        public Criteria andSourceTaskIdIsNull() { addCriterion("source_task_id is null"); return (Criteria) this; }
        public Criteria andSourceTaskIdIsNotNull() { addCriterion("source_task_id is not null"); return (Criteria) this; }
        public Criteria andSourceTaskIdEqualTo(String value) { addCriterion("source_task_id =", value, "sourceTaskId"); return (Criteria) this; }
        public Criteria andSourceTaskIdNotEqualTo(String value) { addCriterion("source_task_id <>", value, "sourceTaskId"); return (Criteria) this; }
        public Criteria andSourceTaskIdGreaterThan(String value) { addCriterion("source_task_id >", value, "sourceTaskId"); return (Criteria) this; }
        public Criteria andSourceTaskIdLessThan(String value) { addCriterion("source_task_id <", value, "sourceTaskId"); return (Criteria) this; }
        public Criteria andSourceTaskIdLike(String value) { addCriterion("source_task_id like", value, "sourceTaskId"); return (Criteria) this; }
        public Criteria andSourceTaskIdIn(List<String> values) { addCriterion("source_task_id in", values, "sourceTaskId"); return (Criteria) this; }
        public Criteria andSelectedFlagIsNull() { addCriterion("selected_flag is null"); return (Criteria) this; }
        public Criteria andSelectedFlagIsNotNull() { addCriterion("selected_flag is not null"); return (Criteria) this; }
        public Criteria andSelectedFlagEqualTo(Integer value) { addCriterion("selected_flag =", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagNotEqualTo(Integer value) { addCriterion("selected_flag <>", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagGreaterThan(Integer value) { addCriterion("selected_flag >", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagLessThan(Integer value) { addCriterion("selected_flag <", value, "selectedFlag"); return (Criteria) this; }
        public Criteria andSelectedFlagIn(List<Integer> values) { addCriterion("selected_flag in", values, "selectedFlag"); return (Criteria) this; }
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

