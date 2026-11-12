package com.bgssai.shortai.production.dto;

public class ImageToVideoDtos {

    public static class SubmitReq {
        private String assetId;
        private String shotId;
        private String workId;
        private String episodeId;
        private String prompt;
        private Integer width;
        private Integer height;
        private Integer numFrames;
        private Integer fps;
        private Integer steps;
        private Integer seed;
        private String clientRequestId;

        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
        public String getShotId() { return shotId; }
        public void setShotId(String shotId) { this.shotId = shotId; }
        public String getWorkId() { return workId; }
        public void setWorkId(String workId) { this.workId = workId; }
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        public Integer getNumFrames() { return numFrames; }
        public void setNumFrames(Integer numFrames) { this.numFrames = numFrames; }
        public Integer getFps() { return fps; }
        public void setFps(Integer fps) { this.fps = fps; }
        public Integer getSteps() { return steps; }
        public void setSteps(Integer steps) { this.steps = steps; }
        public Integer getSeed() { return seed; }
        public void setSeed(Integer seed) { this.seed = seed; }
        public String getClientRequestId() { return clientRequestId; }
        public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
    }
}
