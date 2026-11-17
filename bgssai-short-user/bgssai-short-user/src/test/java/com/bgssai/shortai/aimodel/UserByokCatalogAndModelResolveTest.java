package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.aiprovider.AnthropicAiProvider;
import com.bgssai.shortai.aiprovider.DeepSeekAiProvider;
import com.bgssai.shortai.aiprovider.GoogleGeminiAiProvider;
import com.bgssai.shortai.aiprovider.OpenAiCompatibleAiProvider;
import com.bgssai.shortai.aiprovider.SeedanceAiProvider;
import com.bgssai.shortai.aiprovider.TencentTokenHubAiProvider;
import com.bgssai.shortai.aiprovider.VeoAiProvider;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.secret.SysSecretReader;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

class UserByokCatalogAndModelResolveTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void resolveAndReadinessSupportProviderByokEvenWithoutHubKey() {
        AiModelConfigMapper mapper = mock(AiModelConfigMapper.class);
        AiModelConfig deepseekRow = catalogRow("m-ds", "TEXT", "DEEPSEEK", "deepseek-chat", 15);
        AiModelConfig anthropicRow = catalogRow("m-ant", "TEXT", "ANTHROPIC", "claude-3-5-sonnet-latest", 40);
        when(mapper.selectByExample(any(AiModelConfigExample.class))).thenAnswer(inv -> {
            AiModelConfigExample ex = inv.getArgument(0);
            String modelFilter = null;
            if (!ex.getOredCriteria().isEmpty()) {
                for (AiModelConfigExample.Criterion c : ex.getOredCriteria().get(0).getCriteria()) {
                    if ("model_code =".equals(c.getCondition())) {
                        modelFilter = String.valueOf(c.getValue());
                    }
                }
            }
            if ("claude-3-5-sonnet-latest".equals(modelFilter)) {
                return List.of(anthropicRow);
            }
            if ("deepseek-chat".equals(modelFilter)) {
                return List.of(deepseekRow);
            }
            return List.of(deepseekRow, anthropicRow);
        });

        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);
        ResolvedProviderKey dsKey = new ResolvedProviderKey(
                "u-res-1", "DEEPSEEK", "ds-secret", null, null, null, true);
        ResolvedProviderKey antKey = new ResolvedProviderKey(
                "u-res-1", "ANTHROPIC", "ant-secret", null, null, "https://api.anthropic.com", true);
        when(keyService.findActiveKey("u-res-1", "DEEPSEEK")).thenReturn(Optional.of(dsKey));
        when(keyService.findActiveKey("u-res-1", "ANTHROPIC")).thenReturn(Optional.of(antKey));
        when(keyService.activeKeyMap("u-res-1")).thenReturn(Map.of("DEEPSEEK", dsKey, "ANTHROPIC", antKey));
        when(keyService.listActiveKeysForUser("u-res-1")).thenReturn(List.of(dsKey, antKey));
        when(keyService.hasAnyActiveProviderKey("u-res-1")).thenReturn(true);

        SysSecretReader emptySecrets = mock(SysSecretReader.class);
        when(emptySecrets.get(any())).thenReturn("");
        when(emptySecrets.has(any())).thenReturn(false);

        AiProviderRegistry registry = new AiProviderRegistry(
                List.<AiProvider>of(
                        new TencentTokenHubAiProvider(emptySecrets, "https://www.bgssai-tokenhub.cn"),
                        new DeepSeekAiProvider(),
                        new AnthropicAiProvider(),
                        new GoogleGeminiAiProvider()),
                keyService);

        AiModelResolveService resolver = new AiModelResolveService();
        ReflectionTestUtils.setField(resolver, "aiModelConfigMapper", mapper);
        ReflectionTestUtils.setField(resolver, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(resolver, "secrets", emptySecrets);
        ReflectionTestUtils.setField(resolver, "userAiProviderKeyService", keyService);

        // Without user context and without hub key -> blocked
        assertThrows(BizException.class, () -> resolver.resolve("TEXT", "claude-3-5-sonnet-latest"));

        UserContext.setUserId("u-res-1");
        AiModelConfig resolvedAnt = resolver.resolve("TEXT", "claude-3-5-sonnet-latest");
        assertTrue(UserAiModelSupport.isUserKeyModel(resolvedAnt));
        assertEquals(0, resolvedAnt.getUnitPriceCent());
        assertEquals("ANTHROPIC", resolvedAnt.getProviderCode());

        AiModelConfig resolvedDs = resolver.resolve("TEXT", "deepseek-chat");
        assertTrue(UserAiModelSupport.isUserKeyModel(resolvedDs));
        assertEquals(0, resolvedDs.getUnitPriceCent());

        AiModelConfig bpkDs = resolver.resolve("TEXT", "bpk-deepseek");
        assertTrue(UserAiModelSupport.isUserKeyModel(bpkDs));
        assertEquals("deepseek-chat", bpkDs.getModelCode());
        assertEquals(0, bpkDs.getUnitPriceCent());

        ApiResponse listed = resolver.listEnabled("TEXT");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) listed.getResult();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modelList = (List<Map<String, Object>>) body.get("model_list");
        assertTrue(modelList.stream().anyMatch(item ->
                "USER_KEY".equals(item.get("credential_source"))
                        && Integer.valueOf(0).equals(item.get("unit_price_cent"))));

        PipelineReadiness readiness = new PipelineReadiness(
                resolver, emptySecrets, null, "https://www.bgssai-tokenhub.cn");
        ReflectionTestUtils.setField(readiness, "userAiProviderKeyService", keyService);
        assertDoesNotThrow(() -> readiness.assertReady("TEXT", "deepseek-chat"));
        assertDoesNotThrow(() -> readiness.assertReady("TEXT", null));
    }

    @Test
    void allRemainingByokProvidersRespectGuardAndRouteWithUserKeys() {
        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);
        SysSecretReader secrets = mock(SysSecretReader.class);
        AtomicReference<String> lastUrl = new AtomicReference<>();

        DeepSeekAiProvider deepseek = new DeepSeekAiProvider();
        ReflectionTestUtils.setField(deepseek, "userAiProviderKeyService", keyService);
        SeedanceAiProvider seedance = new SeedanceAiProvider(secrets);
        ReflectionTestUtils.setField(seedance, "userAiProviderKeyService", keyService);
        VeoAiProvider veo = new VeoAiProvider(secrets);
        ReflectionTestUtils.setField(veo, "userAiProviderKeyService", keyService);
        OpenAiCompatibleAiProvider compat = new OpenAiCompatibleAiProvider(secrets);
        ReflectionTestUtils.setField(compat, "userAiProviderKeyService", keyService);

        for (String code : List.of("DASHSCOPE", "ZHIPU", "MOONSHOT", "VOLCENGINE", "HUNYUAN")) {
            AiProviderRequest req = req("TEXT", catalogRow("m-" + code, "TEXT", code, "model-" + code, 10));
            assertThrows(BizException.class, () -> compat.execute(req));
        }
        assertThrows(BizException.class,
                () -> deepseek.execute(req("TEXT", catalogRow("m-ds", "TEXT", "DEEPSEEK", "deepseek-chat", 10))));
        assertThrows(BizException.class,
                () -> seedance.execute(req("VIDEO", catalogRow("m-sd", "VIDEO", "VOLCENGINE", "seedance", 10))));
        assertThrows(BizException.class,
                () -> veo.execute(req("VIDEO", catalogRow("m-veo", "VIDEO", "GOOGLE", "veo-3.0", 10))));
        assertEquals(null, lastUrl.get());
    }

    private static AiModelConfig catalogRow(String id, String ability, String provider, String code, int price) {
        AiModelConfig row = new AiModelConfig();
        row.setModelId(id);
        row.setAbilityType(ability);
        row.setProviderCode(provider);
        row.setModelCode(code);
        row.setModelName(code);
        row.setQualityLevel("STANDARD");
        row.setPriceUnit("PER_CALL");
        row.setUnitPriceCent(price);
        row.setDefaultFlag(0);
        row.setEnabledFlag(1);
        row.setDeletedFlag(0);
        row.setConfigJson("{}");
        return row;
    }

    private static AiProviderRequest req(String ability, AiModelConfig model) {
        AiProviderRequest r = new AiProviderRequest();
        r.setAbilityType(ability);
        r.setModel(model);
        r.setPrompt("test prompt");
        return r;
    }
}
