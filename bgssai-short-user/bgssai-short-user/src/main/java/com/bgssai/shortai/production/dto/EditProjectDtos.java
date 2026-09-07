package com.bgssai.shortai.production.dto;

public class EditProjectDtos {

    public static class EditProjectExportReq {
        private String episodeId;
        private String projectFormat;
        private String jianyingVersion;
        private String mediaMode;
        private String draftDir;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getProjectFormat() { return projectFormat; }
        public void setProjectFormat(String projectFormat) { this.projectFormat = projectFormat; }
        public String getJianyingVersion() { return jianyingVersion; }
        public void setJianyingVersion(String jianyingVersion) { this.jianyingVersion = jianyingVersion; }
        public String getMediaMode() { return mediaMode; }
        public void setMediaMode(String mediaMode) { this.mediaMode = mediaMode; }
        public String getDraftDir() { return draftDir; }
        public void setDraftDir(String draftDir) { this.draftDir = draftDir; }
    }
}
