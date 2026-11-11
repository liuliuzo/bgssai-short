package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitKind;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.secret.SysSecretReader;

class ImageConditionedVideoProviderTest {

    private static final String JOB = "i2v_" + "cd".repeat(16);
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Test
    void submitUsesMultipartVideosAndKeepsTheImageOutOfThePrompt() throws Exception {
        List<ImageConditionedVideoProvider.HttpCall> calls = new ArrayList<>();
        ImageConditionedVideoProvider provider = provider(calls, 202, accepted(), "application/json");
        var decision = provider.submit(model("ai.image_to_video.api_key"), PNG, "image/png",
                "a cat walks", 384, 256, 25, 8, 20, 42);
        assertEquals(SubmitKind.ACCEPTED, decision.kind());
        assertEquals(1, calls.size());
        String url = calls.get(0).url();
        assertTrue(url.endsWith("/v1/videos"));
        assertFalse(url.contains("generations"));
        assertEquals("test-bearer", calls.get(0).bearer());
        String body = new String(calls.get(0).body(), StandardCharsets.ISO_8859_1);
        int prompt = body.indexOf("name=\"prompt\"");
        int image = body.indexOf("name=\"image\"");
        assertTrue(prompt >= 0 && image > prompt);
        assertTrue(body.substring(prompt, image).contains("a cat walks"));
        assertFalse(body.substring(prompt, image).contains("iVBORw0KGgo"));
        assertTrue(body.indexOf(new String(PNG, StandardCharsets.ISO_8859_1)) > image);
        assertEquals(HttpClient.Redirect.NEVER, ImageConditionedVideoProvider.REDIRECTS);
    }

    @Test
    void ambiguousSubmitIsNotRetried() {
        List<ImageConditionedVideoProvider.HttpCall> calls = new ArrayList<>();
        ImageConditionedVideoProvider provider = new ImageConditionedVideoProvider(secrets(), call -> {
            calls.add(call);
            throw new IOException("timed out");
        }, ms -> {
        });
        var decision = provider.submit(model("ai.image_to_video.api_key"), PNG, "image/png",
                "a cat walks", 384, 256, 25, 8, 20, 42);
        assertEquals(SubmitKind.AMBIGUOUS, decision.kind());
        assertEquals(1, calls.size());
    }

    @Test
    void failedStatusDoesNotFetchContentOrCopyTheError() {
        List<ImageConditionedVideoProvider.HttpCall> calls = new ArrayList<>();
        String payload = "{\"status\":\"failed\",\"error\":\"ltx-video exploded\",\"metrics\":{\"dtype\":\"bf16\"}}";
        ImageConditionedVideoProvider provider = provider(calls, 200, payload, "application/json");
        var polled = provider.poll(model("ai.image_to_video.api_key"), JOB);
        assertEquals(PollKind.FAILED, polled.kind());
        assertFalse(polled.userMessage().toLowerCase().contains("ltx"));
        assertFalse(polled.userMessage().contains("bf16"));
        assertEquals(1, calls.size());
        assertTrue(calls.get(0).url().endsWith("/v1/videos/" + JOB));
        assertFalse(calls.get(0).url().endsWith("/content"));
    }

    @Test
    void executeNeverCallsTransport() {
        List<ImageConditionedVideoProvider.HttpCall> calls = new ArrayList<>();
        ImageConditionedVideoProvider provider = provider(calls, 202, accepted(), "application/json");
        AiProviderRequest missing = new AiProviderRequest();
        missing.setModel(model("ai.image_to_video.api_key"));
        assertThrows(BizException.class, () -> provider.execute(missing));
        AiProviderRequest present = new AiProviderRequest();
        present.setModel(model("ai.image_to_video.api_key"));
        present.setPrompt("a cat walks");
        present.setParams(Map.of("image_base64", Base64.getEncoder().encodeToString(PNG)));
        assertThrows(BizException.class, () -> provider.execute(present));
        assertTrue(calls.isEmpty());
    }

    @Test
    void otherSecretRefsAreNotUsed() {
        List<ImageConditionedVideoProvider.HttpCall> calls = new ArrayList<>();
        SysSecretReader secrets = new SysSecretReader(key -> "ai.tokenhub_cn.api_key".equals(key) ? "hub-secret" : "");
        ImageConditionedVideoProvider provider = new ImageConditionedVideoProvider(secrets, call -> {
            calls.add(call);
            throw new IOException("should not send");
        }, ms -> {
        });
        var decision = provider.submit(model("ai.tokenhub_cn.api_key"), PNG, "image/png",
                "a cat walks", 384, 256, 25, 8, 20, 42);
        assertEquals(SubmitKind.UNAVAILABLE, decision.kind());
        assertTrue(calls.isEmpty());
    }

    private static ImageConditionedVideoProvider provider(List<ImageConditionedVideoProvider.HttpCall> calls,
            int status, String payload, String type) {
        return new ImageConditionedVideoProvider(secrets(), call -> {
            calls.add(call);
            return new ImageConditionedVideoProvider.HttpReply(status, type, payload.getBytes(StandardCharsets.UTF_8));
        }, ms -> {
        });
    }

    private static SysSecretReader secrets() {
        return new SysSecretReader(key -> SysSecretReader.AI_IMAGE_TO_VIDEO_API_KEY.equals(key) ? "test-bearer" : "");
    }

    private static AiModelConfig model(String secretRef) {
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(ImageConditionedVideoProvider.CODE);
        model.setModelCode(ImageConditionedVideoProvider.MODEL_CODE);
        model.setEnabledFlag(1);
        model.setConfigJson("{\"base_url\":\"https://i2v.invalid\",\"secret_ref\":\"" + secretRef + "\"}");
        return model;
    }

    private static String accepted() {
        return "{\"id\":\"" + JOB + "\",\"status\":\"queued\",\"status_url\":\"/v1/videos/" + JOB + "\"}";
    }
}
