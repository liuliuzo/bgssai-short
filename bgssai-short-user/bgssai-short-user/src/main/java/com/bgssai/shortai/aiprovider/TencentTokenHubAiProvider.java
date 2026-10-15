package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * provider_code=TENCENT_TOKENHUB. The user picks a model id. This client sends
 * that id to BGSSAI TokenHub's capability API. Vendor hosts and wand paths are
 * not used for image, video, speech, or text.
 */
@Component
public class TencentTokenHubAiProvider implements AiProvider {

    public static final String CODE = "TENCENT_TOKENHUB";

    private static final String CHAT_PATH = "/v1/chat/completions";
    private static final String IMAGE_PATH = "/v1/images/generations";
    private static final String VIDEO_PATH = "/v1/videos/generations";
    private static final long MEDIA_POLL_BUDGET_MS = 180_000L;
    private static final long MEDIA_POLL_INTERVAL_MS = 3_000L;
    private static final String SPEECH_PATH = "/v1/audio/speech";
    private static final String ASR_SUBMIT = "/v1/wand/asrproxy/async_create";
    private static final String ASR_TASK = "/v1/wand/asrproxy/tasks/{task_id}";
    private static final String MUSIC_PATH = "/v1/wand/minimax-music/generation";

    private static final Logger log = LoggerFactory.getLogger(TencentTokenHubAiProvider.class);

    @Autowired
    private com.bgssai.shortai.billing.UsageReceiptService receipts;
    private final ThreadLocal<com.bgssai.shortai.billing.UsageReceiptRow> activeReceipt=new ThreadLocal<>();

    private final SysSecretReader secretReader;
    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;
    private final String cnBaseUrl;
    private final String globalBaseUrl;

    @Autowired
    public TencentTokenHubAiProvider(SysSecretReader secretReader,
            @Value("${bgssai.llm.tokenhub-cn-base-url:${bgssai.llm.gateway-base-url:https://www.bgssai-tokenhub.cn}}")
                    String cnBaseUrl,
            @Value("${bgssai.llm.tokenhub-global-base-url:https://www.bgssai-tokenhub.com}")
                    String globalBaseUrl) {
        this(secretReader, new ProviderHttp.JdkClient(), ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        }, cnBaseUrl, globalBaseUrl);
    }

    TencentTokenHubAiProvider(SysSecretReader secretReader, ProviderHttp.Client http,
            ProviderHttp.Sleeper sleeper) {
        this(secretReader, http, sleeper, TokenHubClientSupport.DEFAULT_CN_BASE_URL,
                TokenHubClientSupport.DEFAULT_GLOBAL_BASE_URL);
    }

    TencentTokenHubAiProvider(SysSecretReader secretReader, ProviderHttp.Client http,
            ProviderHttp.Sleeper sleeper, String cnBaseUrl, String globalBaseUrl) {
        this.secretReader = secretReader;
        this.http = http;
        this.sleeper = sleeper;
        this.cnBaseUrl = ProviderHttp.trimSlash(cnBaseUrl == null ? "" : cnBaseUrl);
        this.globalBaseUrl = ProviderHttp.trimSlash(globalBaseUrl == null ? "" : globalBaseUrl);
    }

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (request == null || request.getModel() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model required");
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String hub = TokenHubClientSupport.resolveHub(cfg, request.getModel().getModelCode());
        String base = hubBase(ShortJson.text(cfg.get("base_url")), hub);
        String ref = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("secret_ref")),
                TokenHubClientSupport.defaultSecretRef(hub));
        String apiKey = TokenHubClientSupport.resolveApiKey(secretReader, hub, ref);
        String ability = request.getAbilityType() == null ? "" : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        if (apiKey.isBlank()) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    TokenHubClientSupport.userFacingMessage("model credential unavailable"));
        }
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);
        var receipt=receipts==null?null:receipts.begin(base,apiKey,request.getModel().getModelCode());
        activeReceipt.set(receipt);
        try {
            return switch (ability) {
                case "TEXT" -> text(base, apiKey, request, timeoutSec);
                case "IMAGE" -> image(base, apiKey, request, timeoutSec);
                case "VIDEO" -> video(base, apiKey, cfg, request, timeoutSec);
                case "VOICE" -> speech(base, apiKey, cfg, request, timeoutSec);
                case "ASR" -> asr(base, apiKey, cfg, request, timeoutSec);
                case "MUSIC" -> music(base, apiKey, cfg, request, timeoutSec);
                default -> throw new BizException(CommonErrorCode.RULE_REJECT,
                        "unsupported ability for model gateway: " + ability);
            };
        } catch (BizException ex) {
            throw new BizException(ex.getCode(), TokenHubClientSupport.userFacingMessage(ex.getMessage()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT,
                    TokenHubClientSupport.userFacingMessage("model call interrupted"));
        } catch (Exception ex) {
            log.warn("hub call failed ability={} hub={} model={}", ability, hub,
                    request.getModel().getModelCode(), ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, safe(ex));
        } finally {
            activeReceipt.remove();
            if(receipts!=null)receipts.refresh(receipt,apiKey);
        }
    }

    private AiProviderResult text(String base, String apiKey, AiProviderRequest request, int timeoutSec)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
        body.put("stream", false);
        Map<String, Object> resp = post(base + CHAT_PATH, apiKey, body, Math.min(120, timeoutSec));
        String content = chatContent(resp);
        if (content.isBlank()) {
            throw upstream("model text content missing", resp);
        }
        AiProviderResult result = AiProviderResult.text(content);
        int tokens = nestedInt(resp, "usage", "total_tokens", 0);
        result.setUsageAmount(usageAmount(request, tokens, 0));
        return result;
    }

    private AiProviderResult image(String base, String apiKey, AiProviderRequest request, int timeoutSec)
            throws Exception {
        String model = requireModel(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("n", 1);
        body.put("size", "1024x1792");
        String image = ProviderHttp.imagePayload(request);
        if (!image.isBlank()) {
            body.put("image", image);
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        Map<String, Object> submitted = post(base + IMAGE_PATH, apiKey, body, Math.min(180, timeoutSec));
        Map<String, Object> done = awaitMedia(base, apiKey, model, submitted, cfg);
        return mediaResult(request, done, "image/png", 0, 1080, 1920, timeoutSec);
    }

    private AiProviderResult video(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String model = requireModel(request);
        int duration = duration(request, cfg, 1, 15, 5);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", ProviderHttp.promptOf(request));
        String image = ProviderHttp.imagePayload(request);
        if (!image.isBlank()) {
            body.put("image", image);
        }
        Map<String, Object> submitted = post(base + VIDEO_PATH, apiKey, body, Math.min(60, timeoutSec));
        Map<String, Object> done = awaitMedia(base, apiKey, model, submitted, cfg);
        AiProviderResult ready = mediaResult(request, done, "video/mp4", duration, 1080, 1920, timeoutSec);
        if (ready.getDurationSec() == null || ready.getDurationSec() <= 0) {
            ready.setDurationSec(duration);
        }
        return ready;
    }

    private AiProviderResult speech(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String model = requireModel(request);
        String voice = ProviderHttp.firstNonBlank(ShortJson.text(request.param("voice_id")),
                ShortJson.text(request.param("voice")), ShortJson.text(cfg.get("default_voice_id")), "alloy");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", ProviderHttp.promptOf(request));
        body.put("voice", voice);
        byte[] raw = http.postBytes(base + SPEECH_PATH, generationHeaders(apiKey), ShortJson.toJson(body),
                Math.min(120, timeoutSec));
        if (raw == null || raw.length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model audio missing");
        }
        String text = new String(raw, java.nio.charset.StandardCharsets.UTF_8).trim();
        if (text.startsWith("{")) {
            Map<String, Object> resp = ShortJson.map(text);
            assertBaseOk(resp, "speech");
            Map<String, Object> data = child(resp, "data");
            String audio = ProviderHttp.firstNonBlank(ShortJson.text(data.get("audio")),
                    ShortJson.text(resp.get("audio")), mediaUrl(resp));
            int durationMs = nestedInt(resp, "extra_info", "audio_length", 0);
            return audioResult(request, audio, durationMs, timeoutSec);
        }
        AiProviderResult result = AiProviderResult.binary(raw, "audio/mpeg");
        result.setUsageAmount(usageAmount(request, 0, 0));
        return result;
    }

    private AiProviderResult asr(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String inputUrl = ShortJson.text(request.param("input_url"));
        requireHttps(inputUrl, "ASR input_url");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("input_url", inputUrl);
        body.put("subtitle_format", "srt");
        Map<String, Object> submitted = post(base + path(cfg, "submit_path", ASR_SUBMIT), apiKey, body,
                Math.min(60, timeoutSec));
        String taskId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("id")),
                ShortJson.text(submitted.get("task_id")), nestedText(submitted, "data", "id"));
        Map<String, Object> done = poll(base, apiKey, path(cfg, "task_path", ASR_TASK), taskId,
                timeoutSec, pollInterval(cfg, 2000), PollKind.ASR);
        Map<String, Object> output = child(done, "output");
        if (output.isEmpty()) {
            output = child(done, "data");
        }
        AiProviderResult result = AiProviderResult.text(ShortJson.toJson(output));
        int durationMs = ShortJson.integer(output.get("duration_ms"), 0);
        int durationSec = ceilSeconds(durationMs);
        result.setDurationSec(durationSec);
        result.setUsageAmount(usageAmount(request, 0, durationSec));
        return result;
    }

    private AiProviderResult music(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("prompt", ProviderHttp.promptOf(request));
        String lyrics = ShortJson.text(request.param("lyrics"));
        if (!lyrics.isBlank()) {
            body.put("lyrics", lyrics);
        }
        body.put("is_instrumental", bool(request.param("is_instrumental"), true));
        body.put("audio_setting", audioSetting());
        body.put("output_format", "url");
        Map<String, Object> resp = post(base + path(cfg, "music_path", MUSIC_PATH), apiKey, body,
                Math.min(180, timeoutSec));
        assertBaseOk(resp, "music");
        Map<String, Object> data = child(resp, "data");
        int durationMs = nestedInt(resp, "extra_info", "music_duration", 0);
        if (durationMs <= 0) {
            durationMs = nestedInt(resp, "extra_info", "audio_length", 0);
        }
        return audioResult(request, ShortJson.text(data.get("audio")), durationMs, timeoutSec);
    }

    private AiProviderResult audioResult(AiProviderRequest request, String value, int durationMs,
            int timeoutSec) throws Exception {
        if (value == null || value.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model audio missing");
        }
        byte[] bytes;
        if (value.startsWith("https://")) {
            bytes = http.getBytes(value, Math.min(180, timeoutSec));
        } else {
            String hex = value.startsWith("data:") && value.contains(",")
                    ? value.substring(value.indexOf(',') + 1) : value;
            try {
                bytes = value.startsWith("data:") ? Base64.getDecoder().decode(hex) : decodeAudio(hex);
            } catch (IllegalArgumentException ex) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model audio encoding invalid");
            }
        }
        AiProviderResult result = AiProviderResult.binary(bytes, "audio/mpeg");
        int durationSec = ceilSeconds(durationMs);
        result.setDurationSec(durationSec);
        result.setUsageAmount(usageAmount(request, 0, durationSec));
        return result;
    }

    private AiProviderResult downloadVideo(AiProviderRequest request, String url, int durationSec,
            int width, int height, int timeoutSec) throws Exception {
        requireHttps(url, "video url");
        byte[] bytes = http.getBytes(url, Math.min(180, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        result.setDurationSec(durationSec);
        result.setWidth(width);
        result.setHeight(height);
        result.setUsageAmount(usageAmount(request, 0, durationSec));
        return result;
    }

    private Map<String, Object> awaitMedia(String base, String apiKey, String model,
            Map<String, Object> submitted, Map<String, Object> cfg) throws Exception {
        if (hasMediaPayload(submitted)) {
            return submitted;
        }
        String taskId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("id")),
                ShortJson.text(submitted.get("task_id")), nestedText(submitted, "data", "id"),
                nestedText(submitted, "task", "id"));
        return pollMedia(base, apiKey, model, taskId, cfg);
    }

    private Map<String, Object> pollMedia(String base, String apiKey, String model, String taskId,
            Map<String, Object> cfg) throws Exception {
        if (taskId == null || taskId.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model submit missing task_id");
        }
        String query = base + "/v1/videos/generations/" + encodeQuery(taskId.trim())
                + "?model=" + encodeQuery(model);
        long intervalMs = Math.max(MEDIA_POLL_INTERVAL_MS, ProviderHttp.pollIntervalMs(cfg));
        long deadline = System.currentTimeMillis() + MEDIA_POLL_BUDGET_MS;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> resp = get(query, apiKey, 60);
            PollState state = pollState(resp, PollKind.MEDIA);
            if (state == PollState.SUCCEEDED) {
                return resp;
            }
            if (state == PollState.FAILED) {
                throw upstream("model task failed", resp);
            }
            sleeper.sleep(intervalMs);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "model task timeout");
    }

    private Map<String, Object> poll(String base, String apiKey, String taskPath, String taskId,
            int timeoutSec, long intervalMs, PollKind kind) throws Exception {
        if (taskId == null || taskId.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model submit missing task_id");
        }
        String query = base + taskPath.replace("{task_id}", taskId);
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> resp = get(query, apiKey, Math.min(60, timeoutSec));
            PollState state = pollState(resp, kind);
            if (state == PollState.SUCCEEDED) {
                return resp;
            }
            if (state == PollState.FAILED) {
                throw upstream("model task failed", resp);
            }
            sleeper.sleep(intervalMs);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "model task timeout");
    }

    private static PollState pollState(Map<String, Object> resp, PollKind kind) {
        return switch (kind) {
            case MEDIA -> {
                String status = ProviderHttp.firstNonBlank(ShortJson.text(resp.get("status")),
                        ShortJson.text(resp.get("task_status")), nestedText(resp, "task", "status"),
                        nestedText(resp, "data", "status"));
                PollState named = statusState(status);
                if (named == PollState.FAILED) {
                    yield PollState.FAILED;
                }
                if (named == PollState.SUCCEEDED && hasMediaPayload(resp)) {
                    yield PollState.SUCCEEDED;
                }
                yield PollState.RUNNING;
            }
            case MINIMAX_VIDEO -> statusState(ProviderHttp.firstNonBlank(
                    nestedText(resp, "task", "status"), ShortJson.text(resp.get("status"))));
            case PIXVERSE_VIDEO -> {
                assertZero(resp, "ErrCode", "ErrMsg", "pixverse query failed");
                int status = nestedInt(resp, "Resp", "status", 5);
                yield status == 1 ? PollState.SUCCEEDED : status == 5 ? PollState.RUNNING : PollState.FAILED;
            }
            case KLING_VIDEO -> {
                assertZero(resp, "code", "message", "kling query failed");
                Map<String, Object> item = firstMap(resp.get("data"));
                if (item.isEmpty()) {
                    item = child(resp, "data");
                }
                yield statusState(ShortJson.text(item.get("status")));
            }
            case ASR -> statusState(ProviderHttp.firstNonBlank(ShortJson.text(resp.get("status")),
                    nestedText(resp, "data", "status")));
        };
    }

    private static PollState statusState(String raw) {
        String status = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (List.of("succeeded", "success", "completed", "complete", "succeed").contains(status)) {
            return PollState.SUCCEEDED;
        }
        if (List.of("failed", "fail", "cancelled", "canceled", "deleted", "rejected").contains(status)) {
            return PollState.FAILED;
        }
        return PollState.RUNNING;
    }

    private Map<String, Object> post(String url, String apiKey, Map<String, Object> body, int timeoutSec)
            throws Exception {
        return ShortJson.map(http.postJson(url, generationHeaders(apiKey), ShortJson.toJson(body), timeoutSec));
    }

    private Map<String, Object> get(String url, String apiKey, int timeoutSec) throws Exception {
        return ShortJson.map(http.getJson(url, bearer(apiKey), timeoutSec));
    }

    private Map<String,String> generationHeaders(String key) {
        Map<String,String> headers=new LinkedHashMap<>(bearer(key));
        var receipt=activeReceipt.get();
        headers.put("X-Request-Id",receipt==null?java.util.UUID.randomUUID().toString():receipt.requestId);
        return headers;
    }

    private static Map<String, String> bearer(String key) {
        return Map.of("Authorization", "Bearer " + key);
    }

    private static String chatContent(Map<String, Object> resp) {
        Map<String, Object> first = firstMap(resp.get("choices"));
        return ProviderHttp.firstNonBlank(nestedText(first, "message", "content"),
                ShortJson.text(first.get("text")));
    }

    private static void assertBaseOk(Map<String, Object> resp, String operation) {
        Map<String, Object> base = child(resp, "base_resp");
        if (!base.isEmpty() && ShortJson.integer(base.get("status_code"), 0) != 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, ProviderHttp.firstNonBlank(
                    ShortJson.text(base.get("status_msg")), "model " + operation + " failed"));
        }
    }

    private static void assertZero(Map<String, Object> resp, String codeKey, String messageKey,
            String fallback) {
        Object code = resp.get(codeKey);
        if (code != null && ShortJson.integer(code, -1) != 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    ProviderHttp.firstNonBlank(ShortJson.text(resp.get(messageKey)), fallback));
        }
    }

    private static BizException upstream(String fallback, Map<String, Object> resp) {
        String message = ProviderHttp.firstNonBlank(
                errorText(resp.get("error")), errorText(child(resp, "data").get("error")),
                ShortJson.text(resp.get("message")), ShortJson.text(resp.get("msg")),
                ShortJson.text(resp.get("ErrMsg")), nestedText(resp, "base_resp", "status_msg"), fallback);
        return new BizException(CommonErrorCode.UPSTREAM_ERROR,
                TokenHubClientSupport.userFacingMessage(message));
    }

    private static String errorText(Object error) {
        if (error instanceof Map<?, ?> map) {
            return ProviderHttp.firstNonBlank(ShortJson.text(map.get("message")),
                    ShortJson.text(map.get("msg")), ShortJson.text(map.get("code")));
        }
        return ShortJson.text(error);
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Map<String, Object> child(Map<String, Object> source, String key) {
        Object raw = source == null ? null : source.get(key);
        if (!(raw instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private static Map<String, Object> firstMap(Object raw) {
        if (raw instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : first.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private static String nestedText(Map<String, Object> source, String first, String second) {
        return ShortJson.text(child(source, first).get(second));
    }

    private static int nestedInt(Map<String, Object> source, String first, String second, int fallback) {
        return ShortJson.integer(child(source, first).get(second), fallback);
    }

    private static String path(Map<String, Object> cfg, String key, String fallback) {
        String value = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get(key)), fallback);
        return value.startsWith("/") ? value : "/" + value;
    }

    private static int duration(AiProviderRequest request, Map<String, Object> cfg, int min, int max,
            int fallback) {
        int value = ShortJson.integer(request.param("duration_sec"),
                ShortJson.integer(cfg.get("duration"), fallback));
        return Math.max(min, Math.min(max, value));
    }

    private static long pollInterval(Map<String, Object> cfg, int fallback) {
        int value = ShortJson.integer(cfg.get("poll_interval_ms"), fallback);
        return Math.max(0, Math.min(10_000, value));
    }

    private static Map<String, Object> audioSetting() {
        return Map.of("sample_rate", 32000, "bitrate", 128000, "format", "mp3", "channel", 1);
    }

    private static int usageAmount(AiProviderRequest request, int tokens, int durationSec) {
        String unit = request.getModel().getPriceUnit();
        if ("PER_KILO_TOKEN".equals(unit) && tokens > 0) {
            return Math.max(1, (tokens + 999) / 1000);
        }
        if ("PER_SECOND".equals(unit) && durationSec > 0) {
            return durationSec;
        }
        if ("PER_MINUTE".equals(unit) && durationSec > 0) {
            return Math.max(1, (durationSec + 59) / 60);
        }
        return 1;
    }

    private static int ceilSeconds(int milliseconds) {
        return milliseconds <= 0 ? 0 : Math.max(1, (milliseconds + 999) / 1000);
    }

    private static double decimal(Object value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean bool(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    private static String normalizeVoice(String voiceId) {
        return switch (voiceId) {
            case "v-female-cold-01" -> "female-yujie";
            case "v-female-warm-01" -> "female-shaonv";
            case "v-male-deep-01" -> "male-qn-qingse";
            case "v-male-bright-01" -> "male-qn-jingying";
            case "v-narrator-01" -> "audiobook_male_1";
            default -> voiceId;
        };
    }

    private static void requireHttps(String value, String field) {
        try {
            URI uri = URI.create(value == null ? "" : value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, field + " must be https");
        }
    }

    private static byte[] decodeAudio(String value) {
        try {
            return decodeHex(value);
        } catch (IllegalArgumentException ex) {
            return Base64.getDecoder().decode(value);
        }
    }

    private static byte[] decodeHex(String value) {
        if (value == null || (value.length() & 1) == 1) {
            throw new IllegalArgumentException("invalid hex");
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("invalid hex");
            }
            bytes[i] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    private static String safe(Exception ex) {
        return TokenHubClientSupport.userFacingMessage(ex.getMessage());
    }

    private String hubBase(String configured, String hub) {
        String base = ProviderHttp.trimSlash(configured);
        if (isOwnHub(base)) {
            return base;
        }
        String injected = ProviderHttp.trimSlash(
                TokenHubClientSupport.HUB_GLOBAL.equals(hub) ? globalBaseUrl : cnBaseUrl);
        if (!isOwnHub(injected)) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "模型网关未配置");
        }
        return injected;
    }

    private static boolean isOwnHub(String base) {
        return TokenHubClientSupport.isOfficialHub(base);
    }

    private static String requireModel(AiProviderRequest request) {
        String model = request.getModel().getModelCode();
        if (model == null || model.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model_code required");
        }
        return model.trim();
    }

    private AiProviderResult mediaResult(AiProviderRequest request, Map<String, Object> resp, String mime,
            int durationSec, int width, int height, int timeoutSec) throws Exception {
        String b64 = mediaBase64(resp);
        if (!b64.isBlank()) {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException ex) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model media encoding invalid");
            }
            if (bytes.length == 0) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model media missing");
            }
            AiProviderResult result = AiProviderResult.binary(bytes, mime);
            result.setWidth(width);
            result.setHeight(height);
            if (durationSec > 0) {
                result.setDurationSec(durationSec);
            }
            result.setUsageAmount(usageAmount(request, 0, durationSec));
            return result;
        }
        String url = mediaUrl(resp);
        if (url.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model media missing");
        }
        if (mime.startsWith("video/")) {
            return downloadVideo(request, url, durationSec, width, height, timeoutSec);
        }
        requireHttps(url, "image url");
        byte[] bytes = http.getBytes(url, Math.min(120, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, mime);
        result.setWidth(width);
        result.setHeight(height);
        result.setUsageAmount(usageAmount(request, 0, 0));
        return result;
    }

    private static boolean hasMediaPayload(Map<String, Object> resp) {
        return resp != null && (!mediaBase64(resp).isBlank() || !mediaUrl(resp).isBlank());
    }

    private static String mediaBase64(Map<String, Object> resp) {
        Map<String, Object> item = firstMediaItem(resp);
        return ProviderHttp.firstNonBlank(ShortJson.text(item.get("b64_json")),
                ShortJson.text(item.get("b64")), ShortJson.text(resp.get("b64_json")));
    }

    private static String mediaUrl(Map<String, Object> resp) {
        Map<String, Object> item = firstMediaItem(resp);
        String direct = ProviderHttp.firstNonBlank(ShortJson.text(item.get("url")),
                nestedText(item, "image_url", "url"), ShortJson.text(resp.get("url")),
                nestedText(child(resp, "task"), "content", "url"), nestedText(resp, "data", "url"));
        if (!direct.isBlank()) {
            return direct;
        }
        Map<String, Object> output = firstMap(item.get("outputs"));
        if (output.isEmpty()) {
            output = firstMap(child(resp, "data").get("outputs"));
        }
        return ShortJson.text(output.get("url"));
    }

    private static Map<String, Object> firstMediaItem(Map<String, Object> resp) {
        Map<String, Object> fromData = firstMap(resp == null ? null : resp.get("data"));
        if (!fromData.isEmpty()) {
            return fromData;
        }
        return firstMap(resp == null ? null : resp.get("images"));
    }

    private enum PollKind {
        MEDIA, MINIMAX_VIDEO, PIXVERSE_VIDEO, KLING_VIDEO, ASR
    }

    private enum PollState {
        RUNNING, SUCCEEDED, FAILED
    }
}
