package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortWorkExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortWorkExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andWorkTitleIsNull() { addCriterion("work_title is null"); return (Criteria) this; }
        public Criteria andWorkTitleIsNotNull() { addCriterion("work_title is not null"); return (Criteria) this; }
        public Criteria andWorkTitleEqualTo(String value) { addCriterion("work_title =", value, "workTitle"); return (Criteria) this; }
        public Criteria andWorkTitleNotEqualTo(String value) { addCriterion("work_title <>", value, "workTitle"); return (Criteria) this; }
        public Criteria andWorkTitleGreaterThan(String value) { addCriterion("work_title >", value, "workTitle"); return (Criteria) this; }
        public Criteria andWorkTitleLessThan(String value) { addCriterion("work_title <", value, "workTitle"); return (Criteria) this; }
        public Criteria andWorkTitleLike(String value) { addCriterion("work_title like", value, "workTitle"); return (Criteria) this; }
        public Criteria andWorkTitleIn(List<String> values) { addCriterion("work_title in", values, "workTitle"); return (Criteria) this; }
        public Criteria andOneLinePitchIsNull() { addCriterion("one_line_pitch is null"); return (Criteria) this; }
        public Criteria andOneLinePitchIsNotNull() { addCriterion("one_line_pitch is not null"); return (Criteria) this; }
        public Criteria andOneLinePitchEqualTo(String value) { addCriterion("one_line_pitch =", value, "oneLinePitch"); return (Criteria) this; }
        public Criteria andOneLinePitchNotEqualTo(String value) { addCriterion("one_line_pitch <>", value, "oneLinePitch"); return (Criteria) this; }
        public Criteria andOneLinePitchGreaterThan(String value) { addCriterion("one_line_pitch >", value, "oneLinePitch"); return (Criteria) this; }
        public Criteria andOneLinePitchLessThan(String value) { addCriterion("one_line_pitch <", value, "oneLinePitch"); return (Criteria) this; }
        public Criteria andOneLinePitchLike(String value) { addCriterion("one_line_pitch like", value, "oneLinePitch"); return (Criteria) this; }
        public Criteria andOneLinePitchIn(List<String> values) { addCriterion("one_line_pitch in", values, "oneLinePitch"); return (Criteria) this; }
        public Criteria andGenreIsNull() { addCriterion("genre is null"); return (Criteria) this; }
        public Criteria andGenreIsNotNull() { addCriterion("genre is not null"); return (Criteria) this; }
        public Criteria andGenreEqualTo(String value) { addCriterion("genre =", value, "genre"); return (Criteria) this; }
        public Criteria andGenreNotEqualTo(String value) { addCriterion("genre <>", value, "genre"); return (Criteria) this; }
        public Criteria andGenreGreaterThan(String value) { addCriterion("genre >", value, "genre"); return (Criteria) this; }
        public Criteria andGenreLessThan(String value) { addCriterion("genre <", value, "genre"); return (Criteria) this; }
        public Criteria andGenreLike(String value) { addCriterion("genre like", value, "genre"); return (Criteria) this; }
        public Criteria andGenreIn(List<String> values) { addCriterion("genre in", values, "genre"); return (Criteria) this; }
        public Criteria andWorkStatusIsNull() { addCriterion("work_status is null"); return (Criteria) this; }
        public Criteria andWorkStatusIsNotNull() { addCriterion("work_status is not null"); return (Criteria) this; }
        public Criteria andWorkStatusEqualTo(String value) { addCriterion("work_status =", value, "workStatus"); return (Criteria) this; }
        public Criteria andWorkStatusNotEqualTo(String value) { addCriterion("work_status <>", value, "workStatus"); return (Criteria) this; }
        public Criteria andWorkStatusGreaterThan(String value) { addCriterion("work_status >", value, "workStatus"); return (Criteria) this; }
        public Criteria andWorkStatusLessThan(String value) { addCriterion("work_status <", value, "workStatus"); return (Criteria) this; }
        public Criteria andWorkStatusLike(String value) { addCriterion("work_status like", value, "workStatus"); return (Criteria) this; }
        public Criteria andWorkStatusIn(List<String> values) { addCriterion("work_status in", values, "workStatus"); return (Criteria) this; }
        public Criteria andEpisodeCountIsNull() { addCriterion("episode_count is null"); return (Criteria) this; }
        public Criteria andEpisodeCountIsNotNull() { addCriterion("episode_count is not null"); return (Criteria) this; }
        public Criteria andEpisodeCountEqualTo(Integer value) { addCriterion("episode_count =", value, "episodeCount"); return (Criteria) this; }
        public Criteria andEpisodeCountNotEqualTo(Integer value) { addCriterion("episode_count <>", value, "episodeCount"); return (Criteria) this; }
        public Criteria andEpisodeCountGreaterThan(Integer value) { addCriterion("episode_count >", value, "episodeCount"); return (Criteria) this; }
        public Criteria andEpisodeCountLessThan(Integer value) { addCriterion("episode_count <", value, "episodeCount"); return (Criteria) this; }
        public Criteria andEpisodeCountIn(List<Integer> values) { addCriterion("episode_count in", values, "episodeCount"); return (Criteria) this; }
        public Criteria andEpisodeDurationSecIsNull() { addCriterion("episode_duration_sec is null"); return (Criteria) this; }
        public Criteria andEpisodeDurationSecIsNotNull() { addCriterion("episode_duration_sec is not null"); return (Criteria) this; }
        public Criteria andEpisodeDurationSecEqualTo(Integer value) { addCriterion("episode_duration_sec =", value, "episodeDurationSec"); return (Criteria) this; }
        public Criteria andEpisodeDurationSecNotEqualTo(Integer value) { addCriterion("episode_duration_sec <>", value, "episodeDurationSec"); return (Criteria) this; }
        public Criteria andEpisodeDurationSecGreaterThan(Integer value) { addCriterion("episode_duration_sec >", value, "episodeDurationSec"); return (Criteria) this; }
        public Criteria andEpisodeDurationSecLessThan(Integer value) { addCriterion("episode_duration_sec <", value, "episodeDurationSec"); return (Criteria) this; }
        public Criteria andEpisodeDurationSecIn(List<Integer> values) { addCriterion("episode_duration_sec in", values, "episodeDurationSec"); return (Criteria) this; }
        public Criteria andVisualStyleIsNull() { addCriterion("visual_style is null"); return (Criteria) this; }
        public Criteria andVisualStyleIsNotNull() { addCriterion("visual_style is not null"); return (Criteria) this; }
        public Criteria andVisualStyleEqualTo(String value) { addCriterion("visual_style =", value, "visualStyle"); return (Criteria) this; }
        public Criteria andVisualStyleNotEqualTo(String value) { addCriterion("visual_style <>", value, "visualStyle"); return (Criteria) this; }
        public Criteria andVisualStyleGreaterThan(String value) { addCriterion("visual_style >", value, "visualStyle"); return (Criteria) this; }
        public Criteria andVisualStyleLessThan(String value) { addCriterion("visual_style <", value, "visualStyle"); return (Criteria) this; }
        public Criteria andVisualStyleLike(String value) { addCriterion("visual_style like", value, "visualStyle"); return (Criteria) this; }
        public Criteria andVisualStyleIn(List<String> values) { addCriterion("visual_style in", values, "visualStyle"); return (Criteria) this; }
        public Criteria andAspectRatioIsNull() { addCriterion("aspect_ratio is null"); return (Criteria) this; }
        public Criteria andAspectRatioIsNotNull() { addCriterion("aspect_ratio is not null"); return (Criteria) this; }
        public Criteria andAspectRatioEqualTo(String value) { addCriterion("aspect_ratio =", value, "aspectRatio"); return (Criteria) this; }
        public Criteria andAspectRatioNotEqualTo(String value) { addCriterion("aspect_ratio <>", value, "aspectRatio"); return (Criteria) this; }
        public Criteria andAspectRatioGreaterThan(String value) { addCriterion("aspect_ratio >", value, "aspectRatio"); return (Criteria) this; }
        public Criteria andAspectRatioLessThan(String value) { addCriterion("aspect_ratio <", value, "aspectRatio"); return (Criteria) this; }
        public Criteria andAspectRatioLike(String value) { addCriterion("aspect_ratio like", value, "aspectRatio"); return (Criteria) this; }
        public Criteria andAspectRatioIn(List<String> values) { addCriterion("aspect_ratio in", values, "aspectRatio"); return (Criteria) this; }
        public Criteria andCoverAssetIdIsNull() { addCriterion("cover_asset_id is null"); return (Criteria) this; }
        public Criteria andCoverAssetIdIsNotNull() { addCriterion("cover_asset_id is not null"); return (Criteria) this; }
        public Criteria andCoverAssetIdEqualTo(String value) { addCriterion("cover_asset_id =", value, "coverAssetId"); return (Criteria) this; }
        public Criteria andCoverAssetIdNotEqualTo(String value) { addCriterion("cover_asset_id <>", value, "coverAssetId"); return (Criteria) this; }
        public Criteria andCoverAssetIdGreaterThan(String value) { addCriterion("cover_asset_id >", value, "coverAssetId"); return (Criteria) this; }
        public Criteria andCoverAssetIdLessThan(String value) { addCriterion("cover_asset_id <", value, "coverAssetId"); return (Criteria) this; }
        public Criteria andCoverAssetIdLike(String value) { addCriterion("cover_asset_id like", value, "coverAssetId"); return (Criteria) this; }
        public Criteria andCoverAssetIdIn(List<String> values) { addCriterion("cover_asset_id in", values, "coverAssetId"); return (Criteria) this; }
        public Criteria andCostBudgetCentIsNull() { addCriterion("cost_budget_cent is null"); return (Criteria) this; }
        public Criteria andCostBudgetCentIsNotNull() { addCriterion("cost_budget_cent is not null"); return (Criteria) this; }
        public Criteria andCostBudgetCentEqualTo(Integer value) { addCriterion("cost_budget_cent =", value, "costBudgetCent"); return (Criteria) this; }
        public Criteria andCostBudgetCentNotEqualTo(Integer value) { addCriterion("cost_budget_cent <>", value, "costBudgetCent"); return (Criteria) this; }
        public Criteria andCostBudgetCentGreaterThan(Integer value) { addCriterion("cost_budget_cent >", value, "costBudgetCent"); return (Criteria) this; }
        public Criteria andCostBudgetCentLessThan(Integer value) { addCriterion("cost_budget_cent <", value, "costBudgetCent"); return (Criteria) this; }
        public Criteria andCostBudgetCentIn(List<Integer> values) { addCriterion("cost_budget_cent in", values, "costBudgetCent"); return (Criteria) this; }
        public Criteria andCostSpentCentIsNull() { addCriterion("cost_spent_cent is null"); return (Criteria) this; }
        public Criteria andCostSpentCentIsNotNull() { addCriterion("cost_spent_cent is not null"); return (Criteria) this; }
        public Criteria andCostSpentCentEqualTo(Integer value) { addCriterion("cost_spent_cent =", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentNotEqualTo(Integer value) { addCriterion("cost_spent_cent <>", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentGreaterThan(Integer value) { addCriterion("cost_spent_cent >", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentLessThan(Integer value) { addCriterion("cost_spent_cent <", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentIn(List<Integer> values) { addCriterion("cost_spent_cent in", values, "costSpentCent"); return (Criteria) this; }
        public Criteria andSkipLevelCheckFlagIsNull() { addCriterion("skip_level_check_flag is null"); return (Criteria) this; }
        public Criteria andSkipLevelCheckFlagIsNotNull() { addCriterion("skip_level_check_flag is not null"); return (Criteria) this; }
        public Criteria andSkipLevelCheckFlagEqualTo(Integer value) { addCriterion("skip_level_check_flag =", value, "skipLevelCheckFlag"); return (Criteria) this; }
        public Criteria andSkipLevelCheckFlagNotEqualTo(Integer value) { addCriterion("skip_level_check_flag <>", value, "skipLevelCheckFlag"); return (Criteria) this; }
        public Criteria andSkipLevelCheckFlagGreaterThan(Integer value) { addCriterion("skip_level_check_flag >", value, "skipLevelCheckFlag"); return (Criteria) this; }
        public Criteria andSkipLevelCheckFlagLessThan(Integer value) { addCriterion("skip_level_check_flag <", value, "skipLevelCheckFlag"); return (Criteria) this; }
        public Criteria andSkipLevelCheckFlagIn(List<Integer> values) { addCriterion("skip_level_check_flag in", values, "skipLevelCheckFlag"); return (Criteria) this; }
        public Criteria andQualityThresholdScoreIsNull() { addCriterion("quality_threshold_score is null"); return (Criteria) this; }
        public Criteria andQualityThresholdScoreIsNotNull() { addCriterion("quality_threshold_score is not null"); return (Criteria) this; }
        public Criteria andQualityThresholdScoreEqualTo(Integer value) { addCriterion("quality_threshold_score =", value, "qualityThresholdScore"); return (Criteria) this; }
        public Criteria andQualityThresholdScoreNotEqualTo(Integer value) { addCriterion("quality_threshold_score <>", value, "qualityThresholdScore"); return (Criteria) this; }
        public Criteria andQualityThresholdScoreGreaterThan(Integer value) { addCriterion("quality_threshold_score >", value, "qualityThresholdScore"); return (Criteria) this; }
        public Criteria andQualityThresholdScoreLessThan(Integer value) { addCriterion("quality_threshold_score <", value, "qualityThresholdScore"); return (Criteria) this; }
        public Criteria andQualityThresholdScoreIn(List<Integer> values) { addCriterion("quality_threshold_score in", values, "qualityThresholdScore"); return (Criteria) this; }
        public Criteria andOverallQualityScoreIsNull() { addCriterion("overall_quality_score is null"); return (Criteria) this; }
        public Criteria andOverallQualityScoreIsNotNull() { addCriterion("overall_quality_score is not null"); return (Criteria) this; }
        public Criteria andOverallQualityScoreEqualTo(Integer value) { addCriterion("overall_quality_score =", value, "overallQualityScore"); return (Criteria) this; }
        public Criteria andOverallQualityScoreNotEqualTo(Integer value) { addCriterion("overall_quality_score <>", value, "overallQualityScore"); return (Criteria) this; }
        public Criteria andOverallQualityScoreGreaterThan(Integer value) { addCriterion("overall_quality_score >", value, "overallQualityScore"); return (Criteria) this; }
        public Criteria andOverallQualityScoreLessThan(Integer value) { addCriterion("overall_quality_score <", value, "overallQualityScore"); return (Criteria) this; }
        public Criteria andOverallQualityScoreIn(List<Integer> values) { addCriterion("overall_quality_score in", values, "overallQualityScore"); return (Criteria) this; }
        public Criteria andOutlineJsonIsNull() { addCriterion("outline_json is null"); return (Criteria) this; }
        public Criteria andOutlineJsonIsNotNull() { addCriterion("outline_json is not null"); return (Criteria) this; }
        public Criteria andOutlineJsonEqualTo(String value) { addCriterion("outline_json =", value, "outlineJson"); return (Criteria) this; }
        public Criteria andOutlineJsonNotEqualTo(String value) { addCriterion("outline_json <>", value, "outlineJson"); return (Criteria) this; }
        public Criteria andOutlineJsonGreaterThan(String value) { addCriterion("outline_json >", value, "outlineJson"); return (Criteria) this; }
        public Criteria andOutlineJsonLessThan(String value) { addCriterion("outline_json <", value, "outlineJson"); return (Criteria) this; }
        public Criteria andOutlineJsonLike(String value) { addCriterion("outline_json like", value, "outlineJson"); return (Criteria) this; }
        public Criteria andOutlineJsonIn(List<String> values) { addCriterion("outline_json in", values, "outlineJson"); return (Criteria) this; }
        public Criteria andComposeSettingJsonIsNull() { addCriterion("compose_setting_json is null"); return (Criteria) this; }
        public Criteria andComposeSettingJsonIsNotNull() { addCriterion("compose_setting_json is not null"); return (Criteria) this; }
        public Criteria andComposeSettingJsonEqualTo(String value) { addCriterion("compose_setting_json =", value, "composeSettingJson"); return (Criteria) this; }
        public Criteria andComposeSettingJsonNotEqualTo(String value) { addCriterion("compose_setting_json <>", value, "composeSettingJson"); return (Criteria) this; }
        public Criteria andComposeSettingJsonGreaterThan(String value) { addCriterion("compose_setting_json >", value, "composeSettingJson"); return (Criteria) this; }
        public Criteria andComposeSettingJsonLessThan(String value) { addCriterion("compose_setting_json <", value, "composeSettingJson"); return (Criteria) this; }
        public Criteria andComposeSettingJsonLike(String value) { addCriterion("compose_setting_json like", value, "composeSettingJson"); return (Criteria) this; }
        public Criteria andComposeSettingJsonIn(List<String> values) { addCriterion("compose_setting_json in", values, "composeSettingJson"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdIsNull() { addCriterion("narrator_voice_id is null"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdIsNotNull() { addCriterion("narrator_voice_id is not null"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdEqualTo(String value) { addCriterion("narrator_voice_id =", value, "narratorVoiceId"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdNotEqualTo(String value) { addCriterion("narrator_voice_id <>", value, "narratorVoiceId"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdGreaterThan(String value) { addCriterion("narrator_voice_id >", value, "narratorVoiceId"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdLessThan(String value) { addCriterion("narrator_voice_id <", value, "narratorVoiceId"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdLike(String value) { addCriterion("narrator_voice_id like", value, "narratorVoiceId"); return (Criteria) this; }
        public Criteria andNarratorVoiceIdIn(List<String> values) { addCriterion("narrator_voice_id in", values, "narratorVoiceId"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonIsNull() { addCriterion("subtitle_style_json is null"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonIsNotNull() { addCriterion("subtitle_style_json is not null"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonEqualTo(String value) { addCriterion("subtitle_style_json =", value, "subtitleStyleJson"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonNotEqualTo(String value) { addCriterion("subtitle_style_json <>", value, "subtitleStyleJson"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonGreaterThan(String value) { addCriterion("subtitle_style_json >", value, "subtitleStyleJson"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonLessThan(String value) { addCriterion("subtitle_style_json <", value, "subtitleStyleJson"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonLike(String value) { addCriterion("subtitle_style_json like", value, "subtitleStyleJson"); return (Criteria) this; }
        public Criteria andSubtitleStyleJsonIn(List<String> values) { addCriterion("subtitle_style_json in", values, "subtitleStyleJson"); return (Criteria) this; }
        public Criteria andTemplateIdIsNull() { addCriterion("template_id is null"); return (Criteria) this; }
        public Criteria andTemplateIdIsNotNull() { addCriterion("template_id is not null"); return (Criteria) this; }
        public Criteria andTemplateIdEqualTo(String value) { addCriterion("template_id =", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdNotEqualTo(String value) { addCriterion("template_id <>", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdGreaterThan(String value) { addCriterion("template_id >", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdLessThan(String value) { addCriterion("template_id <", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdLike(String value) { addCriterion("template_id like", value, "templateId"); return (Criteria) this; }
        public Criteria andTemplateIdIn(List<String> values) { addCriterion("template_id in", values, "templateId"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonIsNull() { addCriterion("template_override_json is null"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonIsNotNull() { addCriterion("template_override_json is not null"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonEqualTo(String value) { addCriterion("template_override_json =", value, "templateOverrideJson"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonNotEqualTo(String value) { addCriterion("template_override_json <>", value, "templateOverrideJson"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonGreaterThan(String value) { addCriterion("template_override_json >", value, "templateOverrideJson"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonLessThan(String value) { addCriterion("template_override_json <", value, "templateOverrideJson"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonLike(String value) { addCriterion("template_override_json like", value, "templateOverrideJson"); return (Criteria) this; }
        public Criteria andTemplateOverrideJsonIn(List<String> values) { addCriterion("template_override_json in", values, "templateOverrideJson"); return (Criteria) this; }
        public Criteria andArchivedFlagIsNull() { addCriterion("archived_flag is null"); return (Criteria) this; }
        public Criteria andArchivedFlagIsNotNull() { addCriterion("archived_flag is not null"); return (Criteria) this; }
        public Criteria andArchivedFlagEqualTo(Integer value) { addCriterion("archived_flag =", value, "archivedFlag"); return (Criteria) this; }
        public Criteria andArchivedFlagNotEqualTo(Integer value) { addCriterion("archived_flag <>", value, "archivedFlag"); return (Criteria) this; }
        public Criteria andArchivedFlagGreaterThan(Integer value) { addCriterion("archived_flag >", value, "archivedFlag"); return (Criteria) this; }
        public Criteria andArchivedFlagLessThan(Integer value) { addCriterion("archived_flag <", value, "archivedFlag"); return (Criteria) this; }
        public Criteria andArchivedFlagIn(List<Integer> values) { addCriterion("archived_flag in", values, "archivedFlag"); return (Criteria) this; }
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

