package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ShortShotExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    public ShortShotExample() { oredCriteria = new ArrayList<>(); }
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
        public Criteria andShotNoIsNull() { addCriterion("shot_no is null"); return (Criteria) this; }
        public Criteria andShotNoIsNotNull() { addCriterion("shot_no is not null"); return (Criteria) this; }
        public Criteria andShotNoEqualTo(Integer value) { addCriterion("shot_no =", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoNotEqualTo(Integer value) { addCriterion("shot_no <>", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoGreaterThan(Integer value) { addCriterion("shot_no >", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoLessThan(Integer value) { addCriterion("shot_no <", value, "shotNo"); return (Criteria) this; }
        public Criteria andShotNoIn(List<Integer> values) { addCriterion("shot_no in", values, "shotNo"); return (Criteria) this; }
        public Criteria andSceneNoIsNull() { addCriterion("scene_no is null"); return (Criteria) this; }
        public Criteria andSceneNoIsNotNull() { addCriterion("scene_no is not null"); return (Criteria) this; }
        public Criteria andSceneNoEqualTo(Integer value) { addCriterion("scene_no =", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoNotEqualTo(Integer value) { addCriterion("scene_no <>", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoGreaterThan(Integer value) { addCriterion("scene_no >", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoLessThan(Integer value) { addCriterion("scene_no <", value, "sceneNo"); return (Criteria) this; }
        public Criteria andSceneNoIn(List<Integer> values) { addCriterion("scene_no in", values, "sceneNo"); return (Criteria) this; }
        public Criteria andShotSizeIsNull() { addCriterion("shot_size is null"); return (Criteria) this; }
        public Criteria andShotSizeIsNotNull() { addCriterion("shot_size is not null"); return (Criteria) this; }
        public Criteria andShotSizeEqualTo(String value) { addCriterion("shot_size =", value, "shotSize"); return (Criteria) this; }
        public Criteria andShotSizeNotEqualTo(String value) { addCriterion("shot_size <>", value, "shotSize"); return (Criteria) this; }
        public Criteria andShotSizeGreaterThan(String value) { addCriterion("shot_size >", value, "shotSize"); return (Criteria) this; }
        public Criteria andShotSizeLessThan(String value) { addCriterion("shot_size <", value, "shotSize"); return (Criteria) this; }
        public Criteria andShotSizeLike(String value) { addCriterion("shot_size like", value, "shotSize"); return (Criteria) this; }
        public Criteria andShotSizeIn(List<String> values) { addCriterion("shot_size in", values, "shotSize"); return (Criteria) this; }
        public Criteria andCameraMoveIsNull() { addCriterion("camera_move is null"); return (Criteria) this; }
        public Criteria andCameraMoveIsNotNull() { addCriterion("camera_move is not null"); return (Criteria) this; }
        public Criteria andCameraMoveEqualTo(String value) { addCriterion("camera_move =", value, "cameraMove"); return (Criteria) this; }
        public Criteria andCameraMoveNotEqualTo(String value) { addCriterion("camera_move <>", value, "cameraMove"); return (Criteria) this; }
        public Criteria andCameraMoveGreaterThan(String value) { addCriterion("camera_move >", value, "cameraMove"); return (Criteria) this; }
        public Criteria andCameraMoveLessThan(String value) { addCriterion("camera_move <", value, "cameraMove"); return (Criteria) this; }
        public Criteria andCameraMoveLike(String value) { addCriterion("camera_move like", value, "cameraMove"); return (Criteria) this; }
        public Criteria andCameraMoveIn(List<String> values) { addCriterion("camera_move in", values, "cameraMove"); return (Criteria) this; }
        public Criteria andShotDurationSecIsNull() { addCriterion("shot_duration_sec is null"); return (Criteria) this; }
        public Criteria andShotDurationSecIsNotNull() { addCriterion("shot_duration_sec is not null"); return (Criteria) this; }
        public Criteria andShotDurationSecEqualTo(Integer value) { addCriterion("shot_duration_sec =", value, "shotDurationSec"); return (Criteria) this; }
        public Criteria andShotDurationSecNotEqualTo(Integer value) { addCriterion("shot_duration_sec <>", value, "shotDurationSec"); return (Criteria) this; }
        public Criteria andShotDurationSecGreaterThan(Integer value) { addCriterion("shot_duration_sec >", value, "shotDurationSec"); return (Criteria) this; }
        public Criteria andShotDurationSecLessThan(Integer value) { addCriterion("shot_duration_sec <", value, "shotDurationSec"); return (Criteria) this; }
        public Criteria andShotDurationSecIn(List<Integer> values) { addCriterion("shot_duration_sec in", values, "shotDurationSec"); return (Criteria) this; }
        public Criteria andShotDescIsNull() { addCriterion("shot_desc is null"); return (Criteria) this; }
        public Criteria andShotDescIsNotNull() { addCriterion("shot_desc is not null"); return (Criteria) this; }
        public Criteria andShotDescEqualTo(String value) { addCriterion("shot_desc =", value, "shotDesc"); return (Criteria) this; }
        public Criteria andShotDescNotEqualTo(String value) { addCriterion("shot_desc <>", value, "shotDesc"); return (Criteria) this; }
        public Criteria andShotDescGreaterThan(String value) { addCriterion("shot_desc >", value, "shotDesc"); return (Criteria) this; }
        public Criteria andShotDescLessThan(String value) { addCriterion("shot_desc <", value, "shotDesc"); return (Criteria) this; }
        public Criteria andShotDescLike(String value) { addCriterion("shot_desc like", value, "shotDesc"); return (Criteria) this; }
        public Criteria andShotDescIn(List<String> values) { addCriterion("shot_desc in", values, "shotDesc"); return (Criteria) this; }
        public Criteria andDialogueTextIsNull() { addCriterion("dialogue_text is null"); return (Criteria) this; }
        public Criteria andDialogueTextIsNotNull() { addCriterion("dialogue_text is not null"); return (Criteria) this; }
        public Criteria andDialogueTextEqualTo(String value) { addCriterion("dialogue_text =", value, "dialogueText"); return (Criteria) this; }
        public Criteria andDialogueTextNotEqualTo(String value) { addCriterion("dialogue_text <>", value, "dialogueText"); return (Criteria) this; }
        public Criteria andDialogueTextGreaterThan(String value) { addCriterion("dialogue_text >", value, "dialogueText"); return (Criteria) this; }
        public Criteria andDialogueTextLessThan(String value) { addCriterion("dialogue_text <", value, "dialogueText"); return (Criteria) this; }
        public Criteria andDialogueTextLike(String value) { addCriterion("dialogue_text like", value, "dialogueText"); return (Criteria) this; }
        public Criteria andDialogueTextIn(List<String> values) { addCriterion("dialogue_text in", values, "dialogueText"); return (Criteria) this; }
        public Criteria andEmotionIsNull() { addCriterion("emotion is null"); return (Criteria) this; }
        public Criteria andEmotionIsNotNull() { addCriterion("emotion is not null"); return (Criteria) this; }
        public Criteria andEmotionEqualTo(String value) { addCriterion("emotion =", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionNotEqualTo(String value) { addCriterion("emotion <>", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionGreaterThan(String value) { addCriterion("emotion >", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionLessThan(String value) { addCriterion("emotion <", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionLike(String value) { addCriterion("emotion like", value, "emotion"); return (Criteria) this; }
        public Criteria andEmotionIn(List<String> values) { addCriterion("emotion in", values, "emotion"); return (Criteria) this; }
        public Criteria andLightingIsNull() { addCriterion("lighting is null"); return (Criteria) this; }
        public Criteria andLightingIsNotNull() { addCriterion("lighting is not null"); return (Criteria) this; }
        public Criteria andLightingEqualTo(String value) { addCriterion("lighting =", value, "lighting"); return (Criteria) this; }
        public Criteria andLightingNotEqualTo(String value) { addCriterion("lighting <>", value, "lighting"); return (Criteria) this; }
        public Criteria andLightingGreaterThan(String value) { addCriterion("lighting >", value, "lighting"); return (Criteria) this; }
        public Criteria andLightingLessThan(String value) { addCriterion("lighting <", value, "lighting"); return (Criteria) this; }
        public Criteria andLightingLike(String value) { addCriterion("lighting like", value, "lighting"); return (Criteria) this; }
        public Criteria andLightingIn(List<String> values) { addCriterion("lighting in", values, "lighting"); return (Criteria) this; }
        public Criteria andCharacterRefJsonIsNull() { addCriterion("character_ref_json is null"); return (Criteria) this; }
        public Criteria andCharacterRefJsonIsNotNull() { addCriterion("character_ref_json is not null"); return (Criteria) this; }
        public Criteria andCharacterRefJsonEqualTo(String value) { addCriterion("character_ref_json =", value, "characterRefJson"); return (Criteria) this; }
        public Criteria andCharacterRefJsonNotEqualTo(String value) { addCriterion("character_ref_json <>", value, "characterRefJson"); return (Criteria) this; }
        public Criteria andCharacterRefJsonGreaterThan(String value) { addCriterion("character_ref_json >", value, "characterRefJson"); return (Criteria) this; }
        public Criteria andCharacterRefJsonLessThan(String value) { addCriterion("character_ref_json <", value, "characterRefJson"); return (Criteria) this; }
        public Criteria andCharacterRefJsonLike(String value) { addCriterion("character_ref_json like", value, "characterRefJson"); return (Criteria) this; }
        public Criteria andCharacterRefJsonIn(List<String> values) { addCriterion("character_ref_json in", values, "characterRefJson"); return (Criteria) this; }
        public Criteria andSceneCardIdIsNull() { addCriterion("scene_card_id is null"); return (Criteria) this; }
        public Criteria andSceneCardIdIsNotNull() { addCriterion("scene_card_id is not null"); return (Criteria) this; }
        public Criteria andSceneCardIdEqualTo(String value) { addCriterion("scene_card_id =", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdNotEqualTo(String value) { addCriterion("scene_card_id <>", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdGreaterThan(String value) { addCriterion("scene_card_id >", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdLessThan(String value) { addCriterion("scene_card_id <", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdLike(String value) { addCriterion("scene_card_id like", value, "sceneCardId"); return (Criteria) this; }
        public Criteria andSceneCardIdIn(List<String> values) { addCriterion("scene_card_id in", values, "sceneCardId"); return (Criteria) this; }
        public Criteria andExtraPromptIsNull() { addCriterion("extra_prompt is null"); return (Criteria) this; }
        public Criteria andExtraPromptIsNotNull() { addCriterion("extra_prompt is not null"); return (Criteria) this; }
        public Criteria andExtraPromptEqualTo(String value) { addCriterion("extra_prompt =", value, "extraPrompt"); return (Criteria) this; }
        public Criteria andExtraPromptNotEqualTo(String value) { addCriterion("extra_prompt <>", value, "extraPrompt"); return (Criteria) this; }
        public Criteria andExtraPromptGreaterThan(String value) { addCriterion("extra_prompt >", value, "extraPrompt"); return (Criteria) this; }
        public Criteria andExtraPromptLessThan(String value) { addCriterion("extra_prompt <", value, "extraPrompt"); return (Criteria) this; }
        public Criteria andExtraPromptLike(String value) { addCriterion("extra_prompt like", value, "extraPrompt"); return (Criteria) this; }
        public Criteria andExtraPromptIn(List<String> values) { addCriterion("extra_prompt in", values, "extraPrompt"); return (Criteria) this; }
        public Criteria andImagePromptIsNull() { addCriterion("image_prompt is null"); return (Criteria) this; }
        public Criteria andImagePromptIsNotNull() { addCriterion("image_prompt is not null"); return (Criteria) this; }
        public Criteria andImagePromptEqualTo(String value) { addCriterion("image_prompt =", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptNotEqualTo(String value) { addCriterion("image_prompt <>", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptGreaterThan(String value) { addCriterion("image_prompt >", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptLessThan(String value) { addCriterion("image_prompt <", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptLike(String value) { addCriterion("image_prompt like", value, "imagePrompt"); return (Criteria) this; }
        public Criteria andImagePromptIn(List<String> values) { addCriterion("image_prompt in", values, "imagePrompt"); return (Criteria) this; }
        public Criteria andPrevizTextIsNull() { addCriterion("previz_text is null"); return (Criteria) this; }
        public Criteria andPrevizTextIsNotNull() { addCriterion("previz_text is not null"); return (Criteria) this; }
        public Criteria andPrevizTextEqualTo(String value) { addCriterion("previz_text =", value, "previzText"); return (Criteria) this; }
        public Criteria andPrevizTextNotEqualTo(String value) { addCriterion("previz_text <>", value, "previzText"); return (Criteria) this; }
        public Criteria andPrevizTextGreaterThan(String value) { addCriterion("previz_text >", value, "previzText"); return (Criteria) this; }
        public Criteria andPrevizTextLessThan(String value) { addCriterion("previz_text <", value, "previzText"); return (Criteria) this; }
        public Criteria andPrevizTextLike(String value) { addCriterion("previz_text like", value, "previzText"); return (Criteria) this; }
        public Criteria andPrevizTextIn(List<String> values) { addCriterion("previz_text in", values, "previzText"); return (Criteria) this; }
        public Criteria andBlockingJsonIsNull() { addCriterion("blocking_json is null"); return (Criteria) this; }
        public Criteria andBlockingJsonIsNotNull() { addCriterion("blocking_json is not null"); return (Criteria) this; }
        public Criteria andBlockingJsonEqualTo(String value) { addCriterion("blocking_json =", value, "blockingJson"); return (Criteria) this; }
        public Criteria andBlockingJsonNotEqualTo(String value) { addCriterion("blocking_json <>", value, "blockingJson"); return (Criteria) this; }
        public Criteria andBlockingJsonGreaterThan(String value) { addCriterion("blocking_json >", value, "blockingJson"); return (Criteria) this; }
        public Criteria andBlockingJsonLessThan(String value) { addCriterion("blocking_json <", value, "blockingJson"); return (Criteria) this; }
        public Criteria andBlockingJsonLike(String value) { addCriterion("blocking_json like", value, "blockingJson"); return (Criteria) this; }
        public Criteria andBlockingJsonIn(List<String> values) { addCriterion("blocking_json in", values, "blockingJson"); return (Criteria) this; }
        public Criteria andAxisFlagIsNull() { addCriterion("axis_flag is null"); return (Criteria) this; }
        public Criteria andAxisFlagIsNotNull() { addCriterion("axis_flag is not null"); return (Criteria) this; }
        public Criteria andAxisFlagEqualTo(Integer value) { addCriterion("axis_flag =", value, "axisFlag"); return (Criteria) this; }
        public Criteria andAxisFlagNotEqualTo(Integer value) { addCriterion("axis_flag <>", value, "axisFlag"); return (Criteria) this; }
        public Criteria andAxisFlagGreaterThan(Integer value) { addCriterion("axis_flag >", value, "axisFlag"); return (Criteria) this; }
        public Criteria andAxisFlagLessThan(Integer value) { addCriterion("axis_flag <", value, "axisFlag"); return (Criteria) this; }
        public Criteria andAxisFlagIn(List<Integer> values) { addCriterion("axis_flag in", values, "axisFlag"); return (Criteria) this; }
        public Criteria andPrevizLevelIsNull() { addCriterion("previz_level is null"); return (Criteria) this; }
        public Criteria andPrevizLevelIsNotNull() { addCriterion("previz_level is not null"); return (Criteria) this; }
        public Criteria andPrevizLevelEqualTo(String value) { addCriterion("previz_level =", value, "previzLevel"); return (Criteria) this; }
        public Criteria andPrevizLevelNotEqualTo(String value) { addCriterion("previz_level <>", value, "previzLevel"); return (Criteria) this; }
        public Criteria andPrevizLevelGreaterThan(String value) { addCriterion("previz_level >", value, "previzLevel"); return (Criteria) this; }
        public Criteria andPrevizLevelLessThan(String value) { addCriterion("previz_level <", value, "previzLevel"); return (Criteria) this; }
        public Criteria andPrevizLevelLike(String value) { addCriterion("previz_level like", value, "previzLevel"); return (Criteria) this; }
        public Criteria andPrevizLevelIn(List<String> values) { addCriterion("previz_level in", values, "previzLevel"); return (Criteria) this; }
        public Criteria andStaleFlagIsNull() { addCriterion("stale_flag is null"); return (Criteria) this; }
        public Criteria andStaleFlagIsNotNull() { addCriterion("stale_flag is not null"); return (Criteria) this; }
        public Criteria andStaleFlagEqualTo(Integer value) { addCriterion("stale_flag =", value, "staleFlag"); return (Criteria) this; }
        public Criteria andStaleFlagNotEqualTo(Integer value) { addCriterion("stale_flag <>", value, "staleFlag"); return (Criteria) this; }
        public Criteria andStaleFlagGreaterThan(Integer value) { addCriterion("stale_flag >", value, "staleFlag"); return (Criteria) this; }
        public Criteria andStaleFlagLessThan(Integer value) { addCriterion("stale_flag <", value, "staleFlag"); return (Criteria) this; }
        public Criteria andStaleFlagIn(List<Integer> values) { addCriterion("stale_flag in", values, "staleFlag"); return (Criteria) this; }
        public Criteria andPromptVersionIsNull() { addCriterion("prompt_version is null"); return (Criteria) this; }
        public Criteria andPromptVersionIsNotNull() { addCriterion("prompt_version is not null"); return (Criteria) this; }
        public Criteria andPromptVersionEqualTo(Integer value) { addCriterion("prompt_version =", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionNotEqualTo(Integer value) { addCriterion("prompt_version <>", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionGreaterThan(Integer value) { addCriterion("prompt_version >", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionLessThan(Integer value) { addCriterion("prompt_version <", value, "promptVersion"); return (Criteria) this; }
        public Criteria andPromptVersionIn(List<Integer> values) { addCriterion("prompt_version in", values, "promptVersion"); return (Criteria) this; }
        public Criteria andShotStatusIsNull() { addCriterion("shot_status is null"); return (Criteria) this; }
        public Criteria andShotStatusIsNotNull() { addCriterion("shot_status is not null"); return (Criteria) this; }
        public Criteria andShotStatusEqualTo(String value) { addCriterion("shot_status =", value, "shotStatus"); return (Criteria) this; }
        public Criteria andShotStatusNotEqualTo(String value) { addCriterion("shot_status <>", value, "shotStatus"); return (Criteria) this; }
        public Criteria andShotStatusGreaterThan(String value) { addCriterion("shot_status >", value, "shotStatus"); return (Criteria) this; }
        public Criteria andShotStatusLessThan(String value) { addCriterion("shot_status <", value, "shotStatus"); return (Criteria) this; }
        public Criteria andShotStatusLike(String value) { addCriterion("shot_status like", value, "shotStatus"); return (Criteria) this; }
        public Criteria andShotStatusIn(List<String> values) { addCriterion("shot_status in", values, "shotStatus"); return (Criteria) this; }
        public Criteria andBlockedFlagIsNull() { addCriterion("blocked_flag is null"); return (Criteria) this; }
        public Criteria andBlockedFlagIsNotNull() { addCriterion("blocked_flag is not null"); return (Criteria) this; }
        public Criteria andBlockedFlagEqualTo(Integer value) { addCriterion("blocked_flag =", value, "blockedFlag"); return (Criteria) this; }
        public Criteria andBlockedFlagNotEqualTo(Integer value) { addCriterion("blocked_flag <>", value, "blockedFlag"); return (Criteria) this; }
        public Criteria andBlockedFlagGreaterThan(Integer value) { addCriterion("blocked_flag >", value, "blockedFlag"); return (Criteria) this; }
        public Criteria andBlockedFlagLessThan(Integer value) { addCriterion("blocked_flag <", value, "blockedFlag"); return (Criteria) this; }
        public Criteria andBlockedFlagIn(List<Integer> values) { addCriterion("blocked_flag in", values, "blockedFlag"); return (Criteria) this; }
        public Criteria andRiskLevelIsNull() { addCriterion("risk_level is null"); return (Criteria) this; }
        public Criteria andRiskLevelIsNotNull() { addCriterion("risk_level is not null"); return (Criteria) this; }
        public Criteria andRiskLevelEqualTo(String value) { addCriterion("risk_level =", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelNotEqualTo(String value) { addCriterion("risk_level <>", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelGreaterThan(String value) { addCriterion("risk_level >", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelLessThan(String value) { addCriterion("risk_level <", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelLike(String value) { addCriterion("risk_level like", value, "riskLevel"); return (Criteria) this; }
        public Criteria andRiskLevelIn(List<String> values) { addCriterion("risk_level in", values, "riskLevel"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdIsNull() { addCriterion("first_frame_asset_id is null"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdIsNotNull() { addCriterion("first_frame_asset_id is not null"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdEqualTo(String value) { addCriterion("first_frame_asset_id =", value, "firstFrameAssetId"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdNotEqualTo(String value) { addCriterion("first_frame_asset_id <>", value, "firstFrameAssetId"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdGreaterThan(String value) { addCriterion("first_frame_asset_id >", value, "firstFrameAssetId"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdLessThan(String value) { addCriterion("first_frame_asset_id <", value, "firstFrameAssetId"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdLike(String value) { addCriterion("first_frame_asset_id like", value, "firstFrameAssetId"); return (Criteria) this; }
        public Criteria andFirstFrameAssetIdIn(List<String> values) { addCriterion("first_frame_asset_id in", values, "firstFrameAssetId"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdIsNull() { addCriterion("last_frame_asset_id is null"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdIsNotNull() { addCriterion("last_frame_asset_id is not null"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdEqualTo(String value) { addCriterion("last_frame_asset_id =", value, "lastFrameAssetId"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdNotEqualTo(String value) { addCriterion("last_frame_asset_id <>", value, "lastFrameAssetId"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdGreaterThan(String value) { addCriterion("last_frame_asset_id >", value, "lastFrameAssetId"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdLessThan(String value) { addCriterion("last_frame_asset_id <", value, "lastFrameAssetId"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdLike(String value) { addCriterion("last_frame_asset_id like", value, "lastFrameAssetId"); return (Criteria) this; }
        public Criteria andLastFrameAssetIdIn(List<String> values) { addCriterion("last_frame_asset_id in", values, "lastFrameAssetId"); return (Criteria) this; }
        public Criteria andFrameLinkModeIsNull() { addCriterion("frame_link_mode is null"); return (Criteria) this; }
        public Criteria andFrameLinkModeIsNotNull() { addCriterion("frame_link_mode is not null"); return (Criteria) this; }
        public Criteria andFrameLinkModeEqualTo(String value) { addCriterion("frame_link_mode =", value, "frameLinkMode"); return (Criteria) this; }
        public Criteria andFrameLinkModeNotEqualTo(String value) { addCriterion("frame_link_mode <>", value, "frameLinkMode"); return (Criteria) this; }
        public Criteria andFrameLinkModeGreaterThan(String value) { addCriterion("frame_link_mode >", value, "frameLinkMode"); return (Criteria) this; }
        public Criteria andFrameLinkModeLessThan(String value) { addCriterion("frame_link_mode <", value, "frameLinkMode"); return (Criteria) this; }
        public Criteria andFrameLinkModeLike(String value) { addCriterion("frame_link_mode like", value, "frameLinkMode"); return (Criteria) this; }
        public Criteria andFrameLinkModeIn(List<String> values) { addCriterion("frame_link_mode in", values, "frameLinkMode"); return (Criteria) this; }
        public Criteria andLastFramePromptIsNull() { addCriterion("last_frame_prompt is null"); return (Criteria) this; }
        public Criteria andLastFramePromptIsNotNull() { addCriterion("last_frame_prompt is not null"); return (Criteria) this; }
        public Criteria andLastFramePromptEqualTo(String value) { addCriterion("last_frame_prompt =", value, "lastFramePrompt"); return (Criteria) this; }
        public Criteria andLastFramePromptNotEqualTo(String value) { addCriterion("last_frame_prompt <>", value, "lastFramePrompt"); return (Criteria) this; }
        public Criteria andLastFramePromptGreaterThan(String value) { addCriterion("last_frame_prompt >", value, "lastFramePrompt"); return (Criteria) this; }
        public Criteria andLastFramePromptLessThan(String value) { addCriterion("last_frame_prompt <", value, "lastFramePrompt"); return (Criteria) this; }
        public Criteria andLastFramePromptLike(String value) { addCriterion("last_frame_prompt like", value, "lastFramePrompt"); return (Criteria) this; }
        public Criteria andLastFramePromptIn(List<String> values) { addCriterion("last_frame_prompt in", values, "lastFramePrompt"); return (Criteria) this; }
        public Criteria andVideoAssetIdIsNull() { addCriterion("video_asset_id is null"); return (Criteria) this; }
        public Criteria andVideoAssetIdIsNotNull() { addCriterion("video_asset_id is not null"); return (Criteria) this; }
        public Criteria andVideoAssetIdEqualTo(String value) { addCriterion("video_asset_id =", value, "videoAssetId"); return (Criteria) this; }
        public Criteria andVideoAssetIdNotEqualTo(String value) { addCriterion("video_asset_id <>", value, "videoAssetId"); return (Criteria) this; }
        public Criteria andVideoAssetIdGreaterThan(String value) { addCriterion("video_asset_id >", value, "videoAssetId"); return (Criteria) this; }
        public Criteria andVideoAssetIdLessThan(String value) { addCriterion("video_asset_id <", value, "videoAssetId"); return (Criteria) this; }
        public Criteria andVideoAssetIdLike(String value) { addCriterion("video_asset_id like", value, "videoAssetId"); return (Criteria) this; }
        public Criteria andVideoAssetIdIn(List<String> values) { addCriterion("video_asset_id in", values, "videoAssetId"); return (Criteria) this; }
        public Criteria andRetryCountIsNull() { addCriterion("retry_count is null"); return (Criteria) this; }
        public Criteria andRetryCountIsNotNull() { addCriterion("retry_count is not null"); return (Criteria) this; }
        public Criteria andRetryCountEqualTo(Integer value) { addCriterion("retry_count =", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountNotEqualTo(Integer value) { addCriterion("retry_count <>", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountGreaterThan(Integer value) { addCriterion("retry_count >", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountLessThan(Integer value) { addCriterion("retry_count <", value, "retryCount"); return (Criteria) this; }
        public Criteria andRetryCountIn(List<Integer> values) { addCriterion("retry_count in", values, "retryCount"); return (Criteria) this; }
        public Criteria andCostSpentCentIsNull() { addCriterion("cost_spent_cent is null"); return (Criteria) this; }
        public Criteria andCostSpentCentIsNotNull() { addCriterion("cost_spent_cent is not null"); return (Criteria) this; }
        public Criteria andCostSpentCentEqualTo(Integer value) { addCriterion("cost_spent_cent =", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentNotEqualTo(Integer value) { addCriterion("cost_spent_cent <>", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentGreaterThan(Integer value) { addCriterion("cost_spent_cent >", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentLessThan(Integer value) { addCriterion("cost_spent_cent <", value, "costSpentCent"); return (Criteria) this; }
        public Criteria andCostSpentCentIn(List<Integer> values) { addCriterion("cost_spent_cent in", values, "costSpentCent"); return (Criteria) this; }
        public Criteria andShotVersionIsNull() { addCriterion("shot_version is null"); return (Criteria) this; }
        public Criteria andShotVersionIsNotNull() { addCriterion("shot_version is not null"); return (Criteria) this; }
        public Criteria andShotVersionEqualTo(Integer value) { addCriterion("shot_version =", value, "shotVersion"); return (Criteria) this; }
        public Criteria andShotVersionNotEqualTo(Integer value) { addCriterion("shot_version <>", value, "shotVersion"); return (Criteria) this; }
        public Criteria andShotVersionGreaterThan(Integer value) { addCriterion("shot_version >", value, "shotVersion"); return (Criteria) this; }
        public Criteria andShotVersionLessThan(Integer value) { addCriterion("shot_version <", value, "shotVersion"); return (Criteria) this; }
        public Criteria andShotVersionIn(List<Integer> values) { addCriterion("shot_version in", values, "shotVersion"); return (Criteria) this; }
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

