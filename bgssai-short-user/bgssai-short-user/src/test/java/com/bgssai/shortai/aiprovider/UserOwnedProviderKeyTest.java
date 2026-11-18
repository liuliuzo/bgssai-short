package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.secret.SysSecretReader;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

class UserOwnedProviderKeyTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void presetsKeepExistingProvidersAndAddOfficialOnes() {
        assertEquals("KLING", UserAiModelSupport.normalizeProvider("KLINGAI"));
        assertEquals("ANTHROPIC", UserAiModelSupport.normalizeProvider("CLAUDE"));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.normalizeProvider("TOKENHUB"));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.normalizeProvider("BGSSAI-TOKENHUB"));
        assertEquals("TENCENT_TOKENHUB", UserAiModelSupport.normalizeProvider("TENCENT-TOKENHUB"));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider("TENCENT_TOKENHUB", null));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider("TENCENT_TOKENHUB", ""));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT_TOKENHUB", "https://www.bgssai-tokenhub.cn"));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT_TOKENHUB", "https://www.bgssai-tokenhub.cn/v1"));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT_TOKENHUB", "https://dev.user.bgssai-tokenhub.cn"));
        assertEquals("TENCENT_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT_TOKENHUB", "https://tokenhub.tencentmaas.com"));
        assertEquals("TENCENT_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT_TOKENHUB", "https://tokenhub.tencentmaas.com/v1"));
        assertEquals("TENCENT_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT-TOKENHUB", "https://tokenhub.tencentmaas.cn"));
        assertEquals("TENCENT_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT_TOKENHUB", "https://tokenhub-intl.tencentmaas.com"));
        assertEquals("TENCENT_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TENCENT_TOKENHUB", "https://tokenhub-intl.tencentmaas.cn/v1"));
        assertEquals("BGSSAI_TOKENHUB", UserAiModelSupport.canonicalizeOwnedProvider(
                "TOKENHUB", "https://tokenhub.tencentmaas.com"));
        UserAiModelSupport.assertOwnedHost("TENCENT_TOKENHUB", "https://tokenhub.tencentmaas.com/v1");
        UserAiModelSupport.assertOwnedHost("TENCENT_TOKENHUB", "https://tokenhub-intl.tencentmaas.cn");
        assertThrows(BizException.class, () -> UserAiModelSupport.normalizeBaseUrl(
                "https://tokenhub.tencentmaas.com", "BGSSAI_TOKENHUB"));
        assertThrows(BizException.class, () -> UserAiModelSupport.normalizeBaseUrl(
                "https://www.bgssai-tokenhub.cn", "TENCENT_TOKENHUB"));
        assertThrows(BizException.class, () -> UserAiModelSupport.normalizeBaseUrl(
                "https://www.bgssai-tokenhub.com", "BGSSAI_TOKENHUB"));
        assertThrows(BizException.class, () -> UserAiModelSupport.assertOwnedHost(
                "TENCENT_TOKENHUB", "https://evil.tencentmaas.com"));
        assertThrows(BizException.class, () -> UserAiModelSupport.assertOwnedHost(
                "TENCENT_TOKENHUB", "https://tokenhub.tencentmaas.com/v1/chat/completions"));
        assertEquals("https://api.openai.com/v1", UserAiModelSupport.defaultBaseUrl("OPENAI"));
        assertEquals("https://api.anthropic.com", UserAiModelSupport.defaultBaseUrl("ANTHROPIC"));
        assertEquals("https://www.bgssai-tokenhub.cn", UserAiModelSupport.defaultBaseUrl("BGSSAI_TOKENHUB"));
        assertEquals("https://www.bgssai-tokenhub.cn", UserAiModelSupport.defaultBaseUrl("TOKENHUB"));
        assertEquals("https://tokenhub.tencentmaas.com", UserAiModelSupport.defaultBaseUrl("TENCENT_TOKENHUB"));
        assertEquals("https://api.openai.com/v1/models",
                UserAiModelSupport.connectivityProbeUrl("OPENAI", "https://api.openai.com/v1"));
        assertEquals("https://api.anthropic.com/v1/models",
                UserAiModelSupport.connectivityProbeUrl("ANTHROPIC", "https://api.anthropic.com"));
        assertEquals("https://www.bgssai-tokenhub.cn/v1/models",
                UserAiModelSupport.connectivityProbeUrl("BGSSAI_TOKENHUB", "https://www.bgssai-tokenhub.cn"));
        assertEquals("https://tokenhub.tencentmaas.com/v1/models",
                UserAiModelSupport.connectivityProbeUrl("TENCENT_TOKENHUB", "https://tokenhub.tencentmaas.com/v1"));
        assertEquals("https://api.deepseek.com/models",
                UserAiModelSupport.connectivityProbeUrl("DEEPSEEK", "https://api.deepseek.com"));
        assertEquals("https://api.x.ai/v1/models",
                UserAiModelSupport.connectivityProbeUrl("XAI", "https://api.x.ai/v1"));
        List<String> codes = UserAiModelSupport.buildProviderPresets().stream()
                .map(item -> item.getProviderCode()).toList();
        assertEquals(List.of("MINIMAX", "KLING", "PIXVERSE", "DEEPSEEK", "XAI", "OPENAI", "ANTHROPIC",
                "BGSSAI_TOKENHUB", "TENCENT_TOKENHUB"), codes);
        var tencent = UserAiModelSupport.buildProviderPresets().stream()
                .filter(item -> "TENCENT_TOKENHUB".equals(item.getProviderCode())).findFirst().orElseThrow();
        assertEquals("腾讯云 TokenHub", tencent.getProviderName());
        assertEquals(List.of("TEXT"), tencent.getAbilities().stream().map(ab -> ab.getAbilityType()).toList());
        assertEquals("hy4", tencent.getAbilities().get(0).getModels().get(0).getModelCode());

        UserAiModelKey row = new UserAiModelKey();
        row.setKeyId("umk-openai-1");
        row.setUserId("u-1");
        row.setAbilityType("TEXT");
        row.setProviderCode("TENCENT_TOKENHUB");
        row.setBaseUrl("https://www.bgssai-tokenhub.cn");
        AiModelConfig legacy = UserAiModelSupport.toTransientModelConfig(row);
        assertEquals("BGSSAI_TOKENHUB", legacy.getProviderCode());
        assertEquals(0, legacy.getUnitPriceCent());
        assertTrue(UserAiModelSupport.isUserKeyModel(legacy));

        row.setKeyId("umk-openai-1");
        row.setProviderCode("OPENAI");
        row.setBaseUrl("https://api.openai.com/v1");
        row.setModelCode("gpt-6-luna");
        row.setModelName("GPT-6 Luna");
        row.setBaseUrl("https://api.openai.com/v1");
        row.setEnabled(1);
        AiModelConfig priced = UserAiModelSupport.toTransientModelConfig(row);
        assertEquals(0, priced.getUnitPriceCent());
        assertTrue(UserAiModelSupport.isUserKeyModel(priced));
    }

    @Test
    void registrySendsOwnedOpenAiToOfficialProviderAndLeavesPlatformOnHub() {
        UserAiProviderKeyService keys = mock(UserAiProviderKeyService.class);
        when(keys.findActiveKey("u-oa", "OPENAI")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-oa", "OPENAI", "oa-owned", null, null, null, true)));
        SysSecretReader secrets = mock(SysSecretReader.class);
        TencentTokenHubAiProvider hub = new TencentTokenHubAiProvider(secrets, "https://www.bgssai-tokenhub.cn");
        OpenAiCompatibleAiProvider compat = new OpenAiCompatibleAiProvider(secrets);
        OpenAiAiProvider openai = new OpenAiAiProvider();
        AiProviderRegistry registry = new AiProviderRegistry(List.of(hub, compat, openai), keys);

        AiModelConfig platform = model("TEXT", "OPENAI", "gpt-6-luna", "{}");
        assertSame(hub, registry.resolve(platform));

        UserContext.setUserId("u-oa");
        assertSame(openai, registry.resolve(platform));
        assertSame(openai, registry.resolve(userModel("TEXT", "OPENAI", "gpt-6-luna",
                "https://api.openai.com/v1", "umk-oa")));
    }

    @Test
    void sameOpenAiModelUsesTheSelectedRowCredentialEvenWithLegacyProviderKeys() {
        UserAiModelKeyService modelKeys = mock(UserAiModelKeyService.class);
        when(modelKeys.requirePlainApiKey("u-1", "umk-first")).thenReturn("first-test-key");
        when(modelKeys.requirePlainApiKey("u-1", "umk-second")).thenReturn("second-test-key");
        AtomicReference<Map<String, String>> headers = new AtomicReference<>();
        OpenAiAiProvider provider = new OpenAiAiProvider(modelKeys, jsonClient(new AtomicReference<>(), headers,
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}"));
        UserAiProviderKeyService legacyKeys = mock(UserAiProviderKeyService.class);
        provider.setUserAiProviderKeyService(legacyKeys);

        for (String name : List.of("first", "second")) {
            AiModelConfig model = userModel("TEXT", "OPENAI", "gpt-6-luna", "https://api.openai.com/v1", "umk-" + name);
            provider.execute(request("TEXT", model, "test"));
            assertEquals("Bearer " + name + "-test-key", headers.get().get("Authorization"));
        }
        verifyNoInteractions(legacyKeys);
    }

    @Test
    void openAiUserKeyUsesOfficialTextImageAndAsrEndpoints() throws Exception {
        UserAiModelKeyService modelKeys = mock(UserAiModelKeyService.class);
        when(modelKeys.requirePlainApiKey("u-1", "umk-oa")).thenReturn("oa-user-secret");
        AtomicReference<String> url = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<Map<String, String>> headers = new AtomicReference<>();
        AtomicReference<Map<String, String>> fields = new AtomicReference<>();
        ProviderHttp.Client http = new ProviderHttp.Client() {
            @Override
            public String postJson(String target, Map<String, String> h, String json, int timeoutSec) {
                url.set(target);
                headers.set(h);
                body.set(json);
                if (target.endsWith("/images/generations")) {
                    return "{\"data\":[{\"b64_json\":\"aGVsbG8=\"}]}";
                }
                return "{\"choices\":[{\"message\":{\"content\":\"ok openai\"}}],\"usage\":{\"total_tokens\":9}}";
            }

            @Override
            public String postMultipartField(String target, Map<String, String> h, String fileField,
                    byte[] fileBytes, String filename, String mimeType, Map<String, String> form, int timeoutSec) {
                url.set(target);
                headers.set(h);
                fields.set(form);
                assertEquals("image", fileField);
                return "{\"data\":[{\"b64_json\":\"aGVsbG8=\"}]}";
            }

            @Override
            public String postMultipart(String target, Map<String, String> h, byte[] fileBytes, String filename,
                    String mimeType, Map<String, String> form, int timeoutSec) {
                url.set(target);
                headers.set(h);
                fields.set(form);
                return "{\"text\":\"你好\"}";
            }

            @Override
            public String getJson(String target, Map<String, String> h, int timeoutSec) {
                return "{}";
            }

            @Override
            public byte[] getBytes(String target, int timeoutSec) {
                return new byte[0];
            }
        };
        OpenAiAiProvider provider = new OpenAiAiProvider(modelKeys, http);
        AiModelConfig textModel = userModel("TEXT", "OPENAI", "gpt-6-luna", "https://api.openai.com/v1", "umk-oa");
        AiProviderResult text = provider.execute(request("TEXT", textModel, "写一集"));
        assertEquals("ok openai", text.getTextContent());
        assertEquals("https://api.openai.com/v1/chat/completions", url.get());
        assertEquals("Bearer oa-user-secret", headers.get().get("Authorization"));
        assertTrue(body.get().contains("\"model\":\"gpt-6-luna\""));

        AiProviderResult image = provider.execute(request("IMAGE",
                userModel("IMAGE", "OPENAI", "gpt-image-2.6-sunburst", "https://api.openai.com/v1", "umk-oa"),
                "夜景"));
        assertEquals("https://api.openai.com/v1/images/generations", url.get());
        assertTrue(image.getBinaryContent().length > 0);
        assertTrue(body.get().contains("\"model\":\"gpt-image-2.6-sunburst\""));

        AiProviderRequest edit = request("IMAGE",
                userModel("IMAGE", "OPENAI", "gpt-image-2.6-flare", "https://api.openai.com/v1", "umk-oa"),
                "改成侧脸");
        edit.getParams().put("image_base64", "aGVsbG8=");
        provider.execute(edit);
        assertEquals("https://api.openai.com/v1/images/edits", url.get());
        assertEquals("gpt-image-2.6-flare", fields.get().get("model"));

        AiProviderRequest asr = request("ASR",
                userModel("ASR", "OPENAI", "gpt-transcribe", "https://api.openai.com/v1", "umk-oa"), "转写");
        asr.getParams().put("audio_base64", "aGVsbG8=");
        AiProviderResult transcript = provider.execute(asr);
        assertEquals("https://api.openai.com/v1/audio/transcriptions", url.get());
        assertEquals("gpt-transcribe", fields.get().get("model"));
        assertTrue(transcript.getTextContent().contains("你好"));

        assertThrows(BizException.class, () -> provider.execute(request("VIDEO", textModel, "视频")));
    }

    @Test
    void claudeUserKeyPostsMessagesAndVendorKeyStillWorks() throws Exception {
        UserAiModelKeyService modelKeys = mock(UserAiModelKeyService.class);
        when(modelKeys.requirePlainApiKey("u-1", "umk-claude")).thenReturn("claude-user-secret");
        UserAiProviderKeyService providerKeys = mock(UserAiProviderKeyService.class);
        when(providerKeys.findActiveKey("u-vendor", "ANTHROPIC")).thenReturn(Optional.of(
                new ResolvedProviderKey("u-vendor", "ANTHROPIC", "vendor-claude", null, null, null, true)));
        AtomicReference<String> url = new AtomicReference<>();
        AtomicReference<Map<String, String>> headers = new AtomicReference<>();
        ProviderHttp.Client http = jsonClient(url, headers,
                "{\"content\":[{\"type\":\"text\",\"text\":\"ok claude\"}],\"usage\":{\"input_tokens\":3,\"output_tokens\":4}}");
        AnthropicAiProvider provider = new AnthropicAiProvider(http, providerKeys);
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "userAiModelKeyService", modelKeys);

        AiProviderResult owned = provider.execute(request("TEXT",
                userModel("TEXT", "ANTHROPIC", "claude-opus-5-5", "https://api.anthropic.com", "umk-claude"),
                "大纲"));
        assertEquals("ok claude", owned.getTextContent());
        assertEquals(7, owned.getUsageAmount());
        assertEquals("https://api.anthropic.com/v1/messages", url.get());
        assertEquals("claude-user-secret", headers.get().get("x-api-key"));
        assertEquals("2023-06-01", headers.get().get("anthropic-version"));

        UserContext.setUserId("u-vendor");
        provider.execute(request("TEXT", model("TEXT", "ANTHROPIC", "claude-sonnet-5-5", "{}"), "续写"));
        assertEquals("vendor-claude", headers.get().get("x-api-key"));
        assertThrows(BizException.class, () -> provider.execute(request("IMAGE",
                userModel("IMAGE", "ANTHROPIC", "claude-opus-5-5", "https://api.anthropic.com", "umk-claude"),
                "图")));
    }

    @Test
    void tokenHubUserKeyUsesCnGatewayAndSkipsPlatformSecret() throws Exception {
        UserAiModelKeyService modelKeys = mock(UserAiModelKeyService.class);
        when(modelKeys.requirePlainApiKey("u-1", "umk-hub")).thenReturn("hub-user-secret");
        SysSecretReader secrets = mock(SysSecretReader.class);
        AtomicReference<String> url = new AtomicReference<>();
        AtomicReference<Map<String, String>> headers = new AtomicReference<>();
        ProviderHttp.Client http = jsonClient(url, headers,
                "{\"choices\":[{\"message\":{\"content\":\"ok hub\"}}],\"usage\":{\"total_tokens\":2}}");
        TencentTokenHubAiProvider provider = new TencentTokenHubAiProvider(
                secrets, http, ms -> { }, "https://www.bgssai-tokenhub.cn");
        provider.setUserAiModelKeyService(modelKeys);

        AiProviderResult result = provider.execute(request("TEXT",
                userModel("TEXT", "TENCENT_TOKENHUB", "deepseek-chat", "https://www.bgssai-tokenhub.cn", "umk-hub"),
                "大纲"));
        assertEquals("ok hub", result.getTextContent());
        assertEquals("https://www.bgssai-tokenhub.cn/v1/chat/completions", url.get());
        assertEquals("Bearer hub-user-secret", headers.get().get("Authorization"));
        verifyNoInteractions(secrets);

        AiModelConfig global = userModel("TEXT", "TENCENT_TOKENHUB", "deepseek-chat",
                "https://www.bgssai-tokenhub.com", "umk-hub");
        BizException rejected = assertThrows(BizException.class, () -> provider.execute(request("TEXT", global, "x")));
        assertTrue(rejected.getMessage().contains("不在当前可用范围内")
                || rejected.getMessage().contains("base_url"));

        AiModelConfig tencentHost = userModel("TEXT", "BGSSAI_TOKENHUB", "deepseek-chat",
                "https://tokenhub.tencentmaas.com", "umk-hub");
        BizException crossed = assertThrows(BizException.class, () -> provider.execute(request("TEXT", tencentHost, "x")));
        assertTrue(crossed.getMessage().contains("不在当前可用范围内"));
        assertEquals("https://www.bgssai-tokenhub.cn/v1/chat/completions", url.get());
    }

    @Test
    void tencentCloudKeyStaysOnOfficialHostAndLegacyCompanyHubStaysOnBgssai() throws Exception {
        UserAiModelKeyService modelKeys = mock(UserAiModelKeyService.class);
        when(modelKeys.requirePlainApiKey("u-1", "umk-tx")).thenReturn("tencent-user-secret");
        when(modelKeys.requirePlainApiKey("u-1", "umk-hub")).thenReturn("hub-user-secret");
        SysSecretReader secrets = mock(SysSecretReader.class);
        TencentTokenHubAiProvider hub = new TencentTokenHubAiProvider(secrets, "https://www.bgssai-tokenhub.cn");
        hub.setUserAiModelKeyService(modelKeys);
        AtomicReference<String> url = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<Map<String, String>> headers = new AtomicReference<>();
        ProviderHttp.Client http = new ProviderHttp.Client() {
            @Override
            public String postJson(String target, Map<String, String> h, String json, int timeoutSec) {
                url.set(target);
                headers.set(h);
                body.set(json);
                return "{\"choices\":[{\"message\":{\"content\":\"ok tencent\"}}],\"usage\":{\"total_tokens\":6}}";
            }

            @Override
            public String getJson(String target, Map<String, String> h, int timeoutSec) {
                throw new AssertionError("unexpected get");
            }

            @Override
            public byte[] getBytes(String target, int timeoutSec) {
                throw new AssertionError("unexpected bytes");
            }
        };
        TencentCloudTokenHubAiProvider cloud = new TencentCloudTokenHubAiProvider(modelKeys, http);
        AiProviderRegistry registry = new AiProviderRegistry(List.of(hub, cloud), mock(UserAiProviderKeyService.class));

        AiModelConfig legacy = userModel("TEXT", "TENCENT_TOKENHUB", "deepseek-chat",
                "https://www.bgssai-tokenhub.cn", "umk-hub");
        assertSame(hub, registry.resolve(legacy));
        AiModelConfig named = userModel("TEXT", "BGSSAI_TOKENHUB", "deepseek-chat",
                "https://www.bgssai-tokenhub.cn/v1", "umk-hub");
        assertSame(hub, registry.resolve(named));

        AiModelConfig guangzhou = userModel("TEXT", "TENCENT_TOKENHUB", "hy4",
                "https://tokenhub.tencentmaas.com", "umk-tx");
        assertSame(cloud, registry.resolve(guangzhou));
        AiProviderResult text = cloud.execute(request("TEXT", guangzhou, "大纲"));
        assertEquals("ok tencent", text.getTextContent());
        assertEquals(6, text.getUsageAmount());
        assertEquals("https://tokenhub.tencentmaas.com/v1/chat/completions", url.get());
        assertEquals("Bearer tencent-user-secret", headers.get().get("Authorization"));
        assertTrue(body.get().contains("\"model\":\"hy4\""));
        verifyNoInteractions(secrets);

        AiModelConfig versioned = userModel("TEXT", "TENCENT_TOKENHUB", "deepseek-v4-pro",
                "https://tokenhub.tencentmaas.com/v1", "umk-tx");
        assertSame(cloud, registry.resolve(versioned));
        cloud.execute(request("TEXT", versioned, "续写"));
        assertEquals("https://tokenhub.tencentmaas.com/v1/chat/completions", url.get());
        assertTrue(body.get().contains("\"model\":\"deepseek-v4-pro\""));

        AiModelConfig singapore = userModel("TEXT", "TENCENT-TOKENHUB", "kimi-k3",
                "https://tokenhub-intl.tencentmaas.com", "umk-tx");
        assertSame(cloud, registry.resolve(singapore));

        ProviderHttp.Client blocked = new ProviderHttp.Client() {
            @Override
            public String postJson(String target, Map<String, String> h, String json, int timeoutSec) {
                throw new AssertionError("must not send key");
            }

            @Override
            public String getJson(String target, Map<String, String> h, int timeoutSec) {
                throw new AssertionError("must not send key");
            }

            @Override
            public byte[] getBytes(String target, int timeoutSec) {
                throw new AssertionError("must not send key");
            }
        };
        TencentCloudTokenHubAiProvider sealed = new TencentCloudTokenHubAiProvider(modelKeys, blocked);
        AiModelConfig companyOnTencent = userModel("TEXT", "TENCENT_TOKENHUB", "hy4",
                "https://www.bgssai-tokenhub.cn", "umk-tx");
        BizException companyRejected = assertThrows(BizException.class,
                () -> sealed.execute(request("TEXT", companyOnTencent, "x")));
        assertTrue(companyRejected.getMessage().contains("不在当前可用范围内"));
        assertThrows(BizException.class, () -> sealed.execute(request("IMAGE", guangzhou, "图")));
        assertThrows(BizException.class, () -> sealed.execute(request("VIDEO", guangzhou, "视频")));
        assertThrows(BizException.class, () -> sealed.execute(request("VOICE", guangzhou, "配音")));
    }

    private static ProviderHttp.Client jsonClient(AtomicReference<String> url,
            AtomicReference<Map<String, String>> headers, String body) {
        return new ProviderHttp.Client() {
            @Override
            public String postJson(String target, Map<String, String> h, String json, int timeoutSec) {
                url.set(target);
                headers.set(h);
                return body;
            }

            @Override
            public String getJson(String target, Map<String, String> h, int timeoutSec) {
                return "{}";
            }

            @Override
            public byte[] getBytes(String target, int timeoutSec) {
                return new byte[0];
            }
        };
    }

    private static AiModelConfig userModel(String ability, String provider, String code, String baseUrl, String keyId) {
        AiModelConfig model = model(ability, provider, code,
                "{\"credential_source\":\"USER_KEY\",\"user_key_id\":\"" + keyId
                        + "\",\"user_id\":\"u-1\",\"base_url\":\"" + baseUrl + "\"}");
        model.setModelId(keyId);
        return model;
    }

    private static AiModelConfig model(String ability, String provider, String code, String configJson) {
        AiModelConfig model = new AiModelConfig();
        model.setModelId("m-" + code);
        model.setAbilityType(ability);
        model.setProviderCode(provider);
        model.setModelCode(code);
        model.setModelName(code);
        model.setEnabledFlag(1);
        model.setDeletedFlag(0);
        model.setConfigJson(configJson);
        return model;
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
