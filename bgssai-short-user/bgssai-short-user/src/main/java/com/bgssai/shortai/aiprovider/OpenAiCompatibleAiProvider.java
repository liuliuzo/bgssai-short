package com.bgssai.shortai.aiprovider;

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
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * provider_code=OPENAI_COMPAT. Runs only against the company host
 * {@code https://chat.bgssai.com}. Any other host is rejected so billed
 * third-party calls stay on the CN hub.
 */
@Component
public class OpenAiCompatibleAiProvider implements AiProvider {

    public static final String CODE = "OPENAI_COMPAT";

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAiProvider.class);
    private static final long MEDIA_POLL_FLOOR_MS = 180_000L;

    private final SysSecretReader secretReader;
    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;

    @Autowired
    public OpenAiCompatibleAiProvider(SysSecretReader secretReader) {
        this(secretReader, new ProviderHttp.JdkClient(), Thread::sleep);
    }

    OpenAiCompatibleAiProvider(SysSecretReader secretReader, ProviderHttp.Client http,
            ProviderHttp.Sleeper sleeper) {
        this.secretReader = secretReader;
        this.http = http;
        this.sleeper = sleeper;
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
        String baseUrl = ProviderHttp.trimSlash(ShortJson.text(cfg.get("base_url")));
        if (!CompanyCompatGateway.allows(baseUrl)) {
            throw DirectVendorGuard.rejected(CODE);
        }
        String secretRef = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("secret_ref")),
                SysSecretReader.AI_OPENAI_COMPAT_API_KEY);
        String apiKey = secretReader.get(secretRef);
        if (apiKey.isBlank()) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    TokenHubClientSupport.userFacingMessage("AI api key missing for " + secretRef));
        }
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);
        String ability = request.getAbilityType() == null ? "" : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        try {
            if ("TEXT".equals(ability)) {
                return chat(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            if ("IMAGE".equals(ability)) {
                return image(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            if ("VIDEO".equals(ability)) {
                return video(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            if ("VOICE".equals(ability)) {
                return voice(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "unsupported ability");
        } catch (BizException ex) {
            throw new BizException(ex.getCode(), TokenHubClientSupport.userFacingMessage(ex.getMessage()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT,
                    TokenHubClientSupport.userFacingMessage("timeout"));
        } catch (Exception ex) {
            log.warn("company gateway call failed ability={} model={}", ability,
                    request.getModel().getModelCode());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    TokenHubClientSupport.userFacingMessage(ex.getMessage()));
        }
    }

    private AiProviderResult chat(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("chat_path")), "/chat/completions");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
        String raw = http.postJson(join(baseUrl, path), headers(apiKey, cfg), ShortJson.toJson(body), timeoutSec);
        String content = extractChatContent(ShortJson.map(raw));
        if (content == null || content.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    TokenHubClientSupport.userFacingMessage("empty chat content"));
        }
        return AiProviderResult.text(content);
    }

    private AiProviderResult image(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("image_path")), "/images/generations");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("n", 1);
        body.put("size", ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("size")), "512x768"));
        String raw = http.postJson(join(baseUrl, path), headers(apiKey, cfg), ShortJson.toJson(body),
                Math.min(180, timeoutSec));
        Map<String, Object> submitted = ShortJson.map(raw);
        if (!hasMedia(submitted)) {
            String taskId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("id")),
                    ShortJson.text(submitted.get("task_id")));
            if (!taskId.isBlank()) {
                submitted = awaitMedia(baseUrl, apiKey, request.getModel().getModelCode(), submitted, cfg, timeoutSec);
            }
        }
        return mediaBytes(submitted, apiKey, timeoutSec, "image/png");
    }

    private AiProviderResult video(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("video_path")), "/videos/generations");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("prompt", ProviderHttp.promptOf(request));
        String size = ShortJson.text(cfg.get("size"));
        if (!size.isBlank()) {
            body.put("size", size);
        }
        String image = ProviderHttp.imagePayload(request);
        if (!image.isBlank()) {
            body.put("image", image);
        }
        String last = ProviderHttp.lastImagePayload(request);
        if (!last.isBlank()) {
            body.put("last_image", last);
        }
        Map<String, Object> submitted = ShortJson.map(http.postJson(join(baseUrl, path), headers(apiKey, cfg),
                ShortJson.toJson(body), Math.min(60, timeoutSec)));
        Map<String, Object> done = awaitMedia(baseUrl, apiKey, request.getModel().getModelCode(), submitted, cfg, timeoutSec);
        AiProviderResult result = mediaBytes(done, apiKey, timeoutSec, "video/mp4");
        if (result.getDurationSec() == null || result.getDurationSec() <= 0) {
            int duration = ShortJson.integer(request.param("duration_sec"), 0);
            if (duration > 0) {
                result.setDurationSec(duration);
            }
        }
        return result;
    }

    private AiProviderResult voice(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("speech_path")),
                ShortJson.text(cfg.get("audio_path")), "/audio/speech");
        String voice = ProviderHttp.firstNonBlank(ShortJson.text(request.param("voice_id")),
                ShortJson.text(request.param("voice")), ShortJson.text(cfg.get("default_voice_id")), "zh");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("input", ProviderHttp.promptOf(request));
        body.put("voice", voice);
        byte[] raw = http.postBytes(join(baseUrl, path), headers(apiKey, cfg), ShortJson.toJson(body),
                Math.min(120, timeoutSec));
        if (raw == null || raw.length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    TokenHubClientSupport.userFacingMessage("model audio missing"));
        }
        String text = new String(raw, StandardCharsets.UTF_8).trim();
        if (text.startsWith("{")) {
            return mediaBytes(ShortJson.map(text), apiKey, timeoutSec, "audio/mpeg");
        }
        String mime = raw.length >= 4 && raw[0] == 'R' && raw[1] == 'I' && raw[2] == 'F' && raw[3] == 'F'
                ? "audio/wav" : "audio/mpeg";
        return AiProviderResult.binary(raw, mime);
    }

    private Map<String, Object> awaitMedia(String baseUrl, String apiKey, String model,
            Map<String, Object> submitted, Map<String, Object> cfg, int timeoutSec) throws Exception {
        if (hasMedia(submitted) && !failed(statusOf(submitted))) {
            return submitted;
        }
        String taskId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("id")),
                ShortJson.text(submitted.get("task_id")));
        if (taskId.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    TokenHubClientSupport.userFacingMessage("model submit missing task_id"));
        }
        String template = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("video_task_path")),
                ShortJson.text(cfg.get("task_path")), "/videos/generations/{task_id}");
        String encoded = URLEncoder.encode(taskId.trim(), StandardCharsets.UTF_8);
        String path = template.replace("{task_id}", encoded).replace("{id}", encoded);
        String url = join(baseUrl, path);
        String modelQuery = "model=" + URLEncoder.encode(model == null ? "" : model, StandardCharsets.UTF_8);
        url = url.contains("?") ? url + "&" + modelQuery : url + "?" + modelQuery;
        long interval = ProviderHttp.pollIntervalMs(cfg);
        long deadline = System.currentTimeMillis() + Math.max(MEDIA_POLL_FLOOR_MS, timeoutSec * 1000L);
        Map<String, String> headers = headers(apiKey, cfg);
        while (true) {
            Map<String, Object> resp = ShortJson.map(http.getJson(url, headers, 60));
            String status = statusOf(resp);
            if (failed(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        TokenHubClientSupport.userFacingMessage("model task failed"));
            }
            if (hasMedia(resp) && (status.isBlank() || success(status))) {
                return resp;
            }
            if (success(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        TokenHubClientSupport.userFacingMessage("model media missing"));
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT,
                        TokenHubClientSupport.userFacingMessage("model task timeout"));
            }
            sleeper.sleep(interval);
        }
    }

    private AiProviderResult mediaBytes(Map<String, Object> resp, String apiKey, int timeoutSec, String mime)
            throws Exception {
        String b64 = firstB64(resp);
        if (!b64.isBlank()) {
            return AiProviderResult.binary(decode(b64), mime);
        }
        String url = firstUrl(resp);
        if (url.startsWith("https://")) {
            byte[] bytes = http.getBytes(url, Math.min(180, timeoutSec));
            if (bytes == null || bytes.length == 0) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        TokenHubClientSupport.userFacingMessage("model media missing"));
            }
            return AiProviderResult.binary(bytes, mime);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                TokenHubClientSupport.userFacingMessage("model media missing"));
    }

    private static byte[] decode(String value) {
        String payload = value.trim();
        int comma = payload.indexOf(',');
        if (payload.startsWith("data:") && comma >= 0) {
            payload = payload.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    TokenHubClientSupport.userFacingMessage("model media missing"));
        }
    }

    private static boolean hasMedia(Map<String, Object> resp) {
        return resp != null && (!firstB64(resp).isBlank() || firstUrl(resp).startsWith("https://"));
    }

    @SuppressWarnings("unchecked")
    private static String firstB64(Map<String, Object> resp) {
        if (resp == null) {
            return "";
        }
        String top = ShortJson.text(resp.get("b64_json"));
        if (!top.isBlank()) {
            return top;
        }
        String audio = ShortJson.text(resp.get("audio"));
        if (!audio.isBlank() && !audio.startsWith("https://")) {
            return audio;
        }
        Object data = resp.get("data");
        if (data instanceof Map<?, ?> map) {
            String nested = ProviderHttp.firstNonBlank(ShortJson.text(map.get("b64_json")),
                    ShortJson.text(map.get("audio")));
            if (!nested.isBlank() && !nested.startsWith("https://")) {
                return nested;
            }
        }
        if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return ProviderHttp.firstNonBlank(ShortJson.text(first.get("b64_json")),
                    ShortJson.text(first.get("audio")));
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static String firstUrl(Map<String, Object> resp) {
        if (resp == null) {
            return "";
        }
        String top = ProviderHttp.firstNonBlank(ShortJson.text(resp.get("url")), ShortJson.text(resp.get("audio")));
        if (top.startsWith("https://")) {
            return top;
        }
        Object data = resp.get("data");
        if (data instanceof Map<?, ?> map) {
            String nested = ProviderHttp.firstNonBlank(ShortJson.text(map.get("url")), ShortJson.text(map.get("audio")));
            if (nested.startsWith("https://")) {
                return nested;
            }
        }
        if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return ProviderHttp.firstNonBlank(ShortJson.text(first.get("url")), ShortJson.text(first.get("audio")));
        }
        return "";
    }

    private static String statusOf(Map<String, Object> resp) {
        if (resp == null) {
            return "";
        }
        return ShortJson.text(resp.get("status")).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean success(String status) {
        return "succeeded".equals(status) || "success".equals(status) || "completed".equals(status)
                || "complete".equals(status);
    }

    private static boolean failed(String status) {
        return "failed".equals(status) || "failure".equals(status) || "error".equals(status)
                || "cancelled".equals(status) || "canceled".equals(status);
    }

    private static String extractChatContent(Map<String, Object> resp) {
        Object choices = resp.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> choice)) {
            return null;
        }
        Object message = choice.get("message");
        if (message instanceof Map<?, ?> msg && msg.get("content") != null) {
            return String.valueOf(msg.get("content"));
        }
        Object text = choice.get("text");
        return text == null ? null : String.valueOf(text);
    }

    private static Map<String, String> headers(String apiKey, Map<String, Object> cfg) {
        Map<String, String> headers = new LinkedHashMap<>();
        Object extra = cfg.get("extra_headers");
        if (extra instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    String name = String.valueOf(entry.getKey());
                    if (!"authorization".equalsIgnoreCase(name)) {
                        headers.put(name, String.valueOf(entry.getValue()));
                    }
                }
            }
        }
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    private static String join(String base, String path) {
        String value = path == null ? "" : path.trim();
        if (value.isEmpty()) {
            return base;
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return base + value;
    }
}
