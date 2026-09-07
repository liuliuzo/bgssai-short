package com.bgssai.shortai.production.support;

/**
 * 把 {@link EditTimeline} 序列化成 Final Cut Pro X XML 1.9，达芬奇 Resolve 与 Final Cut 可直接导入。
 *
 * <p>时间一律写成「帧数/帧率s」的有理数形式（如 120/30s），这是 FCPXML 唯一不会因浮点舍入而
 * 错帧的写法；直接写 4.0s 在 30 帧下遇到非整秒时长就会漂。
 *
 * <p>结构上主轨（spine）只放视频，配音挂在首个 asset-clip 下的 lane -1，字幕挂在 lane 1，
 * offset 用序列绝对时间（首个 clip 的 start 为 0s，因此相对与绝对同值）。
 */
public final class FcpxmlWriter {

    private static final long US_PER_SEC = 1000000L;

    private FcpxmlWriter() {
    }

    public static String write(EditTimeline timeline) {
        int fps = timeline.getFps() <= 0 ? 30 : timeline.getFps();
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<!DOCTYPE fcpxml>\n");
        xml.append("<fcpxml version=\"1.9\">\n");
        xml.append("  <resources>\n");
        xml.append("    <format id=\"r0\" name=\"BgssaiVideoFormat\" frameDuration=\"1/").append(fps)
                .append("s\" width=\"").append(timeline.getWidth())
                .append("\" height=\"").append(timeline.getHeight()).append("\"/>\n");

        int resourceIndex = 1;
        for (EditTimeline.Clip clip : timeline.getClipList()) {
            String ref = "r" + resourceIndex;
            xml.append("    <asset id=\"").append(ref).append("\" name=\"")
                    .append(escape(baseName(clip.getFileName()))).append("\" start=\"0s\" duration=\"")
                    .append(frameTime(clip.getDurationUs(), fps))
                    .append("\" hasVideo=\"1\" hasAudio=\"1\" format=\"r0\" audioSources=\"1\""
                            + " audioChannels=\"2\" audioRate=\"48000\">\n");
            xml.append("      <media-rep kind=\"original-media\" src=\"./assets/")
                    .append(escape(clip.getFileName())).append("\"/>\n");
            xml.append("    </asset>\n");
            resourceIndex++;
        }
        int firstVoiceResource = resourceIndex;
        for (EditTimeline.Voice voice : timeline.getVoiceList()) {
            xml.append("    <asset id=\"r").append(resourceIndex).append("\" name=\"")
                    .append(escape(baseName(voice.getFileName()))).append("\" start=\"0s\" duration=\"")
                    .append(frameTime(voice.getDurationUs(), fps))
                    .append("\" hasVideo=\"0\" hasAudio=\"1\" audioSources=\"1\""
                            + " audioChannels=\"2\" audioRate=\"48000\">\n");
            xml.append("      <media-rep kind=\"original-media\" src=\"./assets/")
                    .append(escape(voice.getFileName())).append("\"/>\n");
            xml.append("    </asset>\n");
            resourceIndex++;
        }
        int bgmResource = -1;
        if (timeline.getBgmFileName() != null && !timeline.getBgmFileName().isBlank()) {
            bgmResource = resourceIndex;
            long bgmUs = timeline.getBgmDurationUs() > 0 ? timeline.getBgmDurationUs() : timeline.getTotalUs();
            xml.append("    <asset id=\"r").append(resourceIndex).append("\" name=\"")
                    .append(escape(baseName(timeline.getBgmFileName()))).append("\" start=\"0s\" duration=\"")
                    .append(frameTime(bgmUs, fps))
                    .append("\" hasVideo=\"0\" hasAudio=\"1\" audioSources=\"1\""
                            + " audioChannels=\"2\" audioRate=\"48000\">\n");
            xml.append("      <media-rep kind=\"original-media\" src=\"./assets/")
                    .append(escape(timeline.getBgmFileName())).append("\"/>\n");
            xml.append("    </asset>\n");
            resourceIndex++;
        }
        String titleEffect = "r" + resourceIndex;
        xml.append("    <effect id=\"").append(titleEffect)
                .append("\" name=\"Basic Title\" uid=\".../Titles.localized/Bumper:Opener.localized/"
                        + "Basic Title.localized/Basic Title.moti\"/>\n");
        xml.append("  </resources>\n");

        String projectName = escape(timeline.getProjectTitle() + "_E" + timeline.getEpisodeNo());
        xml.append("  <library>\n");
        xml.append("    <event name=\"").append(projectName).append("\">\n");
        xml.append("      <project name=\"").append(projectName).append("\">\n");
        xml.append("        <sequence format=\"r0\" duration=\"")
                .append(frameTime(timeline.getTotalUs(), fps))
                .append("\" tcStart=\"0s\" tcFormat=\"NDF\" audioLayout=\"stereo\" audioRate=\"48k\">\n");
        xml.append("          <spine>\n");

        boolean attachmentsWritten = false;
        int clipIndex = 0;
        for (EditTimeline.Clip clip : timeline.getClipList()) {
            String ref = "r" + (clipIndex + 1);
            boolean first = clipIndex == 0;
            xml.append("            <asset-clip name=\"")
                    .append(escape(baseName(clip.getFileName()))).append("\" ref=\"").append(ref)
                    .append("\" offset=\"").append(frameTime(clip.getStartUs(), fps))
                    .append("\" duration=\"").append(frameTime(clip.getDurationUs(), fps))
                    .append("\" start=\"0s\" tcFormat=\"NDF\"");
            if (!first || (timeline.getVoiceList().isEmpty() && timeline.getLineList().isEmpty()
                    && bgmResource < 0)) {
                xml.append("/>\n");
                clipIndex++;
                continue;
            }
            xml.append(">\n");
            int voiceResource = firstVoiceResource;
            for (EditTimeline.Voice voice : timeline.getVoiceList()) {
                xml.append("              <asset-clip name=\"")
                        .append(escape(baseName(voice.getFileName()))).append("\" ref=\"r")
                        .append(voiceResource).append("\" lane=\"-1\" offset=\"")
                        .append(frameTime(voice.getStartUs(), fps)).append("\" duration=\"")
                        .append(frameTime(voice.getDurationUs(), fps))
                        .append("\" start=\"0s\" audioRole=\"dialogue\"/>\n");
                voiceResource++;
            }
            if (bgmResource > 0) {
                long bgmUs = timeline.getBgmDurationUs() > 0
                        ? Math.min(timeline.getBgmDurationUs(), timeline.getTotalUs()) : timeline.getTotalUs();
                xml.append("              <asset-clip name=\"")
                        .append(escape(baseName(timeline.getBgmFileName()))).append("\" ref=\"r")
                        .append(bgmResource).append("\" lane=\"-2\" offset=\"0s\" duration=\"")
                        .append(frameTime(bgmUs, fps))
                        .append("\" start=\"0s\" audioRole=\"music\"/>\n");
            }
            int styleIndex = 1;
            for (EditTimeline.Line line : timeline.getLineList()) {
                String styleId = "ts" + styleIndex;
                xml.append("              <title name=\"").append(escape(shorten(line.getLineText())))
                        .append("\" lane=\"1\" offset=\"").append(frameTime(line.getStartUs(), fps))
                        .append("\" ref=\"").append(titleEffect).append("\" duration=\"")
                        .append(frameTime(line.getDurationUs(), fps)).append("\">\n");
                xml.append("                <text><text-style ref=\"").append(styleId).append("\">")
                        .append(escape(line.getLineText())).append("</text-style></text>\n");
                xml.append("                <text-style-def id=\"").append(styleId)
                        .append("\"><text-style font=\"PingFang SC\" fontSize=\"")
                        .append(timeline.getHeight() > timeline.getWidth() ? 64 : 48)
                        .append("\" fontColor=\"1 1 1 1\" bold=\"1\" alignment=\"center\"/>"
                                + "</text-style-def>\n");
                xml.append("              </title>\n");
                styleIndex++;
            }
            xml.append("            </asset-clip>\n");
            attachmentsWritten = true;
            clipIndex++;
        }
        if (!attachmentsWritten && timeline.getClipList().isEmpty()) {
            xml.append("            <gap name=\"Gap\" offset=\"0s\" duration=\"")
                    .append(frameTime(timeline.getTotalUs(), fps)).append("\" start=\"0s\"/>\n");
        }
        xml.append("          </spine>\n");
        xml.append("        </sequence>\n");
        xml.append("      </project>\n");
        xml.append("    </event>\n");
        xml.append("  </library>\n");
        xml.append("</fcpxml>\n");
        return xml.toString();
    }

    /** 微秒按帧率对齐成 FCPXML 的有理数时间，如 120/30s。 */
    private static String frameTime(long microseconds, int fps) {
        long frames = Math.round((double) microseconds * fps / US_PER_SEC);
        return frames + "/" + fps + "s";
    }

    private static String baseName(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 12 ? text : text.substring(0, 12);
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
