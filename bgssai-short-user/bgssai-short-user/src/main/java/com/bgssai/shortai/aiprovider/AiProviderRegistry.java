package com.bgssai.shortai.aiprovider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;

@Component
public class AiProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRegistry.class);
    private static final Set<String> DIRECT_VENDOR_CODES = Set.of(
            KlingAiProvider.CODE,
            VeoAiProvider.CODE,
            MiniMaxAiProvider.CODE,
            SeedanceAiProvider.CODE,
            OpenAiCompatibleAiProvider.CODE);

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
     * Resolves a runnable bean. Billed generation goes through the CN hub.
     * The company host {@code https://chat.bgssai.com} is the only OpenAI-compatible
     * exception. allow_direct is ignored. Local ffmpeg compose is not a billed model.
     */
    public AiProvider resolve(AiModelConfig model) {
        if (model == null || model.getProviderCode() == null || model.getProviderCode().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "provider_code required");
        }
        String code = model.getProviderCode().trim();
        Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
        AiProvider direct = byCode.get(code);

        if (direct != null && FfmpegComposeProvider.CODE.equals(direct.providerCode())) {
            return direct;
        }
        if (direct != null && TencentTokenHubAiProvider.CODE.equals(direct.providerCode())) {
            return direct;
        }
        if (direct != null && ImageConditionedVideoProvider.CODE.equals(direct.providerCode())) {
            return direct;
        }

        String baseUrl = ShortJson.text(cfg.get("base_url")).toLowerCase(Locale.ROOT);
        if (direct != null && OpenAiCompatibleAiProvider.CODE.equals(code)
                && CompanyCompatGateway.allows(baseUrl)) {
            return direct;
        }
        if (TokenHubClientSupport.isTokenHubHost(baseUrl)
                && byCode.containsKey(TencentTokenHubAiProvider.CODE)) {
            return byCode.get(TencentTokenHubAiProvider.CODE);
        }

        if (byCode.containsKey(TencentTokenHubAiProvider.CODE)) {
            if (DIRECT_VENDOR_CODES.contains(code) || isAllowDirect(cfg)) {
                log.info("short generation rerouted to hub from provider_code={}", code);
            }
            return byCode.get(TencentTokenHubAiProvider.CODE);
        }

        throw new BizException(CommonErrorCode.RULE_REJECT,
                "model calls require the token hub provider: " + code);
    }

    public boolean resolvable(AiModelConfig model) {
        if (model == null) {
            return false;
        }
        try {
            resolve(model);
            return true;
        } catch (BizException ex) {
            return false;
        }
    }

    private static boolean isAllowDirect(Map<String, Object> cfg) {
        Object raw = cfg.get("allow_direct");
        if (raw instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(ShortJson.text(raw));
    }
}
