package com.bgssai.shortai.aiprovider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

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

    public AiProvider get(String providerCode) {
        AiProvider provider = byCode.get(providerCode);
        if (provider == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "unknown provider_code: " + providerCode);
        }
        return provider;
    }
}
