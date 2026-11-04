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
    void resolveReroutesVendorToHubUnlessAllowDirect() {
        OpenAiCompatibleAiProvider openAi = new OpenAiCompatibleAiProvider(new SysSecretReader(key -> "k"));
        TencentTokenHubAiProvider hub = hub();
        AiProviderRegistry registry = new AiProviderRegistry(List.of(openAi, hub));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode("HUNYUAN");
        model.setAbilityType("TEXT");
        model.setConfigJson("{\"base_url\":\"https://api.hunyuan.cloud.tencent.com\",\"chat_path\":\"/chat/completions\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(model).providerCode());
        assertTrue(registry.resolvable(model));
    }

    @Test
    void companyHostResolvesToOpenAiCompatEvenWhenHubExists() {
        OpenAiCompatibleAiProvider openAi = new OpenAiCompatibleAiProvider(new SysSecretReader(key -> "k"));
        AiProviderRegistry registry = new AiProviderRegistry(List.of(openAi, hub()));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(OpenAiCompatibleAiProvider.CODE);
        model.setAbilityType("IMAGE");
        model.setConfigJson("{\"base_url\":\"https://chat.bgssai.com/v1\"}");
        assertEquals(OpenAiCompatibleAiProvider.CODE, registry.resolve(model).providerCode());
        AiModelConfig lookalike = new AiModelConfig();
        lookalike.setProviderCode(OpenAiCompatibleAiProvider.CODE);
        lookalike.setConfigJson("{\"base_url\":\"https://chat.bgssai.com.evil/v1\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(lookalike).providerCode());
        AiModelConfig insecure = new AiModelConfig();
        insecure.setProviderCode(OpenAiCompatibleAiProvider.CODE);
        insecure.setConfigJson("{\"base_url\":\"http://chat.bgssai.com/v1\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(insecure).providerCode());
    }

    @Test
    void resolveIgnoresAllowDirectAndUsesHub() {
        OpenAiCompatibleAiProvider openAi = new OpenAiCompatibleAiProvider(new SysSecretReader(key -> "k"));
        AiProviderRegistry registry = new AiProviderRegistry(List.of(openAi, hub()));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(OpenAiCompatibleAiProvider.CODE);
        model.setAbilityType("TEXT");
        model.setConfigJson("{\"allow_direct\":true,\"base_url\":\"https://api.hunyuan.cloud.tencent.com\",\"chat_path\":\"/chat/completions\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(model).providerCode());
    }

    @Test
    void resolveRejectsAllowDirectWhenHubIsMissing() {
        OpenAiCompatibleAiProvider openAi = new OpenAiCompatibleAiProvider(new SysSecretReader(key -> "k"));
        AiProviderRegistry registry = new AiProviderRegistry(List.of(openAi));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(OpenAiCompatibleAiProvider.CODE);
        model.setAbilityType("TEXT");
        model.setConfigJson("{\"allow_direct\":true,\"base_url\":\"https://api.hunyuan.cloud.tencent.com\"}");
        assertFalse(registry.resolvable(model));
        assertThrows(BizException.class, () -> registry.resolve(model));
    }

    @Test
    void resolveFallsBackToTokenHubForTokenhubHost() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of(hub()));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode("WAND");
        model.setAbilityType("IMAGE");
        model.setConfigJson("{\"base_url\":\"https://tokenhub.tencentmaas.com\",\"secret_ref\":\"ai.wand.api_key\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(model).providerCode());
    }

    @Test
    void resolveFallsBackToTokenHubForBgssaiHubHosts() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of(hub()));
        AiModelConfig cn = new AiModelConfig();
        cn.setProviderCode("CUSTOM");
        cn.setAbilityType("TEXT");
        cn.setConfigJson("{\"base_url\":\"https://www.bgssai-tokenhub.cn\",\"hub\":\"cn\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(cn).providerCode());
        AiModelConfig global = new AiModelConfig();
        global.setProviderCode("CUSTOM");
        global.setAbilityType("TEXT");
        global.setConfigJson("{\"base_url\":\"https://www.bgssai-tokenhub.com\",\"hub\":\"global\"}");
        assertEquals(TencentTokenHubAiProvider.CODE, registry.resolve(global).providerCode());
    }

    @Test
    void klingAllowDirectWithoutHubDoesNotResolveToVendor() {
        KlingAiProvider kling = new KlingAiProvider(new SysSecretReader(key -> "k"));
        AiProviderRegistry registry = new AiProviderRegistry(List.of(kling));
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(KlingAiProvider.CODE);
        model.setAbilityType("VIDEO");
        model.setConfigJson("{\"allow_direct\":true,\"base_url\":\"https://api-beijing.klingai.com\"}");
        assertFalse(registry.resolvable(model));
        BizException ex = assertThrows(BizException.class, () -> registry.resolve(model));
        assertTrue(ex.getMessage().contains("token hub"));
        assertTrue(!ex.getMessage().contains("klingai"));
    }

    @Test
    void unknownVideoVendorWithoutHubRejected() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of());
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode("HUNYUAN");
        model.setAbilityType("VIDEO");
        model.setConfigJson("{\"base_url\":\"https://api.hunyuan.cloud.tencent.com\"}");
        assertFalse(registry.resolvable(model));
        assertThrows(BizException.class, () -> registry.resolve(model));
    }

    private static TencentTokenHubAiProvider hub() {
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
        return new TencentTokenHubAiProvider(new SysSecretReader(key -> "k"), noop, ms -> {
        });
    }
}
