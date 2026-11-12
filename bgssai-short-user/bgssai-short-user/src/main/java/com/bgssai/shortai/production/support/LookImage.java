package com.bgssai.shortai.production.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/** User-supplied look photos. JPG, PNG and WebP only, at most 6MB. */
public final class LookImage {

    public static final int MAX_BYTES = 6 * 1024 * 1024;

    private LookImage() {
    }

    public record Decoded(byte[] bytes, String mimeType, int width, int height) {
    }

    public static Decoded decode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请上传图片");
        }
        String text = raw.trim();
        int comma = text.indexOf(',');
        if (text.regionMatches(true, 0, "data:", 0, 5) && comma > 0) {
            text = text.substring(comma + 1);
        }
        text = text.replaceAll("\\s", "");
        if (text.length() > MAX_BYTES * 2) {
            throw new BizException(CommonErrorCode.REQUEST_TOO_LARGE, "图片不能超过 6MB");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException ex) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "图片内容无法识别");
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new BizException(CommonErrorCode.REQUEST_TOO_LARGE, "图片不能超过 6MB");
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
        throw new BizException(CommonErrorCode.INVALID_PARAM, "只支持 JPG、PNG、WebP");
    }

    public static String extension(String mime) {
        if (mime != null && mime.contains("png")) {
            return "png";
        }
        if (mime != null && mime.contains("webp")) {
            return "webp";
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
