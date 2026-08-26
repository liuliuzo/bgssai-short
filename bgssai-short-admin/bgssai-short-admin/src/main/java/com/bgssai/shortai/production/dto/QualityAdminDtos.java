package com.bgssai.shortai.production.dto;

import java.util.List;

public class QualityAdminDtos {

    public static class TemplateReq {
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

    public static class ThresholdSaveReq {
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
