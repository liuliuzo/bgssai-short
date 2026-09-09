package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.secret.SysSecretReader;

class AiProviderRegistryTest {

    @Test
    void resolveFallsBackToOpenAiCompatForTextChatPath() {
        OpenAiCompatibleAiProvider openAi = new OpenAiCompatibleAiProvider(new SysSecretReader(key -> "k"));
        AiProviderRegistry registry = new AiProviderRegistry(List.of(openAi));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode("HUNYUAN");
        model.setAbilityType("TEXT");
        model.setConfigJson("{\"base_url\":\"https://api.hunyuan.cloud.tencent.com\",\"chat_path\":\"/chat/completions\"}");
        assertEquals(OpenAiCompatibleAiProvider.CODE, registry.resolve(model).providerCode());
        assertTrue(registry.resolvable(model));
    }

    @Test
    void resolveFallsBackToTokenHubForTokenhubHost() {
        ProviderHttp.Client noop = new ProviderHttp.Client() {
            @Override
            public String postJson(String url, Map<String, String> headers, String body, int timeoutSec) {
                return "{}";
            }

            @Override
            public String getJson(String url, Map<String, String> headers, int timeoutSec) {
                return "{}";
            }

            @Override
            public byte[] getBytes(String url, int timeoutSec) {
                return new byte[0];
            }
        };
        TencentTokenHubAiProvider hub = new TencentTokenHubAiProvider(
                new SysSecretReader(key -> "k"), noop, ms -> {
                });
        AiProviderRegistry registry = new AiProviderRegistry(List.of(hub));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode("WAND");
        model.setAbilityType("IMAGE");
        model.setConfigJson("{\"base_url\":\"https://tokenhub.tencentmaas.com\",\"secret_ref\":\"ai.wand.api_key\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(model).providerCode());
    }

    @Test
    void unknownVideoVendorWithoutFallbackRejected() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of());
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode("HUNYUAN");
        model.setAbilityType("VIDEO");
        model.setConfigJson("{\"base_url\":\"https://api.hunyuan.cloud.tencent.com\"}");
        assertFalse(registry.resolvable(model));
        assertThrows(BizException.class, () -> registry.resolve(model));
    }
}
