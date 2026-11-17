package com.bgssai.shortai.aiprovider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

@Component
public class AiProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRegistry.class);
    private static final Set<String> DIRECT_VENDOR_CODES = Set.of(
            KlingAiProvider.CODE,
            VeoAiProvider.CODE,
            MiniMaxAiProvider.CODE,
            SeedanceAiProvider.CODE,
            PixVerseAiProvider.CODE,
            DeepSeekAiProvider.CODE,
            OpenAiCompatibleAiProvider.CODE);

    private final Map<String, AiProvider> byCode = new HashMap<>();

    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;

    @Autowired
    public AiProviderRegistry(List<AiProvider> providers) {
        if (providers != null) {
            for (AiProvider provider : providers) {
                byCode.put(provider.providerCode(), provider);
            }
        }
    }

    public AiProviderRegistry(List<AiProvider> providers, UserAiProviderKeyService userAiProviderKeyService) {
        this(providers);
        this.userAiProviderKeyService = userAiProviderKeyService;
    }

    void setUserAiProviderKeyService(UserAiProviderKeyService userAiProviderKeyService) {
        this.userAiProviderKeyService = userAiProviderKeyService;
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

        if (DirectVendorGuard.isExplicitUserModelKey(model)) {
            if (direct != null) {
                return direct;
            }
            AiProvider byokDirect = resolveDirectByokProvider(code, model.getAbilityType());
            if (byokDirect != null) {
                return byokDirect;
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "unknown user provider_code: " + code);
        }

        Optional<ResolvedProviderKey> providerByok = DirectVendorGuard.resolveProviderByok(
                model, Map.of(), userAiProviderKeyService);
        if (providerByok.isPresent()) {
            AiProvider byokDirect = resolveDirectByokProvider(
                    providerByok.get().providerCode(), code, model.getAbilityType());
            if (byokDirect != null) {
                return byokDirect;
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "unknown user provider_code: " + code);
        }

        if (UserAiModelSupport.isUserKeyModel(model)) {
            AiProvider byokDirect = resolveDirectByokProvider(code, model.getAbilityType());
            if (byokDirect != null) {
                return byokDirect;
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "unknown user provider_code: " + code);
        }

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

    private AiProvider resolveDirectByokProvider(String rawCode, String abilityType) {
        return resolveDirectByokProvider(
                UserAiProviderCatalog.normalizeRuntimeProviderCode(rawCode), rawCode, abilityType);
    }

    private AiProvider resolveDirectByokProvider(String canonicalCode, String rawCode, String abilityType) {
        String raw = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        String canonical = canonicalCode == null ? "" : canonicalCode.trim().toUpperCase(Locale.ROOT);
        String ability = abilityType == null ? "" : abilityType.trim().toUpperCase(Locale.ROOT);
        if ("KLING".equals(canonical) || "KLINGAI".equals(raw)) {
            return byCode.get(KlingAiProvider.CODE);
        }
        if ("MINIMAX".equals(canonical)) {
            return byCode.get(MiniMaxAiProvider.CODE);
        }
        if ("DEEPSEEK".equals(canonical)) {
            return byCode.get(DeepSeekAiProvider.CODE);
        }
        if ("VOLCENGINE".equals(canonical)) {
            if ("VIDEO".equals(ability) || "SEEDANCE".equals(raw)) {
                AiProvider seedance = byCode.get(SeedanceAiProvider.CODE);
                if (seedance != null) {
                    return seedance;
                }
            }
            return byCode.get(OpenAiCompatibleAiProvider.CODE);
        }
        if ("GOOGLE".equals(canonical)) {
            if ("VIDEO".equals(ability) || "VEO".equals(raw)) {
                AiProvider veo = byCode.get(VeoAiProvider.CODE);
                if (veo != null) {
                    return veo;
                }
            }
            return byCode.get(GoogleGeminiAiProvider.CODE);
        }
        if ("ANTHROPIC".equals(canonical)) {
            return byCode.get(AnthropicAiProvider.CODE);
        }
        if ("DASHSCOPE".equals(canonical) || "ZHIPU".equals(canonical)
                || "MOONSHOT".equals(canonical) || "HUNYUAN".equals(canonical)
                || "OPENAI".equals(canonical)) {
            return byCode.get(OpenAiCompatibleAiProvider.CODE);
        }
        return byCode.get(raw);
    }

    private static boolean isAllowDirect(Map<String, Object> cfg) {
        Object raw = cfg.get("allow_direct");
        if (raw instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(ShortJson.text(raw));
    }
}
