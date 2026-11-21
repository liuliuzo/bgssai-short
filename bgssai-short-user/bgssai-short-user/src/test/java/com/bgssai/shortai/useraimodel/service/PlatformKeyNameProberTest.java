package com.bgssai.shortai.useraimodel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.useraimodel.service.PlatformKeyNameProber.ProbeResponse;

class PlatformKeyNameProberTest {

    private static final PlatformKeyNameProber.HostResolver PUBLIC_DNS = host ->
            new InetAddress[] { InetAddress.getByAddress(host, new byte[] { 8, 8, 8, 8 }) };

    @Test
    void probesTokenHubXaiAndOpenRouterHostsSafely() {
        AtomicInteger calls = new AtomicInteger();
        PlatformKeyNameProber prober = new PlatformKeyNameProber(
                (url, token) -> {
                    calls.incrementAndGet();
                    return switch (url) {
                        case "https://dev.user.bgssai-tokenhub.cn/v1/api-key",
                             "https://api.bgssai-tokenhub.com/v1/api-key",
                             "https://us.bgssai-tokenhub-global.com/v1/api-key",
                             "https://developers.bgssai.cn/v1/api-key",
                             "https://developers.bgssai.com/v1/api-key" ->
                            new ProbeResponse(200, "{\"name\":\"tokenhub-" + token + "\"}");
                        case "https://api.x.ai/v1/api-key" ->
                            new ProbeResponse(200, "{\"name\":\"xai-user-key\"}");
                        case "https://openrouter.ai/api/v1/auth/key" ->
                            new ProbeResponse(200, "{\"data\":{\"name\":\"\",\"label\":\"openrouter-label\"}}");
                        default -> throw new AssertionError("Unexpected URL: " + url);
                    };
                },
                PUBLIC_DNS);

        assertEquals("tokenhub-k1", prober.probe("OPENAI", "https://dev.user.bgssai-tokenhub.cn/v1", "k1"));
        assertEquals("tokenhub-k2", prober.probe("OPENAI", "https://api.bgssai-tokenhub.com/", "k2"));
        assertEquals("tokenhub-k3", prober.probe("OPENAI", "https://us.bgssai-tokenhub-global.com/v1/", "k3"));
        assertEquals("tokenhub-k4", prober.probe("OPENAI", "https://developers.bgssai.cn/v1", "k4"));
        assertEquals("tokenhub-k5", prober.probe("OPENAI", "https://developers.bgssai.com", "k5"));
        assertEquals("xai-user-key", prober.probe("XAI", null, "xk-1"));
        assertEquals("xai-user-key", prober.probe("GROK", "", "xk-2"));
        assertEquals("xai-user-key", prober.probe("OPENAI", "https://api.x.ai/v1", "xk-3"));
        assertEquals("openrouter-label", prober.probe("OPENROUTER", null, "or-1"));
        assertEquals("openrouter-label", prober.probe("OPENAI", "https://openrouter.ai/api/v1", "or-2"));
        assertEquals(10, calls.get());
    }

    @Test
    void unsupportedProvidersAndSsrfUrlsMakeZeroHttpCalls() {
        AtomicInteger calls = new AtomicInteger();
        PlatformKeyNameProber prober = new PlatformKeyNameProber(
                (url, token) -> {
                    calls.incrementAndGet();
                    return new ProbeResponse(200, "{\"name\":\"unexpected\"}");
                },
                PUBLIC_DNS);

        for (String provider : List.of("DEEPSEEK", "MINIMAX", "KLING", "DASHSCOPE", "ZHIPU",
                "MOONSHOT", "VOLCENGINE", "HUNYUAN", "OPENAI", "ANTHROPIC", "GOOGLE")) {
            assertNull(prober.probe(provider, null, "sk-live-test-123"));
        }
        assertNull(prober.probe("OPENAI", "https://api.openai.com/v1", "sk-live-test-123"));
        assertNull(prober.probe("OPENAI", "http://www.bgssai-tokenhub.cn/v1", "sk-live-test-123"));
        assertNull(prober.probe("XAI", "https://localhost/v1", "sk-live-test-123"));
        assertNull(prober.probe("OPENROUTER", "https://metadata.internal/v1", "sk-live-test-123"));
        assertNull(prober.probe("XAI", null, "sk-1****5678"));
        assertEquals(0, calls.get());

        PlatformKeyNameProber privateIpProber = new PlatformKeyNameProber(
                (url, token) -> {
                    calls.incrementAndGet();
                    return new ProbeResponse(200, "{\"name\":\"unexpected\"}");
                },
                host -> new InetAddress[] { InetAddress.getByAddress(host, new byte[] { 100, 64, 0, 1 }) });
        assertNull(privateIpProber.probe("OPENAI", "https://www.bgssai-tokenhub.cn/v1", "sk-test"));
        assertEquals(0, calls.get());
    }
}
