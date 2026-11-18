package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

/**
 * provider_code=OPENAI. Official OpenAI APIs for a user-owned key:
 * TEXT {@code POST /v1/chat/completions}, IMAGE {@code POST /v1/images/generations}
 * or {@code POST /v1/images/edits}, ASR {@code POST /v1/audio/transcriptions}.
 */
@Component
public class OpenAiAiProvider implements AiProvider {

    public static final String CODE = "OPENAI";
    public static final String DEFAULT_TEXT_MODEL = "gpt-6-luna";
    public static final String DEFAULT_IMAGE_MODEL = "gpt-image-2.6-sunburst";
    public static final String DEFAULT_ASR_MODEL = "gpt-transcribe";

    private static final Logger log = LoggerFactory.getLogger(OpenAiAiProvider.class);

    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;

    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;

    private final ProviderHttp.Client http;

    public OpenAiAiProvider() {
        this.http = new ProviderHttp.JdkClient();
    }

    OpenAiAiProvider(UserAiModelKeyService userAiModelKeyService, ProviderHttp.Client http) {
        this.userAiModelKeyService = userAiModelKeyService;
        this.http = http;
    }

    void setUserAiProviderKeyService(UserAiProviderKeyService userAiProviderKeyService) {
        this.userAiProviderKeyService = userAiProviderKeyService;
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
        OwnedCredential cred = resolveCredential(request);
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        String modelCode = modelCode(request, ability);
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);
        log.info("调用 OpenAI 模型 userId={} keyId={} ability={} model={}",
                cred.userId(), cred.keyId(), ability, modelCode);
        try {
            return switch (ability) {
                case "TEXT" -> chat(cred, modelCode, request, timeoutSec);
                case "IMAGE" -> image(cred, modelCode, request, timeoutSec);
                case "ASR" -> transcribe(cred, modelCode, request, timeoutSec);
                case "VIDEO", "VOICE", "MUSIC" -> throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                        "OpenAI 当前仅支持文本、图片和语音识别");
                default -> throw new BizException(CommonErrorCode.RULE_REJECT, "unsupported ability: " + ability);
            };
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "OpenAI 模型调用被中断，请稍后重试");
        } catch (Exception ex) {
            log.warn("OpenAI 模型调用异常 userId={} keyId={} ability={} model={} errType={}",
                    cred.userId(), cred.keyId(), ability, modelCode, ex.getClass().getSimpleName());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "OpenAI 模型请求失败，请检查密钥与网络后重试");
        }
    }

    private AiProviderResult chat(OwnedCredential cred, String modelCode, AiProviderRequest request, int timeoutSec)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
        body.put("stream", false);
        String raw = http.postJson(cred.baseUrl() + "/chat/completions", bearer(cred.apiKey()),
                ShortJson.toJson(body), Math.min(180, timeoutSec));
        Map<String, Object> parsed = ShortJson.map(raw);
        String content = UserProviderMediaSupport.extractChatContent(parsed);
        if (content.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "OpenAI 文本返回内容为空");
        }
        AiProviderResult result = AiProviderResult.text(content);
        int tokens = UserProviderMediaSupport.nestedInt(parsed, "usage", "total_tokens", 1);
        result.setUsageAmount(Math.max(1, tokens));
        return result;
    }

    private AiProviderResult image(OwnedCredential cred, String modelCode, AiProviderRequest request, int timeoutSec)
            throws Exception {
        byte[] reference = UserProviderMediaSupport.resolveImageBytes(request, http, false, timeoutSec);
        int budget = Math.max(180, timeoutSec);
        String raw;
        if (reference.length > 0) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("model", modelCode);
            fields.put("prompt", ProviderHttp.promptOf(request));
            fields.put("n", "1");
            String mime = UserProviderMediaSupport.guessImageMime(ShortJson.text(request.param("image_url")));
            String filename = mime.contains("jpeg") ? "image.jpg" : "image.png";
            raw = http.postMultipartField(cred.baseUrl() + "/images/edits", bearer(cred.apiKey()),
                    "image", reference, filename, mime, fields, budget);
        } else {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelCode);
            body.put("prompt", ProviderHttp.promptOf(request));
            body.put("n", 1);
            if (modelCode.startsWith("dall-e")) {
                body.put("response_format", "b64_json");
                body.put("size", "dall-e-2".equals(modelCode) ? "1024x1024" : "1024x1792");
            }
            raw = http.postJson(cred.baseUrl() + "/images/generations", bearer(cred.apiKey()),
                    ShortJson.toJson(body), budget);
        }
        return imageBytes(ShortJson.map(raw), timeoutSec);
    }

    private AiProviderResult transcribe(OwnedCredential cred, String modelCode, AiProviderRequest request,
            int timeoutSec) throws Exception {
        byte[] audio = UserProviderMediaSupport.audioPayload(request, http, timeoutSec);
        if (audio.length == 0) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "audio input required");
        }
        String mime = ProviderHttp.firstNonBlank(ShortJson.text(request.param("audio_mime")),
                UserProviderMediaSupport.guessAudioMime(audio));
        String filename = mime.contains("wav") ? "audio.wav" : "audio.mp3";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", modelCode);
        String language = ProviderHttp.firstNonBlank(ShortJson.text(request.param("language")), "zh");
        if (!language.isBlank()) {
            fields.put("language", language);
        }
        String raw = http.postMultipart(cred.baseUrl() + "/audio/transcriptions", bearer(cred.apiKey()),
                audio, filename, mime, fields, Math.min(300, Math.max(60, timeoutSec)));
        Map<String, Object> parsed = new LinkedHashMap<>(ShortJson.map(raw));
        if (parsed.isEmpty() && raw != null && !raw.isBlank() && !raw.trim().startsWith("{")) {
            parsed.put("text", raw.trim());
        }
        UserProviderMediaSupport.normalizeSttSentences(parsed);
        if (ShortJson.text(parsed.get("text")).isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "OpenAI 识别返回内容为空");
        }
        return AiProviderResult.text(ShortJson.toJson(parsed));
    }

    private AiProviderResult imageBytes(Map<String, Object> parsed, int timeoutSec) throws Exception {
        Object data = parsed.get("data");
        if (!(data instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> first)) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "OpenAI 图片返回内容为空");
        }
        String b64 = ShortJson.text(first.get("b64_json"));
        if (!b64.isBlank()) {
            return AiProviderResult.binary(UserProviderMediaSupport.decodeBase64(b64), "image/png");
        }
        String url = ShortJson.text(first.get("url"));
        if (url.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "OpenAI 图片返回内容为空");
        }
        UserAiModelSupport.validatePublicHttpsUrl(url, "image_url");
        byte[] bytes = http.getBytes(url, Math.min(120, timeoutSec));
        if (bytes == null || bytes.length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "OpenAI 图片返回内容为空");
        }
        return AiProviderResult.binary(bytes, UserProviderMediaSupport.guessImageMime(url));
    }

    private OwnedCredential resolveCredential(AiProviderRequest request) {
        Optional<ResolvedProviderKey> providerByok = DirectVendorGuard.isExplicitUserModelKey(request.getModel())
                ? Optional.empty()
                : DirectVendorGuard.resolveProviderByok(request, userAiProviderKeyService);
        boolean modelKey = UserAiModelSupport.isUserKeyModel(request.getModel());
        if ((providerByok.isEmpty() || providerByok.get().apiKey() == null || providerByok.get().apiKey().isBlank())
                && !modelKey) {
            throw DirectVendorGuard.rejected(CODE);
        }
        if (providerByok.isPresent() && providerByok.get().apiKey() != null && !providerByok.get().apiKey().isBlank()) {
            ResolvedProviderKey key = providerByok.get();
            String base = UserAiProviderCatalog.resolveEffectiveBaseUrl(key.baseUrl(), CODE);
            return new OwnedCredential(key.userId(), UserAiModelSupport.PROVIDER_KEY_PREFIX + "openai",
                    openAiApiRoot(base), key.apiKey());
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String userId = ShortJson.text(cfg.get("user_id"));
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                request.getModel().getModelId());
        if (userAiModelKeyService == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "自有模型密钥配置缺失");
        }
        String apiKey = userAiModelKeyService.requirePlainApiKey(userId, keyId);
        String base = ShortJson.text(cfg.get("base_url"));
        return new OwnedCredential(userId, keyId, openAiApiRoot(base), apiKey);
    }

    static String openAiApiRoot(String rawBaseUrl) {
        String base = UserAiModelSupport.normalizeBaseUrl(rawBaseUrl, UserAiModelSupport.PROVIDER_OPENAI);
        String path;
        try {
            path = URI.create(base).getPath();
        } catch (IllegalArgumentException ex) {
            path = "";
        }
        if (path == null || path.isBlank() || "/".equals(path)) {
            return base + "/v1";
        }
        return base;
    }

    private static String modelCode(AiProviderRequest request, String ability) {
        String configured = request.getModel().getModelCode();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return switch (ability) {
            case "IMAGE" -> DEFAULT_IMAGE_MODEL;
            case "ASR" -> DEFAULT_ASR_MODEL;
            default -> DEFAULT_TEXT_MODEL;
        };
    }

    private static Map<String, String> bearer(String apiKey) {
        return Map.of("Authorization", "Bearer " + apiKey);
    }

    private record OwnedCredential(String userId, String keyId, String baseUrl, String apiKey) {
    }
}
