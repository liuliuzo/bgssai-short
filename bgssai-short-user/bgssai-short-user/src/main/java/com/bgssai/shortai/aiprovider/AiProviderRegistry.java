package com.bgssai.shortai.aiprovider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;

@Component
public class AiProviderRegistry {

    private final Map<String, AiProvider> byCode = new HashMap<>();

    public AiProviderRegistry(List<AiProvider> providers) {
        if (providers != null) {
            for (AiProvider provider : providers) {
                byCode.put(provider.providerCode(), provider);
            }
        }
    }

    public boolean supports(String providerCode) {
        return providerCode != null && byCode.containsKey(providerCode);
    }

    public Set<String> registeredCodes() {
        return Collections.unmodifiableSet(byCode.keySet());
    }

    public AiProvider get(String providerCode) {
        AiProvider provider = byCode.get(providerCode);
        if (provider == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "unknown provider_code: " + providerCode);
        }
        return provider;
    }

    /**
     * Resolves a runnable bean for the model row. Catalog may list vendor codes that
     * share OPENAI_COMPAT / TENCENT_TOKENHUB implementations; fall back only when
     * config_json clearly indicates that path.
     */
    public AiProvider resolve(AiModelConfig model) {
        if (model == null || model.getProviderCode() == null || model.getProviderCode().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "provider_code required");
        }
        String code = model.getProviderCode().trim();
        AiProvider direct = byCode.get(code);
        if (direct != null) {
            return direct;
        }
        Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
        String baseUrl = ShortJson.text(cfg.get("base_url")).toLowerCase(Locale.ROOT);
        if (TokenHubClientSupport.isTokenHubHost(baseUrl) && byCode.containsKey(TencentTokenHubAiProvider.CODE)) {
            return byCode.get(TencentTokenHubAiProvider.CODE);
        }
        String chatPath = ShortJson.text(cfg.get("chat_path"));
        if (!chatPath.isBlank() && byCode.containsKey(OpenAiCompatibleAiProvider.CODE)
                && "TEXT".equalsIgnoreCase(model.getAbilityType())) {
            return byCode.get(OpenAiCompatibleAiProvider.CODE);
        }
        throw new BizException(CommonErrorCode.RULE_REJECT, "unknown provider_code: " + code);
    }

    public boolean resolvable(AiModelConfig model) {
        if (model == null) {
            return false;
        }
        if (supports(model.getProviderCode())) {
            return true;
        }
        Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
        String baseUrl = ShortJson.text(cfg.get("base_url")).toLowerCase(Locale.ROOT);
        if (TokenHubClientSupport.isTokenHubHost(baseUrl) && supports(TencentTokenHubAiProvider.CODE)) {
            return true;
        }
        String chatPath = ShortJson.text(cfg.get("chat_path"));
        return !chatPath.isBlank() && "TEXT".equalsIgnoreCase(model.getAbilityType())
                && supports(OpenAiCompatibleAiProvider.CODE);
    }
}
