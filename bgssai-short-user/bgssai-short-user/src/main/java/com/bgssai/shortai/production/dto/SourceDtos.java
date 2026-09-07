package com.bgssai.shortai.production.dto;

import java.util.List;

public class SourceDtos {

    public static class SourceImportReq {
        private String workId;
        private String sourceType;
        private String sourceTitle;
        private String sourceContent;
        private Integer rightsConfirmFlag;
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getSourceTitle() { return sourceTitle; }
        public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
        public String getSourceContent() { return sourceContent; }
        public void setSourceContent(String sourceContent) { this.sourceContent = sourceContent; }
        public Integer getRightsConfirmFlag() { return rightsConfirmFlag; }
        public void setRightsConfirmFlag(Integer rightsConfirmFlag) { this.rightsConfirmFlag = rightsConfirmFlag; }
    }

    public static class SourceChaptersSaveReq {
        private String sourceId;
        private List<ChapterItem> chapterList;
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public List<ChapterItem> getChapterList() { return chapterList; }
        public void setChapterList(List<ChapterItem> chapterList) { this.chapterList = chapterList; }

        public static class ChapterItem {
            private Integer chapterNo;
            private String chapterTitle;
            private String chapterText;
            public Integer getChapterNo() { return chapterNo; }
            public void setChapterNo(Integer chapterNo) { this.chapterNo = chapterNo; }
            public String getChapterTitle() { return chapterTitle; }
            public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
            public String getChapterText() { return chapterText; }
            public void setChapterText(String chapterText) { this.chapterText = chapterText; }
        }
    }

    public static class SourceAdaptReq {
        private String sourceId;
        private Integer chapterNoFrom;
        private Integer chapterNoTo;
        private Integer targetEpisodeCount;
        private String modelCode;
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public Integer getChapterNoFrom() { return chapterNoFrom; }
        public void setChapterNoFrom(Integer chapterNoFrom) { this.chapterNoFrom = chapterNoFrom; }
        public Integer getChapterNoTo() { return chapterNoTo; }
        public void setChapterNoTo(Integer chapterNoTo) { this.chapterNoTo = chapterNoTo; }
        public Integer getTargetEpisodeCount() { return targetEpisodeCount; }
        public void setTargetEpisodeCount(Integer targetEpisodeCount) { this.targetEpisodeCount = targetEpisodeCount; }
        public String getModelCode() { return modelCode; }
        public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    }

    public static class SourceDeleteReq {
        private String sourceId;
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    }
}
