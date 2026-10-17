package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.secret.SysSecretReader;

class MultiVideoProviderTest {

    @Test
    void minimaxImageToVideoDownloadsFile() {
        FakeHttp http = new FakeHttp();
        http.postJson = "{\"task_id\":\"mm-1\",\"base_resp\":{\"status_code\":0}}";
        http.getByUrl.put("https://api.minimaxi.com/v1/query/video_generation?task_id=mm-1",
                "{\"status\":\"Success\",\"file_id\":\"f-1\",\"base_resp\":{\"status_code\":0}}");
        http.getByUrl.put("https://api.minimaxi.com/v1/files/retrieve?file_id=f-1",
                "{\"file\":{\"download_url\":\"https://cdn.example/v.mp4\"},\"base_resp\":{\"status_code\":0}}");
        http.bytes = new byte[] { 1, 2 };
        MiniMaxAiProvider provider = new MiniMaxAiProvider(secret("k"), http, ms -> {
        });
        AiProviderRequest req = request("VIDEO", "MINIMAX", "MiniMax-Hailuo-2.3",
                "https://api.minimaxi.com", "ai.minimax.api_key");
        req.getParams().put("image_base64", "aaaa");
        // Direct vendor execute is disabled; legacy path remains for transport regression only.
        org.junit.jupiter.api.Assertions.assertThrows(
                com.bgssai.shortai.common.exception.BizException.class, () -> provider.execute(req));
        AiProviderResult result = provider.executeLegacy(req);
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertEquals("video/mp4", result.getMimeType());
        assertTrue(http.postedUrls.get(0).endsWith("/v1/video_generation"));
        assertTrue(http.postedBodies.get(0).contains("first_frame_image"));
    }

    @Test
    void seedanceTextToVideo() {
        FakeHttp http = new FakeHttp();
        http.postJson = "{\"id\":\"cgt-1\"}";
        http.getByUrl.put("https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks/cgt-1",
                "{\"id\":\"cgt-1\",\"status\":\"succeeded\",\"content\":{\"video_url\":\"https://cdn.example/s.mp4\"},\"duration\":5}");
        http.bytes = new byte[] { 3 };
        SeedanceAiProvider provider = new SeedanceAiProvider(secret("k"), http, ms -> {
        });
        AiProviderRequest req = request("VIDEO", "SEEDANCE", "doubao-seedance-2-0-260128",
                "https://ark.cn-beijing.volces.com/api/v3", "ai.seedance.api_key");
        org.junit.jupiter.api.Assertions.assertThrows(
                com.bgssai.shortai.common.exception.BizException.class, () -> provider.execute(req));
        AiProviderResult result = provider.executeLegacy(req);
        assertArrayEquals(http.bytes, result.getBinaryContent());
        assertTrue(http.postedBodies.get(0).contains("\"ratio\":\"9:16\""));
    }

    @Test
    void veoDirectVendorDisabled() {
        FakeHttp http = new FakeHttp();
        VeoAiProvider provider = new VeoAiProvider(secret("k"), http, ms -> {
        });
        org.junit.jupiter.api.Assertions.assertThrows(
                com.bgssai.shortai.common.exception.BizException.class,
                () -> provider.execute(request("VIDEO", "VEO", "veo-3.1-generate-preview",
                        "https://generativelanguage.googleapis.com/v1beta", "ai.veo.api_key")));
        assertTrue(http.postedUrls.isEmpty());
    }

    private static SysSecretReader secret(String value) {
        return new SysSecretReader(key -> value);
    }

    private static AiProviderRequest request(String ability, String provider, String modelCode,
            String baseUrl, String secretRef) {
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(provider);
        model.setModelCode(modelCode);
        model.setQualityLevel("STANDARD");
        model.setTimeoutSec(60);
        model.setConfigJson("{\"base_url\":\"" + baseUrl + "\",\"secret_ref\":\"" + secretRef
                + "\",\"poll_interval_ms\":0}");
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType(ability);
        request.setModel(model);
        request.setPrompt("镜头推进");
        request.setParams(new LinkedHashMap<>());
        return request;
    }

    private static final class FakeHttp implements ProviderHttp.Client {
        private String postJson = "{}";
        private byte[] bytes = new byte[0];
        private final Map<String, String> getByUrl = new LinkedHashMap<>();
        private final List<String> postedUrls = new ArrayList<>();
        private final List<String> postedBodies = new ArrayList<>();

        @Override
        public String postJson(String url, Map<String, String> headers, String body, int timeoutSec) {
            postedUrls.add(url);
            postedBodies.add(body);
            return postJson;
        }

        @Override
        public String getJson(String url, Map<String, String> headers, int timeoutSec) {
            return getByUrl.getOrDefault(url, "{}");
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) {
            return bytes;
        }
    }
}
