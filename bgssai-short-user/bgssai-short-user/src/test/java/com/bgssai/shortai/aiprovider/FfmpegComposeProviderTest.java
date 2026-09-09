package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.bgssai.shortai.model.AiModelConfig;

class FfmpegComposeProviderTest {

    @TempDir
    Path temp;

    @Test
    void concatenatesClipsAndBurnsSubtitles() throws Exception {
        // Generate two tiny silent H264 clips with ffmpeg, then compose with SRT.
        Path clip1 = temp.resolve("c1.mp4");
        Path clip2 = temp.resolve("c2.mp4");
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=320x240:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", clip1.toString());
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=320x240:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", clip2.toString());

        FfmpegComposeProvider provider = new FfmpegComposeProvider();
        AiProviderRequest req = new AiProviderRequest();
        req.setAbilityType("COMPOSE");
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(FfmpegComposeProvider.CODE);
        model.setTimeoutSec(120);
        req.setModel(model);
        req.getParams().put(FfmpegComposeProvider.PARAM_CLIPS,
                List.of(Files.readAllBytes(clip1), Files.readAllBytes(clip2)));
        req.getParams().put(FfmpegComposeProvider.PARAM_SUBTITLE_SRT,
                "1\n00:00:00,000 --> 00:00:01,500\nhello\n\n");
        req.getParams().put("duration_sec", 2);

        AiProviderResult result = provider.execute(req);
        assertEquals("video/mp4", result.getMimeType());
        assertTrue(result.getBinaryContent() != null && result.getBinaryContent().length > 1000);
    }

    private static void runLocal(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException("ffmpeg fixture failed: " + String.join(" ", cmd));
        }
    }
}
