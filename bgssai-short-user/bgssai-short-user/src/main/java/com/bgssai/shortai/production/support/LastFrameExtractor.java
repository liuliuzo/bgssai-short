package com.bgssai.shortai.production.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * 用本机 ffmpeg 从一段视频里抽出最后一帧，作为该镜的尾帧素材。
 *
 * <p>尾帧优先走抽帧而不是再出一张图：抽出来的帧与视频画面**必然一致**，且不花钱；
 * 单独出图既要付费，又可能和视频最后一帧对不上，那样的「衔接」反而制造新的跳。
 *
 * <p>与成片合成同一条 ffmpeg 口径：不在就明确失败并说清原因，不静默跳过——
 * 静默跳过会让创作者以为衔接开着，实际每一镜仍在各拍各的。
 */
public final class LastFrameExtractor {

    private static final Logger log = LoggerFactory.getLogger(LastFrameExtractor.class);

    private static final int PROBE_TIMEOUT_SEC = 5;

    private static final int EXTRACT_TIMEOUT_SEC = 60;

    private LastFrameExtractor() {
    }

    /**
     * 抽最后一帧，返回 PNG 字节。videoBytes 为该镜视频原始字节。
     */
    public static byte[] extract(byte[] videoBytes) {
        if (videoBytes == null || videoBytes.length == 0) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "视频内容为空，无法抽取尾帧");
        }
        requireFfmpeg();
        Path dir = null;
        try {
            dir = Files.createTempDirectory("bgssai-lastframe-");
            Path input = dir.resolve("input.mp4");
            Path output = dir.resolve("last.png");
            Files.write(input, videoBytes);
            // sseof -0.1 从末尾回退 0.1 秒起解，再取一帧：直接 seek 到 duration 常常什么都取不到，
            // 因为最后一帧的时间戳未必正好落在 duration 上。
            boolean ok = run(dir, EXTRACT_TIMEOUT_SEC, "ffmpeg", "-y", "-sseof", "-0.1",
                    "-i", input.getFileName().toString(), "-update", "1", "-q:v", "2",
                    output.getFileName().toString());
            if (!ok || !Files.exists(output) || Files.size(output) == 0) {
                // 回退：某些编码用 sseof 定位不到，改成整段解码。-update 1 会让每一帧都覆盖写同一个文件，
                // 解完留在磁盘上的自然就是最后一帧。慢，但对任何能解的视频都成立。
                ok = run(dir, EXTRACT_TIMEOUT_SEC, "ffmpeg", "-y",
                        "-i", input.getFileName().toString(), "-update", "1", "-q:v", "2",
                        output.getFileName().toString());
            }
            if (!ok || !Files.exists(output) || Files.size(output) == 0) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "ffmpeg 未能抽出尾帧");
            }
            return Files.readAllBytes(output);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "尾帧抽取异常：" + ex.getMessage());
        } finally {
            cleanup(dir);
        }
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
                    "本机未安装 ffmpeg，无法抽取尾帧（部署机需预装 ffmpeg）");
        }
    }

    private static boolean run(Path dir, int timeoutSec, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "ffmpeg 抽帧超过 " + timeoutSec + " 秒未完成");
        }
        if (process.exitValue() != 0) {
            log.warn("ffmpeg last-frame exit={} output={}", process.exitValue(),
                    output.length() > 1000 ? output.substring(output.length() - 1000) : output);
            return false;
        }
        return true;
    }

    private static void cleanup(Path dir) {
        if (dir == null) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    log.debug("temp file left behind: {}", path);
                }
            });
        } catch (IOException ex) {
            log.debug("temp dir cleanup failed: {}", dir, ex);
        }
    }
}
