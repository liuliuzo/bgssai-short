package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.secret.SysSecretReader;

class KlingAiProviderTest {

    @Test
    void imageSubmitPollDownload() {
        FakeTransport http = new FakeTransport();
        http.submitJson = "{\"code\":0,\"data\":{\"task_id\":\"img-1\",\"task_status\":\"submitted\"}}";
        http.pollJson = "{\"code\":0,\"data\":{\"task_id\":\"img-1\",\"task_status\":\"succeed\","
                + "\"task_result\":{\"images\":[{\"url\":\"https://cdn.example/a.png\"}]}}}";
        http.bytes = new byte[] { 1, 2, 3, 4 };
        KlingAiProvider provider = new KlingAiProvider(secret("k"), http, ms -> {
        });
        AiProviderResult result = provider.executeLegacy(request("IMAGE", "kling-v2-1", "a cat"));
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertEquals("image/png", result.getMimeType());
        assertEquals(1080, result.getWidth());
        assertEquals(1920, result.getHeight());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/images/generations"));
        assertTrue(http.postedBodies.get(0).contains("\"aspect_ratio\":\"9:16\""));
        assertTrue(http.postedBodies.get(0).contains("\"model_name\":\"kling-v2-1\""));
        assertTrue(http.polledUrls.get(0).endsWith("/v1/images/generations/img-1"));
    }

    @Test
    void videoUsesImage2VideoWhenFirstFramePresent() {
        FakeTransport http = new FakeTransport();
        http.submitJson = "{\"code\":0,\"data\":{\"task_id\":\"vid-1\",\"task_status\":\"submitted\"}}";
        http.pollJson = "{\"code\":0,\"data\":{\"task_id\":\"vid-1\",\"task_status\":\"succeed\","
                + "\"task_result\":{\"videos\":[{\"url\":\"https://cdn.example/a.mp4\",\"duration\":\"5\"}]}}}";
        http.bytes = new byte[] { 9, 9 };
        KlingAiProvider provider = new KlingAiProvider(secret("k"), http, ms -> {
        });
        AiProviderRequest req = request("VIDEO", "kling-v2-6", "人物转头");
        req.getParams().put("image_base64", "aaaabbbb");
        req.getParams().put("duration_sec", 4);
        AiProviderResult result = provider.executeLegacy(req);
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertEquals("video/mp4", result.getMimeType());
        assertEquals(5, result.getDurationSec());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/videos/image2video"));
        assertTrue(http.postedBodies.get(0).contains("\"image\":\"aaaabbbb\""));
        assertTrue(http.postedBodies.get(0).contains("\"duration\":\"5\""));
    }

    @Test
    void videoFallsBackToText2VideoWithoutFrame() {
        FakeTransport http = new FakeTransport();
        http.submitJson = "{\"code\":0,\"data\":{\"task_id\":\"vid-2\",\"task_status\":\"submitted\"}}";
        http.pollJson = "{\"code\":0,\"data\":{\"task_id\":\"vid-2\",\"task_status\":\"succeeded\","
                + "\"task_result\":{\"videos\":[{\"url\":\"https://cdn.example/b.mp4\",\"duration\":10}]}}}";
        http.bytes = new byte[] { 7 };
        KlingAiProvider provider = new KlingAiProvider(secret("k"), http, ms -> {
        });
        AiProviderRequest req = request("VIDEO", "kling-v2-6", "夜雨街头");
        req.getParams().put("duration_sec", 8);
        provider.executeLegacy(req);
        assertTrue(http.postedUrls.get(0).endsWith("/v1/videos/text2video"));
        assertTrue(http.postedBodies.get(0).contains("\"aspect_ratio\":\"9:16\""));
        assertTrue(http.postedBodies.get(0).contains("\"duration\":\"10\""));
    }

    @Test
    void missingKeyRejected() {
        KlingAiProvider provider = new KlingAiProvider(secret(""), new FakeTransport(), ms -> {
        });
        BizException ex = assertThrows(BizException.class, () -> provider.executeLegacy(request("IMAGE", "kling-v2-1", "x")));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
    }

    @Test
    void textAbilityRejected() {
        KlingAiProvider provider = new KlingAiProvider(secret("k"), new FakeTransport(), ms -> {
        });
        BizException ex = assertThrows(BizException.class, () -> provider.executeLegacy(request("TEXT", "kling-v2-1", "x")));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
    }

    @Test
    void upstreamErrorCodeSurfaced() {
        FakeTransport http = new FakeTransport();
        http.submitJson = "{\"code\":1200,\"message\":\"invalid api key\"}";
        KlingAiProvider provider = new KlingAiProvider(secret("k"), http, ms -> {
        });
        BizException ex = assertThrows(BizException.class, () -> provider.executeLegacy(request("IMAGE", "kling-v2-1", "x")));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        assertEquals("invalid api key", ex.getMessage());
    }

    @Test
    void pollFailedStatus() {
        FakeTransport http = new FakeTransport();
        http.submitJson = "{\"code\":0,\"data\":{\"task_id\":\"img-f\",\"task_status\":\"submitted\"}}";
        http.pollJson = "{\"code\":0,\"data\":{\"task_id\":\"img-f\",\"task_status\":\"failed\","
                + "\"task_status_msg\":\"content risk\"}}";
        KlingAiProvider provider = new KlingAiProvider(secret("k"), http, ms -> {
        });
        BizException ex = assertThrows(BizException.class, () -> provider.executeLegacy(request("IMAGE", "kling-v2-1", "x")));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        assertEquals("content risk", ex.getMessage());
    }

    private static SysSecretReader secret(String value) {
        return new SysSecretReader(key -> value);
    }

    private static AiProviderRequest request(String ability, String modelCode, String prompt) {
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(KlingAiProvider.CODE);
        model.setModelCode(modelCode);
        model.setQualityLevel("STANDARD");
        model.setTimeoutSec(60);
        model.setConfigJson("{\"base_url\":\"https://api-beijing.klingai.com\",\"secret_ref\":\"ai.kling.api_key\","
                + "\"poll_interval_ms\":0}");
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType(ability);
        request.setModel(model);
        request.setPrompt(prompt);
        request.setParams(new LinkedHashMap<>());
        return request;
    }

    private static final class FakeTransport implements KlingAiProvider.Transport {
        private String submitJson = "{}";
        private String pollJson = "{}";
        private byte[] bytes = new byte[0];
        private final List<String> postedUrls = new ArrayList<>();
        private final List<String> postedBodies = new ArrayList<>();
        private final List<String> polledUrls = new ArrayList<>();

        @Override
        public String postJson(String url, String bearer, String body, int timeoutSec) {
            postedUrls.add(url);
            postedBodies.add(body);
            return submitJson;
        }

        @Override
        public String getJson(String url, String bearer, int timeoutSec) {
            polledUrls.add(url);
            return pollJson;
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) {
            return bytes;
        }
    }
}
