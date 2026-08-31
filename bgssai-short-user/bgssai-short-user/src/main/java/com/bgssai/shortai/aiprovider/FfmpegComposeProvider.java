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
 * 保持 provider 本身不依赖存储实现。
 */
@Component
public class FfmpegComposeProvider implements AiProvider {

    public static final String CODE = "FFMPEG";

    /** params 中承载片段字节的键。 */
    public static final String PARAM_CLIPS = "clip_list";

    /** params 中承载可选背景音乐字节的键。 */
    public static final String PARAM_BGM = "bgm_audio";

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
            Path out = dir.resolve("film.mp4");
            Path concatOut = bgm == null ? out : dir.resolve("video.mp4");
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
            if (bgm != null) {
                Files.write(dir.resolve("bgm-audio"), bgm);
                if (!run(dir, timeoutSec, "ffmpeg", "-y", "-i", concatOut.getFileName().toString(),
                        "-stream_loop", "-1", "-i", "bgm-audio", "-map", "0:v:0", "-map", "1:a:0",
                        "-c:v", "copy", "-c:a", "aac", "-shortest", out.getFileName().toString())) {
                    throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 背景音乐混流失败");
                }
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

    private static byte[] binaryParam(AiProviderRequest request, String name) {
        Object raw = request.param(name);
        return raw instanceof byte[] bytes && bytes.length > 0 ? bytes : null;
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
