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
    void minimaxVideoSubmitsPollsAndDownloads() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"task_id\":\"m-1\"}");
        http.gets.add("{\"task\":{\"status\":\"success\",\"duration\":6,\"content\":{\"url\":\"https://cdn.example/m.mp4\"}}}");
        http.bytes = new byte[] { 1, 2 };
        AiProviderResult result = provider(http, "key").execute(request("VIDEO", "minimax-video-h3", "人物奔跑"));
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertEquals(6, result.getDurationSec());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/wand/minimax-video-v2/generation"));
        assertTrue(http.polledUrls.get(0).endsWith("/v1/wand/minimax-video-v2/tasks/m-1"));
    }

    @Test
    void pixverseImageVideoUsesDocumentedImageField() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"ErrCode\":0,\"Resp\":{\"video_id\":\"p-1\"}}");
        http.gets.add("{\"ErrCode\":0,\"Resp\":{\"status\":1,\"url\":\"https://cdn.example/p.mp4\",\"outputWidth\":720,\"outputHeight\":1280}}");
        http.bytes = new byte[] { 3 };
        AiProviderRequest request = request("VIDEO", "pixverse-video-v6.0", "镜头推进");
        request.getParams().put("image_url", "https://cdn.example/frame.png");
        AiProviderResult result = provider(http, "key").execute(request);
        assertEquals(720, result.getWidth());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/wand/pixverse/image-to-video"));
        assertTrue(http.postedBodies.get(0).contains("\"img_id\":\"https://cdn.example/frame.png\""));
    }

    @Test
    void klingImageVideoUsesContentsContract() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"code\":0,\"data\":{\"id\":\"k-1\"}}");
        http.gets.add("{\"code\":0,\"data\":[{\"status\":\"succeeded\",\"outputs\":[{\"url\":\"https://cdn.example/k.mp4\",\"duration\":5}]}]}");
        http.bytes = new byte[] { 4 };
        AiProviderRequest request = request("VIDEO", "kling-video-v3", "转身");
        request.getParams().put("image_url", "https://cdn.example/frame.png");
        AiProviderResult result = provider(http, "key").execute(request);
        assertEquals(5, result.getDurationSec());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/wand/kling/image-to-video"));
        assertTrue(http.postedBodies.get(0).contains("\"type\":\"first_frame\""));
    }

    @Test
    void speechDecodesAudioAndUsesSupplierDuration() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"base_resp\":{\"status_code\":0},\"data\":{\"audio\":\"0102\"},\"extra_info\":{\"audio_length\":1500}}");
        AiProviderRequest request = request("VOICE", "minimax-speech-2.8-hd", "你好");
        request.getParams().put("voice_id", "v-male-deep-01");
        AiProviderResult result = provider(http, "key").execute(request);
        assertArrayEquals(new byte[] { 1, 2 }, result.getBinaryContent());
        assertEquals(2, result.getDurationSec());
        assertTrue(http.postedBodies.get(0).contains("\"voice_id\":\"male-qn-qingse\""));
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
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
        assertTrue(http.postedUrls.isEmpty());
    }

    @Test
    void textPrefersBgssaiTokenhubWhenGatewayConfigured() {
        FakeClient http = new FakeClient();
        http.posts.add("{\"choices\":[{\"message\":{\"content\":\"hub\"}}],\"usage\":{\"total_tokens\":3}}");
        TencentTokenHubAiProvider provider = new TencentTokenHubAiProvider(
                new SysSecretReader(secretRef -> "native-key"), http, ms -> {
                }, "https://www.bgssai-tokenhub-cn.com/", "sk-hub");
        AiProviderResult result = provider.execute(request("TEXT", "hy4-preview", "写剧本"));
        assertEquals("hub", result.getTextContent());
        assertTrue(http.postedUrls.get(0).startsWith("https://www.bgssai-tokenhub-cn.com/v1/chat/completions"));
        assertEquals("Bearer sk-hub", http.postedHeaders.get(0).get("Authorization"));
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
        model.setConfigJson("{\"base_url\":\"https://tokenhub.tencentmaas.com\",\"poll_interval_ms\":0}");
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
            return bytes;
        }
    }
}
