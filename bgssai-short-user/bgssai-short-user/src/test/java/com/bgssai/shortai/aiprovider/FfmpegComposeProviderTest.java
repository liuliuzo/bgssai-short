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

    @Test
    void mixesPartialDubbingIntoFinalMp4() throws Exception {
        Path clip1 = temp.resolve("c1.mp4");
        Path clip2 = temp.resolve("c2.mp4");
        Path voice = temp.resolve("vo.wav");
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=320x240:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", clip1.toString());
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=320x240:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", clip2.toString());
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "sine=frequency=880:duration=1",
                "-ar", "44100", "-ac", "2", voice.toString());

        FfmpegComposeProvider provider = new FfmpegComposeProvider();
        AiProviderRequest req = new AiProviderRequest();
        req.setAbilityType("COMPOSE");
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(FfmpegComposeProvider.CODE);
        model.setTimeoutSec(120);
        req.setModel(model);
        req.getParams().put(FfmpegComposeProvider.PARAM_CLIPS,
                List.of(Files.readAllBytes(clip1), Files.readAllBytes(clip2)));
        req.getParams().put(FfmpegComposeProvider.PARAM_DUBBING_LIST,
                List.of(Files.readAllBytes(voice), new byte[0]));
        req.getParams().put(FfmpegComposeProvider.PARAM_CLIP_DURATION_SEC_LIST, List.of(1, 1));
        req.getParams().put("duration_sec", 2);

        AiProviderResult result = provider.execute(req);
        assertEquals("video/mp4", result.getMimeType());
        Path out = temp.resolve("out.mp4");
        Files.write(out, result.getBinaryContent());
        Process probe = new ProcessBuilder("ffprobe", "-v", "error", "-select_streams", "a",
                "-show_entries", "stream=codec_type", "-of", "csv=p=0", out.toString())
                .redirectErrorStream(true).start();
        String audio = new String(probe.getInputStream().readAllBytes()).trim();
        assertTrue(probe.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0, probe.exitValue());
        assertTrue(audio.contains("audio"), "composed film should contain dubbed audio track");
    }

    @Test
    void replaceAudioCommandMapsVideoThenVo() {
        List<String> cmd = FfmpegComposeProvider.buildReplaceAudioCommand("v.mp4", "vo.wav", "out.mp4");
        assertEquals(List.of("ffmpeg", "-y", "-i", "v.mp4", "-i", "vo.wav",
                "-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy", "-c:a", "aac",
                "-shortest", "out.mp4"), cmd);
    }

    @Test
    void bgmAmixCommandKeepsExistingAudio() {
        List<String> cmd = FfmpegComposeProvider.buildBgmAmixCommand("v.mp4", "bgm", "out.mp4");
        assertTrue(cmd.contains("-filter_complex"));
        assertTrue(cmd.stream().anyMatch(s -> s.contains("amix")));
        assertTrue(cmd.contains("[aout]"));
    }

    private static void runLocal(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException("ffmpeg fixture failed: " + String.join(" ", cmd));
        }
    }
}
