package com.bgssai.shortai.production.media;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.configuration.BgssaiMediaProperties;
import com.bgssai.shortai.secret.SysSecretReader;

@Component
public class RestMediaIngestClient implements MediaIngestClient {

    private final RestTemplate restTemplate;
    private final BgssaiMediaProperties properties;
    private final SysSecretReader secretReader;

    @Autowired
    public RestMediaIngestClient(RestTemplate restTemplate, BgssaiMediaProperties properties,
            SysSecretReader secretReader) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.secretReader = secretReader;
    }

    RestMediaIngestClient(RestTemplate restTemplate, BgssaiMediaProperties properties,
            SysSecretReader secretReader, boolean testCtor) {
        this(restTemplate, properties, secretReader);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IngestResult ingest(Map<String, Object> payload) {
        String base = properties.getBaseUrl();
        if (base == null || base.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "bgssai.media.base-url is not configured");
        }
        String token = resolveToken();
        if (token.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "bgssai.media.ingest-token is not configured");
        }
        String url = trimSlash(base) + MediaIngestContract.INGEST_PATH;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(MediaIngestContract.TOKEN_HEADER, token);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers),
                    Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        "media ingest failed: HTTP " + response.getStatusCode().value());
            }
            Map<String, Object> body = response.getBody();
            Map<String, Object> result = unwrapResult(body);
            String mediaId = text(result.get("media_id"));
            String playUrl = text(result.get("play_url"));
            String status = text(result.get("status"));
            if (mediaId.isBlank() || playUrl.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        "media ingest response missing media_id or play_url");
            }
            if (status.isBlank()) {
                status = "READY";
            }
            return new IngestResult(mediaId, playUrl, status);
        } catch (BizException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "media service unavailable: " + shortMsg(ex), ex);
        }
    }

    private String resolveToken() {
        String fromProps = properties.getIngestToken();
        if (fromProps != null && !fromProps.isBlank()) {
            return fromProps.trim();
        }
        return secretReader.get(MediaIngestContract.SECRET_KEY);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapResult(Map<String, Object> body) {
        Object nested = body.get("result");
        if (nested instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return body;
    }

    private static String trimSlash(String base) {
        String s = base.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String shortMsg(Throwable ex) {
        String m = ex.getMessage();
        if (m == null || m.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return m.length() > 240 ? m.substring(0, 240) : m;
    }
}
