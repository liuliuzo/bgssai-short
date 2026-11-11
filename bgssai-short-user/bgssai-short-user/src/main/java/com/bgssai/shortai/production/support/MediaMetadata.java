package com.bgssai.shortai.production.support;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/** Inspect returned bytes before storing or settling a generated media asset. */
public final class MediaMetadata {
    private static final int OUTPUT_LIMIT = 65_536;
    private static final ObjectMapper JSON = new ObjectMapper();
    private MediaMetadata() { }

    public static void verify(AiProviderResult result, String ability) {
        verify(result, ability, List.of("ffprobe"), 15_000);
    }

    static void verify(AiProviderResult result, String ability, List<String> executable, long timeoutMs) {
        if (!List.of("IMAGE", "VIDEO", "VOICE", "MUSIC", "COMPOSE").contains(ability)) return;
        byte[] bytes = result == null ? null : result.getBinaryContent();
        if (bytes == null || bytes.length == 0) throw invalid();
        Path dir = null;
        Process process = null;
        Thread reader = null;
        try {
            // Read the header without decoding a potentially large pixel buffer.
            if ("IMAGE".equals(ability)) {
                try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                    var readers = ImageIO.getImageReaders(input);
                    if (readers.hasNext()) {
                        ImageReader image = readers.next();
                        try {
                            image.setInput(input, true, true);
                            int width = image.getWidth(0), height = image.getHeight(0);
                            if (width <= 0 || height <= 0) throw invalid();
                            result.setWidth(width);
                            result.setHeight(height);
                            result.setDurationSec(null);
                            return;
                        } finally { image.dispose(); }
                    }
                }
            }
            dir = Files.createTempDirectory("short-media-probe-");
            Path input = dir.resolve("input.bin");
            Files.write(input, bytes);
            List<String> command = new ArrayList<>(executable);
            command.addAll(List.of("-v", "error", "-protocol_whitelist", "file,pipe",
                    "-show_entries", "format=format_name,duration:stream=codec_type,codec_name,width,height,duration",
                    "-of", "json", input.toString()));
            process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            Process running = process;
            CompletableFuture<byte[]> output = new CompletableFuture<>();
            reader = Thread.ofVirtual().start(() -> {
                try (var stream = running.getInputStream()) {
                    byte[] data = stream.readNBytes(OUTPUT_LIMIT + 1);
                    if (data.length > OUTPUT_LIMIT) {
                        running.destroyForcibly();
                        output.completeExceptionally(new IllegalStateException("probe output limit"));
                    } else output.complete(data);
                } catch (Exception ex) { output.completeExceptionally(ex); }
            });
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "媒体信息解析超时，任务未完成");
            }
            if (process.exitValue() != 0) throw invalid();
            JsonNode metadata = JSON.readTree(new String(output.get(1, TimeUnit.SECONDS), StandardCharsets.UTF_8));
            boolean picture = List.of("IMAGE", "VIDEO", "COMPOSE").contains(ability);
            JsonNode stream = null;
            for (JsonNode candidate : metadata.path("streams")) {
                if ((picture ? "video" : "audio").equals(candidate.path("codec_type").asText())) {
                    stream = candidate;
                    break;
                }
            }
            if (stream == null) throw invalid();
            if ("IMAGE".equals(ability)) {
                String format = metadata.path("format").path("format_name").asText();
                String codec = stream.path("codec_name").asText();
                if (!List.of("image2", "png_pipe", "jpeg_pipe", "webp_pipe", "bmp_pipe", "tiff_pipe", "gif", "ico", "apng").contains(format)
                        || !List.of("png", "mjpeg", "bmp", "tiff", "webp", "gif", "apng").contains(codec)) throw invalid();
            }
            if (picture) {
                int width = stream.path("width").asInt(), height = stream.path("height").asInt();
                if (width <= 0 || height <= 0) throw invalid();
                result.setWidth(width);
                result.setHeight(height);
            } else {
                result.setWidth(null);
                result.setHeight(null);
            }
            if ("IMAGE".equals(ability)) {
                result.setDurationSec(null);
                return;
            }
            double duration = metadata.path("format").path("duration").asDouble(0);
            if (duration <= 0) duration = stream.path("duration").asDouble(0);
            if (!Double.isFinite(duration) || duration <= 0 || duration > Integer.MAX_VALUE - 1) throw invalid();
            result.setDurationSec((int) Math.ceil(duration));
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "媒体信息解析已中断，任务未完成");
        } catch (java.io.IOException ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "无法解析媒体信息，请检查媒体文件与 ffprobe 服务依赖");
        } catch (Exception ex) {
            throw invalid();
        } finally {
            boolean interrupted = Thread.interrupted();
            if (process != null) {
                if (process.isAlive()) process.destroyForcibly();
                try {
                    process.waitFor(1, TimeUnit.SECONDS);
                    process.getInputStream().close();
                } catch (InterruptedException ex) { interrupted = true; }
                catch (java.io.IOException ignored) { }
            }
            if (reader != null && reader.isAlive()) {
                reader.interrupt();
                try { reader.join(250); } catch (InterruptedException ex) { interrupted = true; }
            }
            if (dir != null) {
                try { Files.deleteIfExists(dir.resolve("input.bin")); Files.deleteIfExists(dir); }
                catch (java.io.IOException ignored) { /* A still-exiting process may retain its own temporary input. */ }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static BizException invalid() {
        return new BizException(CommonErrorCode.UPSTREAM_ERROR, "模型返回的媒体文件无效或缺少实际媒体信息");
    }
}
