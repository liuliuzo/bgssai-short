package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.AiUsageRecordMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortSettlementMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiUsageRecord;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.production.service.ShortTaskSettlementService;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.secret.SysSecretReader;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

class UserByokRuntimeTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void registryRoutesPlatformCallsToHubAndByokCallsDirectlyToVendorProviders() {
        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);
        when(keyService.findActiveKey("u-byok-1", "MINIMAX")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-byok-1", "MINIMAX", "mm-key-1234", null, null, "https://api.minimax.io", true)));
        when(keyService.findActiveKey("u-byok-1", "KLING")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-byok-1", "KLING", "kling-key-123", null, null, "https://api.klingai.com", true)));
        when(keyService.findActiveKey("u-byok-1", "DEEPSEEK")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-byok-1", "DEEPSEEK", "ds-key-123", null, null, null, true)));
        when(keyService.findActiveKey("u-byok-1", "OPENAI")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-byok-1", "OPENAI", "oa-key-123", null, null, null, true)));
        when(keyService.findActiveKey("u-byok-1", "ANTHROPIC")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-byok-1", "ANTHROPIC", "ant-key-123", null, null, null, true)));
        when(keyService.findActiveKey("u-byok-1", "GOOGLE")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-byok-1", "GOOGLE", "goog-key-123", null, null, null, true)));
        when(keyService.findActiveKey("u-byok-1", "VOLCENGINE")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-byok-1", "VOLCENGINE", "ark-key-123", null, null, null, true)));

        SysSecretReader secrets = mock(SysSecretReader.class);
        TencentTokenHubAiProvider hub = new TencentTokenHubAiProvider(secrets, "https://www.bgssai-tokenhub.cn");
        MiniMaxAiProvider minimax = new MiniMaxAiProvider(secrets);
        KlingAiProvider kling = new KlingAiProvider(secrets);
        DeepSeekAiProvider deepseek = new DeepSeekAiProvider();
        OpenAiCompatibleAiProvider compat = new OpenAiCompatibleAiProvider(secrets);
        SeedanceAiProvider seedance = new SeedanceAiProvider(secrets);
        VeoAiProvider veo = new VeoAiProvider(secrets);
        AnthropicAiProvider anthropic = new AnthropicAiProvider();
        GoogleGeminiAiProvider google = new GoogleGeminiAiProvider();

        AiProviderRegistry registry = new AiProviderRegistry(
                List.of(hub, minimax, kling, deepseek, compat, seedance, veo, anthropic, google),
                keyService);

        // Unauthenticated / non-BYOK platform model still stays locked to TokenHub
        assertSame(hub, registry.resolve(model("TEXT", "MINIMAX", "MiniMax-Text-01")));
        assertSame(hub, registry.resolve(model("VIDEO", "KLINGAI", "kling-v2-master")));
        assertSame(hub, registry.resolve(model("TEXT", "OPENAI", "gpt-4o")));

        UserContext.setUserId("u-byok-1");
        assertSame(minimax, registry.resolve(model("TEXT", "MINIMAX", "MiniMax-Text-01")));
        assertSame(kling, registry.resolve(model("VIDEO", "KLINGAI", "kling-v2-master")));
        assertSame(deepseek, registry.resolve(model("TEXT", "DEEPSEEK", "deepseek-chat")));
        assertSame(compat, registry.resolve(model("TEXT", "OPENAI", "gpt-4o")));
        assertSame(anthropic, registry.resolve(model("TEXT", "ANTHROPIC", "claude-3-5-sonnet-latest")));
        assertSame(google, registry.resolve(model("TEXT", "GOOGLE", "gemini-2.5-flash")));
        assertSame(veo, registry.resolve(model("VIDEO", "GOOGLE", "veo-3.0-generate-preview")));
        assertSame(seedance, registry.resolve(model("VIDEO", "VOLCENGINE", "doubao-seedance-1-0-pro-250528")));
        assertSame(compat, registry.resolve(model("TEXT", "VOLCENGINE", "doubao-1-5-pro-32k-250115")));
    }

    @Test
    void minimaxSupportsMinimaxiAndMinimaxIoBaseUrlOverridesAndBlocksNonByok() throws Exception {
        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);
        SysSecretReader secrets = mock(SysSecretReader.class);
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        AtomicReference<Map<String, String>> capturedHeaders = new AtomicReference<>();

        ProviderHttp.Client mockHttp = new ProviderHttp.Client() {
            @Override
            public String postJson(String url, Map<String, String> headers, String json, int timeoutSec) {
                capturedUrl.set(url);
                capturedHeaders.set(headers);
                return "{\"base_resp\":{\"status_code\":0},\"choices\":[{\"message\":{\"content\":\"ok minimax\"}}]}";
            }

            @Override
            public String getJson(String url, Map<String, String> headers, int timeoutSec) { return "{}"; }

            @Override
            public byte[] getBytes(String url, int timeoutSec) { return new byte[0]; }

            @Override
            public byte[] getBytes(String url, Map<String, String> headers, int timeoutSec) { return new byte[0]; }
        };

        MiniMaxAiProvider provider = new MiniMaxAiProvider(secrets, mockHttp, ms -> {});
        provider.setUserAiProviderKeyService(keyService);

        AiProviderRequest req = request("TEXT", model("TEXT", "MINIMAX", "MiniMax-Text-01"), "hello");
        assertThrows(BizException.class, () -> provider.execute(req));

        UserContext.setUserId("u-mm");
        when(keyService.findActiveKey("u-mm", "MINIMAX")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-mm", "MINIMAX", "mm-io-key-999", null, null, "https://api.minimax.io", true)));

        AiProviderResult res = provider.execute(req);
        assertEquals("ok minimax", res.getTextContent());
        assertEquals("https://api.minimax.io/v1/chat/completions", capturedUrl.get());
        assertEquals("Bearer mm-io-key-999", capturedHeaders.get().get("Authorization"));
    }

    @Test
    void klingSupportsSingleBearerAndAkSkJwtAndCustomDomainsAndBlocksNonByok() {
        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);
        SysSecretReader secrets = mock(SysSecretReader.class);
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();

        KlingAiProvider.Transport mockTransport = new KlingAiProvider.Transport() {
            @Override
            public String postJson(String url, String apiKey, String json, int timeoutSec) { return "{}"; }

            @Override
            public String getJson(String url, String apiKey, int timeoutSec) { return "{}"; }

            @Override
            public byte[] getBytes(String url, int timeoutSec) {
                return new byte[] {1, 2, 3, 4};
            }
        };

        ProviderHttp.Client mockUserHttp = new ProviderHttp.Client() {
            @Override
            public String postJson(String url, Map<String, String> headers, String json, int timeoutSec) {
                capturedUrl.set(url);
                capturedAuth.set(headers.get("Authorization"));
                return "{\"code\":0,\"data\":{\"task_id\":\"kt-1\",\"task_status\":\"succeed\",\"task_result\":{\"images\":[{\"url\":\"https://93.184.216.34/a.png\"}]}}}";
            }

            @Override
            public String getJson(String url, Map<String, String> headers, int timeoutSec) {
                return "{\"code\":0,\"data\":{\"task_id\":\"kt-1\",\"task_status\":\"succeed\",\"task_result\":{\"images\":[{\"url\":\"https://93.184.216.34/a.png\"}]}}}";
            }

            @Override
            public byte[] getBytes(String url, int timeoutSec) {
                return new byte[] {1, 2, 3, 4};
            }
        };

        KlingAiProvider provider = new KlingAiProvider(secrets, mockTransport, ms -> {}, mockUserHttp);
        provider.setUserAiProviderKeyService(keyService);

        AiProviderRequest req = request("IMAGE", model("IMAGE", "KLINGAI", "kling-v2"), "a cat");
        assertThrows(BizException.class, () -> provider.execute(req));

        UserContext.setUserId("u-kling");
        // Single Bearer token on api.klingai.com
        when(keyService.findActiveKey("u-kling", "KLING")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-kling", "KLING", "single-bearer-token-xyz", null, null, "https://api.klingai.com", true)));
        AiProviderResult bearerRes = provider.execute(req);
        assertEquals(4, bearerRes.getBinaryContent().length);
        assertEquals("https://api.klingai.com/v1/images/generations", capturedUrl.get());
        assertEquals("Bearer single-bearer-token-xyz", capturedAuth.get());

        // AccessKey + SecretKey HS256 JWT on api-beijing.klingai.com
        when(keyService.findActiveKey("u-kling", "KLING")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-kling", "KLING", null, "ak-12345678", "sk-secret-87654321", "https://api-beijing.klingai.com", true)));
        AiProviderResult jwtRes = provider.execute(req);
        assertEquals(4, jwtRes.getBinaryContent().length);
        assertEquals("https://api-beijing.klingai.com/v1/images/generations", capturedUrl.get());
        String jwtPart = capturedAuth.get().substring("Bearer ".length());
        assertEquals(3, jwtPart.split("\\.").length);
    }

    @Test
    void anthropicGoogleAndOpenAiCompatExecuteWithByokAndBlockWithoutByok() {
        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);
        SysSecretReader secrets = mock(SysSecretReader.class);
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        AtomicReference<Map<String, String>> capturedHeaders = new AtomicReference<>();

        ProviderHttp.Client transport = new ProviderHttp.Client() {
            @Override
            public String postJson(String url, Map<String, String> headers, String json, int timeoutSec) {
                capturedUrl.set(url);
                capturedHeaders.set(headers);
                if (url.contains("/v1/messages")) {
                    return "{\"content\":[{\"type\":\"text\",\"text\":\"ok claude\"}],\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}";
                }
                if (url.contains(":generateContent")) {
                    return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok gemini\"}]}}],\"usageMetadata\":{\"totalTokenCount\":42}}";
                }
                return "{\"choices\":[{\"message\":{\"content\":\"ok openai\"}}],\"usage\":{\"total_tokens\":15}}";
            }

            @Override
            public String getJson(String url, Map<String, String> headers, int timeoutSec) { return "{}"; }

            @Override
            public byte[] getBytes(String url, int timeoutSec) { return new byte[0]; }

            @Override
            public byte[] getBytes(String url, Map<String, String> headers, int timeoutSec) { return new byte[0]; }
        };

        AnthropicAiProvider anthropic = new AnthropicAiProvider(transport, keyService);
        GoogleGeminiAiProvider google = new GoogleGeminiAiProvider(transport, keyService);
        OpenAiCompatibleAiProvider compat = new OpenAiCompatibleAiProvider(secrets, transport, ms -> {});
        compat.setUserAiProviderKeyService(keyService);

        assertThrows(BizException.class,
                () -> anthropic.execute(request("TEXT", model("TEXT", "ANTHROPIC", "claude-3-5-sonnet-latest"), "hi")));
        assertThrows(BizException.class,
                () -> google.execute(request("TEXT", model("TEXT", "GOOGLE", "gemini-2.5-flash"), "hi")));
        assertThrows(BizException.class,
                () -> compat.execute(request("TEXT", model("TEXT", "OPENAI", "gpt-4o"), "hi")));

        UserContext.setUserId("u-multi");
        when(keyService.findActiveKey("u-multi", "ANTHROPIC")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-multi", "ANTHROPIC", "ant-live-key", null, null, null, true)));
        when(keyService.findActiveKey("u-multi", "GOOGLE")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-multi", "GOOGLE", "goog-live-key", null, null, null, true)));
        when(keyService.findActiveKey("u-multi", "OPENAI")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-multi", "OPENAI", "oa-live-key", null, null, null, true)));

        AiProviderResult antRes = anthropic.execute(
                request("TEXT", model("TEXT", "ANTHROPIC", "claude-3-5-sonnet-latest"), "hi"));
        assertEquals("ok claude", antRes.getTextContent());
        assertEquals(30, antRes.getUsageAmount());
        assertEquals("https://api.anthropic.com/v1/messages", capturedUrl.get());
        assertEquals("ant-live-key", capturedHeaders.get().get("x-api-key"));

        AiProviderResult googRes = google.execute(
                request("TEXT", model("TEXT", "GOOGLE", "gemini-2.5-flash"), "hi"));
        assertEquals("ok gemini", googRes.getTextContent());
        assertEquals(42, googRes.getUsageAmount());
        assertTrue(capturedUrl.get().endsWith("/v1beta/models/gemini-2.5-flash:generateContent"));
        assertEquals("goog-live-key", capturedHeaders.get().get("x-goog-api-key"));

        AiProviderResult oaRes = compat.execute(
                request("TEXT", model("TEXT", "OPENAI", "gpt-4o"), "hi"));
        assertEquals("ok openai", oaRes.getTextContent());
        assertEquals("https://api.openai.com/v1/chat/completions", capturedUrl.get());
        assertEquals("Bearer oa-live-key", capturedHeaders.get().get("Authorization"));
    }

    @Test
    void settlementSkipsQuotaDeductionWhenProviderByokIsActive() {
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        when(tasks.updateByExampleSelective(any(), any())).thenReturn(1);
        QuotaConsumeService quota = mock(QuotaConsumeService.class);
        AiUsageRecordMapper usageMapper = mock(AiUsageRecordMapper.class);
        ShortSettlementMapper works = mock(ShortSettlementMapper.class);
        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);

        when(keyService.findActiveKey("u-free-byok", "MINIMAX")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-free-byok", "MINIMAX", "mm-byok-key", null, null, null, true)));

        ShortTaskSettlementService settlement = new ShortTaskSettlementService(
                tasks, quota, usageMapper, works, null);
        settlement.setUserAiProviderKeyService(keyService);

        ShortAiTask task = new ShortAiTask();
        task.setId(101L);
        task.setTaskId("t-byok-1");
        task.setUserId("u-free-byok");
        task.setWorkId("w-1");
        task.setTaskType("VIDEO");
        task.setCostCent(500);

        AiModelConfig m = model("VIDEO", "MINIMAX", "MiniMax-Hailuo-2.3");
        m.setUnitPriceCent(null);

        assertTrue(settlement.settle(task, m, 5));
        verifyNoInteractions(quota);
        verifyNoInteractions(works);

        ArgumentCaptor<AiUsageRecord> usageCaptor = ArgumentCaptor.forClass(AiUsageRecord.class);
        verify(usageMapper).insertSelective(usageCaptor.capture());
        assertEquals(0, usageCaptor.getValue().getCostCent());

        // Also test enriched withProviderByokContext model
        AiModelConfig enriched = UserAiModelSupport.withProviderByokContext(m, "u-free-byok", "MINIMAX", null);
        assertEquals(0, enriched.getUnitPriceCent());
        assertTrue(UserAiModelSupport.isUserKeyModel(enriched));
    }

    @Test
    void deepseekAndAllOpenAiCompatProvidersExecuteWithUserByokBaseUrls() {
        UserAiProviderKeyService keyService = mock(UserAiProviderKeyService.class);
        SysSecretReader secrets = mock(SysSecretReader.class);
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        AtomicReference<Map<String, String>> capturedHeaders = new AtomicReference<>();

        ProviderHttp.Client client = new ProviderHttp.Client() {
            @Override
            public String postJson(String url, Map<String, String> headers, String json, int timeoutSec) {
                capturedUrl.set(url);
                capturedHeaders.set(headers);
                return "{\"choices\":[{\"message\":{\"content\":\"ok compat\"}}],\"usage\":{\"total_tokens\":8}}";
            }

            @Override
            public String getJson(String url, Map<String, String> headers, int timeoutSec) { return "{}"; }

            @Override
            public byte[] getBytes(String url, int timeoutSec) { return new byte[0]; }
        };

        DeepSeekAiProvider deepseek = new DeepSeekAiProvider(null, client);
        deepseek.setUserAiProviderKeyService(keyService);
        OpenAiCompatibleAiProvider compat = new OpenAiCompatibleAiProvider(secrets, client, ms -> {});
        compat.setUserAiProviderKeyService(keyService);

        UserContext.setUserId("u-compat");
        when(keyService.findActiveKey("u-compat", "DEEPSEEK")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-compat", "DEEPSEEK", "ds-user-key", null, null, null, true)));
        AiProviderResult dsRes = deepseek.execute(request("TEXT", model("TEXT", "DEEPSEEK", "deepseek-chat"), "hi"));
        assertEquals("ok compat", dsRes.getTextContent());
        assertEquals("https://api.deepseek.com/chat/completions", capturedUrl.get());
        assertEquals("Bearer ds-user-key", capturedHeaders.get().get("Authorization"));

        Map<String, String> expectedEndpoints = Map.of(
                "DASHSCOPE", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                "ZHIPU", "https://open.bigmodel.cn/api/paas/v4/chat/completions",
                "MOONSHOT", "https://api.moonshot.cn/v1/chat/completions",
                "VOLCENGINE", "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                "HUNYUAN", "https://api.hunyuan.cloud.tencent.com/v1/chat/completions"
        );
        for (Map.Entry<String, String> entry : expectedEndpoints.entrySet()) {
            String code = entry.getKey();
            when(keyService.findActiveKey("u-compat", code)).thenReturn(Optional.of(
                    new ResolvedProviderKey("u-compat", code, code + "-secret", null, null, null, true)));
            AiProviderResult res = compat.execute(request("TEXT", model("TEXT", code, "m-" + code), "hi"));
            assertEquals("ok compat", res.getTextContent());
            assertEquals(entry.getValue(), capturedUrl.get());
            assertEquals("Bearer " + code + "-secret", capturedHeaders.get().get("Authorization"));
        }
    }

    private static AiModelConfig model(String ability, String provider, String code) {
        AiModelConfig m = new AiModelConfig();
        m.setModelId("m-" + code);
        m.setAbilityType(ability);
        m.setProviderCode(provider);
        m.setModelCode(code);
        m.setModelName(code);
        m.setEnabledFlag(1);
        m.setDeletedFlag(0);
        m.setConfigJson("{}");
        return m;
    }

    private static AiProviderRequest request(String ability, AiModelConfig model, String prompt) {
        AiProviderRequest req = new AiProviderRequest();
        req.setAbilityType(ability);
        req.setModel(model);
        req.setPrompt(prompt);
        req.setParams(new LinkedHashMap<>());
        return req;
    }
}
