package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.sun.net.httpserver.HttpServer;

class GatewayErrorTextTest {

    @Test
    void providerNotConfiguredBecomesARetryableSentence() {
        String body = "{\"error\":{\"message\":\"Provider is not configured: deepseek\","
                + "\"type\":\"service_unavailable_error\",\"code\":\"provider_not_configured\"}}";
        assertEquals("模型服务暂未开通：deepseek，请稍后重试", GatewayErrorText.explain(503, body));
        assertEquals("模型服务暂未开通：deepseek", GatewayErrorText.reason(503, body));
        assertFalse(GatewayErrorText.probeMeansReady(503, body));
        assertFalse(GatewayErrorText.explain(503, body).toLowerCase().contains("provider_not_configured"));
    }

    @Test
    void validationAfterTheProviderCheckCountsAsReady() {
        String body = "{\"error\":{\"message\":\"prompt is required\",\"type\":\"invalid_request_error\","
                + "\"code\":\"invalid_request\"}}";
        assertTrue(GatewayErrorText.probeMeansReady(400, body));
        String missing = "{\"error\":{\"message\":\"Model is not registered for capability: audio_speech\","
                + "\"type\":\"invalid_request_error\",\"code\":\"unsupported_model\"}}";
        assertFalse(GatewayErrorText.probeMeansReady(400, missing));
        assertEquals("该模型暂未开放", GatewayErrorText.reason(400, missing));
    }

    @Test
    void httpClientKeepsTheGatewayCodeOutOfTheUserMessage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] payload = ("{\"error\":{\"message\":\"Provider is not configured: deepseek\","
                + "\"type\":\"service_unavailable_error\",\"code\":\"provider_not_configured\"}}")
                .getBytes(StandardCharsets.UTF_8);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.sendResponseHeaders(503, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
            BizException ex = assertThrows(BizException.class,
                    () -> new ProviderHttp.JdkClient().postJson(url, Map.of(), "{}", 15));
            assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
            assertEquals("模型服务暂未开通：deepseek，请稍后重试", ex.getMessage());
        } finally {
            server.stop(0);
        }
    }
}
