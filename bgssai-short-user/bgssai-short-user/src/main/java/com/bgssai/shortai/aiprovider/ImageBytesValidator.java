package com.bgssai.shortai.aiprovider;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * Checks a local image before it is stored or sent as an image-to-video condition.
 * MIME text is ignored. PNG and JPEG are recognized from magic bytes, then decoded
 * so a truncated file is rejected. Pixel count is capped at 16,000,000 because the
 * current service build does not itself enforce that limit.
 */
public final class ImageBytesValidator {

    public static final int MAX_BYTES = 8 * 1024 * 1024;
    public static final long MAX_PIXELS = 16_000_000L;

    private ImageBytesValidator() {
    }

    public record ImageInfo(String mime, String extension, int width, int height) {
    }

    public static ImageInfo inspect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw invalid("图片无法用于图生视频。请换一张 PNG 或 JPEG。");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BizException(CommonErrorCode.REQUEST_TOO_LARGE, "图片超过 8MB，无法用于图生视频。");
        }
        String kind = magic(bytes);
        if (kind == null) {
            throw invalid("图片无法用于图生视频。请换一张 PNG 或 JPEG。");
        }
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalid("图片无法用于图生视频。请换一张 PNG 或 JPEG。");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * (long) height > MAX_PIXELS) {
                    throw new BizException(CommonErrorCode.REQUEST_TOO_LARGE,
                            "图片像素超过 1600 万，无法用于图生视频。");
                }
                BufferedImage image = reader.read(0);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw invalid("图片无法用于图生视频。请换一张 PNG 或 JPEG。");
                }
                if ("png".equals(kind)) {
                    return new ImageInfo("image/png", "png", width, height);
                }
                return new ImageInfo("image/jpeg", "jpg", width, height);
            } finally {
                reader.dispose();
            }
        } catch (BizException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalid("图片已损坏，无法用于图生视频。");
        }
    }

    private static String magic(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return "png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        return null;
    }

    private static BizException invalid(String message) {
        return new BizException(CommonErrorCode.INVALID_PARAM, message);
    }

    public static String safeExtension(String mime) {
        if (mime == null) {
            return "img";
        }
        String value = mime.toLowerCase(Locale.ROOT);
        if (value.contains("png")) {
            return "png";
        }
        if (value.contains("jpeg") || value.contains("jpg")) {
            return "jpg";
        }
        return "img";
    }
}
