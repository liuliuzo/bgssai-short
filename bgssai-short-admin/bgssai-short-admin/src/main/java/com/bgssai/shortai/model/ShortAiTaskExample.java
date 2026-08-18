package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortAiTaskExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortAiTaskExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andTaskIdIsNull() { addCriterion("task_id is null"); return (Criteria) this; }
        public Criteria andTaskIdIsNotNull() { addCriterion("task_id is not null"); return (Criteria) this; }
        public Criteria andTaskIdEqualTo(String value) { addCriterion("task_id =", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdNotEqualTo(String value) { addCriterion("task_id <>", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdGreaterThan(String value) { addCriterion("task_id >", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdLessThan(String value) { addCriterion("task_id <", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdLike(String value) { addCriterion("task_id like", value, "taskId"); return (Criteria) this; }
        public Criteria andTaskIdIn(List<String> values) { addCriterion("task_id in", values, "taskId"); return (Criteria) this; }
        public Criteria andBatchIdIsNull() { addCriterion("batch_id is null"); return (Criteria) this; }
        public Criteria andBatchIdIsNotNull() { addCriterion("batch_id is not null"); return (Criteria) this; }
        public Criteria andBatchIdEqualTo(String value) { addCriterion("batch_id =", value, "batchId"); return (Criteria) this; }
        public Criteria andBatchIdNotEqualTo(String value) { addCriterion("batch_id <>", value, "batchId"); return (Criteria) this; }
        public Criteria andBatchIdGreaterThan(String value) { addCriterion("batch_id >", value, "batchId"); return (Criteria) this; }
        public Criteria andBatchIdLessThan(String value) { addCriterion("batch_id <", value, "batchId"); return (Criteria) this; }
        public Criteria andBatchIdLike(String value) { addCriterion("batch_id like", value, "batchId"); return (Criteria) this; }
        public Criteria andBatchIdIn(List<String> values) { addCriterion("batch_id in", values, "batchId"); return (Criteria) this; }
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
        public Criteria andTaskTypeIsNull() { addCriterion("task_type is null"); return (Criteria) this; }
        public Criteria andTaskTypeIsNotNull() { addCriterion("task_type is not null"); return (Criteria) this; }
        public Criteria andTaskTypeEqualTo(String value) { addCriterion("task_type =", value, "taskType"); return (Criteria) this; }
        public Criteria andTaskTypeNotEqualTo(String value) { addCriterion("task_type <>", value, "taskType"); return (Criteria) this; }
        public Criteria andTaskTypeGreaterThan(String value) { addCriterion("task_type >", value, "taskType"); return (Criteria) this; }
        public Criteria andTaskTypeLessThan(String value) { addCriterion("task_type <", value, "taskType"); return (Criteria) this; }
        public Criteria andTaskTypeLike(String value) { addCriterion("task_type like", value, "taskType"); return (Criteria) this; }
        public Criteria andTaskTypeIn(List<String> values) { addCriterion("task_type in", values, "taskType"); return (Criteria) this; }
        public Criteria andTaskStatusIsNull() { addCriterion("task_status is null"); return (Criteria) this; }
        public Criteria andTaskStatusIsNotNull() { addCriterion("task_status is not null"); return (Criteria) this; }
        public Criteria andTaskStatusEqualTo(String value) { addCriterion("task_status =", value, "taskStatus"); return (Criteria) this; }
        public Criteria andTaskStatusNotEqualTo(String value) { addCriterion("task_status <>", value, "taskStatus"); return (Criteria) this; }
        public Criteria andTaskStatusGreaterThan(String value) { addCriterion("task_status >", value, "taskStatus"); return (Criteria) this; }
        public Criteria andTaskStatusLessThan(String value) { addCriterion("task_status <", value, "taskStatus"); return (Criteria) this; }
        public Criteria andTaskStatusLike(String value) { addCriterion("task_status like", value, "taskStatus"); return (Criteria) this; }
        public Criteria andTaskStatusIn(List<String> values) { addCriterion("task_status in", values, "taskStatus"); return (Criteria) this; }
        public Criteria andTargetTypeIsNull() { addCriterion("target_type is null"); return (Criteria) this; }
        public Criteria andTargetTypeIsNotNull() { addCriterion("target_type is not null"); return (Criteria) this; }
        public Criteria andTargetTypeEqualTo(String value) { addCriterion("target_type =", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeNotEqualTo(String value) { addCriterion("target_type <>", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeGreaterThan(String value) { addCriterion("target_type >", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeLessThan(String value) { addCriterion("target_type <", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeLike(String value) { addCriterion("target_type like", value, "targetType"); return (Criteria) this; }
        public Criteria andTargetTypeIn(List<String> values) { addCriterion("target_type in", values, "targetType"); return (Criteria) this; }
        public Criteria andTargetIdIsNull() { addCriterion("target_id is null"); return (Criteria) this; }
        public Criteria andTargetIdIsNotNull() { addCriterion("target_id is not null"); return (Criteria) this; }
        public Criteria andTargetIdEqualTo(String value) { addCriterion("target_id =", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdNotEqualTo(String value) { addCriterion("target_id <>", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdGreaterThan(String value) { addCriterion("target_id >", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdLessThan(String value) { addCriterion("target_id <", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdLike(String value) { addCriterion("target_id like", value, "targetId"); return (Criteria) this; }
        public Criteria andTargetIdIn(List<String> values) { addCriterion("target_id in", values, "targetId"); return (Criteria) this; }
        public Criteria andModelCodeIsNull() { addCriterion("model_code is null"); return (Criteria) this; }
        public Criteria andModelCodeIsNotNull() { addCriterion("model_code is not null"); return (Criteria) this; }
        public Criteria andModelCodeEqualTo(String value) { addCriterion("model_code =", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeNotEqualTo(String value) { addCriterion("model_code <>", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeGreaterThan(String value) { addCriterion("model_code >", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeLessThan(String value) { addCriterion("model_code <", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeLike(String value) { addCriterion("model_code like", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeIn(List<String> values) { addCriterion("model_code in", values, "modelCode"); return (Criteria) this; }
        public Criteria andRequestJsonIsNull() { addCriterion("request_json is null"); return (Criteria) this; }
        public Criteria andRequestJsonIsNotNull() { addCriterion("request_json is not null"); return (Criteria) this; }
        public Criteria andRequestJsonEqualTo(String value) { addCriterion("request_json =", value, "requestJson"); return (Criteria) this; }
        public Criteria andRequestJsonNotEqualTo(String value) { addCriterion("request_json <>", value, "requestJson"); return (Criteria) this; }
        public Criteria andRequestJsonGreaterThan(String value) { addCriterion("request_json >", value, "requestJson"); return (Criteria) this; }
        public Criteria andRequestJsonLessThan(String value) { addCriterion("request_json <", value, "requestJson"); return (Criteria) this; }
        public Criteria andRequestJsonLike(String value) { addCriterion("request_json like", value, "requestJson"); return (Criteria) this; }
        public Criteria andRequestJsonIn(List<String> values) { addCriterion("request_json in", values, "requestJson"); return (Criteria) this; }
        public Criteria andResultJsonIsNull() { addCriterion("result_json is null"); return (Criteria) this; }
        public Criteria andResultJsonIsNotNull() { addCriterion("result_json is not null"); return (Criteria) this; }
        public Criteria andResultJsonEqualTo(String value) { addCriterion("result_json =", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonNotEqualTo(String value) { addCriterion("result_json <>", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonGreaterThan(String value) { addCriterion("result_json >", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonLessThan(String value) { addCriterion("result_json <", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonLike(String value) { addCriterion("result_json like", value, "resultJson"); return (Criteria) this; }
        public Criteria andResultJsonIn(List<String> values) { addCriterion("result_json in", values, "resultJson"); return (Criteria) this; }
        public Criteria andProgressPercentIsNull() { addCriterion("progress_percent is null"); return (Criteria) this; }
        public Criteria andProgressPercentIsNotNull() { addCriterion("progress_percent is not null"); return (Criteria) this; }
        public Criteria andProgressPercentEqualTo(Integer value) { addCriterion("progress_percent =", value, "progressPercent"); return (Criteria) this; }
        public Criteria andProgressPercentNotEqualTo(Integer value) { addCriterion("progress_percent <>", value, "progressPercent"); return (Criteria) this; }
        public Criteria andProgressPercentGreaterThan(Integer value) { addCriterion("progress_percent >", value, "progressPercent"); return (Criteria) this; }
        public Criteria andProgressPercentLessThan(Integer value) { addCriterion("progress_percent <", value, "progressPercent"); return (Criteria) this; }
        public Criteria andProgressPercentIn(List<Integer> values) { addCriterion("progress_percent in", values, "progressPercent"); return (Criteria) this; }
        public Criteria andRetryCountIsNull() { addCriterion("retry_count is null"); return (Criteria) this; }
        public Criteria andRetryCountIsNotNull() { addCriterion("retry_count is not null"); return (Criteria) this; }
        public Criteria andRetryCountEqualTo(Integer value) { addCriterion("retry_count =", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountNotEqualTo(Integer value) { addCriterion("retry_count <>", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountGreaterThan(Integer value) { addCriterion("retry_count >", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountLessThan(Integer value) { addCriterion("retry_count <", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountIn(List<Integer> values) { addCriterion("retry_count in", values, "retryCount"); return (Criteria) this; }
        public Criteria andFailReasonIsNull() { addCriterion("fail_reason is null"); return (Criteria) this; }
        public Criteria andFailReasonIsNotNull() { addCriterion("fail_reason is not null"); return (Criteria) this; }
        public Criteria andFailReasonEqualTo(String value) { addCriterion("fail_reason =", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonNotEqualTo(String value) { addCriterion("fail_reason <>", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonGreaterThan(String value) { addCriterion("fail_reason >", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonLessThan(String value) { addCriterion("fail_reason <", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonLike(String value) { addCriterion("fail_reason like", value, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonIn(List<String> values) { addCriterion("fail_reason in", values, "failReason"); return (Criteria) this; }
        public Criteria andFailReasonCodeIsNull() { addCriterion("fail_reason_code is null"); return (Criteria) this; }
        public Criteria andFailReasonCodeIsNotNull() { addCriterion("fail_reason_code is not null"); return (Criteria) this; }
        public Criteria andFailReasonCodeEqualTo(String value) { addCriterion("fail_reason_code =", value, "failReasonCode"); return (Criteria) this; }
        public Criteria andFailReasonCodeNotEqualTo(String value) { addCriterion("fail_reason_code <>", value, "failReasonCode"); return (Criteria) this; }
        public Criteria andFailReasonCodeGreaterThan(String value) { addCriterion("fail_reason_code >", value, "failReasonCode"); return (Criteria) this; }
        public Criteria andFailReasonCodeLessThan(String value) { addCriterion("fail_reason_code <", value, "failReasonCode"); return (Criteria) this; }
        public Criteria andFailReasonCodeLike(String value) { addCriterion("fail_reason_code like", value, "failReasonCode"); return (Criteria) this; }
        public Criteria andFailReasonCodeIn(List<String> values) { addCriterion("fail_reason_code in", values, "failReasonCode"); return (Criteria) this; }
        public Criteria andAutoNextFlagIsNull() { addCriterion("auto_next_flag is null"); return (Criteria) this; }
        public Criteria andAutoNextFlagIsNotNull() { addCriterion("auto_next_flag is not null"); return (Criteria) this; }
        public Criteria andAutoNextFlagEqualTo(Integer value) { addCriterion("auto_next_flag =", value, "autoNextFlag"); return (Criteria) this; }
        public Criteria andAutoNextFlagNotEqualTo(Integer value) { addCriterion("auto_next_flag <>", value, "autoNextFlag"); return (Criteria) this; }
        public Criteria andAutoNextFlagGreaterThan(Integer value) { addCriterion("auto_next_flag >", value, "autoNextFlag"); return (Criteria) this; }
        public Criteria andAutoNextFlagLessThan(Integer value) { addCriterion("auto_next_flag <", value, "autoNextFlag"); return (Criteria) this; }
        public Criteria andAutoNextFlagIn(List<Integer> values) { addCriterion("auto_next_flag in", values, "autoNextFlag"); return (Criteria) this; }
        public Criteria andCostCentIsNull() { addCriterion("cost_cent is null"); return (Criteria) this; }
        public Criteria andCostCentIsNotNull() { addCriterion("cost_cent is not null"); return (Criteria) this; }
        public Criteria andCostCentEqualTo(Integer value) { addCriterion("cost_cent =", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentNotEqualTo(Integer value) { addCriterion("cost_cent <>", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentGreaterThan(Integer value) { addCriterion("cost_cent >", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentLessThan(Integer value) { addCriterion("cost_cent <", value, "costCent"); return (Criteria) this; }
        public Criteria andCostCentIn(List<Integer> values) { addCriterion("cost_cent in", values, "costCent"); return (Criteria) this; }
        public Criteria andQueuedTimeIsNull() { addCriterion("queued_time is null"); return (Criteria) this; }
        public Criteria andQueuedTimeIsNotNull() { addCriterion("queued_time is not null"); return (Criteria) this; }
        public Criteria andQueuedTimeEqualTo(Date value) { addCriterion("queued_time =", value, "queuedTime"); return (Criteria) this; }
        public Criteria andQueuedTimeNotEqualTo(Date value) { addCriterion("queued_time <>", value, "queuedTime"); return (Criteria) this; }
        public Criteria andQueuedTimeGreaterThan(Date value) { addCriterion("queued_time >", value, "queuedTime"); return (Criteria) this; }
        public Criteria andQueuedTimeLessThan(Date value) { addCriterion("queued_time <", value, "queuedTime"); return (Criteria) this; }
        public Criteria andQueuedTimeIn(List<Date> values) { addCriterion("queued_time in", values, "queuedTime"); return (Criteria) this; }
        public Criteria andStartedTimeIsNull() { addCriterion("started_time is null"); return (Criteria) this; }
        public Criteria andStartedTimeIsNotNull() { addCriterion("started_time is not null"); return (Criteria) this; }
        public Criteria andStartedTimeEqualTo(Date value) { addCriterion("started_time =", value, "startedTime"); return (Criteria) this; }
        public Criteria andStartedTimeNotEqualTo(Date value) { addCriterion("started_time <>", value, "startedTime"); return (Criteria) this; }
        public Criteria andStartedTimeGreaterThan(Date value) { addCriterion("started_time >", value, "startedTime"); return (Criteria) this; }
        public Criteria andStartedTimeLessThan(Date value) { addCriterion("started_time <", value, "startedTime"); return (Criteria) this; }
        public Criteria andStartedTimeIn(List<Date> values) { addCriterion("started_time in", values, "startedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeIsNull() { addCriterion("finished_time is null"); return (Criteria) this; }
        public Criteria andFinishedTimeIsNotNull() { addCriterion("finished_time is not null"); return (Criteria) this; }
        public Criteria andFinishedTimeEqualTo(Date value) { addCriterion("finished_time =", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeNotEqualTo(Date value) { addCriterion("finished_time <>", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeGreaterThan(Date value) { addCriterion("finished_time >", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeLessThan(Date value) { addCriterion("finished_time <", value, "finishedTime"); return (Criteria) this; }
        public Criteria andFinishedTimeIn(List<Date> values) { addCriterion("finished_time in", values, "finishedTime"); return (Criteria) this; }
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

