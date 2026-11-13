package com.bgssai.shortai.production.support;

/**
 * What the official shot button tells the user. Image-to-video cannot do 9:16 or a 6 second clip.
 */
public final class OfficialVideoCopy {

    public static final String ROUTE_I2V = "IMAGE_TO_VIDEO";
    public static final String ROUTE_T2V = "TEXT_TO_VIDEO";
    public static final String I2V_SIZE = "图生视频成片是 384×256、约 3 秒（25 帧、每秒 8 帧）。不是竖屏 9:16，也不是约 6 秒的文生视频。";
    public static final String T2V_SIZE = "文生视频约 720×480、约 6 秒。参考图不会被采用。";
    public static final String T2V_NO_PHOTO = "没有可用于图生视频的真实照片（占位图和 SVG 不算），所以改走文生视频。文生视频约 720×480、约 6 秒，参考图不会被采用。";
    public static final String T2V_UNAVAILABLE = "图生视频暂不可用，这一镜改走文生视频。文生视频约 720×480、约 6 秒，参考图不会被采用。";
    public static final String IN_PROGRESS = "这一镜视频已在生成中";
    public static final String CONFIRM = "请先确认这一次生成并计费";

    private OfficialVideoCopy() {
    }

    public static boolean pngOrJpeg(String storageKey, String mimeType) {
        if (OfficialVideoLane.isPlaceholderImage(storageKey, mimeType, null)) {
            return false;
        }
        String mime = mimeType == null ? "" : mimeType.toLowerCase(java.util.Locale.ROOT);
        String key = storageKey == null ? "" : storageKey.toLowerCase(java.util.Locale.ROOT);
        if (mime.contains("webp") || key.endsWith(".webp") || mime.contains("svg") || key.contains("<svg")) {
            return false;
        }
        return mime.contains("png") || mime.contains("jpeg") || mime.contains("jpg")
                || key.endsWith(".png") || key.endsWith(".jpg") || key.endsWith(".jpeg");
    }
}
