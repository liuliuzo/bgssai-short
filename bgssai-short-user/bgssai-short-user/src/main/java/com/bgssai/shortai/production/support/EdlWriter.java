package com.bgssai.shortai.production.support;

/**
 * 把 {@link EditTimeline} 序列化成 CMX3600 EDL，以及随包附带的 SRT 字幕与素材清单。
 *
 * <p>EDL 是最老实的交换格式，几乎所有剪辑软件都认，但它只有剪辑点、没有文本轨，
 * 所以字幕单独出一份 SRT 放进同一个包里，而不是硬塞进 EDL 注释里让人自己扒。
 */
public final class EdlWriter {

    private static final long US_PER_SEC = 1000000L;

    private EdlWriter() {
    }

    public static String writeEdl(EditTimeline timeline) {
        int fps = timeline.getFps() <= 0 ? 30 : timeline.getFps();
        StringBuilder edl = new StringBuilder(2048);
        edl.append("TITLE: ").append(safeTitle(timeline)).append("\r\n");
        edl.append("FCM: NON-DROP FRAME\r\n\r\n");
        int event = 1;
        for (EditTimeline.Clip clip : timeline.getClipList()) {
            String sourceIn = timecode(0L, fps);
            String sourceOut = timecode(clip.getDurationUs(), fps);
            String recordIn = timecode(clip.getStartUs(), fps);
            String recordOut = timecode(clip.getStartUs() + clip.getDurationUs(), fps);
            edl.append(String.format("%03d", event)).append("  AX       V     C        ")
                    .append(sourceIn).append(" ").append(sourceOut).append(" ")
                    .append(recordIn).append(" ").append(recordOut).append("\r\n");
            edl.append("* FROM CLIP NAME: ").append(clip.getFileName()).append("\r\n");
            if (clip.getShotDesc() != null && !clip.getShotDesc().isBlank()) {
                edl.append("* COMMENT: SHOT ").append(clip.getShotNo()).append(" ")
                        .append(oneLine(clip.getShotDesc())).append("\r\n");
            }
            edl.append("\r\n");
            event++;
        }
        for (EditTimeline.Voice voice : timeline.getVoiceList()) {
            String sourceIn = timecode(0L, fps);
            String sourceOut = timecode(voice.getDurationUs(), fps);
            String recordIn = timecode(voice.getStartUs(), fps);
            String recordOut = timecode(voice.getStartUs() + voice.getDurationUs(), fps);
            edl.append(String.format("%03d", event)).append("  AX       A     C        ")
                    .append(sourceIn).append(" ").append(sourceOut).append(" ")
                    .append(recordIn).append(" ").append(recordOut).append("\r\n");
            edl.append("* FROM CLIP NAME: ").append(voice.getFileName()).append("\r\n\r\n");
            event++;
        }
        if (timeline.getBgmFileName() != null && !timeline.getBgmFileName().isBlank()) {
            long bgmUs = timeline.getBgmDurationUs() > 0
                    ? Math.min(timeline.getBgmDurationUs(), timeline.getTotalUs()) : timeline.getTotalUs();
            edl.append(String.format("%03d", event)).append("  AX       A2    C        ")
                    .append(timecode(0L, fps)).append(" ").append(timecode(bgmUs, fps)).append(" ")
                    .append(timecode(0L, fps)).append(" ").append(timecode(bgmUs, fps)).append("\r\n");
            edl.append("* FROM CLIP NAME: ").append(timeline.getBgmFileName()).append("\r\n\r\n");
        }
        return edl.toString();
    }

    public static String writeSrt(EditTimeline timeline) {
        StringBuilder srt = new StringBuilder(1024);
        int index = 1;
        for (EditTimeline.Line line : timeline.getLineList()) {
            srt.append(index).append("\r\n");
            srt.append(srtTime(line.getStartUs())).append(" --> ")
                    .append(srtTime(line.getStartUs() + line.getDurationUs())).append("\r\n");
            srt.append(line.getLineText() == null ? "" : line.getLineText()).append("\r\n\r\n");
            index++;
        }
        return srt.toString();
    }

    /** HH:MM:SS:FF，帧号按帧率取整。 */
    private static String timecode(long microseconds, int fps) {
        long totalFrames = Math.round((double) microseconds * fps / US_PER_SEC);
        long frames = totalFrames % fps;
        long totalSeconds = totalFrames / fps;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        return String.format("%02d:%02d:%02d:%02d", hours, minutes, seconds, frames);
    }

    private static String srtTime(long microseconds) {
        long totalMillis = microseconds / 1000L;
        long millis = totalMillis % 1000;
        long totalSeconds = totalMillis / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }

    private static String safeTitle(EditTimeline timeline) {
        String title = timeline.getProjectTitle() == null ? "BGSSAI" : timeline.getProjectTitle();
        return oneLine(title) + "_E" + timeline.getEpisodeNo();
    }

    private static String oneLine(String text) {
        return text.replace("\r", " ").replace("\n", " ").trim();
    }
}
