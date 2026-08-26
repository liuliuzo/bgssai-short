package com.bgssai.shortai.production.dto;

import java.util.List;
import java.util.Map;

public class ProductionDtos {

    public static class WorkCreateReq {
        private String workTitle;
        private String oneLinePitch;
        private String genre;
        private Integer episodeCount;
        private Integer episodeDurationSec;
        private String visualStyle;
        public String getWorkTitle() { return workTitle; }
        public void setWorkTitle(String workTitle) { this.workTitle = workTitle; }
        public String getOneLinePitch() { return oneLinePitch; }
        public void setOneLinePitch(String oneLinePitch) { this.oneLinePitch = oneLinePitch; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public Integer getEpisodeCount() { return episodeCount; }
        public void setEpisodeCount(Integer episodeCount) { this.episodeCount = episodeCount; }
        public Integer getEpisodeDurationSec() { return episodeDurationSec; }
        public void setEpisodeDurationSec(Integer episodeDurationSec) { this.episodeDurationSec = episodeDurationSec; }
        public String getVisualStyle() { return visualStyle; }
        public void setVisualStyle(String visualStyle) { this.visualStyle = visualStyle; }
    }

    public static class WorkUpdateReq {
        private String workId;
        private String workTitle;
        private String oneLinePitch;
        private String genre;
        private String visualStyle;
        private Integer costBudgetCent;
        private Integer qualityThresholdScore;
        private Integer skipLevelCheckFlag;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getWorkTitle() { return workTitle; }
        public void setWorkTitle(String workTitle) { this.workTitle = workTitle; }
        public String getOneLinePitch() { return oneLinePitch; }
        public void setOneLinePitch(String oneLinePitch) { this.oneLinePitch = oneLinePitch; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public String getVisualStyle() { return visualStyle; }
        public void setVisualStyle(String visualStyle) { this.visualStyle = visualStyle; }
        public Integer getCostBudgetCent() { return costBudgetCent; }
        public void setCostBudgetCent(Integer costBudgetCent) { this.costBudgetCent = costBudgetCent; }
        public Integer getQualityThresholdScore() { return qualityThresholdScore; }
        public void setQualityThresholdScore(Integer qualityThresholdScore) { this.qualityThresholdScore = qualityThresholdScore; }
        public Integer getSkipLevelCheckFlag() { return skipLevelCheckFlag; }
        public void setSkipLevelCheckFlag(Integer skipLevelCheckFlag) { this.skipLevelCheckFlag = skipLevelCheckFlag; }
    }

    public static class WorkArchiveReq {
        private String workId;
        private Integer archivedFlag;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public Integer getArchivedFlag() { return archivedFlag; }
        public void setArchivedFlag(Integer archivedFlag) { this.archivedFlag = archivedFlag; }
    }

    public static class EpisodeReq {
        private String workId;
        private String episodeId;
        private String episodeTitle;
        private String episodeSynopsis;
        private String endingHook;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getEpisodeTitle() { return episodeTitle; }
        public void setEpisodeTitle(String episodeTitle) { this.episodeTitle = episodeTitle; }
        public String getEpisodeSynopsis() { return episodeSynopsis; }
        public void setEpisodeSynopsis(String episodeSynopsis) { this.episodeSynopsis = episodeSynopsis; }
        public String getEndingHook() { return endingHook; }
        public void setEndingHook(String endingHook) { this.endingHook = endingHook; }
    }

    public static class OutlineSaveReq {
        private String workId;
        private List<Map<String, Object>> episodeList;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public List<Map<String, Object>> getEpisodeList() { return episodeList; }
        public void setEpisodeList(List<Map<String, Object>> episodeList) { this.episodeList = episodeList; }
    }

    public static class ScriptSaveReq {
        private String episodeId;
        private Integer scriptVersion;
        private List<Map<String, Object>> sceneList;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public Integer getScriptVersion() { return scriptVersion; }
        public void setScriptVersion(Integer scriptVersion) { this.scriptVersion = scriptVersion; }
        public List<Map<String, Object>> getSceneList() { return sceneList; }
        public void setSceneList(List<Map<String, Object>> sceneList) { this.sceneList = sceneList; }
    }

    public static class ScriptGenerateReq {
        private String workId;
        private String episodeId;
        private String extraPrompt;
        private Integer sceneNo;
        private Integer lineNo;
        private String instruction;
        private Integer forceReadyFlag;
        private Integer scriptVersion;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getExtraPrompt() { return extraPrompt; }
        public void setExtraPrompt(String extraPrompt) { this.extraPrompt = extraPrompt; }
        public Integer getSceneNo() { return sceneNo; }
        public void setSceneNo(Integer sceneNo) { this.sceneNo = sceneNo; }
        public Integer getLineNo() { return lineNo; }
        public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }
        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
        public Integer getForceReadyFlag() { return forceReadyFlag; }
        public void setForceReadyFlag(Integer forceReadyFlag) { this.forceReadyFlag = forceReadyFlag; }
        public Integer getScriptVersion() { return scriptVersion; }
        public void setScriptVersion(Integer scriptVersion) { this.scriptVersion = scriptVersion; }
    }

    public static class CharacterReq {
        private String workId;
        private String characterId;
        private String sceneCardId;
        private String characterName;
        private String characterRole;
        private String appearancePrompt;
        private String cardName;
        private String cardType;
        private String assetId;
        private Integer candidateCount;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getCharacterId() { return characterId; }
        public void setCharacterId(String characterId) { this.characterId = characterId; }
        public String getSceneCardId() { return sceneCardId; }
        public void setSceneCardId(String sceneCardId) { this.sceneCardId = sceneCardId; }
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
        public String getCharacterRole() { return characterRole; }
        public void setCharacterRole(String characterRole) { this.characterRole = characterRole; }
        public String getAppearancePrompt() { return appearancePrompt; }
        public void setAppearancePrompt(String appearancePrompt) { this.appearancePrompt = appearancePrompt; }
        public String getCardName() { return cardName; }
        public void setCardName(String cardName) { this.cardName = cardName; }
        public String getCardType() { return cardType; }
        public void setCardType(String cardType) { this.cardType = cardType; }
        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
        public Integer getCandidateCount() { return candidateCount; }
        public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    }

    public static class ShotReq {
        private String episodeId;
        private String shotId;
        private String shotDesc;
        private String dialogueText;
        private String shotSize;
        private String cameraMove;
        private Integer shotDurationSec;
        private String extraPrompt;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public String getShotDesc() { return shotDesc; }
        public void setShotDesc(String shotDesc) { this.shotDesc = shotDesc; }
        public String getDialogueText() { return dialogueText; }
        public void setDialogueText(String dialogueText) { this.dialogueText = dialogueText; }
        public String getShotSize() { return shotSize; }
        public void setShotSize(String shotSize) { this.shotSize = shotSize; }
        public String getCameraMove() { return cameraMove; }
        public void setCameraMove(String cameraMove) { this.cameraMove = cameraMove; }
        public Integer getShotDurationSec() { return shotDurationSec; }
        public void setShotDurationSec(Integer shotDurationSec) { this.shotDurationSec = shotDurationSec; }
        public String getExtraPrompt() { return extraPrompt; }
        public void setExtraPrompt(String extraPrompt) { this.extraPrompt = extraPrompt; }
    }

    public static class AiTaskReq {
        private String workId;
        private String episodeId;
        private String taskId;
        private String batchId;
        private String taskType;
        private String modelCode;
        private List<String> shotIds;
        private Integer autoNextFlag;
        private Integer fallbackModelFlag;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getModelCode() { return modelCode; }
        public void setModelCode(String modelCode) { this.modelCode = modelCode; }
        public List<String> getShotIds() { return shotIds; }
        public void setShotIds(List<String> shotIds) { this.shotIds = shotIds; }
        public Integer getAutoNextFlag() { return autoNextFlag; }
        public void setAutoNextFlag(Integer autoNextFlag) { this.autoNextFlag = autoNextFlag; }
        public Integer getFallbackModelFlag() { return fallbackModelFlag; }
        public void setFallbackModelFlag(Integer fallbackModelFlag) { this.fallbackModelFlag = fallbackModelFlag; }
    }

    public static class FilmReq {
        private String episodeId;
        private String assetId;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
    }

    public static class ReviewUserReq {
        private String episodeId;
        private String workId;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
    }

    public static class PrecheckReq {
        private String checkScope;
        private String episodeId;
        public String getCheckScope() { return checkScope; }
        public void setCheckScope(String checkScope) { this.checkScope = checkScope; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    }
}
