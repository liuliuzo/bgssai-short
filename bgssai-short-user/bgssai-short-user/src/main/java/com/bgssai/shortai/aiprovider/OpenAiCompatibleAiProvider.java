package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * provider_code=OPENAI_COMPAT — chat completions + best-effort image/voice paths from config_json.
 */
@Component
public class OpenAiCompatibleAiProvider implements AiProvider {

    public static final String CODE = "OPENAI_COMPAT";

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAiProvider.class);

    private final SysSecretReader secretReader;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public OpenAiCompatibleAiProvider(SysSecretReader secretReader) {
        this.secretReader = secretReader;
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
        String baseUrl = trimSlash(ShortJson.text(cfg.get("base_url")));
        if (baseUrl.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "config_json.base_url required");
        }
        String secretRef = ShortJson.text(cfg.get("secret_ref"));
        if (secretRef.isBlank()) {
            secretRef = SysSecretReader.AI_OPENAI_COMPAT_API_KEY;
        }
        String apiKey = secretReader.get(secretRef);
        if (apiKey.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "AI api key missing for " + secretRef);
        }
        int timeoutSec = request.getModel().getTimeoutSec() == null ? 60 : request.getModel().getTimeoutSec();
        String ability = request.getAbilityType();
        try {
            if ("TEXT".equals(ability)) {
                return chat(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            if ("IMAGE".equals(ability) || "VIDEO".equals(ability)) {
                return image(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            if ("VOICE".equals(ability)) {
                return voice(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "OPENAI_COMPAT unsupported ability: " + ability);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("OPENAI_COMPAT call failed ability={} model={}", ability, request.getModel().getModelCode(), ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, ex.getMessage());
        }
    }

    private AiProviderResult chat(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = ShortJson.text(cfg.get("chat_path"));
        if (path.isBlank()) {
            path = "/chat/completions";
        }
        String prompt = request.getPrompt();
        if (prompt == null || prompt.isBlank()) {
            prompt = ShortJson.toJson(request.getParams());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        String raw = postJson(baseUrl + path, apiKey, ShortJson.toJson(body), timeoutSec, cfg);
        Map<String, Object> resp = ShortJson.map(raw);
        String content = extractChatContent(resp);
        if (content == null || content.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "empty chat content");
        }
        return AiProviderResult.text(content);
    }

    private AiProviderResult image(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = ShortJson.text(cfg.get("image_path"));
        if (path.isBlank()) {
            path = "/images/generations";
        }
        String prompt = request.getPrompt();
        if (prompt == null || prompt.isBlank()) {
            prompt = ShortJson.text(request.getParams().get("title")) + " "
                    + ShortJson.text(request.getParams().get("subtitle"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("prompt", prompt.trim());
        body.put("n", 1);
        String raw = postJson(baseUrl + path, apiKey, ShortJson.toJson(body), timeoutSec, cfg);
        Map<String, Object> resp = ShortJson.map(raw);
        Object data = resp.get("data");
        if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object b64 = first.get("b64_json");
            if (b64 != null) {
                byte[] bytes = Base64.getDecoder().decode(String.valueOf(b64));
                return AiProviderResult.binary(bytes, "image/png");
            }
            Object url = first.get("url");
            if (url != null) {
                byte[] bytes = httpClient.send(
                        HttpRequest.newBuilder(URI.create(String.valueOf(url))).GET().timeout(Duration.ofSeconds(timeoutSec)).build(),
                        HttpResponse.BodyHandlers.ofByteArray()).body();
                return AiProviderResult.binary(bytes, "image/png");
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "image generation response missing data");
    }

    private AiProviderResult voice(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = ShortJson.text(cfg.get("audio_path"));
        if (path.isBlank()) {
            path = "/audio/speech";
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("input", request.getPrompt() == null ? "" : request.getPrompt());
        body.put("voice", ShortJson.text(request.getParams().getOrDefault("voice", "alloy")));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ShortJson.toJson(body), StandardCharsets.UTF_8));
        applyExtraHeaders(builder, cfg);
        HttpResponse<byte[]> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "audio status " + resp.statusCode());
        }
        return AiProviderResult.binary(resp.body(), "audio/mpeg");
    }

    private String postJson(String url, String apiKey, String json, int timeoutSec, Map<String, Object> cfg)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        applyExtraHeaders(builder, cfg);
        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "status " + resp.statusCode());
        }
        return resp.body();
    }

    @SuppressWarnings("unchecked")
    private static void applyExtraHeaders(HttpRequest.Builder builder, Map<String, Object> cfg) {
        Object extra = cfg.get("extra_headers");
        if (extra instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    builder.header(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractChatContent(Map<String, Object> resp) {
        Object choices = resp.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object message = choice.get("message");
        if (message instanceof Map<?, ?> msg) {
            Object content = msg.get("content");
            return content == null ? null : String.valueOf(content);
        }
        Object text = choice.get("text");
        return text == null ? null : String.valueOf(text);
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
}
