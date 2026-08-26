package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortDubbingExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortDubbingExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andDubbingIdIsNull() { addCriterion("dubbing_id is null"); return (Criteria) this; }
        public Criteria andDubbingIdIsNotNull() { addCriterion("dubbing_id is not null"); return (Criteria) this; }
        public Criteria andDubbingIdEqualTo(String value) { addCriterion("dubbing_id =", value, "dubbingId"); return (Criteria) this; }
        public Criteria andDubbingIdNotEqualTo(String value) { addCriterion("dubbing_id <>", value, "dubbingId"); return (Criteria) this; }
        public Criteria andDubbingIdGreaterThan(String value) { addCriterion("dubbing_id >", value, "dubbingId"); return (Criteria) this; }
        public Criteria andDubbingIdLessThan(String value) { addCriterion("dubbing_id <", value, "dubbingId"); return (Criteria) this; }
        public Criteria andDubbingIdLike(String value) { addCriterion("dubbing_id like", value, "dubbingId"); return (Criteria) this; }
        public Criteria andDubbingIdIn(List<String> values) { addCriterion("dubbing_id in", values, "dubbingId"); return (Criteria) this; }
        public Criteria andShotIdIsNull() { addCriterion("shot_id is null"); return (Criteria) this; }
        public Criteria andShotIdIsNotNull() { addCriterion("shot_id is not null"); return (Criteria) this; }
        public Criteria andShotIdEqualTo(String value) { addCriterion("shot_id =", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdNotEqualTo(String value) { addCriterion("shot_id <>", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdGreaterThan(String value) { addCriterion("shot_id >", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLessThan(String value) { addCriterion("shot_id <", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdLike(String value) { addCriterion("shot_id like", value, "shotId"); return (Criteria) this; }
        public Criteria andShotIdIn(List<String> values) { addCriterion("shot_id in", values, "shotId"); return (Criteria) this; }
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
        public Criteria andLineNoIsNull() { addCriterion("line_no is null"); return (Criteria) this; }
        public Criteria andLineNoIsNotNull() { addCriterion("line_no is not null"); return (Criteria) this; }
        public Criteria andLineNoEqualTo(Integer value) { addCriterion("line_no =", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoNotEqualTo(Integer value) { addCriterion("line_no <>", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoGreaterThan(Integer value) { addCriterion("line_no >", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoLessThan(Integer value) { addCriterion("line_no <", value, "lineNo"); return (Criteria) this; }
        public Criteria andLineNoIn(List<Integer> values) { addCriterion("line_no in", values, "lineNo"); return (Criteria) this; }
        public Criteria andCharacterIdIsNull() { addCriterion("character_id is null"); return (Criteria) this; }
        public Criteria andCharacterIdIsNotNull() { addCriterion("character_id is not null"); return (Criteria) this; }
        public Criteria andCharacterIdEqualTo(String value) { addCriterion("character_id =", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdNotEqualTo(String value) { addCriterion("character_id <>", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdGreaterThan(String value) { addCriterion("character_id >", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdLessThan(String value) { addCriterion("character_id <", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdLike(String value) { addCriterion("character_id like", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdIn(List<String> values) { addCriterion("character_id in", values, "characterId"); return (Criteria) this; }
        public Criteria andVoiceIdIsNull() { addCriterion("voice_id is null"); return (Criteria) this; }
        public Criteria andVoiceIdIsNotNull() { addCriterion("voice_id is not null"); return (Criteria) this; }
        public Criteria andVoiceIdEqualTo(String value) { addCriterion("voice_id =", value, "voiceId"); return (Criteria) this; }
        public Criteria andVoiceIdNotEqualTo(String value) { addCriterion("voice_id <>", value, "voiceId"); return (Criteria) this; }
        public Criteria andVoiceIdGreaterThan(String value) { addCriterion("voice_id >", value, "voiceId"); return (Criteria) this; }
        public Criteria andVoiceIdLessThan(String value) { addCriterion("voice_id <", value, "voiceId"); return (Criteria) this; }
        public Criteria andVoiceIdLike(String value) { addCriterion("voice_id like", value, "voiceId"); return (Criteria) this; }
        public Criteria andVoiceIdIn(List<String> values) { addCriterion("voice_id in", values, "voiceId"); return (Criteria) this; }
        public Criteria andVoiceSpeedIsNull() { addCriterion("voice_speed is null"); return (Criteria) this; }
        public Criteria andVoiceSpeedIsNotNull() { addCriterion("voice_speed is not null"); return (Criteria) this; }
        public Criteria andVoiceSpeedEqualTo(String value) { addCriterion("voice_speed =", value, "voiceSpeed"); return (Criteria) this; }
        public Criteria andVoiceSpeedNotEqualTo(String value) { addCriterion("voice_speed <>", value, "voiceSpeed"); return (Criteria) this; }
        public Criteria andVoiceSpeedGreaterThan(String value) { addCriterion("voice_speed >", value, "voiceSpeed"); return (Criteria) this; }
        public Criteria andVoiceSpeedLessThan(String value) { addCriterion("voice_speed <", value, "voiceSpeed"); return (Criteria) this; }
        public Criteria andVoiceSpeedLike(String value) { addCriterion("voice_speed like", value, "voiceSpeed"); return (Criteria) this; }
        public Criteria andVoiceSpeedIn(List<String> values) { addCriterion("voice_speed in", values, "voiceSpeed"); return (Criteria) this; }
        public Criteria andEmotionIsNull() { addCriterion("emotion is null"); return (Criteria) this; }
        public Criteria andEmotionIsNotNull() { addCriterion("emotion is not null"); return (Criteria) this; }
        public Criteria andEmotionEqualTo(String value) { addCriterion("emotion =", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionNotEqualTo(String value) { addCriterion("emotion <>", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionGreaterThan(String value) { addCriterion("emotion >", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionLessThan(String value) { addCriterion("emotion <", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionLike(String value) { addCriterion("emotion like", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionIn(List<String> values) { addCriterion("emotion in", values, "emotion"); return (Criteria) this; }
        public Criteria andLineTextIsNull() { addCriterion("line_text is null"); return (Criteria) this; }
        public Criteria andLineTextIsNotNull() { addCriterion("line_text is not null"); return (Criteria) this; }
        public Criteria andLineTextEqualTo(String value) { addCriterion("line_text =", value, "lineText"); return (Criteria) this; }
        public Criteria andLineTextNotEqualTo(String value) { addCriterion("line_text <>", value, "lineText"); return (Criteria) this; }
        public Criteria andLineTextGreaterThan(String value) { addCriterion("line_text >", value, "lineText"); return (Criteria) this; }
        public Criteria andLineTextLessThan(String value) { addCriterion("line_text <", value, "lineText"); return (Criteria) this; }
        public Criteria andLineTextLike(String value) { addCriterion("line_text like", value, "lineText"); return (Criteria) this; }
        public Criteria andLineTextIn(List<String> values) { addCriterion("line_text in", values, "lineText"); return (Criteria) this; }
        public Criteria andAudioAssetIdIsNull() { addCriterion("audio_asset_id is null"); return (Criteria) this; }
        public Criteria andAudioAssetIdIsNotNull() { addCriterion("audio_asset_id is not null"); return (Criteria) this; }
        public Criteria andAudioAssetIdEqualTo(String value) { addCriterion("audio_asset_id =", value, "audioAssetId"); return (Criteria) this; }
        public Criteria andAudioAssetIdNotEqualTo(String value) { addCriterion("audio_asset_id <>", value, "audioAssetId"); return (Criteria) this; }
        public Criteria andAudioAssetIdGreaterThan(String value) { addCriterion("audio_asset_id >", value, "audioAssetId"); return (Criteria) this; }
        public Criteria andAudioAssetIdLessThan(String value) { addCriterion("audio_asset_id <", value, "audioAssetId"); return (Criteria) this; }
        public Criteria andAudioAssetIdLike(String value) { addCriterion("audio_asset_id like", value, "audioAssetId"); return (Criteria) this; }
        public Criteria andAudioAssetIdIn(List<String> values) { addCriterion("audio_asset_id in", values, "audioAssetId"); return (Criteria) this; }
        public Criteria andAudioDurationSecIsNull() { addCriterion("audio_duration_sec is null"); return (Criteria) this; }
        public Criteria andAudioDurationSecIsNotNull() { addCriterion("audio_duration_sec is not null"); return (Criteria) this; }
        public Criteria andAudioDurationSecEqualTo(Integer value) { addCriterion("audio_duration_sec =", value, "audioDurationSec"); return (Criteria) this; }
        public Criteria andAudioDurationSecNotEqualTo(Integer value) { addCriterion("audio_duration_sec <>", value, "audioDurationSec"); return (Criteria) this; }
        public Criteria andAudioDurationSecGreaterThan(Integer value) { addCriterion("audio_duration_sec >", value, "audioDurationSec"); return (Criteria) this; }
        public Criteria andAudioDurationSecLessThan(Integer value) { addCriterion("audio_duration_sec <", value, "audioDurationSec"); return (Criteria) this; }
        public Criteria andAudioDurationSecIn(List<Integer> values) { addCriterion("audio_duration_sec in", values, "audioDurationSec"); return (Criteria) this; }
        public Criteria andAlignStatusIsNull() { addCriterion("align_status is null"); return (Criteria) this; }
        public Criteria andAlignStatusIsNotNull() { addCriterion("align_status is not null"); return (Criteria) this; }
        public Criteria andAlignStatusEqualTo(String value) { addCriterion("align_status =", value, "alignStatus"); return (Criteria) this; }
        public Criteria andAlignStatusNotEqualTo(String value) { addCriterion("align_status <>", value, "alignStatus"); return (Criteria) this; }
        public Criteria andAlignStatusGreaterThan(String value) { addCriterion("align_status >", value, "alignStatus"); return (Criteria) this; }
        public Criteria andAlignStatusLessThan(String value) { addCriterion("align_status <", value, "alignStatus"); return (Criteria) this; }
        public Criteria andAlignStatusLike(String value) { addCriterion("align_status like", value, "alignStatus"); return (Criteria) this; }
        public Criteria andAlignStatusIn(List<String> values) { addCriterion("align_status in", values, "alignStatus"); return (Criteria) this; }
        public Criteria andAlignGapSecIsNull() { addCriterion("align_gap_sec is null"); return (Criteria) this; }
        public Criteria andAlignGapSecIsNotNull() { addCriterion("align_gap_sec is not null"); return (Criteria) this; }
        public Criteria andAlignGapSecEqualTo(String value) { addCriterion("align_gap_sec =", value, "alignGapSec"); return (Criteria) this; }
        public Criteria andAlignGapSecNotEqualTo(String value) { addCriterion("align_gap_sec <>", value, "alignGapSec"); return (Criteria) this; }
        public Criteria andAlignGapSecGreaterThan(String value) { addCriterion("align_gap_sec >", value, "alignGapSec"); return (Criteria) this; }
        public Criteria andAlignGapSecLessThan(String value) { addCriterion("align_gap_sec <", value, "alignGapSec"); return (Criteria) this; }
        public Criteria andAlignGapSecLike(String value) { addCriterion("align_gap_sec like", value, "alignGapSec"); return (Criteria) this; }
        public Criteria andAlignGapSecIn(List<String> values) { addCriterion("align_gap_sec in", values, "alignGapSec"); return (Criteria) this; }
        public Criteria andAutoFixedFlagIsNull() { addCriterion("auto_fixed_flag is null"); return (Criteria) this; }
        public Criteria andAutoFixedFlagIsNotNull() { addCriterion("auto_fixed_flag is not null"); return (Criteria) this; }
        public Criteria andAutoFixedFlagEqualTo(Integer value) { addCriterion("auto_fixed_flag =", value, "autoFixedFlag"); return (Criteria) this; }
        public Criteria andAutoFixedFlagNotEqualTo(Integer value) { addCriterion("auto_fixed_flag <>", value, "autoFixedFlag"); return (Criteria) this; }
        public Criteria andAutoFixedFlagGreaterThan(Integer value) { addCriterion("auto_fixed_flag >", value, "autoFixedFlag"); return (Criteria) this; }
        public Criteria andAutoFixedFlagLessThan(Integer value) { addCriterion("auto_fixed_flag <", value, "autoFixedFlag"); return (Criteria) this; }
        public Criteria andAutoFixedFlagIn(List<Integer> values) { addCriterion("auto_fixed_flag in", values, "autoFixedFlag"); return (Criteria) this; }
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

