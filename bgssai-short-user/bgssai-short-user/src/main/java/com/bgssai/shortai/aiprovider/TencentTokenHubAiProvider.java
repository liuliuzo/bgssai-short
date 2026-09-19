package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.util.ArrayList;
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
 * provider_code=TENCENT_TOKENHUB. Dual BGSSAI hubs (CN + Global) front TEXT /
 * VIDEO / VOICE / ASR / MUSIC; keys live in sys_secret only.
 */
@Component
public class TencentTokenHubAiProvider implements AiProvider {

    public static final String CODE = "TENCENT_TOKENHUB";

    private static final String CHAT_PATH = "/v1/chat/completions";
    private static final String MINIMAX_VIDEO_SUBMIT = "/v1/wand/minimax-video-v2/generation";
    private static final String MINIMAX_VIDEO_TASK = "/v1/wand/minimax-video-v2/tasks/{task_id}";
    private static final String PIXVERSE_TEXT_VIDEO = "/v1/wand/pixverse/text-to-video";
    private static final String PIXVERSE_IMAGE_VIDEO = "/v1/wand/pixverse/image-to-video";
    private static final String PIXVERSE_TASK = "/v1/wand/pixverse/tasks/{task_id}";
    private static final String KLING_TEXT_VIDEO = "/v1/wand/kling/text-to-video";
    private static final String KLING_IMAGE_VIDEO = "/v1/wand/kling/image-to-video";
    private static final String KLING_TASK = "/v1/wand/kling/tasks/{task_id}";
    private static final String SPEECH_PATH = "/v1/wand/minimax-tts/sync_tts";
    private static final String ASR_SUBMIT = "/v1/wand/asrproxy/async_create";
    private static final String ASR_TASK = "/v1/wand/asrproxy/tasks/{task_id}";
    private static final String MUSIC_PATH = "/v1/wand/minimax-music/generation";

    private static final Logger log = LoggerFactory.getLogger(TencentTokenHubAiProvider.class);

    private final SysSecretReader secretReader;
    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;
    private final String cnBaseUrl;
    private final String globalBaseUrl;

    @Autowired
    public TencentTokenHubAiProvider(SysSecretReader secretReader,
            @Value("${bgssai.llm.tokenhub-cn-base-url:${bgssai.llm.gateway-base-url:https://tokenhub-cn.bgssai.com}}")
                    String cnBaseUrl,
            @Value("${bgssai.llm.tokenhub-global-base-url:https://tokenhub-global.bgssai.com}")
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
        String base = ProviderHttp.trimSlash(ProviderHttp.firstNonBlank(
                ShortJson.text(cfg.get("base_url")),
                TokenHubClientSupport.defaultBaseUrl(hub, cnBaseUrl, globalBaseUrl)));
        String ref = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("secret_ref")),
                TokenHubClientSupport.defaultSecretRef(hub));
        String apiKey = TokenHubClientSupport.resolveApiKey(secretReader, hub, ref);
        String ability = request.getAbilityType() == null ? "" : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        if (apiKey.isBlank()) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    TokenHubClientSupport.userFacingMessage("model credential unavailable"));
        }
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);
        try {
            return switch (ability) {
                case "TEXT" -> text(base, apiKey, cfg, request, timeoutSec);
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
        }
    }

    private AiProviderResult text(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
        body.put("stream", false);
        String path = path(cfg, "chat_path", CHAT_PATH);
        Map<String, Object> resp = post(base + path, apiKey, body, Math.min(120, timeoutSec));
        String content = chatContent(resp);
        if (content.isBlank()) {
            throw upstream("model text content missing", resp);
        }
        AiProviderResult result = AiProviderResult.text(content);
        int tokens = nestedInt(resp, "usage", "total_tokens", 0);
        result.setUsageAmount(usageAmount(request, tokens, 0));
        return result;
    }

    private AiProviderResult video(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String model = request.getModel().getModelCode();
        if (model == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model_code required");
        }
        if (model.startsWith("minimax-video-")) {
            return minimaxVideo(base, apiKey, cfg, request, timeoutSec);
        }
        if (model.startsWith("pixverse-video-")) {
            return pixverseVideo(base, apiKey, cfg, request, timeoutSec);
        }
        if (model.startsWith("kling-video-")) {
            return klingVideo(base, apiKey, cfg, request, timeoutSec);
        }
        throw new BizException(CommonErrorCode.RULE_REJECT, "unsupported video model: " + model);
    }

    private AiProviderResult minimaxVideo(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String image = ProviderHttp.imagePayload(request);
        String lastImage = ProviderHttp.lastImagePayload(request);
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", ProviderHttp.promptOf(request)));
        if (!image.isBlank()) {
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("type", "image_url");
            frame.put("image_url", Map.of("url", image));
            frame.put("role", "first_frame");
            content.add(frame);
        }
        if (!lastImage.isBlank()) {
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("type", "image_url");
            frame.put("image_url", Map.of("url", lastImage));
            frame.put("role", "last_frame");
            content.add(frame);
        }
        int duration = duration(request, cfg, 4, 15, 6);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("content", content);
        body.put("resolution", ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("resolution")), "768P"));
        body.put("duration", duration);
        body.put("ratio", image.isBlank() ? ProviderHttp.aspectRatio(cfg, request) : "adaptive");
        Map<String, Object> submitted = post(base + path(cfg, "submit_path", MINIMAX_VIDEO_SUBMIT),
                apiKey, body, Math.min(60, timeoutSec));
        String taskId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("task_id")),
                nestedText(submitted, "task", "id"));
        Map<String, Object> done = poll(base, apiKey, path(cfg, "task_path", MINIMAX_VIDEO_TASK), taskId,
                timeoutSec, ProviderHttp.pollIntervalMs(cfg), PollKind.MINIMAX_VIDEO);
        Map<String, Object> task = child(done, "task");
        String url = ProviderHttp.firstNonBlank(nestedText(task, "content", "url"),
                ShortJson.text(task.get("url")), ShortJson.text(done.get("url")));
        int actualDuration = ShortJson.integer(task.get("duration"), duration);
        return downloadVideo(request, url, actualDuration, 1080, 1920, timeoutSec);
    }

    private AiProviderResult pixverseVideo(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String image = ProviderHttp.imagePayload(request);
        int duration = duration(request, cfg, 1, 15, 5);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("duration", duration);
        body.put("quality", ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("quality")), "720p"));
        body.put("generate_audio_switch", false);
        String submitPath;
        if (image.isBlank()) {
            submitPath = path(cfg, "text2video_path", PIXVERSE_TEXT_VIDEO);
            body.put("aspect_ratio", ProviderHttp.aspectRatio(cfg, request));
        } else {
            submitPath = path(cfg, "image2video_path", PIXVERSE_IMAGE_VIDEO);
            body.put("img_id", image);
        }
        Map<String, Object> submitted = post(base + submitPath, apiKey, body, Math.min(60, timeoutSec));
        assertZero(submitted, "ErrCode", "ErrMsg", "pixverse submit failed");
        String taskId = nestedText(submitted, "Resp", "video_id");
        Map<String, Object> done = poll(base, apiKey, path(cfg, "task_path", PIXVERSE_TASK), taskId,
                timeoutSec, ProviderHttp.pollIntervalMs(cfg), PollKind.PIXVERSE_VIDEO);
        Map<String, Object> resp = child(done, "Resp");
        String url = ShortJson.text(resp.get("url"));
        int width = ShortJson.integer(resp.get("outputWidth"), 1080);
        int height = ShortJson.integer(resp.get("outputHeight"), 1920);
        return downloadVideo(request, url, duration, width, height, timeoutSec);
    }

    private AiProviderResult klingVideo(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String image = ProviderHttp.imagePayload(request);
        int duration = duration(request, cfg, 5, 15, 5);
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("resolution", ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("resolution")), "720p"));
        settings.put("duration", duration);
        settings.put("multi_shot", false);
        settings.put("audio", false);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        String submitPath;
        if (image.isBlank()) {
            submitPath = path(cfg, "text2video_path", KLING_TEXT_VIDEO);
            body.put("prompt", ProviderHttp.promptOf(request));
            settings.put("aspect_ratio", ProviderHttp.aspectRatio(cfg, request));
        } else {
            submitPath = path(cfg, "image2video_path", KLING_IMAGE_VIDEO);
            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(Map.of("type", "prompt", "text", ProviderHttp.promptOf(request)));
            contents.add(Map.of("type", "first_frame", "url", image));
            String klingLastImage = ProviderHttp.lastImagePayload(request);
            if (!klingLastImage.isBlank()) {
                contents.add(Map.of("type", "last_frame", "url", klingLastImage));
            }
            body.put("contents", contents);
        }
        body.put("settings", settings);
        Map<String, Object> submitted = post(base + submitPath, apiKey, body, Math.min(60, timeoutSec));
        assertZero(submitted, "code", "message", "kling submit failed");
        String taskId = nestedText(submitted, "data", "id");
        Map<String, Object> done = poll(base, apiKey, path(cfg, "task_path", KLING_TASK), taskId,
                timeoutSec, ProviderHttp.pollIntervalMs(cfg), PollKind.KLING_VIDEO);
        Map<String, Object> item = firstMap(done.get("data"));
        if (item.isEmpty()) {
            item = child(done, "data");
        }
        Map<String, Object> output = firstMap(item.get("outputs"));
        String url = ShortJson.text(output.get("url"));
        int actualDuration = (int) Math.ceil(decimal(output.get("duration"), duration));
        return downloadVideo(request, url, actualDuration, 1080, 1920, timeoutSec);
    }

    private AiProviderResult speech(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> voice = new LinkedHashMap<>();
        voice.put("voice_id", normalizeVoice(ProviderHttp.firstNonBlank(ShortJson.text(request.param("voice_id")),
                ShortJson.text(request.param("voice")), ShortJson.text(cfg.get("default_voice_id")), "male-qn-qingse")));
        voice.put("speed", decimal(request.param("speed"), 1.0));
        voice.put("vol", decimal(request.param("volume"), 1.0));
        voice.put("pitch", ShortJson.integer(request.param("pitch"), 0));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("text", ProviderHttp.promptOf(request));
        body.put("voice_setting", voice);
        body.put("audio_setting", audioSetting());
        body.put("subtitle_enable", false);
        body.put("output_format", "url");
        Map<String, Object> resp = post(base + path(cfg, "speech_path", SPEECH_PATH), apiKey, body,
                Math.min(120, timeoutSec));
        assertBaseOk(resp, "speech");
        Map<String, Object> data = child(resp, "data");
        int durationMs = nestedInt(resp, "extra_info", "audio_length", 0);
        return audioResult(request, ShortJson.text(data.get("audio")), durationMs, timeoutSec);
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
                bytes = value.startsWith("data:") ? Base64.getDecoder().decode(hex) : decodeHex(hex);
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
        return ShortJson.map(http.postJson(url, bearer(apiKey), ShortJson.toJson(body), timeoutSec));
    }

    private Map<String, Object> get(String url, String apiKey, int timeoutSec) throws Exception {
        return ShortJson.map(http.getJson(url, bearer(apiKey), timeoutSec));
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
                ShortJson.text(resp.get("message")), ShortJson.text(resp.get("msg")),
                ShortJson.text(resp.get("ErrMsg")), nestedText(resp, "base_resp", "status_msg"), fallback);
        return new BizException(CommonErrorCode.UPSTREAM_ERROR,
                TokenHubClientSupport.userFacingMessage(message));
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

    private enum PollKind {
        MINIMAX_VIDEO, PIXVERSE_VIDEO, KLING_VIDEO, ASR
    }

    private enum PollState {
        RUNNING, SUCCEEDED, FAILED
    }
}
