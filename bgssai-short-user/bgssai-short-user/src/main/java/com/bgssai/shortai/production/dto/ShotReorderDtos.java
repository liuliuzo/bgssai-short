package com.bgssai.shortai.production.dto;

import java.util.List;

public class ShotReorderDtos {

    public static class ShotReorderReq {
        private String episodeId;
        private Integer sceneNo;
        private List<String> shotIdList;
        public String getEpisodeId() { return episodeId; }
        public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
        public Integer getSceneNo() { return sceneNo; }
        public void setSceneNo(Integer sceneNo) { this.sceneNo = sceneNo; }
        public List<String> getShotIdList() { return shotIdList; }
        public void setShotIdList(List<String> shotIdList) { this.shotIdList = shotIdList; }
    }
}
