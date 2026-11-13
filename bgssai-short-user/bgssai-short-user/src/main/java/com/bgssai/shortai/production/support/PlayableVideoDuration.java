package com.bgssai.shortai.production.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Computes the whole-second timeline length that {@code FfmpegComposeProvider} can safely trim
 * from a video clip without hitting {@code info.duration() + 0.001 < plannedDuration}.
 * <p>
 * Billing and asset metadata ({@link MediaMetadata}) intentionally keep {@code Math.ceil(duration)},
 * whereas FFmpeg composition requires {@code plannedDuration <= videoStreamDuration}.
 */
public final class PlayableVideoDuration {

    private static final ObjectMapper JSON = new ObjectMapper();

    private PlayableVideoDuration() {
    }

    public static Integer floorSeconds(byte[] mp4Bytes) {
        return floorSeconds(mp4Bytes, List.of("ffprobe"), 5_000L);
    }

    static Integer floorSeconds(byte[] mp4Bytes, List<String> executable, long timeoutMs) {
        if (mp4Bytes == null || mp4Bytes.length < 16) {
            return null;
        }
        Path dir = null;
        Process process = null;
        try {
            dir = Files.createTempDirectory("short-playable-dur-");
            Path input = dir.resolve("clip.mp4");
            Files.write(input, mp4Bytes);
            List<String> cmd = new ArrayList<>(executable);
            cmd.addAll(List.of("-v", "error", "-protocol_whitelist", "file,pipe",
                    "-show_entries", "format=duration:stream=codec_type,duration",
                    "-of", "json", input.toString()));
            process = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            byte[] out = process.getInputStream().readNBytes(65_536);
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
                return null;
            }
            JsonNode root = JSON.readTree(new String(out, StandardCharsets.UTF_8));
            double duration = 0;
            for (JsonNode stream : root.path("streams")) {
                if ("video".equals(stream.path("codec_type").asText())) {
                    duration = stream.path("duration").asDouble(0);
                    if (duration > 0) {
                        break;
                    }
                }
            }
            if (duration <= 0) {
                duration = root.path("format").path("duration").asDouble(0);
            }
            if (!Double.isFinite(duration) || duration <= 0 || duration > Integer.MAX_VALUE - 1) {
                return null;
            }
            return Math.max(1, (int) Math.floor(duration + 0.05));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (dir != null) {
                try {
                    Files.deleteIfExists(dir.resolve("clip.mp4"));
                    Files.deleteIfExists(dir);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
