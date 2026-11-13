package com.bgssai.shortai.production.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/** User-supplied look photos and short clips. JPG, PNG, WebP and MP4, at most 6MB. */
public final class LookImage {

    public static final int MAX_BYTES = 6 * 1024 * 1024;

    private LookImage() {
    }

    public record Decoded(byte[] bytes, String mimeType, int width, int height) {
    }

    public static Decoded decode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请上传图片或视频");
        }
        String text = raw.trim();
        int comma = text.indexOf(',');
        if (text.regionMatches(true, 0, "data:", 0, 5) && comma > 0) {
            text = text.substring(comma + 1);
        }
        text = text.replaceAll("\\s", "");
        if (text.length() > MAX_BYTES * 2) {
            throw new BizException(CommonErrorCode.REQUEST_TOO_LARGE, "文件不能超过 6MB");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException ex) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "文件内容无法识别");
        }
        return decodeBytes(bytes);
    }

    public static Decoded decodeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请上传图片或视频");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BizException(CommonErrorCode.REQUEST_TOO_LARGE, "文件不能超过 6MB");
        }
        String mime = sniff(bytes);
        int[] size = sizeOf(bytes);
        return new Decoded(bytes, mime, size[0], size[1]);
    }

    static String sniff(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        if (bytes.length >= 12
                && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') {
            return "video/mp4";
        }
        throw new BizException(CommonErrorCode.INVALID_PARAM, "只支持 JPG、PNG、WebP、MP4");
    }

    public static String extension(String mime) {
        if (mime != null && mime.contains("png")) {
            return "png";
        }
        if (mime != null && mime.contains("webp")) {
            return "webp";
        }
        if (mime != null && mime.startsWith("video/")) {
            return "mp4";
        }
        return "jpg";
    }

    private static int[] sizeOf(byte[] bytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return new int[] {0, 0};
            }
            return new int[] {image.getWidth(), image.getHeight()};
        } catch (Exception ex) {
            return new int[] {0, 0};
        }
    }
}
