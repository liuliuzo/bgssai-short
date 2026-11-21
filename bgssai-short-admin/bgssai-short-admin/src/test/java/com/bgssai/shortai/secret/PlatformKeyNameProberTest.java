package com.bgssai.shortai.secret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.secret.PlatformKeyNameProber.ProbeResponse;

class PlatformKeyNameProberTest {

    private static final PlatformKeyNameProber.HostResolver PUBLIC_DNS = host ->
            new InetAddress[] { InetAddress.getByAddress(host, new byte[] { 8, 8, 8, 8 }) };

    @Test
    void probesTokenHubXaiAndOpenRouterEndpointsAndParsesNames() {
        AtomicInteger calls = new AtomicInteger();
        PlatformKeyNameProber prober = new PlatformKeyNameProber(
                "https://www.bgssai-tokenhub.cn/v1/",
                (url, token) -> {
                    calls.incrementAndGet();
                    if ("https://www.bgssai-tokenhub.cn/v1/api-key".equals(url)) {
                        return new ProbeResponse(200, "{\"name\":\"  bgssai-short-cn-prod  \"}");
                    }
                    if ("https://api.x.ai/v1/api-key".equals(url)) {
                        return new ProbeResponse(200, "{\"name\":\"xai-studio-key\"}");
                    }
                    if ("https://openrouter.ai/api/v1/auth/key".equals(url)) {
                        if ("or-label-only".equals(token)) {
                            return new ProbeResponse(200, "{\"data\":{\"name\":\"  \",\"label\":\"or-fallback-label\"}}");
                        }
                        return new ProbeResponse(200, "{\"data\":{\"name\":\"or-primary-name\",\"label\":\"ignored\"}}");
                    }
                    throw new AssertionError("Unexpected URL: " + url);
                },
                PUBLIC_DNS);

        assertEquals("bgssai-short-cn-prod", prober.probe("ai.tokenhub_cn.api_key", "sk-cn-1"));
        assertEquals("bgssai-short-cn-prod", prober.probe("ai.tencent_tokenhub.api_key", "sk-cn-2"));
        assertEquals("xai-studio-key", prober.probe("ai.xai.api_key", "xai-key-1"));
        assertEquals("or-primary-name", prober.probe("ai.openrouter.api_key", "or-1"));
        assertEquals("or-fallback-label", prober.probe("ai.openrouter.api_key", "or-label-only"));
        assertEquals(5, calls.get());
    }

    @Test
    void unsupportedProvidersAndNonAiKeysTriggerZeroHttpCalls() {
        AtomicInteger calls = new AtomicInteger();
        PlatformKeyNameProber prober = new PlatformKeyNameProber(
                "https://www.bgssai-tokenhub.cn",
                (url, token) -> {
                    calls.incrementAndGet();
                    return new ProbeResponse(200, "{\"name\":\"should-never-probe\"}");
                },
                PUBLIC_DNS);

        List<String> unsupported = List.of(
                "ai.hunyuan.secret_key",
                "ai.wand.api_key",
                "ai.deepseek.api_key",
                "ai.zhipu.api_key",
                "ai.moonshot.api_key",
                "ai.minimax.api_key",
                "ai.kling.api_key",
                "ai.vidu.api_key",
                "ai.dashscope.api_key",
                "ai.tripo.api_key",
                "ai.pixverse.api_key",
                "ai.xiaomi.api_key",
                "ai.volcengine.api_key",
                "ai.anthropic.api_key",
                "ai.google.api_key",
                "ai.openai_compat.api_key",
                "ai.tokenhub_global.api_key",
                "bgssai.sms.secret-key",
                "bgssai.oauth.client-secret",
                "bgssai.smtp.password");

        for (String key : unsupported) {
            assertNull(prober.probe(key, "sk-live-secret-123"));
        }
        assertNull(prober.probe("ai.tokenhub_cn.api_key", "sk-t****1234"));
        assertNull(prober.probe("ai.tokenhub_cn.api_key", "   "));
        assertEquals(0, calls.get());
    }

    @Test
    void ssrfUnsafeOrFailingProbesReturnNullSafely() {
        AtomicInteger calls = new AtomicInteger();
        PlatformKeyNameProber ssrfProber = new PlatformKeyNameProber(
                "https://127.0.0.1/v1",
                (url, token) -> {
                    calls.incrementAndGet();
                    return new ProbeResponse(200, "{\"name\":\"bad\"}");
                },
                host -> new InetAddress[] { InetAddress.getByAddress(host, new byte[] { 127, 0, 0, 1 }) });
        assertNull(ssrfProber.probe("ai.tokenhub_cn.api_key", "sk-test"));
        assertEquals(0, calls.get());

        PlatformKeyNameProber non200Prober = new PlatformKeyNameProber(
                "https://www.bgssai-tokenhub.cn",
                (url, token) -> new ProbeResponse(502, "{\"name\":\"ignored\"}"),
                PUBLIC_DNS);
        assertNull(non200Prober.probe("ai.tokenhub_cn.api_key", "sk-test"));

        PlatformKeyNameProber longNameProber = new PlatformKeyNameProber(
                "https://www.bgssai-tokenhub.cn",
                (url, token) -> new ProbeResponse(200, "{\"name\":\"" + "x".repeat(150) + "\"}"),
                PUBLIC_DNS);
        String truncated = longNameProber.probe("ai.tokenhub_cn.api_key", "sk-test");
        assertEquals(128, truncated.length());
    }
}
