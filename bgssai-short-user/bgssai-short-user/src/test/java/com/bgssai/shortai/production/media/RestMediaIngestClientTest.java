package com.bgssai.shortai.production.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.web.client.ResourceAccessException;
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
                .thenThrow(new ResourceAccessException("Connection refused"));

        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        Map<String, Object> payload = MediaIngestContract.buildPayload(
                "w", "e", "f", "t", null, "https://v.mp4", 10, List.of());
        BizException ex = assertThrows(BizException.class, () -> client.ingest(payload));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("不可达") || ex.getMessage().contains("Media"));
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(2))
                .postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void mediaBizCode6xxxxSurfacedClearly() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("tok");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "60021");
        body.put("message", "quota exceeded");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        BizException ex = assertThrows(BizException.class, () -> client.ingest(
                MediaIngestContract.buildPayload("w", "e", "f", "t", null, "https://v.mp4", 1, List.of())));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("60021"));
        assertTrue(ex.getMessage().contains("业务拒绝"));
    }

    @Test
    void missingBaseUrlDevFriendly() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("");
        props.setIngestToken("tok");
        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        BizException ex = assertThrows(BizException.class, () -> client.ingest(
                MediaIngestContract.buildPayload("w", "e", "f", "t", null, "https://v.mp4", 1, List.of())));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("base-url"));
    }

    @Test
    void configStatusReflectsMissingToken() {
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("");
        RestMediaIngestClient client = new RestMediaIngestClient(mock(RestTemplate.class), props,
                new SysSecretReader(k -> ""), true);
        RestMediaIngestClient.ConfigStatus status = client.configStatus();
        assertTrue(status.baseUrlConfigured());
        assertFalse(status.tokenConfigured());
        assertFalse(status.ready());
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

    @Test
    void genericRestClientExceptionStillUpstream() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("tok");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("404 Not Found"));
        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        BizException ex = assertThrows(BizException.class, () -> client.ingest(
                MediaIngestContract.buildPayload("w", "e", "f", "t", null, "https://v.mp4", 1, List.of())));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
    }

    @Test
    void retriesOnceOnHttp500ThenSucceeds() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("tok");
        Map<String, Object> ok = Map.of("media_id", "md-r", "play_url", "https://p/r", "status", "READY");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new org.springframework.web.client.HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenReturn(new ResponseEntity<>(ok, HttpStatus.OK));
        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        MediaIngestClient.IngestResult out = client.ingest(MediaIngestContract.buildPayload(
                "w", "e", "f", "t", null, "https://v.mp4", 1, List.of()));
        assertEquals("md-r", out.mediaId());
        assertEquals(2, out.attempts());
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(2))
                .postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void doesNotRetryOnBizCode6xxxx() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("tok");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "60021");
        body.put("message", "quota");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        assertThrows(BizException.class, () -> client.ingest(
                MediaIngestContract.buildPayload("w", "e", "f", "t", null, "https://v.mp4", 1, List.of())));
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(1))
                .postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void retriesOnceOnTimeoutThenFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BgssaiMediaProperties props = new BgssaiMediaProperties();
        props.setBaseUrl("http://media.local");
        props.setIngestToken("tok");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Read timed out",
                        new java.net.SocketTimeoutException("Read timed out")));
        RestMediaIngestClient client = new RestMediaIngestClient(restTemplate, props,
                new SysSecretReader(k -> ""), true);
        MediaPublishException ex = assertThrows(MediaPublishException.class, () -> client.ingest(
                MediaIngestContract.buildPayload("w", "e", "f", "t", null, "https://v.mp4", 1, List.of())));
        assertEquals(CommonErrorCode.UPSTREAM_TIMEOUT.code(), ex.getCode());
        assertEquals(2, ex.getAttempts());
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(2))
                .postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class));
    }
}
