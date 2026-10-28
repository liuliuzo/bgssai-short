package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

class TencentTokenHubAiProviderTest {

    @Test
    void legacyGlobalAliasRejectedForShortExecutionButCatalogRecognizesHost() throws Exception {
        String alias = "https://www.bgssai-tokenhub-global.com";
        FakeClient http = new FakeClient();
        http.posts.add("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        AiProviderRequest request = request("TEXT", "route", "test");
        request.getModel().setConfigJson("{\"hub\":\"global\",\"base_url\":\"" + alias + "\"}");
        BizException ex = assertThrows(BizException.class, () -> provider(http, "key").execute(request));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertTrue(http.postedUrls.isEmpty());
        var catalog = new com.bgssai.shortai.aimodel.TokenHubModelCatalog(TokenHubClientSupport.DEFAULT_CN_BASE_URL);
        assertEquals(TokenHubClientSupport.DEFAULT_CN_BASE_URL,
                catalog.endpointFor(Map.of("base_url", alias), "route"));
        var devCatalog = new com.bgssai.shortai.aimodel.TokenHubModelCatalog("https://dev.user.bgssai-tokenhub.cn");
        assertEquals("https://dev.user.bgssai-tokenhub.cn",
                devCatalog.endpointFor(Map.of("base_url", "https://www.bgssai-tokenhub.cn"), "deepseek-chat"));
        var resolver = com.bgssai.shortai.aimodel.AiModelResolveService.class.getDeclaredMethod("isHubConfig", AiModelConfig.class);
        resolver.setAccessible(true);
        request.getModel().setProviderCode("legacy");
        assertEquals(false, resolver.invoke(null, request.getModel()));
        request.getModel().setConfigJson("{\"hub\":\"cn\",\"base_url\":\"https://dev.user.bgssai-tokenhub.cn\"}");
        assertEquals(true, resolver.invoke(null, request.getModel()));
        assertTrue(!TokenHubClientSupport.isOfficialHub("https://www.bgssai-tokenhub.com.evil.example"));
        assertTrue(!TokenHubClientSupport.isOfficialHub("https://evil.example/path/bgssai-tokenhub.com"));
        assertTrue(TokenHubClientSupport.isOfficialHub("https://dev.user.bgssai-tokenhub.cn"));
        assertTrue(!TokenHubClientSupport.isOfficialHub("https://dev.user.bgssai-tokenhub.com"));
        assertTrue(!TokenHubClientSupport.isOfficialHub(alias));
    }

    @Test
    void textUsesOpenAiCompatibleChatContract() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"choices\":[{\"message\":{\"content\":\"ok\"}}],\"usage\":{\"total_tokens\":1200}}");
        AiProviderResult result = provider(http, "key").execute(request("TEXT", "hy4-preview", "写剧本"));
        assertEquals("ok", result.getTextContent());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/chat/completions"));
        assertTrue(http.postedBodies.get(0).contains("\"model\":\"hy4-preview\""));
        assertEquals("Bearer key", http.postedHeaders.get(0).get("Authorization"));
    }

    @Test
    void selectedVideoModelIsSentToTokenHub() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"data\":[{\"url\":\"https://cdn.example/m.mp4\"}]}");
        http.bytes = new byte[] { 1, 2 };
        AiProviderResult result = provider(http, "key").execute(request("VIDEO", "wan2.6", "人物奔跑"));
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertTrue(http.postedUrls.get(0).startsWith("https://www.bgssai-tokenhub.cn/v1/videos/generations"));
        assertTrue(http.postedBodies.get(0).contains("\"model\":\"wan2.6\""));
        assertTrue(http.polledUrls.isEmpty());
    }

    @Test
    void selectedImageModelIgnoresVendorHost() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"data\":[{\"b64_json\":\"AQIDBA==\"}]}");
        AiProviderRequest request = request("IMAGE", "kling-v3", "定妆");
        request.getModel().setConfigJson(
                "{\"hub\":\"cn\",\"base_url\":\"https://api-beijing.klingai.com\",\"secret_ref\":\"ai.kling.api_key\"}");
        AiProviderResult result = provider(http, "key").execute(request);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result.getBinaryContent());
        assertEquals("image/png", result.getMimeType());
        assertTrue(http.postedUrls.get(0).startsWith("https://www.bgssai-tokenhub.cn/v1/images/generations"));
        assertTrue(http.postedBodies.get(0).contains("\"model\":\"kling-v3\""));
    }

    @Test
    void videoPollsTokenHubTaskForTheSelectedModel() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"id\":\"k-1\",\"status\":\"queued\"}");
        http.gets.add("{\"status\":\"succeeded\",\"data\":[{\"url\":\"https://cdn.example/k.mp4\"}]}");
        http.bytes = new byte[] { 4 };
        AiProviderRequest request = request("VIDEO", "kling-v3", "转身");
        request.getParams().put("image_url", "https://cdn.example/frame.png");
        AiProviderResult result = provider(http, "key").execute(request);
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertTrue(http.postedBodies.get(0).contains("\"model\":\"kling-v3\""));
        assertTrue(http.postedBodies.get(0).contains("\"image\":\"https://cdn.example/frame.png\""));
        assertTrue(http.polledUrls.get(0).contains("/v1/videos/generations/k-1?model=kling-v3"));
    }

    @Test
    void asyncVideoReturnsUrlOnlyAfterSucceeded() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"id\":\"job-9\",\"status\":\"queued\"}");
        http.gets.add("{\"status\":\"running\"}");
        http.gets.add("{\"status\":\"succeeded\",\"data\":[{\"url\":\"https://cdn.example/job.mp4\"}]}");
        http.bytes = new byte[] { 9, 8, 7 };
        List<Long> sleeps = new ArrayList<>();
        TencentTokenHubAiProvider polling = new TencentTokenHubAiProvider(
                new SysSecretReader(secretRef -> "key"), http, sleeps::add);
        AiProviderRequest request = request("VIDEO", "kling-v3", "奔跑");
        request.getParams().put("image_url", "https://cdn.example/frame.png");
        AiProviderResult result = polling.execute(request);
        assertArrayEquals(new byte[] { 9, 8, 7 }, result.getBinaryContent());
        assertEquals("video/mp4", result.getMimeType());
        assertEquals(1, http.downloads);
        assertTrue(http.postedUrls.get(0).startsWith("https://www.bgssai-tokenhub.cn/v1/videos/generations"));
        assertTrue(!http.postedUrls.get(0).contains("klingai"));
        assertTrue(http.postedBodies.get(0).contains("\"model\":\"kling-v3\""));
        assertTrue(http.postedBodies.get(0).contains("\"image\":\"https://cdn.example/frame.png\""));
        assertTrue(!http.postedBodies.get(0).contains("duration"));
        assertEquals(2, http.polledUrls.size());
        assertTrue(http.polledUrls.get(0).contains("/v1/videos/generations/job-9?model=kling-v3"));
        assertTrue(http.polledUrls.get(1).endsWith("/v1/videos/generations/job-9?model=kling-v3"));
        assertTrue(!sleeps.isEmpty());
        assertTrue(sleeps.get(0) >= 3000L);
    }

    @Test
    void failedVideoDoesNotDownloadMedia() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"id\":\"job-f\",\"status\":\"queued\"}");
        http.gets.add("{\"status\":\"failed\",\"error\":\"上游拒绝\"}");
        BizException ex = assertThrows(BizException.class,
                () -> provider(http, "key").execute(request("VIDEO", "kling-v3", "失败")));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("上游拒绝"));
        assertEquals(0, http.downloads);
        assertTrue(http.polledUrls.get(0).contains("/v1/videos/generations/job-f?model=kling-v3"));
        assertTrue(!http.postedUrls.get(0).contains("klingai"));
    }

    @Test
    void missingHubConfigFailsInsteadOfCallingKling() {
        FakeClient http = new FakeClient();
        TencentTokenHubAiProvider polling = new TencentTokenHubAiProvider(
                new SysSecretReader(secretRef -> "key"), http, ms -> {
                }, "");
        AiProviderRequest request = request("VIDEO", "kling-v3", "跑");
        request.getModel().setConfigJson(
                "{\"allow_direct\":true,\"hub\":\"cn\",\"base_url\":\"https://api-beijing.klingai.com\",\"secret_ref\":\"ai.kling.api_key\"}");
        BizException ex = assertThrows(BizException.class, () -> polling.execute(request));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertTrue(http.postedUrls.isEmpty());
        assertTrue(http.polledUrls.isEmpty());
        assertEquals(0, http.downloads);
        assertTrue(!ex.getMessage().toLowerCase().contains("klingai"));
    }

    @Test
    void speechSendsSelectedModelToTokenHub() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"audio\":\"0102\",\"extra_info\":{\"audio_length\":1500}}");
        AiProviderRequest request = request("VOICE", "qwen-tts", "你好");
        request.getParams().put("voice_id", "v-male-deep-01");
        AiProviderResult result = provider(http, "key").execute(request);
        assertArrayEquals(new byte[] { 1, 2 }, result.getBinaryContent());
        assertEquals(2, result.getDurationSec());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/audio/speech"));
        assertTrue(http.postedBodies.get(0).contains("\"model\":\"qwen-tts\""));
        assertTrue(http.postedBodies.get(0).contains("\"voice\":\"v-male-deep-01\""));
    }

    @Test
    void asrSubmitsAndReturnsStructuredTranscript() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"id\":\"a-1\"}");
        http.gets.add("{\"status\":\"succeeded\",\"output\":{\"text\":\"你好\",\"duration_ms\":2100,\"sentences\":[{\"text\":\"你好\",\"start_ms\":0,\"end_ms\":2000}]}}");
        AiProviderRequest request = request("ASR", "hy-asr-3.0-preview", "识别");
        request.getParams().put("input_url", "https://cdn.example/a.mp3");
        AiProviderResult result = provider(http, "key").execute(request);
        assertEquals("你好", ShortJson.text(ShortJson.map(result.getTextContent()).get("text")));
        assertEquals(3, result.getDurationSec());
        assertTrue(http.polledUrls.get(0).endsWith("/v1/wand/asrproxy/tasks/a-1"));
    }

    @Test
    void musicGeneratesDownloadableAudio() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"base_resp\":{\"status_code\":0},\"data\":{\"audio\":\"https://cdn.example/music.mp3\"},\"extra_info\":{\"music_duration\":30000}}");
        http.bytes = new byte[] { 7, 8 };
        AiProviderResult result = provider(http, "key").execute(request("MUSIC", "minimax-music-v2.6", "悬疑纯音乐"));
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertEquals(30, result.getDurationSec());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/wand/minimax-music/generation"));
    }

    @Test
    void missingCredentialIsRejectedBeforeNetworkCall() {
        FakeClient http = new FakeClient();
        BizException ex = assertThrows(BizException.class,
                () -> provider(http, "").execute(request("TEXT", "hy4-preview", "x")));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertTrue(http.postedUrls.isEmpty());
    }

    @Test
    void devProfileHostWinsOverCatalogWww() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"choices\":[{\"message\":{\"content\":\"dev\"}}],\"usage\":{\"total_tokens\":2}}");
        TencentTokenHubAiProvider provider = new TencentTokenHubAiProvider(
                new SysSecretReader(secretRef -> "sk-hub"), http, ms -> {
                }, "https://dev.user.bgssai-tokenhub.cn");
        AiProviderRequest req = request("TEXT", "deepseek-chat", "大纲");
        req.getModel().setConfigJson(
                "{\"hub\":\"cn\",\"base_url\":\"https://www.bgssai-tokenhub.cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        AiProviderResult result = provider.execute(req);
        assertEquals("dev", result.getTextContent());
        assertTrue(http.postedUrls.get(0).startsWith("https://dev.user.bgssai-tokenhub.cn/v1/chat/completions"));
        http.posts.add("{\"choices\":[{\"message\":{\"content\":\"dev\"}}],\"usage\":{\"total_tokens\":2}}");
        AiProviderRequest bare = request("TEXT", "deepseek-chat", "大纲");
        bare.getModel().setConfigJson("{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        provider.execute(bare);
        assertTrue(http.postedUrls.get(1).startsWith("https://dev.user.bgssai-tokenhub.cn/v1/chat/completions"));
    }

    @Test
    void textPrefersBgssaiTokenhubWhenGatewayConfigured() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"choices\":[{\"message\":{\"content\":\"hub\"}}],\"usage\":{\"total_tokens\":3}}");
        TencentTokenHubAiProvider provider = new TencentTokenHubAiProvider(
                new SysSecretReader(secretRef -> {
                    if (SysSecretReader.AI_TOKENHUB_CN_API_KEY.equals(secretRef)
                            || SysSecretReader.AI_TENCENT_TOKENHUB_API_KEY.equals(secretRef)) {
                        return "sk-hub";
                    }
                    return "";
                }), http, ms -> {
                }, "https://www.bgssai-tokenhub.cn/");
        AiProviderRequest req = request("TEXT", "hy4-preview", "写剧本");
        req.getModel().setConfigJson(
                "{\"hub\":\"cn\",\"base_url\":\"https://www.bgssai-tokenhub.cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        AiProviderResult result = provider.execute(req);
        assertEquals("hub", result.getTextContent());
        assertTrue(http.postedUrls.get(0).startsWith("https://www.bgssai-tokenhub.cn/v1/chat/completions"));
        assertEquals("Bearer sk-hub", http.postedHeaders.get(0).get("Authorization"));
    }

    @Test
    void globalHubRejectedForShortCnPin() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"choices\":[{\"message\":{\"content\":\"intl\"}}],\"usage\":{\"total_tokens\":2}}");
        TencentTokenHubAiProvider provider = new TencentTokenHubAiProvider(
                new SysSecretReader(secretRef -> SysSecretReader.AI_TOKENHUB_GLOBAL_API_KEY.equals(secretRef)
                        ? "sk-global" : "sk-cn"),
                http, ms -> {
                }, "https://www.bgssai-tokenhub.cn");
        AiProviderRequest req = request("TEXT", "gpt-4o-mini", "hello");
        req.getModel().setConfigJson(
                "{\"hub\":\"global\",\"secret_ref\":\"ai.tokenhub_global.api_key\",\"chat_path\":\"/v1/chat/completions\"}");
        BizException ex = assertThrows(BizException.class, () -> provider.execute(req));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertTrue(http.postedUrls.isEmpty());
        assertTrue(!ex.getMessage().toLowerCase().contains("tokenhub"));
    }

    @Test
    void userFacingErrorsHideTokenHubBrand() {
        FakeClient http = new FakeClient();
        BizException ex = assertThrows(BizException.class,
                () -> provider(http, "").execute(request("TEXT", "hy4-preview", "x")));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertTrue(!ex.getMessage().toLowerCase().contains("tokenhub"));
        assertTrue(!ex.getMessage().toLowerCase().contains("api key"));
        assertTrue(http.postedUrls.isEmpty());
    }

    private static TencentTokenHubAiProvider provider(FakeClient http, String key) {
        return new TencentTokenHubAiProvider(new SysSecretReader(secretRef -> key), http, ms -> {
        });
    }

    private static AiProviderRequest request(String ability, String modelCode, String prompt) {
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(TencentTokenHubAiProvider.CODE);
        model.setModelCode(modelCode);
        model.setPriceUnit("PER_CALL");
        model.setTimeoutSec(60);
        model.setConfigJson(
                "{\"hub\":\"cn\",\"base_url\":\"https://www.bgssai-tokenhub.cn\",\"poll_interval_ms\":0,\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType(ability);
        request.setModel(model);
        request.setPrompt(prompt);
        request.setParams(new LinkedHashMap<>());
        return request;
    }

    private static final class FakeClient implements ProviderHttp.Client {
        private final Queue<String> posts = new ArrayDeque<>();
        private final Queue<String> gets = new ArrayDeque<>();
        private final List<String> postedUrls = new ArrayList<>();
        private final List<String> postedBodies = new ArrayList<>();
        private final List<Map<String, String>> postedHeaders = new ArrayList<>();
        private final List<String> polledUrls = new ArrayList<>();
        private byte[] bytes = new byte[0];
        private int downloads;

        @Override
        public String postJson(String url, Map<String, String> headers, String body, int timeoutSec) {
            postedUrls.add(url);
            postedBodies.add(body);
            postedHeaders.add(headers);
            return posts.remove();
        }

        @Override
        public String getJson(String url, Map<String, String> headers, int timeoutSec) {
            polledUrls.add(url);
            return gets.remove();
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) {
            downloads++;
            return bytes;
        }
    }
}
