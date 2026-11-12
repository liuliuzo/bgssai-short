package com.bgssai.shortai.aiprovider;

import com.bgssai.shortai.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Real ffmpeg fixtures with moving testsrc overlay; these are tests, never product model output. */
class FfmpegComposeTimelineTest {
    @TempDir Path dir;
    private final ObjectMapper json = new ObjectMapper();
    private int sequence;

    @BeforeEach
    void tools() {
        for (String tool : List.of("ffmpeg", "ffprobe")) {
            try { run(List.of(tool, "-version")); }
            catch (Exception unavailable) { Assumptions.abort(tool + " is required for real media regression"); }
        }
    }

    @Test
    void mixedRateOffsetClipsKeepEveryBoundaryAndSilentLastShot() throws Exception {
        Path red = fixture("red", "2.125", "8", true, false, "2");
        Path blue = fixture("blue", "1.625", "12", false, true, "0");
        Path green = fixture("lime", "1.25", "24", false, false, "0");
        assertTrue(probe(red).path("streams").get(0).path("start_time").asDouble() >= 1.9);
        AiProviderRequest request = request(List.of(red, blue, green), List.of(1, 1, 1), "9:16");
        Path film = compose(request);
        assertMedia(film, 1080, 1920, 3, 90);
        assertColor(film, .15, 100, 960, 0);
        assertColor(film, .95, 100, 960, 0);
        assertColor(film, 1.05, 100, 960, 2);
        assertColor(film, 1.95, 100, 960, 2);
        assertColor(film, 2.05, 100, 960, 1);
        assertColor(film, 2.95, 100, 960, 1); // Last shot must survive the shorter original audio.
        assertBlack(film, .5, 100, 100); // Landscape source is letterboxed, not stretched.
        assertTrue(rms(film, .2) > .01, "original first-shot audio must remain audible");
        assertTrue(rms(film, 1.2) < .001, "middle missing audio occupies its own timeline interval");
        assertTrue(rms(film, 2.8) < .001, "silent final shot still has its full interval");
    }

    @ParameterizedTest
    @CsvSource({"16:9,1920,1080,20,540", "1:1,1080,1080,540,20", "3:4,1080,1440,540,20"})
    void savedAspectIsAppliedAndLongDubbingIsTrimmed(String ratio, int width, int height, int padX, int padY) throws Exception {
        Path clip = fixture("red", "1.5", "10", false, false, "0");
        Path voice = tone("2.2");
        AiProviderRequest request = request(List.of(clip), List.of(1), ratio);
        request.getParams().put(FfmpegComposeProvider.PARAM_DUBBING_LIST, List.of(Files.readAllBytes(voice)));
        Path film = compose(request);
        assertMedia(film, width, height, 1, 30);
        assertBlack(film, .5, padX, padY);
        assertColor(film, .5, width / 3, height / 2, 0);
        assertTrue(rms(film, .75) > .01);
    }

    @Test
    void shortDubbingPadsInsideShotAndBgmDoesNotRemoveLastVideo() throws Exception {
        Path first = fixture("red", "1.25", "8", false, false, "0");
        Path last = fixture("lime", "1.5", "12", false, false, "0");
        Path voice = tone("0.25");
        AiProviderRequest request = request(List.of(first, last), List.of(1, 1), "1:1");
        request.getParams().put(FfmpegComposeProvider.PARAM_DUBBING_LIST,
                List.of(Files.readAllBytes(voice), new byte[0]));
        Path plain = compose(request);
        assertMedia(plain, 1080, 1080, 2, 60);
        assertTrue(rms(plain, .05) > .01);
        assertTrue(rms(plain, .7) < .001);
        assertColor(plain, 1.95, 100, 540, 1);
        request.getParams().put(FfmpegComposeProvider.PARAM_BGM, Files.readAllBytes(voice));
        Path withBgm = compose(request);
        assertMedia(withBgm, 1080, 1080, 2, 60);
        assertColor(withBgm, 1.95, 100, 540, 1);
        assertTrue(rms(withBgm, 1.7) > .001, "BGM should loop through the last shot");
    }

    @Test
    void exactVideoDurationRejectsShortClipEvenWithLongerAudio() throws Exception {
        Path video = fixture("red", "4.1", "10", false, false, "0");
        Path audio = tone("5.5");
        Path combined = dir.resolve("video-short-audio-long.mp4");
        run(List.of("ffmpeg", "-y", "-i", video.toString(), "-i", audio.toString(), "-map", "0:v:0",
                "-map", "1:a:0", "-c:v", "copy", "-c:a", "aac", combined.toString()));
        assertTrue(probe(combined).path("format").path("duration").asDouble() > 5);
        BizException failure = assertThrows(BizException.class,
                () -> new FfmpegComposeProvider().execute(request(List.of(combined), List.of(5), "9:16")));
        assertTrue(failure.getMessage().contains("第1镜视频短于计划时长5秒"));
    }

    @Test
    void mismatchedPlanOrUnsupportedAspectCannotProduceAReportedSuccess() throws Exception {
        Path clip = fixture("red", "1.25", "8", false, false, "0");
        assertThrows(BizException.class, () -> new FfmpegComposeProvider().execute(request(List.of(clip), List.of(), "9:16")));
        assertThrows(BizException.class, () -> new FfmpegComposeProvider().execute(request(List.of(clip), List.of(1), "2:1")));
    }

    private AiProviderRequest request(List<Path> clips, List<Integer> durations, String aspect) throws Exception {
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType("COMPOSE");
        List<byte[]> bytes = new ArrayList<>();
        for (Path clip : clips) bytes.add(Files.readAllBytes(clip));
        request.getParams().put(FfmpegComposeProvider.PARAM_CLIPS, bytes);
        request.getParams().put(FfmpegComposeProvider.PARAM_CLIP_DURATION_SEC_LIST, durations);
        request.getParams().put(FfmpegComposeProvider.PARAM_ASPECT_RATIO, aspect);
        return request;
    }

    private Path compose(AiProviderRequest request) throws Exception {
        AiProviderResult result = new FfmpegComposeProvider().execute(request);
        Path output = dir.resolve("film-" + sequence++ + ".mp4");
        Files.write(output, result.getBinaryContent());
        return output;
    }

    private Path fixture(String color, String duration, String fps, boolean audio, boolean variable, String offset) throws Exception {
        Path output = dir.resolve("fixture-" + sequence++ + ".mp4");
        String source = "color=c=" + color + ":s=160x120:r=" + fps + ":d=" + duration
                + "[base];testsrc2=s=32x32:r=" + fps + ":d=" + duration
                + "[motion];[base][motion]overlay=x=64:y=44:shortest=1";
        List<String> command = new ArrayList<>(List.of("ffmpeg", "-y", "-f", "lavfi", "-i", source));
        if (audio) command.addAll(List.of("-f", "lavfi", "-i", "sine=frequency=440:duration=" + duration));
        if (variable) command.addAll(List.of("-vf", "select=not(eq(mod(n\\,3)\\,1))", "-fps_mode", "vfr"));
        command.addAll(List.of("-c:v", "libx264", "-preset", "ultrafast", "-threads", "2", "-pix_fmt", "yuv420p"));
        if (audio) command.addAll(List.of("-c:a", "aac"));
        command.addAll(List.of("-output_ts_offset", offset, output.toString()));
        run(command);
        return output;
    }

    private Path tone(String duration) throws Exception {
        Path output = dir.resolve("tone-" + sequence++ + ".wav");
        run(List.of("ffmpeg", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=" + duration,
                "-ar", "44100", "-ac", "2", output.toString()));
        return output;
    }

    private JsonNode probe(Path media) throws Exception {
        return json.readTree(run(List.of("ffprobe", "-v", "error", "-show_entries",
                "format=duration:stream=codec_type,width,height,duration,start_time,r_frame_rate,sample_aspect_ratio,pix_fmt,nb_frames",
                "-of", "json", media.toString())));
    }

    private void assertMedia(Path media, int width, int height, int duration, int frames) throws Exception {
        JsonNode metadata = probe(media);
        assertEquals(duration, metadata.path("format").path("duration").asDouble(), .04);
        JsonNode video = null, audio = null;
        for (JsonNode stream : metadata.path("streams")) {
            if ("video".equals(stream.path("codec_type").asText())) video = stream;
            if ("audio".equals(stream.path("codec_type").asText())) audio = stream;
        }
        assertNotNull(video); assertNotNull(audio);
        assertEquals(width, video.path("width").asInt()); assertEquals(height, video.path("height").asInt());
        assertEquals("30/1", video.path("r_frame_rate").asText());
        assertEquals("1:1", video.path("sample_aspect_ratio").asText());
        assertEquals("yuv420p", video.path("pix_fmt").asText());
        assertEquals(frames, video.path("nb_frames").asInt());
        assertEquals(duration, video.path("duration").asDouble(), .04);
        assertEquals(duration, audio.path("duration").asDouble(), .04);
    }

    private int[] pixel(Path media, double second, int x, int y) throws Exception {
        byte[] rgb = run(List.of("ffmpeg", "-v", "error", "-ss", String.valueOf(second), "-i", media.toString(),
                "-vf", "format=rgb24,crop=1:1:" + x + ":" + y, "-frames:v", "1", "-threads", "1", "-f", "rawvideo", "pipe:1"));
        assertEquals(3, rgb.length);
        return new int[]{Byte.toUnsignedInt(rgb[0]), Byte.toUnsignedInt(rgb[1]), Byte.toUnsignedInt(rgb[2])};
    }

    private void assertColor(Path media, double second, int x, int y, int channel) throws Exception {
        int[] pixel = pixel(media, second, x, y);
        assertTrue(pixel[channel] > 180, "expected dominant color at " + second + "s: " + java.util.Arrays.toString(pixel));
        for (int i = 0; i < 3; i++) if (i != channel) assertTrue(pixel[i] < 60);
    }

    private void assertBlack(Path media, double second, int x, int y) throws Exception {
        for (int value : pixel(media, second, x, y)) assertTrue(value < 15, "expected letterbox padding");
    }

    private double rms(Path media, double second) throws Exception {
        byte[] pcm = run(List.of("ffmpeg", "-v", "error", "-ss", String.valueOf(second), "-i", media.toString(),
                "-t", "0.15", "-vn", "-ac", "1", "-ar", "44100", "-f", "s16le", "pipe:1"));
        assertTrue(pcm.length > 1000);
        double sum = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            short sample = (short) ((pcm[i] & 255) | (pcm[i + 1] << 8));
            sum += (sample / 32768.0) * (sample / 32768.0);
        }
        return Math.sqrt(sum / (pcm.length / 2));
    }

    private byte[] run(List<String> command) throws Exception {
        Path output = dir.resolve("process-" + sequence++ + ".out");
        Path error = dir.resolve("process-" + sequence++ + ".err");
        Process process = new ProcessBuilder(command).redirectOutput(output.toFile()).redirectError(error.toFile()).start();
        try {
            assertTrue(process.waitFor(90, TimeUnit.SECONDS), "fixture process timeout");
            assertEquals(0, process.exitValue(), () -> { try { return Files.readString(error); } catch (Exception e) { return "fixture failed"; } });
            return Files.readAllBytes(output);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
