package com.bgssai.shortai.production.dto;

public class FrameLinkDtos {

    public static class FrameLinkSaveReq {
        private String shotId;
        private String frameLinkMode;
        private String modelCode;
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public String getFrameLinkMode() { return frameLinkMode; }
        public void setFrameLinkMode(String frameLinkMode) { this.frameLinkMode = frameLinkMode; }
        public String getModelCode() { return modelCode; }
        public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    }

    public static class FrameLinkBatchReq {
        private String episodeId;
        private String frameLinkMode;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getFrameLinkMode() { return frameLinkMode; }
        public void setFrameLinkMode(String frameLinkMode) { this.frameLinkMode = frameLinkMode; }
    }

    public static class LastFrameGenerateReq {
        private String shotId;
        private String lastFramePrompt;
        private String modelCode;
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public String getLastFramePrompt() { return lastFramePrompt; }
        public void setLastFramePrompt(String lastFramePrompt) { this.lastFramePrompt = lastFramePrompt; }
        public String getModelCode() { return modelCode; }
        public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    }
}
