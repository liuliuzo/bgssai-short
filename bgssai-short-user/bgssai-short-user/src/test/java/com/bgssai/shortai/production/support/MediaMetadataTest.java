package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.common.exception.BizException;

class MediaMetadataTest {
    @Test
    void pngAndJpegDimensionsReplaceInventedProviderDimensions() throws Exception {
        for (String format : List.of("png", "jpeg")) {
            var data = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(37, 61, BufferedImage.TYPE_INT_RGB), format, data);
            var result = AiProviderResult.binary(data.toByteArray(), "image/" + format);
            result.setWidth(1080); result.setHeight(1920); result.setDurationSec(5);
            MediaMetadata.verify(result, "IMAGE");
            assertEquals(37, result.getWidth()); assertEquals(61, result.getHeight());
            assertNull(result.getDurationSec());
        }
    }

    @Test
    void realMp3AndWavProvideDurationWithoutProviderJson() throws Exception {
        requireFfprobe();
        for (String file : List.of("probe-tone.mp3", "probe-tone.wav")) {
            var result = fixture(file, "audio/mpeg");
            result.setWidth(1080); result.setHeight(1920);
            MediaMetadata.verify(result, "VOICE");
            assertEquals(2, result.getDurationSec());
            assertNull(result.getWidth()); assertNull(result.getHeight());
        }
    }

    @Test
    void realMp4OverridesRequestedFiveSecondsAndPortraitSize() throws Exception {
        requireFfprobe();
        var result = fixture("probe-video.mp4", "video/mp4");
        result.setDurationSec(5); result.setWidth(1080); result.setHeight(1920);
        MediaMetadata.verify(result, "VIDEO");
        assertEquals(2, result.getDurationSec());
        assertEquals(64, result.getWidth()); assertEquals(96, result.getHeight());
        assertThrows(BizException.class, () -> MediaMetadata.verify(result, "VOICE"));
        assertThrows(BizException.class, () -> MediaMetadata.verify(result, "IMAGE"));
    }

    @Test
    void malformedBytesAndMissingProbeFailWithoutInventedMetadata() throws Exception {
        requireFfprobe();
        var bad = AiProviderResult.binary(new byte[] {1, 2, 3}, "audio/mpeg");
        assertThrows(BizException.class, () -> MediaMetadata.verify(bad, "VOICE"));
        assertNull(bad.getDurationSec());
        assertThrows(BizException.class, () -> MediaMetadata.verify(bad, "VOICE", List.of("missing-short-ffprobe-command"), 100));
    }

    @Test
    void processTimeoutAndExcessOutputAreBounded() throws Exception {
        var bad = AiProviderResult.binary(new byte[] {1}, "audio/mpeg");
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Path marker = java.nio.file.Files.createTempFile("short-probe-test-", ".txt");
        try {
            MediaMetadata.verify(bad, "VOICE", List.of(javaExecutable, "-cp", classes, ProbeProcess.class.getName(), "control", marker.toString()), 5_000);
            assertEquals(1, bad.getDurationSec());
            for (String mode : List.of("hang", "overflow")) {
                java.nio.file.Files.writeString(marker, "");
                long start = System.nanoTime();
                assertThrows(BizException.class, () -> MediaMetadata.verify(bad, "VOICE",
                        List.of(javaExecutable, "-cp", classes, ProbeProcess.class.getName(), mode, marker.toString()), "hang".equals(mode) ? 1_000 : 5_000));
                assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 5);
                List<String> recorded = java.nio.file.Files.readAllLines(marker);
                assertEquals(2, recorded.size(), "The test child must actually have started");
                assertFalse(ProcessHandle.of(Long.parseLong(recorded.get(0))).map(ProcessHandle::isAlive).orElse(false));
                assertFalse(java.nio.file.Files.exists(Path.of(recorded.get(1)).getParent()), "Probe temporary directory must be cleaned");
            }
        } finally { java.nio.file.Files.deleteIfExists(marker); }
    }

    public static class ProbeProcess {
        public static void main(String[] args) throws Exception {
            java.nio.file.Files.writeString(Path.of(args[1]), ProcessHandle.current().pid() + "\n" + args[args.length - 1]);
            if ("hang".equals(args[0])) { Thread.sleep(30_000); return; }
            if ("control".equals(args[0])) {
                System.out.print("{\"streams\":[{\"codec_type\":\"audio\"}],\"format\":{\"duration\":\"0.5\"}}");
                return;
            }
            System.out.write(new byte[70_000]); System.out.flush();
        }
    }

    private static AiProviderResult fixture(String name, String mime) throws Exception {
        try (var input = MediaMetadataTest.class.getResourceAsStream("/media/" + name)) {
            assertNotNull(input);
            return AiProviderResult.binary(input.readAllBytes(), mime);
        }
    }

    private static void requireFfprobe() {
        try {
            Process p = new ProcessBuilder("ffprobe", "-version").redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            Assumptions.assumeTrue(p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0,
                    "Real-media integration requires the deployment ffprobe dependency");
        } catch (Exception ex) { Assumptions.assumeTrue(false, "ffprobe is not installed"); }
    }
}
