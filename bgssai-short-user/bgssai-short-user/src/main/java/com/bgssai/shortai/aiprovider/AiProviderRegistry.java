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
     * Resolves a runnable bean. Generation goes through TokenHub unless the model
     * row explicitly sets allow_direct=true (auditable). Local ffmpeg compose is not a billed model.
     */
    public AiProvider resolve(AiModelConfig model) {
        if (model == null || model.getProviderCode() == null || model.getProviderCode().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "provider_code required");
        }
        String code = model.getProviderCode().trim();
        Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
        boolean allowDirect = isAllowDirect(cfg);
        AiProvider direct = byCode.get(code);

        if (direct != null && FfmpegComposeProvider.CODE.equals(direct.providerCode())) {
            return direct;
        }
        if (direct != null && TencentTokenHubAiProvider.CODE.equals(direct.providerCode())) {
            return direct;
        }

        String baseUrl = ShortJson.text(cfg.get("base_url")).toLowerCase(Locale.ROOT);
        boolean hubHost = TokenHubClientSupport.isTokenHubHost(baseUrl);
        if (hubHost && byCode.containsKey(TencentTokenHubAiProvider.CODE)) {
            return byCode.get(TencentTokenHubAiProvider.CODE);
        }

        if (allowDirect && direct != null) {
            log.info("short generation using allow_direct provider_code={}", code);
            return direct;
        }

        if (byCode.containsKey(TencentTokenHubAiProvider.CODE)
                && (direct == null || DIRECT_VENDOR_CODES.contains(code) || !allowDirect)) {
            if (direct != null && DIRECT_VENDOR_CODES.contains(code) && !allowDirect) {
                log.info("short generation rerouted to hub from provider_code={}", code);
            }
            return byCode.get(TencentTokenHubAiProvider.CODE);
        }

        if (direct != null) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "direct provider not allowed without allow_direct: " + code);
        }
        throw new BizException(CommonErrorCode.RULE_REJECT, "unknown provider_code: " + code);
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
