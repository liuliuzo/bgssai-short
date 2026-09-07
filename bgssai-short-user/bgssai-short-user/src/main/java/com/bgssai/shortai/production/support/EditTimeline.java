package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 剪辑工程导出用的时间轴快照。三种工程格式（剪映草稿 / FCPXML / EDL）都消费同一份，
 * 与 FilmService.timeline 是同一套计算口径：镜头按 shot_no 升序，起点为前序镜头时长累加，
 * 配音与台词落在所属镜头区间内。同一件事只算一次，避免两处口径漂移。
 *
 * <p>时间单位统一用微秒（剪映草稿的原生单位），其余格式按需换算。
 */
public class EditTimeline {

    /** 一段镜头视频。 */
    public static class Clip {
        private int shotNo;
        private String shotDesc;
        private String fileName;
        private long startUs;
        private long durationUs;
        private int width;
        private int height;

        public int getShotNo() {
            return shotNo;
        }

        public void setShotNo(int shotNo) {
            this.shotNo = shotNo;
        }

        public String getShotDesc() {
            return shotDesc;
        }

        public void setShotDesc(String shotDesc) {
            this.shotDesc = shotDesc;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getStartUs() {
            return startUs;
        }

        public void setStartUs(long startUs) {
            this.startUs = startUs;
        }

        public long getDurationUs() {
            return durationUs;
        }

        public void setDurationUs(long durationUs) {
            this.durationUs = durationUs;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }

    /** 一条配音音轨片段。 */
    public static class Voice {
        private int shotNo;
        private String fileName;
        private long startUs;
        private long durationUs;

        public int getShotNo() {
            return shotNo;
        }

        public void setShotNo(int shotNo) {
            this.shotNo = shotNo;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getStartUs() {
            return startUs;
        }

        public void setStartUs(long startUs) {
            this.startUs = startUs;
        }

        public long getDurationUs() {
            return durationUs;
        }

        public void setDurationUs(long durationUs) {
            this.durationUs = durationUs;
        }
    }

    /** 一条字幕。 */
    public static class Line {
        private String lineText;
        private long startUs;
        private long durationUs;

        public String getLineText() {
            return lineText;
        }

        public void setLineText(String lineText) {
            this.lineText = lineText;
        }

        public long getStartUs() {
            return startUs;
        }

        public void setStartUs(long startUs) {
            this.startUs = startUs;
        }

        public long getDurationUs() {
            return durationUs;
        }

        public void setDurationUs(long durationUs) {
            this.durationUs = durationUs;
        }
    }

    /** 未生成视频的镜头，只登记不占时间轴。 */
    public static class Missing {
        private int shotNo;
        private String shotDesc;

        public int getShotNo() {
            return shotNo;
        }

        public void setShotNo(int shotNo) {
            this.shotNo = shotNo;
        }

        public String getShotDesc() {
            return shotDesc;
        }

        public void setShotDesc(String shotDesc) {
            this.shotDesc = shotDesc;
        }
    }

    private String projectTitle;
    private int episodeNo;
    private int width = 1080;
    private int height = 1920;
    private int fps = 30;
    private long totalUs;
    private String bgmFileName;
    private long bgmDurationUs;
    private final List<Clip> clipList = new ArrayList<>();
    private final List<Voice> voiceList = new ArrayList<>();
    private final List<Line> lineList = new ArrayList<>();
    private final List<Missing> missingList = new ArrayList<>();

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public int getEpisodeNo() {
        return episodeNo;
    }

    public void setEpisodeNo(int episodeNo) {
        this.episodeNo = episodeNo;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public long getTotalUs() {
        return totalUs;
    }

    public void setTotalUs(long totalUs) {
        this.totalUs = totalUs;
    }

    public String getBgmFileName() {
        return bgmFileName;
    }

    public void setBgmFileName(String bgmFileName) {
        this.bgmFileName = bgmFileName;
    }

    public long getBgmDurationUs() {
        return bgmDurationUs;
    }

    public void setBgmDurationUs(long bgmDurationUs) {
        this.bgmDurationUs = bgmDurationUs;
    }

    public List<Clip> getClipList() {
        return clipList;
    }

    public List<Voice> getVoiceList() {
        return voiceList;
    }

    public List<Line> getLineList() {
        return lineList;
    }

    public List<Missing> getMissingList() {
        return missingList;
    }
}
