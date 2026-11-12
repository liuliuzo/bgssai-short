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
    void videoRecoveryOnlyQueriesExistingRequestAndReusesReceipt() {
        FakeClient http = new FakeClient();
        http.gets.add("{\"id\":\"video_0123456789abcdef01234567\",\"status\":\"queued\"}");
        http.gets.add("{\"status\":\"succeeded\",\"data\":[{\"url\":\"/v1/videos/generations/video_0123456789abcdef01234567/content?model=bgssai-video\"}]}");
        http.bytes = new byte[] {1};
        var p = provider(http,"key");
        var receipts = org.mockito.Mockito.mock(com.bgssai.shortai.billing.UsageReceiptService.class);
        var receipt = new com.bgssai.shortai.billing.UsageReceiptRow();
        String id="981ab970-447e-4a04-a2f0-a085ebbf435b";
        org.mockito.Mockito.when(receipts.resumeVideo(id,"https://www.bgssai-tokenhub.cn","key","bgssai-video")).thenReturn(receipt);
        org.springframework.test.util.ReflectionTestUtils.setField(p,"receipts",receipts);
        var req=request("VIDEO","bgssai-video","original");req.getParams().put("resume_request_id",id);
        assertArrayEquals(new byte[]{1},p.execute(req).getBinaryContent());
        assertTrue(http.postedUrls.isEmpty());
        assertEquals("https://www.bgssai-tokenhub.cn/v1/videos/generations/by-request/"+id+"?model=bgssai-video",http.polledUrls.get(0));
        assertEquals(2,http.polledUrls.size());
        org.mockito.Mockito.verify(receipts,org.mockito.Mockito.never()).begin(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.verify(receipts).refresh(receipt,"key");
    }

    @Test
    void unavailableRecoveryQueryNeverSubmitsReplacementGeneration() {
        FakeClient http=new FakeClient();var p=provider(http,"key");
        var receipts=org.mockito.Mockito.mock(com.bgssai.shortai.billing.UsageReceiptService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(p,"receipts",receipts);
        var req=request("VIDEO","bgssai-video","test");req.getParams().put("resume_request_id","request");
        assertThrows(BizException.class,()->p.execute(req));
        assertTrue(http.postedUrls.isEmpty());assertEquals(1,http.polledUrls.size());
    }

    @Test
    void legacyCompanySelectionSendsCanonicalGatewayModelAndKeepsTimeout() {
        var mapper = org.mockito.Mockito.mock(com.bgssai.shortai.mapper.AiModelConfigMapper.class);
        var registry = org.mockito.Mockito.mock(AiProviderRegistry.class);
        var resolver = new com.bgssai.shortai.aimodel.AiModelResolveService();
        org.springframework.test.util.ReflectionTestUtils.setField(resolver, "aiModelConfigMapper", mapper);
        org.springframework.test.util.ReflectionTestUtils.setField(resolver, "aiProviderRegistry", registry);
        AiProviderRequest req = request("VIDEO", "cogvideox", "test");
        AiModelConfig legacy = req.getModel();
        legacy.setModelId("m-tokenhub-video-cogvideox");
        legacy.setAbilityType("VIDEO");
        legacy.setEnabledFlag(1);
        legacy.setDeletedFlag(0);
        legacy.setTimeoutSec(900);
        legacy.setUnitPriceCent(20);
        legacy.setConfigJson("{\"hub\":\"cn\",\"public_alias\":\"bgssai-video\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        org.mockito.Mockito.when(mapper.selectByExample(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(legacy));
        org.mockito.Mockito.when(registry.resolvable(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        req.setModel(resolver.resolve("VIDEO", "bgssai-video"));
        FakeClient http = new FakeClient();
        http.posts.add("{\"data\":[{\"url\":\"https://cdn.example/movie.mp4\"}]}");
        http.bytes = new byte[] {1};
        provider(http, "test-key").execute(req);
        assertEquals("bgssai-video", ShortJson.map(http.postedBodies.get(0)).get("model"));
        assertTrue(http.postedUrls.get(0).startsWith("https://www.bgssai-tokenhub.cn/"));
        assertEquals(60, http.postedTimeouts.get(0)); // Existing submission cap; polling retains model budget.
        assertEquals(900, req.getModel().getTimeoutSec());
        assertEquals(900_000L, TencentTokenHubAiProvider.mediaPollBudgetMs(req.getModel().getTimeoutSec()));
        assertEquals("cogvideox", legacy.getModelCode());
        assertEquals(20, req.getModel().getUnitPriceCent());
    }

    @Test
    void gatewayContentUsesConfiguredOriginAndBearerOnlyForExactModelRoute() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"data\":[{\"url\":\"/v1/videos/generations/cg1:video_0123456789abcdef01234567/content?model=bgssai-video\"}]}");
        http.bytes = new byte[] {1};
        var p = new TencentTokenHubAiProvider(new SysSecretReader(ref -> "key"), http, ms -> { }, "https://dev.user.bgssai-tokenhub.cn");
        p.execute(request("VIDEO", "bgssai-video", "test"));
        assertEquals("https://dev.user.bgssai-tokenhub.cn/v1/videos/generations/cg1:video_0123456789abcdef01234567/content?model=bgssai-video", http.downloadedUrls.get(0));
        assertEquals("Bearer key", http.downloadedHeaders.get(0).get("Authorization"));
        for (String url : List.of("//external.test/content", "/v1/videos/generations/job-1/content?model=other",
                "/v1/videos/generations/job-1/content?model=bgssai-video&extra=1", "/v1/videos/generations/../content?model=bgssai-video")) {
            FakeClient blocked = new FakeClient();
            blocked.posts.add(ShortJson.toJson(Map.of("data", List.of(Map.of("url", url)))));
            assertThrows(BizException.class, () -> provider(blocked, "key").execute(request("VIDEO", "bgssai-video", "test")));
            assertEquals(0, blocked.downloads);
        }
        FakeClient external = new FakeClient();
        external.posts.add("{\"data\":[{\"url\":\"https://cdn.example/movie.mp4\"}]}");
        external.bytes = new byte[] {1};
        provider(external, "key").execute(request("VIDEO", "bgssai-video", "test"));
        assertTrue(external.downloadedHeaders.get(0).isEmpty());
    }

    @Test
    void companyImageUsesItsNativeResolutionWithoutChangingOtherModels() {
        for (String model : List.of("bgssai-image", "sd-turbo", "kling-v3")) {
            FakeClient http = new FakeClient();
            http.posts.add("{\"data\":[{\"b64_json\":\"AQIDBA==\"}]}");
            provider(http, "key").execute(request("IMAGE", model, "one person"));
            assertEquals("kling-v3".equals(model) ? "1024x1792" : "512x512",
                    ShortJson.map(http.postedBodies.get(0)).get("size"));
        }
    }

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
    void companySpeechMapsProductPresetsToVerifiedEdgeVoices() {
        Map<String, String> voices = Map.of(
                "v-female-cold-01", "zh-CN-XiaoyiNeural",
                "v-female-warm-01", "zh-CN-XiaoxiaoNeural",
                "v-male-deep-01", "zh-CN-YunjianNeural",
                "v-male-bright-01", "zh-CN-YunxiNeural",
                "v-narrator-01", "zh-CN-YunyangNeural");
        for (String model : List.of("bgssai-tts", "edge-tts")) {
            for (var entry : voices.entrySet()) {
                FakeClient http = new FakeClient(); http.posts.add("{\"audio\":\"0102\"}");
                AiProviderRequest request = request("VOICE", model, "你好");
                request.getParams().put("voice_id", entry.getKey());
                provider(http, "key").execute(request);
                assertTrue(http.postedBodies.get(0).contains("\"voice\":\"" + entry.getValue() + "\""));
                assertTrue(http.postedBodies.get(0).contains("\"model\":\"" + model + "\""));
            }
        }
    }

    @Test
    void companySpeechUsesChineseDefaultAndPreservesNativeVoiceSelection() {
        FakeClient defaults = new FakeClient(); defaults.posts.add("{\"audio\":\"0102\"}");
        provider(defaults, "key").execute(request("VOICE", "bgssai-tts", "你好"));
        assertTrue(defaults.postedBodies.get(0).contains("\"voice\":\"zh-CN-XiaoxiaoNeural\""));
        FakeClient nativeVoice = new FakeClient(); nativeVoice.posts.add("{\"audio\":\"0102\"}");
        AiProviderRequest request = request("VOICE", "edge-tts", "你好");
        request.getParams().put("voice_id", "zh-CN-YunjianNeural");
        provider(nativeVoice, "key").execute(request);
        assertTrue(nativeVoice.postedBodies.get(0).contains("\"voice\":\"zh-CN-YunjianNeural\""));
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
        assertEquals("https://www.bgssai-tokenhub.cn/v1/native", http.postedUrls.get(0));
        Map<String, Object> envelope = ShortJson.map(http.postedBodies.get(0));
        assertEquals(java.util.Set.of("provider", "operation", "payload"), envelope.keySet());
        assertEquals("minimax", envelope.get("provider"));
        assertEquals("music", envelope.get("operation"));
        Map<?, ?> payload = (Map<?, ?>) envelope.get("payload");
        assertEquals("music-2.6", payload.get("model"));
        assertEquals("悬疑纯音乐", payload.get("prompt"));
        assertEquals(true, payload.get("is_instrumental"));
        assertEquals("url", payload.get("output_format"));
        assertEquals("mp3", ((Map<?, ?>) payload.get("audio_setting")).get("format"));
        assertEquals("Bearer key", http.postedHeaders.get(0).get("Authorization"));
    }

    @Test
    void musicUsesDevNativeRouteDespiteLegacyStoredPathAndKeepsLyrics() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"base_resp\":{\"status_code\":0},\"data\":{\"audio\":\"0708\"},\"extra_info\":{\"audio_length\":2100}}");
        TencentTokenHubAiProvider provider = new TencentTokenHubAiProvider(
                new SysSecretReader(secretRef -> "key"), http, ms -> {},
                "https://dev.user.bgssai-tokenhub.cn/");
        AiProviderRequest request = request("MUSIC", "minimax-music-v3.0", "测试歌曲");
        request.getModel().setConfigJson("{\"hub\":\"cn\",\"base_url\":\"https://www.bgssai-tokenhub.cn\","
                + "\"music_path\":\"/v1/wand/minimax-music/generation\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        request.getParams().put("lyrics", "[Verse] 测试歌词");
        request.getParams().put("is_instrumental", false);
        AiProviderResult result = provider.execute(request);
        assertEquals("https://dev.user.bgssai-tokenhub.cn/v1/native", http.postedUrls.get(0));
        Map<String, Object> envelope = ShortJson.map(http.postedBodies.get(0));
        assertTrue(!envelope.containsKey("model"));
        Map<?, ?> payload = (Map<?, ?>) envelope.get("payload");
        assertEquals("music-3.0", payload.get("model"));
        assertEquals("[Verse] 测试歌词", payload.get("lyrics"));
        assertEquals(false, payload.get("is_instrumental"));
        assertArrayEquals(new byte[] { 7, 8 }, result.getBinaryContent());
        assertEquals(3, result.getDurationSec());
        assertEquals(0, http.downloads);
    }

    @Test
    void musicPreservesNativeModelAndRejectsVendorFailureWithoutDownloading() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"base_resp\":{\"status_code\":1008,\"status_msg\":\"insufficient balance\"}}");
        BizException ex = assertThrows(BizException.class,
                () -> provider(http, "key").execute(request("MUSIC", "music-2.6", "测试音乐")));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        Map<?, ?> payload = (Map<?, ?>) ShortJson.map(http.postedBodies.get(0)).get("payload");
        assertEquals("music-2.6", payload.get("model"));
        assertEquals(0, http.downloads);
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

    @Test
    void slowStatusQueryDoesNotFailAVideoThatIsStillRunning() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"id\":\"video_0123456789abcdef01234567\",\"status\":\"queued\"}");
        http.getFailures.add(new java.net.http.HttpTimeoutException("request timed out"));
        http.gets.add("{\"id\":\"video_0123456789abcdef01234567\",\"status\":\"queued\"}");
        http.gets.add("{\"status\":\"succeeded\",\"data\":[{\"url\":\"https://cdn.example/movie.mp4\"}]}");
        http.bytes = new byte[] {9};
        AiProviderRequest req = request("VIDEO", "bgssai-video", "rain");
        req.getModel().setTimeoutSec(60);
        assertArrayEquals(new byte[] {9}, provider(http, "key").execute(req).getBinaryContent());
        assertEquals(3, http.polledUrls.size());
    }

    @Test
    void mediaPollBudgetHonorsModelTimeout() {
        assertEquals(180_000L, TencentTokenHubAiProvider.mediaPollBudgetMs(60));
        assertEquals(900_000L, TencentTokenHubAiProvider.mediaPollBudgetMs(900));
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
        private final Queue<Exception> getFailures = new ArrayDeque<>();
        private final List<String> postedUrls = new ArrayList<>();
        private final List<String> postedBodies = new ArrayList<>();
        private final List<Integer> postedTimeouts = new ArrayList<>();
        private final List<Map<String, String>> postedHeaders = new ArrayList<>();
        private final List<String> polledUrls = new ArrayList<>();
        private final List<String> downloadedUrls = new ArrayList<>();
        private final List<Map<String, String>> downloadedHeaders = new ArrayList<>();
        private byte[] bytes = new byte[0];
        private int downloads;

        @Override
        public String postJson(String url, Map<String, String> headers, String body, int timeoutSec) {
            postedUrls.add(url);
            postedBodies.add(body);
            postedTimeouts.add(timeoutSec);
            postedHeaders.add(headers);
            return posts.remove();
        }

        @Override
        public String getJson(String url, Map<String, String> headers, int timeoutSec) throws Exception {
            polledUrls.add(url);
            if (!getFailures.isEmpty()) {
                throw getFailures.remove();
            }
            return gets.remove();
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) {
            return getBytes(url, Map.of(), timeoutSec);
        }

        @Override
        public byte[] getBytes(String url, Map<String, String> headers, int timeoutSec) {
            downloadedUrls.add(url);
            downloadedHeaders.add(headers);
            downloads++;
            return bytes;
        }
    }
}
