package com.bgssai.shortai.aiprovider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;

/**
 * provider_code=FFMPEG —— 用本机 FFmpeg 把各镜头的视频片段拼成一条 9:16 成片。
 * <p>
 * 合成是唯一一个不该调外部大模型的能力：素材已经在本地，缺的只是拼接。这里不做任何
 * 「合不成就返回一张图」的兜底——ffmpeg 不在或素材不全就直接失败，让创作者看到真实原因。
 * <p>
 * 片段字节由 {@code ShortAiTaskWorker} 从对象存储取出后放进 {@code params.clip_list}，
 * 可选配音 {@code params.dubbing_list}（与镜头一一对应，空字节表示该镜静音占位）、
 * 可选 BGM 与 SRT。保持 provider 本身不依赖存储实现。
 */
@Component
public class FfmpegComposeProvider implements AiProvider {

    public static final String CODE = "FFMPEG";

    /** params 中承载片段字节的键。 */
    public static final String PARAM_CLIPS = "clip_list";

    /** params 中承载可选背景音乐字节的键。 */
    public static final String PARAM_BGM = "bgm_audio";

    /** params 中承载可选 SRT 字幕文本的键（UTF-8）。 */
    public static final String PARAM_SUBTITLE_SRT = "subtitle_srt";

    /**
     * 与 clip_list 等长的配音字节列表；某项为空 / null 表示该镜无配音，用静音占位。
     */
    public static final String PARAM_DUBBING_LIST = "dubbing_list";

    /** 与 clip_list 等长的每镜时长（秒），用于静音占位与配音对齐。 */
    public static final String PARAM_CLIP_DURATION_SEC_LIST = "clip_duration_sec_list";

    private static final Logger log = LoggerFactory.getLogger(FfmpegComposeProvider.class);

    private static final int PROBE_TIMEOUT_SEC = 10;

    private static final int DEFAULT_RUN_TIMEOUT_SEC = 300;

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (request == null || !"COMPOSE".equals(request.getAbilityType())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "FFMPEG unsupported ability: " + (request == null ? "null" : request.getAbilityType()));
        }
        List<byte[]> clips = clipsOf(request);
        if (clips.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "没有可合成的镜头视频：请先把每个镜头生成视频再合成");
        }
        requireFfmpeg();

        int timeoutSec = request.getModel() == null || request.getModel().getTimeoutSec() == null
                ? DEFAULT_RUN_TIMEOUT_SEC
                : Math.max(60, request.getModel().getTimeoutSec());
        Path dir = null;
        try {
            dir = Files.createTempDirectory("bgssai-compose-");
            List<String> names = new ArrayList<>();
            for (int i = 0; i < clips.size(); i++) {
                String name = String.format("clip-%03d.mp4", i);
                Files.write(dir.resolve(name), clips.get(i));
                names.add(name);
            }
            StringBuilder listFile = new StringBuilder();
            for (String name : names) {
                listFile.append("file '").append(name).append("'\n");
            }
            Files.write(dir.resolve("concat.txt"), listFile.toString().getBytes(StandardCharsets.UTF_8));

            byte[] bgm = binaryParam(request, PARAM_BGM);
            String srt = textParam(request, PARAM_SUBTITLE_SRT);
            List<byte[]> dubbings = dubbingListOf(request);
            List<Integer> durations = durationListOf(request, clips.size());
            boolean hasDubbing = hasAnyAudio(dubbings);
            boolean needsPost = bgm != null || (srt != null && !srt.isBlank()) || hasDubbing;
            Path out = dir.resolve("film.mp4");
            Path concatOut = needsPost ? dir.resolve("video.mp4") : out;
            // 先试无损拼接；片段编码参数不一致时 concat 会失败，再退到统一重编码。
            if (!run(dir, timeoutSec, "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                    "-i", "concat.txt", "-c", "copy", concatOut.getFileName().toString())) {
                log.info("ffmpeg stream copy concat failed, re-encoding {} clips", clips.size());
                if (!run(dir, timeoutSec, "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                        "-i", "concat.txt", "-c:v", "libx264", "-preset", "veryfast",
                        "-pix_fmt", "yuv420p", "-c:a", "aac", concatOut.getFileName().toString())) {
                    throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                            "ffmpeg 合成失败，请检查镜头视频是否可解码");
                }
            }
            Path current = concatOut;
            if (hasDubbing) {
                Path dubbed = (srt != null && !srt.isBlank()) || bgm != null
                        ? dir.resolve("video-dub.mp4")
                        : out;
                mixDubbingTrack(dir, timeoutSec, current, dubbed, dubbings, durations);
                current = dubbed;
            }
            if (srt != null && !srt.isBlank()) {
                Files.writeString(dir.resolve("subs.srt"), srt, StandardCharsets.UTF_8);
                Path burned = bgm == null ? out : dir.resolve("video-subs.mp4");
                // Absolute path with escaped separators for the subtitles filter.
                String srtPath = dir.resolve("subs.srt").toAbsolutePath().toString()
                        .replace("\\", "/")
                        .replace(":", "\\:")
                        .replace("'", "\\'");
                if (!run(dir, timeoutSec, "ffmpeg", "-y", "-i", current.getFileName().toString(),
                        "-vf", "subtitles=" + srtPath + ":force_style='FontSize=22,Outline=1,Shadow=0'",
                        "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p",
                        "-c:a", "copy", burned.getFileName().toString())) {
                    throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 字幕烧录失败");
                }
                current = burned;
            }
            if (bgm != null) {
                Files.write(dir.resolve("bgm-audio"), bgm);
                if (hasDubbing || hasAudioStream(dir, timeoutSec, current)) {
                    // Mix BGM under existing VO / clip audio instead of replacing it.
                    if (!run(dir, timeoutSec, "ffmpeg", "-y", "-i", current.getFileName().toString(),
                            "-stream_loop", "-1", "-i", "bgm-audio",
                            "-filter_complex",
                            "[1:a]volume=0.22[bgm];[0:a][bgm]amix=inputs=2:duration=first:dropout_transition=2[aout]",
                            "-map", "0:v:0", "-map", "[aout]", "-c:v", "copy", "-c:a", "aac",
                            "-shortest", out.getFileName().toString())) {
                        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 背景音乐混流失败");
                    }
                } else if (!run(dir, timeoutSec, "ffmpeg", "-y", "-i", current.getFileName().toString(),
                        "-stream_loop", "-1", "-i", "bgm-audio", "-map", "0:v:0", "-map", "1:a:0",
                        "-c:v", "copy", "-c:a", "aac", "-shortest", out.getFileName().toString())) {
                    throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 背景音乐混流失败");
                }
            } else if (!current.equals(out)) {
                Files.move(current, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            byte[] mp4 = Files.readAllBytes(out);
            if (mp4.length == 0) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 输出为空");
            }
            AiProviderResult result = AiProviderResult.binary(mp4, "video/mp4");
            result.setWidth(1080);
            result.setHeight(1920);
            Integer duration = ShortJson.integer(request.param("duration_sec"), 0);
            if (duration != null && duration > 0) {
                result.setDurationSec(duration);
            }
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 合成异常：" + ex.getMessage());
        } finally {
            deleteQuietly(dir);
        }
    }

    /**
     * Build a continuous VO track (per-shot dubbing or silence) and replace the video audio.
     * Package-visible for tests that assert intermediate files / command success.
     */
    void mixDubbingTrack(Path dir, int timeoutSec, Path videoIn, Path videoOut,
            List<byte[]> dubbings, List<Integer> durations) throws Exception {
        StringBuilder audioList = new StringBuilder();
        for (int i = 0; i < dubbings.size(); i++) {
            int dur = i < durations.size() ? Math.max(1, durations.get(i)) : 3;
            String segment = String.format("vo-%03d.wav", i);
            byte[] audio = dubbings.get(i);
            if (audio != null && audio.length > 0) {
                String raw = String.format("vo-raw-%03d", i);
                Files.write(dir.resolve(raw), audio);
                if (!run(dir, timeoutSec, "ffmpeg", "-y", "-i", raw, "-t", String.valueOf(dur),
                        "-af", "apad=whole_dur=" + dur, "-ar", "44100", "-ac", "2", segment)) {
                    // Corrupt / unsupported audio → silence for this shot, keep composing.
                    log.warn("dubbing decode failed index={}, using silence", i);
                    if (!run(dir, timeoutSec, "ffmpeg", "-y", "-f", "lavfi",
                            "-i", "anullsrc=r=44100:cl=stereo", "-t", String.valueOf(dur), segment)) {
                        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 配音静音占位失败");
                    }
                }
            } else if (!run(dir, timeoutSec, "ffmpeg", "-y", "-f", "lavfi",
                    "-i", "anullsrc=r=44100:cl=stereo", "-t", String.valueOf(dur), segment)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 配音静音占位失败");
            }
            audioList.append("file '").append(segment).append("'\n");
        }
        Files.writeString(dir.resolve("vo-concat.txt"), audioList.toString(), StandardCharsets.UTF_8);
        if (!run(dir, timeoutSec, "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                "-i", "vo-concat.txt", "-c", "copy", "vo-track.wav")) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 配音轨拼接失败");
        }
        List<String> cmd = buildReplaceAudioCommand(videoIn.getFileName().toString(), "vo-track.wav",
                videoOut.getFileName().toString());
        if (!run(dir, timeoutSec, cmd.toArray(String[]::new))) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 配音混入成片失败");
        }
    }

    /** Public for unit tests that assert ffmpeg argv for VO replace. */
    public static List<String> buildReplaceAudioCommand(String videoFile, String audioFile, String outFile) {
        return List.of("ffmpeg", "-y", "-i", videoFile, "-i", audioFile,
                "-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy", "-c:a", "aac",
                "-shortest", outFile);
    }

    /** Public for unit tests that assert ffmpeg argv for BGM underlay mix. */
    public static List<String> buildBgmAmixCommand(String videoFile, String bgmFile, String outFile) {
        return List.of("ffmpeg", "-y", "-i", videoFile, "-stream_loop", "-1", "-i", bgmFile,
                "-filter_complex",
                "[1:a]volume=0.22[bgm];[0:a][bgm]amix=inputs=2:duration=first:dropout_transition=2[aout]",
                "-map", "0:v:0", "-map", "[aout]", "-c:v", "copy", "-c:a", "aac",
                "-shortest", outFile);
    }

    @SuppressWarnings("unchecked")
    private static List<byte[]> clipsOf(AiProviderRequest request) {
        Object raw = request.param(PARAM_CLIPS);
        if (!(raw instanceof List<?>)) {
            return List.of();
        }
        List<byte[]> clips = new ArrayList<>();
        for (Object item : (List<Object>) raw) {
            if (item instanceof byte[] bytes && bytes.length > 0) {
                clips.add(bytes);
            }
        }
        return clips;
    }

    @SuppressWarnings("unchecked")
    private static List<byte[]> dubbingListOf(AiProviderRequest request) {
        Object raw = request.param(PARAM_DUBBING_LIST);
        if (!(raw instanceof List<?>)) {
            return List.of();
        }
        List<byte[]> list = new ArrayList<>();
        for (Object item : (List<Object>) raw) {
            if (item instanceof byte[] bytes) {
                list.add(bytes);
            } else {
                list.add(new byte[0]);
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> durationListOf(AiProviderRequest request, int clipCount) {
        Object raw = request.param(PARAM_CLIP_DURATION_SEC_LIST);
        List<Integer> list = new ArrayList<>();
        if (raw instanceof List<?> rows) {
            for (Object item : rows) {
                if (item instanceof Number n) {
                    list.add(Math.max(1, n.intValue()));
                } else {
                    list.add(3);
                }
            }
        }
        while (list.size() < clipCount) {
            list.add(3);
        }
        return list;
    }

    private static boolean hasAnyAudio(List<byte[]> dubbings) {
        for (byte[] bytes : dubbings) {
            if (bytes != null && bytes.length > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAudioStream(Path dir, int timeoutSec, Path media) throws Exception {
        Process process = new ProcessBuilder("ffprobe", "-v", "error", "-select_streams", "a",
                "-show_entries", "stream=codec_type", "-of", "csv=p=0", media.getFileName().toString())
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try (java.io.InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return false;
        }
        return process.exitValue() == 0 && !output.isBlank();
    }

    private static byte[] binaryParam(AiProviderRequest request, String name) {
        Object raw = request.param(name);
        return raw instanceof byte[] bytes && bytes.length > 0 ? bytes : null;
    }

    private static String textParam(AiProviderRequest request, String name) {
        Object raw = request.param(name);
        if (raw instanceof String s && !s.isBlank()) {
            return s;
        }
        if (raw instanceof byte[] bytes && bytes.length > 0) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return null;
    }

    private static void requireFfmpeg() {
        try {
            Process probe = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            if (!probe.waitFor(PROBE_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                probe.destroyForcibly();
                throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "ffmpeg 探测超时");
            }
            if (probe.exitValue() != 0) {
                throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "ffmpeg 不可用");
            }
        } catch (BizException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    "本机未安装 ffmpeg，无法合成成片（部署机需预装 ffmpeg）");
        }
    }

    private static boolean run(Path dir, int timeoutSec, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try (java.io.InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 超过 " + timeoutSec + " 秒未完成");
        }
        if (process.exitValue() != 0) {
            log.warn("ffmpeg exit={} output={}", process.exitValue(),
                    output.length() > 2000 ? output.substring(output.length() - 2000) : output);
            return false;
        }
        return true;
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 临时目录清理失败不影响合成结果
                }
            });
        } catch (IOException ex) {
            log.warn("compose temp dir cleanup failed dir={}", dir, ex);
        }
    }
}
