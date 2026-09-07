package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortEditExportExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortEditExportExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andExportIdIsNull() { addCriterion("export_id is null"); return (Criteria) this; }
        public Criteria andExportIdIsNotNull() { addCriterion("export_id is not null"); return (Criteria) this; }
        public Criteria andExportIdEqualTo(String value) { addCriterion("export_id =", value, "exportId"); return (Criteria) this; }
        public Criteria andExportIdNotEqualTo(String value) { addCriterion("export_id <>", value, "exportId"); return (Criteria) this; }
        public Criteria andExportIdGreaterThan(String value) { addCriterion("export_id >", value, "exportId"); return (Criteria) this; }
        public Criteria andExportIdLessThan(String value) { addCriterion("export_id <", value, "exportId"); return (Criteria) this; }
        public Criteria andExportIdLike(String value) { addCriterion("export_id like", value, "exportId"); return (Criteria) this; }
        public Criteria andExportIdIn(List<String> values) { addCriterion("export_id in", values, "exportId"); return (Criteria) this; }
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
        public Criteria andProjectFormatIsNull() { addCriterion("project_format is null"); return (Criteria) this; }
        public Criteria andProjectFormatIsNotNull() { addCriterion("project_format is not null"); return (Criteria) this; }
        public Criteria andProjectFormatEqualTo(String value) { addCriterion("project_format =", value, "projectFormat"); return (Criteria) this; }
        public Criteria andProjectFormatNotEqualTo(String value) { addCriterion("project_format <>", value, "projectFormat"); return (Criteria) this; }
        public Criteria andProjectFormatGreaterThan(String value) { addCriterion("project_format >", value, "projectFormat"); return (Criteria) this; }
        public Criteria andProjectFormatLessThan(String value) { addCriterion("project_format <", value, "projectFormat"); return (Criteria) this; }
        public Criteria andProjectFormatLike(String value) { addCriterion("project_format like", value, "projectFormat"); return (Criteria) this; }
        public Criteria andProjectFormatIn(List<String> values) { addCriterion("project_format in", values, "projectFormat"); return (Criteria) this; }
        public Criteria andJianyingVersionIsNull() { addCriterion("jianying_version is null"); return (Criteria) this; }
        public Criteria andJianyingVersionIsNotNull() { addCriterion("jianying_version is not null"); return (Criteria) this; }
        public Criteria andJianyingVersionEqualTo(String value) { addCriterion("jianying_version =", value, "jianyingVersion"); return (Criteria) this; }
        public Criteria andJianyingVersionNotEqualTo(String value) { addCriterion("jianying_version <>", value, "jianyingVersion"); return (Criteria) this; }
        public Criteria andJianyingVersionGreaterThan(String value) { addCriterion("jianying_version >", value, "jianyingVersion"); return (Criteria) this; }
        public Criteria andJianyingVersionLessThan(String value) { addCriterion("jianying_version <", value, "jianyingVersion"); return (Criteria) this; }
        public Criteria andJianyingVersionLike(String value) { addCriterion("jianying_version like", value, "jianyingVersion"); return (Criteria) this; }
        public Criteria andJianyingVersionIn(List<String> values) { addCriterion("jianying_version in", values, "jianyingVersion"); return (Criteria) this; }
        public Criteria andMediaModeIsNull() { addCriterion("media_mode is null"); return (Criteria) this; }
        public Criteria andMediaModeIsNotNull() { addCriterion("media_mode is not null"); return (Criteria) this; }
        public Criteria andMediaModeEqualTo(String value) { addCriterion("media_mode =", value, "mediaMode"); return (Criteria) this; }
        public Criteria andMediaModeNotEqualTo(String value) { addCriterion("media_mode <>", value, "mediaMode"); return (Criteria) this; }
        public Criteria andMediaModeGreaterThan(String value) { addCriterion("media_mode >", value, "mediaMode"); return (Criteria) this; }
        public Criteria andMediaModeLessThan(String value) { addCriterion("media_mode <", value, "mediaMode"); return (Criteria) this; }
        public Criteria andMediaModeLike(String value) { addCriterion("media_mode like", value, "mediaMode"); return (Criteria) this; }
        public Criteria andMediaModeIn(List<String> values) { addCriterion("media_mode in", values, "mediaMode"); return (Criteria) this; }
        public Criteria andDraftDirIsNull() { addCriterion("draft_dir is null"); return (Criteria) this; }
        public Criteria andDraftDirIsNotNull() { addCriterion("draft_dir is not null"); return (Criteria) this; }
        public Criteria andDraftDirEqualTo(String value) { addCriterion("draft_dir =", value, "draftDir"); return (Criteria) this; }
        public Criteria andDraftDirNotEqualTo(String value) { addCriterion("draft_dir <>", value, "draftDir"); return (Criteria) this; }
        public Criteria andDraftDirGreaterThan(String value) { addCriterion("draft_dir >", value, "draftDir"); return (Criteria) this; }
        public Criteria andDraftDirLessThan(String value) { addCriterion("draft_dir <", value, "draftDir"); return (Criteria) this; }
        public Criteria andDraftDirLike(String value) { addCriterion("draft_dir like", value, "draftDir"); return (Criteria) this; }
        public Criteria andDraftDirIn(List<String> values) { addCriterion("draft_dir in", values, "draftDir"); return (Criteria) this; }
        public Criteria andExportStatusIsNull() { addCriterion("export_status is null"); return (Criteria) this; }
        public Criteria andExportStatusIsNotNull() { addCriterion("export_status is not null"); return (Criteria) this; }
        public Criteria andExportStatusEqualTo(String value) { addCriterion("export_status =", value, "exportStatus"); return (Criteria) this; }
        public Criteria andExportStatusNotEqualTo(String value) { addCriterion("export_status <>", value, "exportStatus"); return (Criteria) this; }
        public Criteria andExportStatusGreaterThan(String value) { addCriterion("export_status >", value, "exportStatus"); return (Criteria) this; }
        public Criteria andExportStatusLessThan(String value) { addCriterion("export_status <", value, "exportStatus"); return (Criteria) this; }
        public Criteria andExportStatusLike(String value) { addCriterion("export_status like", value, "exportStatus"); return (Criteria) this; }
        public Criteria andExportStatusIn(List<String> values) { addCriterion("export_status in", values, "exportStatus"); return (Criteria) this; }
        public Criteria andClipCountIsNull() { addCriterion("clip_count is null"); return (Criteria) this; }
        public Criteria andClipCountIsNotNull() { addCriterion("clip_count is not null"); return (Criteria) this; }
        public Criteria andClipCountEqualTo(Integer value) { addCriterion("clip_count =", value, "clipCount"); return (Criteria) this; }
        public Criteria andClipCountNotEqualTo(Integer value) { addCriterion("clip_count <>", value, "clipCount"); return (Criteria) this; }
        public Criteria andClipCountGreaterThan(Integer value) { addCriterion("clip_count >", value, "clipCount"); return (Criteria) this; }
        public Criteria andClipCountLessThan(Integer value) { addCriterion("clip_count <", value, "clipCount"); return (Criteria) this; }
        public Criteria andClipCountIn(List<Integer> values) { addCriterion("clip_count in", values, "clipCount"); return (Criteria) this; }
        public Criteria andPackageByteIsNull() { addCriterion("package_byte is null"); return (Criteria) this; }
        public Criteria andPackageByteIsNotNull() { addCriterion("package_byte is not null"); return (Criteria) this; }
        public Criteria andPackageByteEqualTo(Long value) { addCriterion("package_byte =", value, "packageByte"); return (Criteria) this; }
        public Criteria andPackageByteNotEqualTo(Long value) { addCriterion("package_byte <>", value, "packageByte"); return (Criteria) this; }
        public Criteria andPackageByteGreaterThan(Long value) { addCriterion("package_byte >", value, "packageByte"); return (Criteria) this; }
        public Criteria andPackageByteLessThan(Long value) { addCriterion("package_byte <", value, "packageByte"); return (Criteria) this; }
        public Criteria andPackageByteIn(List<Long> values) { addCriterion("package_byte in", values, "packageByte"); return (Criteria) this; }
        public Criteria andAssetIdIsNull() { addCriterion("asset_id is null"); return (Criteria) this; }
        public Criteria andAssetIdIsNotNull() { addCriterion("asset_id is not null"); return (Criteria) this; }
        public Criteria andAssetIdEqualTo(String value) { addCriterion("asset_id =", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdNotEqualTo(String value) { addCriterion("asset_id <>", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdGreaterThan(String value) { addCriterion("asset_id >", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdLessThan(String value) { addCriterion("asset_id <", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdLike(String value) { addCriterion("asset_id like", value, "assetId"); return (Criteria) this; }
        public Criteria andAssetIdIn(List<String> values) { addCriterion("asset_id in", values, "assetId"); return (Criteria) this; }
        public Criteria andPackageNameIsNull() { addCriterion("package_name is null"); return (Criteria) this; }
        public Criteria andPackageNameIsNotNull() { addCriterion("package_name is not null"); return (Criteria) this; }
        public Criteria andPackageNameEqualTo(String value) { addCriterion("package_name =", value, "packageName"); return (Criteria) this; }
        public Criteria andPackageNameNotEqualTo(String value) { addCriterion("package_name <>", value, "packageName"); return (Criteria) this; }
        public Criteria andPackageNameGreaterThan(String value) { addCriterion("package_name >", value, "packageName"); return (Criteria) this; }
        public Criteria andPackageNameLessThan(String value) { addCriterion("package_name <", value, "packageName"); return (Criteria) this; }
        public Criteria andPackageNameLike(String value) { addCriterion("package_name like", value, "packageName"); return (Criteria) this; }
        public Criteria andPackageNameIn(List<String> values) { addCriterion("package_name in", values, "packageName"); return (Criteria) this; }
        public Criteria andMissingJsonIsNull() { addCriterion("missing_json is null"); return (Criteria) this; }
        public Criteria andMissingJsonIsNotNull() { addCriterion("missing_json is not null"); return (Criteria) this; }
        public Criteria andMissingJsonEqualTo(String value) { addCriterion("missing_json =", value, "missingJson"); return (Criteria) this; }
        public Criteria andMissingJsonNotEqualTo(String value) { addCriterion("missing_json <>", value, "missingJson"); return (Criteria) this; }
        public Criteria andMissingJsonGreaterThan(String value) { addCriterion("missing_json >", value, "missingJson"); return (Criteria) this; }
        public Criteria andMissingJsonLessThan(String value) { addCriterion("missing_json <", value, "missingJson"); return (Criteria) this; }
        public Criteria andMissingJsonLike(String value) { addCriterion("missing_json like", value, "missingJson"); return (Criteria) this; }
        public Criteria andMissingJsonIn(List<String> values) { addCriterion("missing_json in", values, "missingJson"); return (Criteria) this; }
        public Criteria andFailReasonIsNull() { addCriterion("fail_reason is null"); return (Criteria) this; }
        public Criteria andFailReasonIsNotNull() { addCriterion("fail_reason is not null"); return (Criteria) this; }
        public Criteria andFailReasonEqualTo(String value) { addCriterion("fail_reason =", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonNotEqualTo(String value) { addCriterion("fail_reason <>", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonGreaterThan(String value) { addCriterion("fail_reason >", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonLessThan(String value) { addCriterion("fail_reason <", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonLike(String value) { addCriterion("fail_reason like", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonIn(List<String> values) { addCriterion("fail_reason in", values, "failReason"); return (Criteria) this; }
        public Criteria andFinishedTimeIsNull() { addCriterion("finished_time is null"); return (Criteria) this; }
        public Criteria andFinishedTimeIsNotNull() { addCriterion("finished_time is not null"); return (Criteria) this; }
        public Criteria andFinishedTimeEqualTo(Date value) { addCriterion("finished_time =", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeNotEqualTo(Date value) { addCriterion("finished_time <>", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeGreaterThan(Date value) { addCriterion("finished_time >", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeLessThan(Date value) { addCriterion("finished_time <", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeIn(List<Date> values) { addCriterion("finished_time in", values, "finishedTime"); return (Criteria) this; }
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

