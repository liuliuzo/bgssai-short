package com.bgssai.shortai.production.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.configuration.BgssaiMediaProperties;
import com.bgssai.shortai.secret.SysSecretReader;

class RestMediaIngestClientTest {

    @Test
    void postsSnakeCaseBodyAndParsesWrappedResult() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local/");
        props.setIngestToken("tok-1");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("media_id", "md-1");
        result.put("play_url", "https://play/md-1");
        result.put("status", "READY");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "0");
        body.put("result", result);
        when(restTemplate.postForEntity(eq("http://media.local/bgssai/user/media/ingest/short-drama"),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        Map<String, Object> payload = MediaIngestContract.buildPayload(
                "w", "e", "f", "t", null, "https://v.mp4", 10, List.of());
        MediaIngestClient.IngestResult out = client.ingest(payload);
        assertEquals("md-1", out.mediaId());
        assertEquals("https://play/md-1", out.playUrl());
        assertEquals("READY", out.status());
    }

    @Test
    void mediaDownBecomesUpstreamBizException() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("tok");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("404 Not Found"));

        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        Map<String, Object> payload = MediaIngestContract.buildPayload(
                "w", "e", "f", "t", null, "https://v.mp4", 10, List.of());
        BizException ex = assertThrows(BizException.class, () -> client.ingest(payload));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        assertTrueMessage(ex.getMessage());
    }

    @Test
    void fallsBackToSysSecretWhenPropertiesTokenBlank() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("");
        Map<String, Object> result = Map.of("media_id", "md-2", "play_url", "https://p/2", "status", "READY");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(result, HttpStatus.OK));

        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> MediaIngestContract.SECRET_KEY.equals(k) ? "secret-tok" : ""), true);
        MediaIngestClient.IngestResult out = client.ingest(MediaIngestContract.buildPayload(
                "w", "e", "f", "t", null, "https://v.mp4", 1, List.of()));
        assertEquals("md-2", out.mediaId());
    }

    private static void assertTrueMessage(String message) {
        if (message == null || !message.contains("media")) {
            throw new AssertionError("expected media unavailable message, got: " + message);
        }
    }
}
