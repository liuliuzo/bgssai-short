package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.secret.SysSecretReader;

class OpenAiCompatibleAiProviderTest {

    @Test
    void allowsOnlyTheCompanyHost() {
        assertTrue(CompanyCompatGateway.allows("https://chat.bgssai.com/v1"));
        assertTrue(CompanyCompatGateway.allows("https://chat.bgssai.com/v1/"));
        assertFalse(CompanyCompatGateway.allows("http://chat.bgssai.com/v1"));
        assertFalse(CompanyCompatGateway.allows("https://user:pw@chat.bgssai.com/v1"));
        assertFalse(CompanyCompatGateway.allows("https://chat.bgssai.com/v1?x=1"));
        assertFalse(CompanyCompatGateway.allows("https://chat.bgssai.com.evil/v1"));
        assertFalse(CompanyCompatGateway.allows("https://api.openai.com/v1"));
        assertFalse(CompanyCompatGateway.directModel("TEXT", "qwen-tts"));
        assertTrue(CompanyCompatGateway.directModel("TEXT", "qwen2.5-instruct"));
        assertTrue(CompanyCompatGateway.directModel("VIDEO", "cogvideox-5b"));
        assertTrue(CompanyCompatGateway.directModel("ASR", "whisper-1"));
        assertTrue(CompanyCompatGateway.directModel("ASR", "faster-whisper-small"));
        assertFalse(CompanyCompatGateway.directModel("IMAGE", "kling-v3"));
    }

    @Test
    void asrUploadsMultipartAudioAndMapsSegmentsToSentences() {
        FakeClient http = new FakeClient();
        http.multipartReply = "{\"text\":\"第一句 第二句\",\"segments\":["
                + "{\"start\":0.0,\"end\":1.25,\"text\":\" 第一句 \"},"
                + "{\"start\":1.25,\"end\":2.8,\"text\":\"第二句\"}"
                + "]}";
        AiProviderRequest request = request("ASR", "whisper-1", "");
        request.getParams().put("audio_bytes", new byte[] {'R', 'I', 'F', 'F', 1, 2, 3, 4});
        request.getParams().put("audio_mime", "audio/wav");
        AiProviderResult result = provider(http, "sk-company").execute(request);
        assertTrue(http.postedUrls.get(0).endsWith("/v1/audio/transcriptions"));
        assertEquals("audio.wav", http.multipartFilename);
        assertEquals("whisper-1", http.multipartFields.get("model"));
        assertEquals("zh", http.multipartFields.get("language"));
        assertTrue(result.getTextContent().contains("\"begin_ms\":0"));
        assertTrue(result.getTextContent().contains("\"end_ms\":1250"));
        assertTrue(result.getTextContent().contains("\"text\":\"第一句\""));
        assertTrue(result.getTextContent().contains("\"begin_ms\":1250"));
        assertTrue(result.getTextContent().contains("\"end_ms\":2800"));
    }

    @Test
    void imageReadsBase64FromTheCompanyGateway() throws Exception {
        FakeClient http = new FakeClient();
        http.posts.add("{\"data\":[{\"b64_json\":\"" + Base64.getEncoder().encodeToString("png".getBytes(StandardCharsets.UTF_8)) + "\"}]}");
        AiProviderResult result = provider(http, "sk-company").execute(request("IMAGE", "sd-turbo", "a street"));
        assertEquals("image/png", result.getMimeType());
        assertEquals("png", new String(result.getBinaryContent(), StandardCharsets.UTF_8));
        assertTrue(http.postedUrls.get(0).startsWith("https://chat.bgssai.com/v1/images/generations"));
        assertEquals("Bearer sk-company", http.postedHeaders.get(0).get("Authorization"));
        assertTrue(http.postedBodies.get(0).contains("\"size\":\"512x768\""));
        assertFalse(http.postedBodies.get(0).contains("\"image\""));
    }

    @Test
    void imageForwardsTheReferencePhoto() throws Exception {
        FakeClient http = new FakeClient();
        http.posts.add("{\"data\":[{\"b64_json\":\"" + Base64.getEncoder().encodeToString("png".getBytes(StandardCharsets.UTF_8)) + "\"}]}");
        AiProviderRequest request = request("IMAGE", "bgssai-image", "背面");
        request.getParams().put("image_base64", Base64.getEncoder().encodeToString("face".getBytes(StandardCharsets.UTF_8)));
        request.getParams().put("image_mime", "image/jpeg");
        provider(http, "sk-company").execute(request);
        assertTrue(http.postedBodies.get(0).contains("\"image\":\"data:image/jpeg;base64,"));
    }

    @Test
    void videoPollsUntilMediaArrives() throws Exception {
        FakeClient http = new FakeClient();
        http.posts.add("{\"id\":\"vid-1\",\"status\":\"queued\"}");
        http.gets.add("{\"status\":\"running\"}");
        http.gets.add("{\"status\":\"succeeded\",\"data\":[{\"b64_json\":\""
                + Base64.getEncoder().encodeToString("mp4".getBytes(StandardCharsets.UTF_8)) + "\"}]}");
        AiProviderResult result = provider(http, "sk-company").execute(request("VIDEO", "cogvideox", "rain"));
        assertEquals("video/mp4", result.getMimeType());
        assertEquals("mp4", new String(result.getBinaryContent(), StandardCharsets.UTF_8));
        assertTrue(http.postedUrls.get(0).endsWith("/videos/generations"));
        assertTrue(http.polledUrls.get(0).contains("/videos/generations/vid-1"));
        assertTrue(http.polledUrls.get(0).contains("model=cogvideox"));
        assertEquals(1, http.sleeps);
    }

    @Test
    void voiceAcceptsWavBytesAndJsonAudio() throws Exception {
        FakeClient wav = new FakeClient();
        wav.speech = new byte[] {'R', 'I', 'F', 'F', 1, 2};
        AiProviderResult wavResult = provider(wav, "sk-company").execute(request("VOICE", "piper", "你好"));
        assertEquals("audio/wav", wavResult.getMimeType());
        assertTrue(wav.postedBodies.get(0).contains("\"voice\":\"zh\""));

        FakeClient json = new FakeClient();
        json.speech = ("{\"audio\":\"" + Base64.getEncoder().encodeToString("mp3".getBytes(StandardCharsets.UTF_8)) + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        AiProviderResult jsonResult = provider(json, "sk-company").execute(request("VOICE", "piper", "你好"));
        assertEquals("audio/mpeg", jsonResult.getMimeType());
        assertEquals("mp3", new String(jsonResult.getBinaryContent(), StandardCharsets.UTF_8));
    }

    @Test
    void otherHostsAndMissingKeysDoNotLeakSecrets() {
        FakeClient http = new FakeClient();
        AiModelConfig foreign = new AiModelConfig();
        foreign.setAbilityType("TEXT");
        foreign.setProviderCode(OpenAiCompatibleAiProvider.CODE);
        foreign.setModelCode("gpt-4o-mini");
        foreign.setTimeoutSec(60);
        foreign.setConfigJson("{\"base_url\":\"https://api.openai.com/v1\",\"secret_ref\":\"ai.openai_compat.api_key\"}");
        BizException rejected = assertThrows(BizException.class,
                () -> provider(http, "sk-company").execute(requestWith(foreign)));
        assertFalse(rejected.getMessage().toLowerCase().contains("api key"));
        assertTrue(http.postedUrls.isEmpty());

        BizException missing = assertThrows(BizException.class,
                () -> provider(http, "").execute(request("IMAGE", "sd-turbo", "a street")));
        assertFalse(missing.getMessage().contains("ai.openai_compat.api_key"));
        assertFalse(missing.getMessage().toLowerCase().contains("api key"));
        assertTrue(http.postedUrls.isEmpty());
    }

    private static OpenAiCompatibleAiProvider provider(FakeClient http, String key) {
        return new OpenAiCompatibleAiProvider(new SysSecretReader(ref -> key), http, ms -> http.sleeps++);
    }

    private static AiProviderRequest request(String ability, String modelCode, String prompt) {
        return requestWith(CompanyCompatGateway.synthetic(ability, modelCode));
    }

    private static AiProviderRequest requestWith(AiModelConfig model) {
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType(model.getAbilityType());
        request.setModel(model);
        request.setPrompt("prompt");
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
        private byte[] speech;
        private String multipartReply = "{}";
        private String multipartFilename;
        private Map<String, String> multipartFields = Map.of();
        private int sleeps;

        @Override
        public String postJson(String url, Map<String, String> headers, String body, int timeoutSec) {
            postedUrls.add(url);
            postedBodies.add(body);
            postedHeaders.add(headers);
            return posts.remove();
        }

        @Override
        public byte[] postBytes(String url, Map<String, String> headers, String body, int timeoutSec) {
            postedUrls.add(url);
            postedBodies.add(body);
            postedHeaders.add(headers);
            return speech;
        }

        @Override
        public String postMultipart(String url, Map<String, String> headers, byte[] fileBytes, String filename,
                String fileMime, Map<String, String> fields, int timeoutSec) {
            postedUrls.add(url);
            postedHeaders.add(headers);
            this.multipartFilename = filename;
            this.multipartFields = fields;
            return multipartReply;
        }

        @Override
        public String getJson(String url, Map<String, String> headers, int timeoutSec) {
            polledUrls.add(url);
            return gets.remove();
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) {
            return new byte[0];
        }
    }
}
