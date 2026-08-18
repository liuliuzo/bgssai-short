package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortCharacterExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortCharacterExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andCharacterIdIsNull() { addCriterion("character_id is null"); return (Criteria) this; }
        public Criteria andCharacterIdIsNotNull() { addCriterion("character_id is not null"); return (Criteria) this; }
        public Criteria andCharacterIdEqualTo(String value) { addCriterion("character_id =", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdNotEqualTo(String value) { addCriterion("character_id <>", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdGreaterThan(String value) { addCriterion("character_id >", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdLessThan(String value) { addCriterion("character_id <", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdLike(String value) { addCriterion("character_id like", value, "characterId"); return (Criteria) this; }
        public Criteria andCharacterIdIn(List<String> values) { addCriterion("character_id in", values, "characterId"); return (Criteria) this; }
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
        public Criteria andCharacterNameIsNull() { addCriterion("character_name is null"); return (Criteria) this; }
        public Criteria andCharacterNameIsNotNull() { addCriterion("character_name is not null"); return (Criteria) this; }
        public Criteria andCharacterNameEqualTo(String value) { addCriterion("character_name =", value, "characterName"); return (Criteria) this; }
        public Criteria andCharacterNameNotEqualTo(String value) { addCriterion("character_name <>", value, "characterName"); return (Criteria) this; }
        public Criteria andCharacterNameGreaterThan(String value) { addCriterion("character_name >", value, "characterName"); return (Criteria) this; }
        public Criteria andCharacterNameLessThan(String value) { addCriterion("character_name <", value, "characterName"); return (Criteria) this; }
        public Criteria andCharacterNameLike(String value) { addCriterion("character_name like", value, "characterName"); return (Criteria) this; }
        public Criteria andCharacterNameIn(List<String> values) { addCriterion("character_name in", values, "characterName"); return (Criteria) this; }
        public Criteria andCharacterRoleIsNull() { addCriterion("character_role is null"); return (Criteria) this; }
        public Criteria andCharacterRoleIsNotNull() { addCriterion("character_role is not null"); return (Criteria) this; }
        public Criteria andCharacterRoleEqualTo(String value) { addCriterion("character_role =", value, "characterRole"); return (Criteria) this; }
        public Criteria andCharacterRoleNotEqualTo(String value) { addCriterion("character_role <>", value, "characterRole"); return (Criteria) this; }
        public Criteria andCharacterRoleGreaterThan(String value) { addCriterion("character_role >", value, "characterRole"); return (Criteria) this; }
        public Criteria andCharacterRoleLessThan(String value) { addCriterion("character_role <", value, "characterRole"); return (Criteria) this; }
        public Criteria andCharacterRoleLike(String value) { addCriterion("character_role like", value, "characterRole"); return (Criteria) this; }
        public Criteria andCharacterRoleIn(List<String> values) { addCriterion("character_role in", values, "characterRole"); return (Criteria) this; }
        public Criteria andCardStatusIsNull() { addCriterion("card_status is null"); return (Criteria) this; }
        public Criteria andCardStatusIsNotNull() { addCriterion("card_status is not null"); return (Criteria) this; }
        public Criteria andCardStatusEqualTo(String value) { addCriterion("card_status =", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusNotEqualTo(String value) { addCriterion("card_status <>", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusGreaterThan(String value) { addCriterion("card_status >", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusLessThan(String value) { addCriterion("card_status <", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusLike(String value) { addCriterion("card_status like", value, "cardStatus"); return (Criteria) this; }
        public Criteria andCardStatusIn(List<String> values) { addCriterion("card_status in", values, "cardStatus"); return (Criteria) this; }
        public Criteria andAppearancePromptIsNull() { addCriterion("appearance_prompt is null"); return (Criteria) this; }
        public Criteria andAppearancePromptIsNotNull() { addCriterion("appearance_prompt is not null"); return (Criteria) this; }
        public Criteria andAppearancePromptEqualTo(String value) { addCriterion("appearance_prompt =", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptNotEqualTo(String value) { addCriterion("appearance_prompt <>", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptGreaterThan(String value) { addCriterion("appearance_prompt >", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptLessThan(String value) { addCriterion("appearance_prompt <", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptLike(String value) { addCriterion("appearance_prompt like", value, "appearancePrompt"); return (Criteria) this; }
        public Criteria andAppearancePromptIn(List<String> values) { addCriterion("appearance_prompt in", values, "appearancePrompt"); return (Criteria) this; }
        public Criteria andConsistencySeedIsNull() { addCriterion("consistency_seed is null"); return (Criteria) this; }
        public Criteria andConsistencySeedIsNotNull() { addCriterion("consistency_seed is not null"); return (Criteria) this; }
        public Criteria andConsistencySeedEqualTo(Long value) { addCriterion("consistency_seed =", value, "consistencySeed"); return (Criteria) this; }
        public Criteria andConsistencySeedNotEqualTo(Long value) { addCriterion("consistency_seed <>", value, "consistencySeed"); return (Criteria) this; }
        public Criteria andConsistencySeedGreaterThan(Long value) { addCriterion("consistency_seed >", value, "consistencySeed"); return (Criteria) this; }
        public Criteria andConsistencySeedLessThan(Long value) { addCriterion("consistency_seed <", value, "consistencySeed"); return (Criteria) this; }
        public Criteria andConsistencySeedIn(List<Long> values) { addCriterion("consistency_seed in", values, "consistencySeed"); return (Criteria) this; }
        public Criteria andRefImageAssetIdIsNull() { addCriterion("ref_image_asset_id is null"); return (Criteria) this; }
        public Criteria andRefImageAssetIdIsNotNull() { addCriterion("ref_image_asset_id is not null"); return (Criteria) this; }
        public Criteria andRefImageAssetIdEqualTo(String value) { addCriterion("ref_image_asset_id =", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdNotEqualTo(String value) { addCriterion("ref_image_asset_id <>", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdGreaterThan(String value) { addCriterion("ref_image_asset_id >", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdLessThan(String value) { addCriterion("ref_image_asset_id <", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdLike(String value) { addCriterion("ref_image_asset_id like", value, "refImageAssetId"); return (Criteria) this; }
        public Criteria andRefImageAssetIdIn(List<String> values) { addCriterion("ref_image_asset_id in", values, "refImageAssetId"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeIsNull() { addCriterion("default_outfit_code is null"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeIsNotNull() { addCriterion("default_outfit_code is not null"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeEqualTo(String value) { addCriterion("default_outfit_code =", value, "defaultOutfitCode"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeNotEqualTo(String value) { addCriterion("default_outfit_code <>", value, "defaultOutfitCode"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeGreaterThan(String value) { addCriterion("default_outfit_code >", value, "defaultOutfitCode"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeLessThan(String value) { addCriterion("default_outfit_code <", value, "defaultOutfitCode"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeLike(String value) { addCriterion("default_outfit_code like", value, "defaultOutfitCode"); return (Criteria) this; }
        public Criteria andDefaultOutfitCodeIn(List<String> values) { addCriterion("default_outfit_code in", values, "defaultOutfitCode"); return (Criteria) this; }
        public Criteria andOutfitJsonIsNull() { addCriterion("outfit_json is null"); return (Criteria) this; }
        public Criteria andOutfitJsonIsNotNull() { addCriterion("outfit_json is not null"); return (Criteria) this; }
        public Criteria andOutfitJsonEqualTo(String value) { addCriterion("outfit_json =", value, "outfitJson"); return (Criteria) this; }
        public Criteria andOutfitJsonNotEqualTo(String value) { addCriterion("outfit_json <>", value, "outfitJson"); return (Criteria) this; }
        public Criteria andOutfitJsonGreaterThan(String value) { addCriterion("outfit_json >", value, "outfitJson"); return (Criteria) this; }
        public Criteria andOutfitJsonLessThan(String value) { addCriterion("outfit_json <", value, "outfitJson"); return (Criteria) this; }
        public Criteria andOutfitJsonLike(String value) { addCriterion("outfit_json like", value, "outfitJson"); return (Criteria) this; }
        public Criteria andOutfitJsonIn(List<String> values) { addCriterion("outfit_json in", values, "outfitJson"); return (Criteria) this; }
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

