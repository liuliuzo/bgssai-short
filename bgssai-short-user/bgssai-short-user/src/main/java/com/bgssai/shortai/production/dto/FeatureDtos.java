package com.bgssai.shortai.production.dto;

import java.util.List;
import java.util.Map;

public class FeatureDtos {

    public static class DubbingSubmitReq {
        private String episodeId;
        private List<String> shotIdList;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public List<String> getShotIdList() { return shotIdList; }
        public void setShotIdList(List<String> shotIdList) { this.shotIdList = shotIdList; }
    }

    public static class DubbingRegenReq {
        private String shotId;
        private Integer lineNo;
        private String voiceId;
        private String voiceSpeed;
        private String emotion;
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public Integer getLineNo() { return lineNo; }
        public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }
        public String getVoiceId() { return voiceId; }
        public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
        public String getVoiceSpeed() { return voiceSpeed; }
        public void setVoiceSpeed(String voiceSpeed) { this.voiceSpeed = voiceSpeed; }
        public String getEmotion() { return emotion; }
        public void setEmotion(String emotion) { this.emotion = emotion; }
    }

    public static class DubbingAlignReq {
        private String shotId;
        private String alignAction;
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public String getAlignAction() { return alignAction; }
        public void setAlignAction(String alignAction) { this.alignAction = alignAction; }
    }

    public static class DubbingAlignBatchReq {
        private String episodeId;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    }

    public static class ApplyVoiceReq {
        private String characterId;
        private String voiceId;
        private String voiceSpeed;
        private String emotion;
        public String getCharacterId() { return characterId; }
        public void setCharacterId(String characterId) { this.characterId = characterId; }
        public String getVoiceId() { return voiceId; }
        public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
        public String getVoiceSpeed() { return voiceSpeed; }
        public void setVoiceSpeed(String voiceSpeed) { this.voiceSpeed = voiceSpeed; }
        public String getEmotion() { return emotion; }
        public void setEmotion(String emotion) { this.emotion = emotion; }
    }

    public static class SubtitleRegenReq {
        private String episodeId;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    }

    public static class SubtitleUpdateReq {
        private String subtitleId;
        private String subtitleText;
        private Integer startMs;
        private Integer endMs;
        public String getSubtitleId() { return subtitleId; }
        public void setSubtitleId(String subtitleId) { this.subtitleId = subtitleId; }
        public String getSubtitleText() { return subtitleText; }
        public void setSubtitleText(String subtitleText) { this.subtitleText = subtitleText; }
        public Integer getStartMs() { return startMs; }
        public void setStartMs(Integer startMs) { this.startMs = startMs; }
        public Integer getEndMs() { return endMs; }
        public void setEndMs(Integer endMs) { this.endMs = endMs; }
    }

    public static class SubtitleStyleSaveReq {
        private String workId;
        private Integer fontSize;
        private String fontColor;
        private String strokeColor;
        private String position;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }
        public String getFontColor() { return fontColor; }
        public void setFontColor(String fontColor) { this.fontColor = fontColor; }
        public String getStrokeColor() { return strokeColor; }
        public void setStrokeColor(String strokeColor) { this.strokeColor = strokeColor; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
    }

    public static class FilmSettingsSaveReq {
        private String workId;
        private Map<String, Object> composeSetting;
        private Integer burnSubtitleFlag;
        private String aspectRatio;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public Map<String, Object> getComposeSetting() { return composeSetting; }
        public void setComposeSetting(Map<String, Object> composeSetting) { this.composeSetting = composeSetting; }
        public Integer getBurnSubtitleFlag() { return burnSubtitleFlag; }
        public void setBurnSubtitleFlag(Integer burnSubtitleFlag) { this.burnSubtitleFlag = burnSubtitleFlag; }
        public String getAspectRatio() { return aspectRatio; }
        public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }
    }

    public static class FilmExportReq {
        private List<String> episodeIdList;
        private List<String> versionList;
        public List<String> getEpisodeIdList() { return episodeIdList; }
        public void setEpisodeIdList(List<String> episodeIdList) { this.episodeIdList = episodeIdList; }
        public List<String> getVersionList() { return versionList; }
        public void setVersionList(List<String> versionList) { this.versionList = versionList; }
    }

    public static class FilmRebakeReq {
        private String episodeId;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    }

    public static class PrevizGenerateReq {
        private String episodeId;
        private List<String> shotIdList;
        private String targetLevel;
        private Integer skipLevelCheckFlag;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public List<String> getShotIdList() { return shotIdList; }
        public void setShotIdList(List<String> shotIdList) { this.shotIdList = shotIdList; }
        public String getTargetLevel() { return targetLevel; }
        public void setTargetLevel(String targetLevel) { this.targetLevel = targetLevel; }
        public Integer getSkipLevelCheckFlag() { return skipLevelCheckFlag; }
        public void setSkipLevelCheckFlag(Integer skipLevelCheckFlag) { this.skipLevelCheckFlag = skipLevelCheckFlag; }
    }

    public static class PrevizSaveReq {
        private String shotId;
        private String previzText;
        private String imagePrompt;
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public String getPrevizText() { return previzText; }
        public void setPrevizText(String previzText) { this.previzText = previzText; }
        public String getImagePrompt() { return imagePrompt; }
        public void setImagePrompt(String imagePrompt) { this.imagePrompt = imagePrompt; }
    }

    public static class PrevizPromptReq {
        private String shotId;
        private String imagePrompt;
        private Integer fromVersion;
        private Integer toVersion;
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public String getImagePrompt() { return imagePrompt; }
        public void setImagePrompt(String imagePrompt) { this.imagePrompt = imagePrompt; }
        public Integer getFromVersion() { return fromVersion; }
        public void setFromVersion(Integer fromVersion) { this.fromVersion = fromVersion; }
        public Integer getToVersion() { return toVersion; }
        public void setToVersion(Integer toVersion) { this.toVersion = toVersion; }
    }

    public static class StoryMapGenerateReq {
        private String workId;
        private String episodeId;
        private String mapScope;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getMapScope() { return mapScope; }
        public void setMapScope(String mapScope) { this.mapScope = mapScope; }
    }

    public static class RelationSaveReq {
        private String relationId;
        private String workId;
        private String fromCharacterId;
        private String toCharacterId;
        private String relationType;
        private Integer asOfEpisodeNo;
        private String relationDesc;
        private Integer deletedFlag;
        public String getRelationId() { return relationId; }
        public void setRelationId(String relationId) { this.relationId = relationId; }
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getFromCharacterId() { return fromCharacterId; }
        public void setFromCharacterId(String fromCharacterId) { this.fromCharacterId = fromCharacterId; }
        public String getToCharacterId() { return toCharacterId; }
        public void setToCharacterId(String toCharacterId) { this.toCharacterId = toCharacterId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public Integer getAsOfEpisodeNo() { return asOfEpisodeNo; }
        public void setAsOfEpisodeNo(Integer asOfEpisodeNo) { this.asOfEpisodeNo = asOfEpisodeNo; }
        public String getRelationDesc() { return relationDesc; }
        public void setRelationDesc(String relationDesc) { this.relationDesc = relationDesc; }
        public Integer getDeletedFlag() { return deletedFlag; }
        public void setDeletedFlag(Integer deletedFlag) { this.deletedFlag = deletedFlag; }
    }

    public static class BlockingSaveReq {
        private String shotId;
        private Map<String, Object> blocking;
        private Integer axisFlag;
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public Map<String, Object> getBlocking() { return blocking; }
        public void setBlocking(Map<String, Object> blocking) { this.blocking = blocking; }
        public Integer getAxisFlag() { return axisFlag; }
        public void setAxisFlag(Integer axisFlag) { this.axisFlag = axisFlag; }
    }

    public static class QualityGateRunReq {
        private String workId;
        private String episodeId;
        private String gateCode;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getGateCode() { return gateCode; }
        public void setGateCode(String gateCode) { this.gateCode = gateCode; }
    }

    public static class QualityForcePassReq {
        private String gateId;
        private String forcePassReason;
        public String getGateId() { return gateId; }
        public void setGateId(String gateId) { this.gateId = gateId; }
        public String getForcePassReason() { return forcePassReason; }
        public void setForcePassReason(String forcePassReason) { this.forcePassReason = forcePassReason; }
    }

    public static class QualityIssueHandleReq {
        private String issueId;
        private String handleStatus;
        private String handleReason;
        public String getIssueId() { return issueId; }
        public void setIssueId(String issueId) { this.issueId = issueId; }
        public String getHandleStatus() { return handleStatus; }
        public void setHandleStatus(String handleStatus) { this.handleStatus = handleStatus; }
        public String getHandleReason() { return handleReason; }
        public void setHandleReason(String handleReason) { this.handleReason = handleReason; }
    }

    public static class QualityThresholdAdjustReq {
        private String workId;
        private Integer newThresholdScore;
        private String adjustReason;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public Integer getNewThresholdScore() { return newThresholdScore; }
        public void setNewThresholdScore(Integer newThresholdScore) { this.newThresholdScore = newThresholdScore; }
        public String getAdjustReason() { return adjustReason; }
        public void setAdjustReason(String adjustReason) { this.adjustReason = adjustReason; }
    }

    public static class QualityVariantGenerateReq {
        private String workId;
        private String episodeId;
        private String variantNode;
        private String targetId;
        private Integer variantCount;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getVariantNode() { return variantNode; }
        public void setVariantNode(String variantNode) { this.variantNode = variantNode; }
        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
        public Integer getVariantCount() { return variantCount; }
        public void setVariantCount(Integer variantCount) { this.variantCount = variantCount; }
    }

    public static class QualityVariantSelectReq {
        private String variantId;
        public String getVariantId() { return variantId; }
        public void setVariantId(String variantId) { this.variantId = variantId; }
    }

    public static class ForeshadowSaveReq {
        private String foreshadowId;
        private String workId;
        private Integer plantEpisodeNo;
        private Integer payoffEpisodeNo;
        private String foreshadowDesc;
        private String status;
        private Integer deletedFlag;
        public String getForeshadowId() { return foreshadowId; }
        public void setForeshadowId(String foreshadowId) { this.foreshadowId = foreshadowId; }
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public Integer getPlantEpisodeNo() { return plantEpisodeNo; }
        public void setPlantEpisodeNo(Integer plantEpisodeNo) { this.plantEpisodeNo = plantEpisodeNo; }
        public Integer getPayoffEpisodeNo() { return payoffEpisodeNo; }
        public void setPayoffEpisodeNo(Integer payoffEpisodeNo) { this.payoffEpisodeNo = payoffEpisodeNo; }
        public String getForeshadowDesc() { return foreshadowDesc; }
        public void setForeshadowDesc(String foreshadowDesc) { this.foreshadowDesc = foreshadowDesc; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getDeletedFlag() { return deletedFlag; }
        public void setDeletedFlag(Integer deletedFlag) { this.deletedFlag = deletedFlag; }
    }

    public static class ContinuityRecheckReq {
        private String workId;
        private String episodeId;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    }

    public static class EpisodeBatchReq {
        private String workId;
        private List<String> episodeIdList;
        private String action;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public List<String> getEpisodeIdList() { return episodeIdList; }
        public void setEpisodeIdList(List<String> episodeIdList) { this.episodeIdList = episodeIdList; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    public static class TemplateAdminReq {
        private String templateId;
        private String templateName;
        private String genre;
        private List<String> hookTypeList;
        private Object beatSheetJson;
        private Object turningPointJson;
        private List<String> tabooList;
        private Integer enabledFlag;
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public List<String> getHookTypeList() { return hookTypeList; }
        public void setHookTypeList(List<String> hookTypeList) { this.hookTypeList = hookTypeList; }
        public Object getBeatSheetJson() { return beatSheetJson; }
        public void setBeatSheetJson(Object beatSheetJson) { this.beatSheetJson = beatSheetJson; }
        public Object getTurningPointJson() { return turningPointJson; }
        public void setTurningPointJson(Object turningPointJson) { this.turningPointJson = turningPointJson; }
        public List<String> getTabooList() { return tabooList; }
        public void setTabooList(List<String> tabooList) { this.tabooList = tabooList; }
        public Integer getEnabledFlag() { return enabledFlag; }
        public void setEnabledFlag(Integer enabledFlag) { this.enabledFlag = enabledFlag; }
    }

    public static class ThresholdSaveAdminReq {
        private String genre;
        private Integer defaultThresholdScore;
        private Integer firstEpisodeThresholdScore;
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public Integer getDefaultThresholdScore() { return defaultThresholdScore; }
        public void setDefaultThresholdScore(Integer defaultThresholdScore) { this.defaultThresholdScore = defaultThresholdScore; }
        public Integer getFirstEpisodeThresholdScore() { return firstEpisodeThresholdScore; }
        public void setFirstEpisodeThresholdScore(Integer firstEpisodeThresholdScore) { this.firstEpisodeThresholdScore = firstEpisodeThresholdScore; }
    }
}
