package com.bgssai.shortai.aiprovider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
 * Anthropic Messages 直连提供者（仅在用户 BYOK 授权上下文下启用）。
 */
@Component
public class AnthropicAiProvider implements AiProvider {

    public static final String CODE = "ANTHROPIC";
    public static final String DEFAULT_TEXT_MODEL = "claude-opus-5-5";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ProviderHttp.Client http;

    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;

    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;

    public AnthropicAiProvider() {
        this(new ProviderHttp.JdkClient());
    }

    AnthropicAiProvider(ProviderHttp.Client http) {
        this.http = http;
    }

    AnthropicAiProvider(ProviderHttp.Client http, UserAiProviderKeyService userAiProviderKeyService) {
        this.http = http;
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
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        if (!"TEXT".equals(ability)) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "Claude 当前仅支持文本");
        }
        Optional<ResolvedProviderKey> byok = DirectVendorGuard.isExplicitUserModelKey(request.getModel())
                ? Optional.empty()
                : DirectVendorGuard.resolveProviderByok(request, userAiProviderKeyService);
        boolean modelKey = UserAiModelSupport.isUserKeyModel(request.getModel());
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String apiKey;
        String baseUrl;
        if (byok.isPresent() && hasText(byok.get().apiKey())) {
            apiKey = byok.get().apiKey();
            baseUrl = UserAiProviderCatalog.resolveEffectiveBaseUrl(byok.get().baseUrl(), CODE);
        } else if (modelKey) {
            String userId = ShortJson.text(cfg.get("user_id"));
            String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                    request.getModel().getModelId());
            if (userAiModelKeyService == null) {
                throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "自有模型密钥配置缺失");
            }
            apiKey = userAiModelKeyService.requirePlainApiKey(userId, keyId);
            baseUrl = UserAiModelSupport.normalizeBaseUrl(ShortJson.text(cfg.get("base_url")), CODE);
        } else {
            throw DirectVendorGuard.rejected(CODE);
        }
        String apiRoot = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        String url = apiRoot + "/v1/messages";
        String modelCode = ProviderHttp.firstNonBlank(request.getModel().getModelCode(), DEFAULT_TEXT_MODEL);
        int maxTokens = ShortJson.integer(request.param("max_tokens"),
                ShortJson.integer(cfg.get("max_tokens"), 4096));
        int timeoutSec = Math.max(30, ShortJson.integer(cfg.get("timeout_sec"), 120));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("max_tokens", maxTokens);

        String systemPrompt = ProviderHttp.firstNonBlank(
                ShortJson.text(request.param("system_prompt")),
                ShortJson.text(cfg.get("system_prompt")));
        if (!systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        String imgB64 = ShortJson.text(request.param("image_base64"));
        if (!imgB64.isBlank()) {
            String mime = ProviderHttp.firstNonBlank(ShortJson.text(request.param("image_mime")), "image/png");
            Map<String, Object> imgPart = new LinkedHashMap<>();
            imgPart.put("type", "image");
            imgPart.put("source", Map.of(
                    "type", "base64",
                    "media_type", mime,
                    "data", stripDataPrefix(imgB64)
            ));
            parts.add(imgPart);
        }
        parts.add(Map.of("type", "text", "text", ProviderHttp.promptOf(request)));

        body.put("messages", List.of(Map.of("role", "user", "content", parts)));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", ANTHROPIC_VERSION);

        try {
            String raw = http.postJson(url, headers, ShortJson.toJson(body), timeoutSec);
            Map<String, Object> resp = ShortJson.map(raw);
            String text = extractText(resp);
            if (text == null || text.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "anthropic empty content");
            }
            AiProviderResult result = AiProviderResult.text(text);
            int usage = extractUsage(resp);
            if (usage > 0) {
                result.setUsageAmount(usage);
            }
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "anthropic request failed: " + ex.getMessage());
        }
    }

    private static String extractText(Map<String, Object> resp) {
        Object content = resp.get("content");
        if (!(content instanceof List<?> items) || items.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : items) {
            if (item instanceof Map<?, ?> block && "text".equals(String.valueOf(block.get("type")))) {
                Object t = block.get("text");
                if (t != null) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(t);
                }
            }
        }
        return sb.toString();
    }

    private static int extractUsage(Map<String, Object> resp) {
        Object usage = resp.get("usage");
        if (usage instanceof Map<?, ?> map) {
            int in = ShortJson.integer(map.get("input_tokens"), 0);
            int out = ShortJson.integer(map.get("output_tokens"), 0);
            return in + out;
        }
        return 0;
    }

    private static String stripDataPrefix(String value) {
        String v = value.trim();
        int idx = v.indexOf(',');
        return v.startsWith("data:") && idx >= 0 ? v.substring(idx + 1) : v;
    }

    private static boolean hasText(String v) {
        return v != null && !v.isBlank();
    }
}
