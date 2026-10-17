package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * provider_code=KLING — 可灵官方异步任务（文生图 / 文生视频 / 图生视频）。
 * 鉴权：Authorization Bearer API Key。默认域名为北京站。
 */
@Component
public class KlingAiProvider implements AiProvider {

    public static final String CODE = "KLING";

    static final String DEFAULT_BASE_URL = "https://api-beijing.klingai.com";
    static final String DEFAULT_IMAGE_PATH = "/v1/images/generations";
    static final String DEFAULT_TEXT2VIDEO_PATH = "/v1/videos/text2video";
    static final String DEFAULT_IMAGE2VIDEO_PATH = "/v1/videos/image2video";

    private static final Logger log = LoggerFactory.getLogger(KlingAiProvider.class);
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;

    private final SysSecretReader secretReader;
    private final Transport transport;
    private final Sleeper sleeper;

    @Autowired
    public KlingAiProvider(SysSecretReader secretReader) {
        this(secretReader, new JdkTransport(), ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        });
    }

    KlingAiProvider(SysSecretReader secretReader, Transport transport, Sleeper sleeper) {
        this.secretReader = secretReader;
        this.transport = transport;
        this.sleeper = sleeper;
    }

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        throw DirectVendorGuard.rejected(CODE);
    }

    /** Retained for unit coverage of legacy transport; production path never calls this. */
    AiProviderResult executeLegacy(AiProviderRequest request) {
        if (request == null || request.getModel() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model required");
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String baseUrl = trimSlash(firstNonBlank(ShortJson.text(cfg.get("base_url")), DEFAULT_BASE_URL));
        String secretRef = ShortJson.text(cfg.get("secret_ref"));
        if (secretRef.isBlank()) {
            secretRef = SysSecretReader.AI_KLING_API_KEY;
        }
        String apiKey = secretReader.get(secretRef);
        if (apiKey.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "AI api key missing for " + secretRef);
        }
        int timeoutSec = request.getModel().getTimeoutSec() == null ? 180 : request.getModel().getTimeoutSec();
        if (timeoutSec < 30) {
            timeoutSec = 30;
        }
        String ability = request.getAbilityType();
        try {
            if ("IMAGE".equals(ability)) {
                return generateImage(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            if ("VIDEO".equals(ability)) {
                return generateVideo(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "KLING unsupported ability: " + ability);
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "kling interrupted");
        } catch (Exception ex) {
            log.warn("KLING call failed ability={} model={}", ability, request.getModel().getModelCode(), ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, safeMessage(ex));
        }
    }

    private AiProviderResult generateImage(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = firstNonBlank(ShortJson.text(cfg.get("image_path")), DEFAULT_IMAGE_PATH);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_name", request.getModel().getModelCode());
        body.put("prompt", promptOf(request));
        String negative = firstNonBlank(ShortJson.text(request.param("negative_prompt")),
                ShortJson.text(cfg.get("negative_prompt")));
        if (!negative.isBlank()) {
            body.put("negative_prompt", negative);
        }
        body.put("n", 1);
        body.put("aspect_ratio", aspectRatio(cfg, request));
        String image = imagePayload(request);
        if (image != null && !image.isBlank()) {
            body.put("image", image);
            String ref = firstNonBlank(ShortJson.text(request.param("image_reference")), "subject");
            body.put("image_reference", ref);
            Double fidelity = ShortJson.decimal(request.param("image_fidelity"), 0.75d);
            body.put("image_fidelity", fidelity);
            if ("subject".equals(ref)) {
                body.put("human_fidelity", ShortJson.decimal(request.param("human_fidelity"), 0.55d));
            }
        }
        Map<String, Object> data = submitAndPoll(baseUrl, path, apiKey, body, timeoutSec, cfg);
        String url = firstImageUrl(data);
        byte[] bytes = transport.getBytes(url, Math.min(120, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, guessImageMime(url));
        result.setWidth(1080);
        result.setHeight(1920);
        return result;
    }

    private AiProviderResult generateVideo(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String image = imagePayload(request);
        boolean image2video = image != null && !image.isBlank();
        String path = image2video
                ? firstNonBlank(ShortJson.text(cfg.get("image2video_path")), DEFAULT_IMAGE2VIDEO_PATH)
                : firstNonBlank(ShortJson.text(cfg.get("text2video_path")), DEFAULT_TEXT2VIDEO_PATH);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_name", request.getModel().getModelCode());
        body.put("prompt", promptOf(request));
        String negative = firstNonBlank(ShortJson.text(request.param("negative_prompt")),
                ShortJson.text(cfg.get("negative_prompt")));
        if (!negative.isBlank()) {
            body.put("negative_prompt", negative);
        }
        body.put("mode", modeOf(cfg, request));
        body.put("duration", durationOf(cfg, request));
        if (image2video) {
            body.put("image", image);
        } else {
            body.put("aspect_ratio", aspectRatio(cfg, request));
        }
        Map<String, Object> data = submitAndPoll(baseUrl, path, apiKey, body, timeoutSec, cfg);
        String url = firstVideoUrl(data);
        byte[] bytes = transport.getBytes(url, Math.min(180, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        result.setWidth(1080);
        result.setHeight(1920);
        result.setDurationSec(videoDurationSec(data, ShortJson.integer(request.param("duration_sec"), 5)));
        return result;
    }

    private Map<String, Object> submitAndPoll(String baseUrl, String path, String apiKey,
            Map<String, Object> body, int timeoutSec, Map<String, Object> cfg) throws Exception {
        int httpTimeout = Math.min(60, timeoutSec);
        String raw = transport.postJson(baseUrl + path, apiKey, ShortJson.toJson(body), httpTimeout);
        Map<String, Object> submitted = requireOk(raw, "submit");
        String taskId = taskIdOf(submitted);
        if (taskId.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling submit missing task_id");
        }
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        long intervalMs = pollIntervalMs(cfg);
        String queryUrl = baseUrl + path + "/" + taskId;
        Map<String, Object> last = submitted;
        while (System.currentTimeMillis() < deadline) {
            String status = taskStatusOf(last);
            if (isSuccess(status) && hasMedia(last)) {
                return dataOf(last);
            }
            if (isFailed(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, failMessage(last));
            }
            sleeper.sleep(intervalMs);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            raw = transport.getJson(queryUrl, apiKey, httpTimeout);
            last = requireOk(raw, "poll");
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "kling task timeout task_id=" + taskId);
    }

    private static Map<String, Object> requireOk(String raw, String phase) {
        Map<String, Object> resp = ShortJson.map(raw);
        Object code = resp.get("code");
        if (code != null && !"0".equals(String.valueOf(code))) {
            String msg = firstNonBlank(ShortJson.text(resp.get("message")), "kling " + phase + " failed");
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, msg);
        }
        return resp;
    }

    private static Map<String, Object> dataOf(Map<String, Object> resp) {
        Object data = resp.get("data");
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            return out;
        }
        return new LinkedHashMap<>();
    }

    private static String taskIdOf(Map<String, Object> resp) {
        Map<String, Object> data = dataOf(resp);
        return firstNonBlank(ShortJson.text(data.get("task_id")), ShortJson.text(data.get("id")));
    }

    private static String taskStatusOf(Map<String, Object> resp) {
        Map<String, Object> data = dataOf(resp);
        return firstNonBlank(ShortJson.text(data.get("task_status")), ShortJson.text(data.get("status"))).toLowerCase();
    }

    private static boolean isSuccess(String status) {
        return "succeed".equals(status) || "succeeded".equals(status) || "success".equals(status);
    }

    private static boolean isFailed(String status) {
        return "failed".equals(status) || "fail".equals(status) || "error".equals(status) || "canceled".equals(status)
                || "cancelled".equals(status);
    }

    private static boolean hasMedia(Map<String, Object> resp) {
        try {
            firstImageUrl(dataOf(resp));
            return true;
        } catch (BizException ignored) {
            try {
                firstVideoUrl(dataOf(resp));
                return true;
            } catch (BizException ignored2) {
                return false;
            }
        }
    }

    private static String failMessage(Map<String, Object> resp) {
        Map<String, Object> data = dataOf(resp);
        return firstNonBlank(ShortJson.text(data.get("task_status_msg")), ShortJson.text(data.get("message")),
                "kling task failed");
    }

    @SuppressWarnings("unchecked")
    private static String firstImageUrl(Map<String, Object> data) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object images = map.get("images");
            if (images instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                String url = firstNonBlank(ShortJson.text(first.get("url")), ShortJson.text(first.get("url_1")));
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling image url missing");
    }

    @SuppressWarnings("unchecked")
    private static String firstVideoUrl(Map<String, Object> data) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object videos = map.get("videos");
            if (videos instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                String url = ShortJson.text(first.get("url"));
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling video url missing");
    }

    @SuppressWarnings("unchecked")
    private static int videoDurationSec(Map<String, Object> data, int fallback) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object videos = map.get("videos");
            if (videos instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                return ShortJson.integer(first.get("duration"), fallback);
            }
        }
        return fallback;
    }

    private static String promptOf(AiProviderRequest request) {
        String prompt = request.getPrompt();
        if (prompt != null && !prompt.isBlank()) {
            return prompt.trim();
        }
        String title = ShortJson.text(request.param("title"));
        String subtitle = ShortJson.text(request.param("subtitle"));
        String joined = (title + " " + subtitle).trim();
        if (joined.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "prompt required");
        }
        return joined;
    }

    private static String imagePayload(AiProviderRequest request) {
        String url = ShortJson.text(request.param("image_url"));
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String b64 = ShortJson.text(request.param("image_base64"));
        if (b64.isBlank()) {
            return "";
        }
        int approxBytes = b64.length() * 3 / 4;
        if (approxBytes > MAX_IMAGE_BYTES) {
            log.warn("KLING skip first frame: image exceeds {} bytes", MAX_IMAGE_BYTES);
            return "";
        }
        return b64;
    }

    private static String aspectRatio(Map<String, Object> cfg, AiProviderRequest request) {
        return firstNonBlank(ShortJson.text(request.param("aspect_ratio")),
                ShortJson.text(cfg.get("aspect_ratio")), "9:16");
    }

    private static String modeOf(Map<String, Object> cfg, AiProviderRequest request) {
        String mode = firstNonBlank(ShortJson.text(request.param("mode")), ShortJson.text(cfg.get("mode")));
        if (!mode.isBlank()) {
            return mode;
        }
        String quality = request.getModel().getQualityLevel();
        if ("HIGH".equalsIgnoreCase(quality) || "PRO".equalsIgnoreCase(quality)) {
            return "pro";
        }
        return "std";
    }

    private static String durationOf(Map<String, Object> cfg, AiProviderRequest request) {
        int sec = ShortJson.integer(request.param("duration_sec"), 0);
        if (sec <= 0) {
            sec = ShortJson.integer(cfg.get("duration"), 5);
        }
        if (sec <= 5) {
            return "5";
        }
        return "10";
    }

    private static long pollIntervalMs(Map<String, Object> cfg) {
        int ms = ShortJson.integer(cfg.get("poll_interval_ms"), 3000);
        if (ms < 0) {
            return 0;
        }
        return Math.min(10_000, ms);
    }

    private static String guessImageMime(String url) {
        String lower = url == null ? "" : url.toLowerCase();
        if (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains("image/jpeg")) {
            return "image/jpeg";
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String v = url.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "kling call failed";
        }
        if (msg.length() > 240) {
            return msg.substring(0, 240);
        }
        return msg;
    }

    interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    interface Transport {
        String postJson(String url, String bearer, String body, int timeoutSec) throws Exception;

        String getJson(String url, String bearer, int timeoutSec) throws Exception;

        byte[] getBytes(String url, int timeoutSec) throws Exception;
    }

    static final class JdkTransport implements Transport {

        private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

        @Override
        public String postJson(String url, String bearer, String body, int timeoutSec) throws Exception {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .header("Authorization", "Bearer " + bearer)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return handleJson(resp);
        }

        @Override
        public String getJson(String url, String bearer, int timeoutSec) throws Exception {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .header("Authorization", "Bearer " + bearer)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return handleJson(resp);
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) throws Exception {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 400) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling download status " + resp.statusCode());
            }
            byte[] body = resp.body();
            if (body == null || body.length == 0) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling download empty");
            }
            return body;
        }

        private static String handleJson(HttpResponse<String> resp) {
            if (resp.statusCode() >= 400) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling http " + resp.statusCode());
            }
            return resp.body() == null ? "" : resp.body();
        }
    }
}
