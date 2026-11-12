package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

class ImageBytesValidatorTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Test
    void pngAndJpegPassMagicAndDecode() throws Exception {
        var png = ImageBytesValidator.inspect(PNG);
        assertEquals("image/png", png.mime());
        assertEquals(1, png.width());
        assertEquals(1, png.height());
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "jpeg", jpeg));
        var info = ImageBytesValidator.inspect(jpeg.toByteArray());
        assertEquals("image/jpeg", info.mime());
        assertEquals(8, info.width());
    }

    @Test
    void rejectsTypeSizeCorruptionAndPixelCap() {
        BizException svg = assertThrows(BizException.class,
                () -> ImageBytesValidator.inspect("<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes()));
        assertEquals(CommonErrorCode.INVALID_PARAM.code(), svg.getCode());
        byte[] huge = new byte[ImageBytesValidator.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, huge, 0, PNG.length);
        BizException oversize = assertThrows(BizException.class, () -> ImageBytesValidator.inspect(huge));
        assertEquals(CommonErrorCode.REQUEST_TOO_LARGE.code(), oversize.getCode());
        byte[] broken = new byte[32];
        System.arraycopy(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 0, broken, 0, 8);
        BizException corrupt = assertThrows(BizException.class, () -> ImageBytesValidator.inspect(broken));
        assertEquals(CommonErrorCode.INVALID_PARAM.code(), corrupt.getCode());
        BizException pixels = assertThrows(BizException.class, () -> ImageBytesValidator.inspect(pngClaim(5000, 5000)));
        assertEquals(CommonErrorCode.REQUEST_TOO_LARGE.code(), pixels.getCode());
        assertTrue(pixels.getMessage().contains("1600"));
    }

    private static byte[] pngClaim(int width, int height) {
        byte[] data = ByteBuffer.allocate(13)
                .putInt(width)
                .putInt(height)
                .put((byte) 8)
                .put((byte) 2)
                .put((byte) 0)
                .put((byte) 0)
                .put((byte) 0)
                .array();
        byte[] type = new byte[] {'I', 'H', 'D', 'R'};
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(data);
        ByteBuffer out = ByteBuffer.allocate(8 + 12 + data.length + 12);
        out.put(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        out.putInt(data.length);
        out.put(type);
        out.put(data);
        out.putInt((int) crc.getValue());
        CRC32 end = new CRC32();
        end.update(new byte[] {'I', 'E', 'N', 'D'});
        out.putInt(0);
        out.put(new byte[] {'I', 'E', 'N', 'D'});
        out.putInt((int) end.getValue());
        return out.array();
    }
}
