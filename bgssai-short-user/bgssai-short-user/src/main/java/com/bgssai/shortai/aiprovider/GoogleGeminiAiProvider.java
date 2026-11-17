package com.bgssai.shortai.aiprovider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

/**
 * Google Gemini 直连提供者（仅在用户 BYOK 授权上下文下启用）。
 */
@Component
public class GoogleGeminiAiProvider implements AiProvider {

    public static final String CODE = "GOOGLE";

    private final ProviderHttp.Client http;

    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;

    public GoogleGeminiAiProvider() {
        this(new ProviderHttp.JdkClient());
    }

    GoogleGeminiAiProvider(ProviderHttp.Client http) {
        this.http = http;
    }

    GoogleGeminiAiProvider(ProviderHttp.Client http, UserAiProviderKeyService userAiProviderKeyService) {
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
        Optional<ResolvedProviderKey> byok = DirectVendorGuard.resolveProviderByok(request, userAiProviderKeyService);
        if (byok.isEmpty() || byok.get().apiKey() == null || byok.get().apiKey().isBlank()) {
            throw DirectVendorGuard.rejected(CODE);
        }
        ResolvedProviderKey key = byok.get();
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String baseUrl = UserAiProviderCatalog.resolveEffectiveBaseUrl(key.baseUrl(), CODE);
        String apiRoot = UserAiModelSupport.stripTrailingPath(
                UserAiModelSupport.stripTrailingPath(baseUrl, "/v1beta"), "/v1");
        String modelCode = ProviderHttp.firstNonBlank(request.getModel().getModelCode(), "gemini-2.5-flash");
        String encodedModel = URLEncoder.encode(modelCode, StandardCharsets.UTF_8);
        String url = apiRoot + "/v1beta/models/" + encodedModel + ":generateContent";
        int timeoutSec = Math.max(30, ShortJson.integer(cfg.get("timeout_sec"), 120));

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", ProviderHttp.promptOf(request)));
        String imgB64 = ShortJson.text(request.param("image_base64"));
        if (!imgB64.isBlank()) {
            String mime = ProviderHttp.firstNonBlank(ShortJson.text(request.param("image_mime")), "image/png");
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", mime,
                    "data", stripDataPrefix(imgB64)
            )));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        String systemPrompt = ProviderHttp.firstNonBlank(
                ShortJson.text(request.param("system_prompt")),
                ShortJson.text(cfg.get("system_prompt")));
        if (!systemPrompt.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }
        body.put("contents", List.of(Map.of("role", "user", "parts", parts)));

        Map<String, Object> genCfg = new LinkedHashMap<>();
        Object temp = cfg.get("temperature") != null ? cfg.get("temperature") : request.param("temperature");
        if (temp != null) {
            genCfg.put("temperature", temp);
        }
        int maxTokens = ShortJson.integer(request.param("max_tokens"), ShortJson.integer(cfg.get("max_tokens"), 0));
        if (maxTokens > 0) {
            genCfg.put("maxOutputTokens", maxTokens);
        }
        if (!genCfg.isEmpty()) {
            body.put("generationConfig", genCfg);
        }

        Map<String, String> headers = Map.of("x-goog-api-key", key.apiKey());
        try {
            String raw = http.postJson(url, headers, ShortJson.toJson(body), timeoutSec);
            Map<String, Object> resp = ShortJson.map(raw);
            String text = extractText(resp);
            if (text == null || text.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "gemini empty content");
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
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "gemini request failed: " + ex.getMessage());
        }
    }

    private static String extractText(Map<String, Object> resp) {
        Object candidates = resp.get("candidates");
        if (!(candidates instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        if (!(list.get(0) instanceof Map<?, ?> first)) {
            return null;
        }
        Object content = first.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) {
            return null;
        }
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?> partList)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object p : partList) {
            if (p instanceof Map<?, ?> pm && pm.get("text") != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(pm.get("text"));
            }
        }
        return sb.toString();
    }

    private static int extractUsage(Map<String, Object> resp) {
        Object meta = resp.get("usageMetadata");
        if (meta instanceof Map<?, ?> map) {
            return ShortJson.integer(map.get("totalTokenCount"), 0);
        }
        return 0;
    }

    private static String stripDataPrefix(String value) {
        String v = value.trim();
        int idx = v.indexOf(',');
        return v.startsWith("data:") && idx >= 0 ? v.substring(idx + 1) : v;
    }
}
