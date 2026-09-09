package com.bgssai.shortai.production.media;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
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
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "Media 未配置：请在 application-*.properties 设置 bgssai.media.base-url"
                            + "（本地联调示例 http://127.0.0.1:8080）");
        }
        String token = resolveToken();
        if (token.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "Media 未配置：请设置 bgssai.media.ingest-token，或在 Admin / sys_secret"
                            + " 写入键 bgssai.media.ingest-token（勿把真实密钥提交到仓库）");
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
                        "Media 发布失败：HTTP " + response.getStatusCode().value());
            }
            return parseBody(response.getBody(), response.getStatusCode().value());
        } catch (BizException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            throw mapHttpStatus(ex);
        } catch (ResourceAccessException ex) {
            throw mapAccess(ex);
        } catch (RestClientException ex) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 服务不可用：" + shortMsg(ex), ex);
        }
    }

    /**
     * Config readiness for UX preflight (does not call media).
     */
    public ConfigStatus configStatus() {
        boolean baseOk = properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
        boolean tokenOk = !resolveToken().isBlank();
        return new ConfigStatus(baseOk, tokenOk);
    }

    public record ConfigStatus(boolean baseUrlConfigured, boolean tokenConfigured) {
        public boolean ready() {
            return baseUrlConfigured && tokenConfigured;
        }
    }

    @SuppressWarnings("unchecked")
    IngestResult parseBody(Map<String, Object> body, int httpStatus) {
        String bizCode = text(body.get("code"));
        if (!bizCode.isBlank() && !isSuccessCode(bizCode)) {
            String bizMsg = text(body.get("message"));
            if (bizMsg.isBlank()) {
                bizMsg = text(body.get("msg"));
            }
            if (looksLike6xxxx(bizCode)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        "Media 业务拒绝（" + bizCode + "）"
                                + (bizMsg.isBlank() ? "" : "：" + bizMsg));
            }
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 返回错误码 " + bizCode
                            + (bizMsg.isBlank() ? "" : "：" + bizMsg));
        }
        Map<String, Object> result = unwrapResult(body);
        String mediaId = text(result.get("media_id"));
        String playUrl = text(result.get("play_url"));
        String status = text(result.get("status"));
        if (mediaId.isBlank() || playUrl.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 响应缺少 media_id 或 play_url（HTTP " + httpStatus + "）");
        }
        if (status.isBlank()) {
            status = "READY";
        }
        return new IngestResult(mediaId, playUrl, status);
    }

    private BizException mapHttpStatus(HttpStatusCodeException ex) {
        int status = ex.getStatusCode().value();
        String bodySnippet = shortMsg(ex.getResponseBodyAsString());
        if (status >= 500) {
            return new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 服务异常（HTTP " + status + "）"
                            + (bodySnippet.isBlank() ? "" : "：" + bodySnippet),
                    ex);
        }
        if (status == 401 || status == 403) {
            return new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 鉴权失败（HTTP " + status + "）：请检查 ingest-token 是否与 media 侧一致",
                    ex);
        }
        if (status == 404) {
            return new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 接口不存在（HTTP 404）：请确认 bgssai.media.base-url 指向正确的 media 服务",
                    ex);
        }
        return new BizException(CommonErrorCode.UPSTREAM_ERROR,
                "Media 发布失败（HTTP " + status + "）"
                        + (bodySnippet.isBlank() ? "" : "：" + bodySnippet),
                ex);
    }

    private BizException mapAccess(ResourceAccessException ex) {
        if (isTimeout(ex)) {
            return new BizException(CommonErrorCode.UPSTREAM_TIMEOUT,
                    "Media 请求超时：请确认 media 服务可达后重试", ex);
        }
        return new BizException(CommonErrorCode.UPSTREAM_ERROR,
                "Media 服务不可达（连接失败）：请确认 media 已启动且 base-url 正确", ex);
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof SocketTimeoutException || cur instanceof TimeoutException) {
                return true;
            }
            String name = cur.getClass().getName();
            String msg = cur.getMessage() == null ? "" : cur.getMessage().toLowerCase();
            if (name.contains("Timeout") || msg.contains("timed out") || msg.contains("timeout")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isSuccessCode(String code) {
        return "0".equals(code) || "200".equals(code) || "OK".equalsIgnoreCase(code);
    }

    private static boolean looksLike6xxxx(String code) {
        return code != null && code.matches("6\\d{4}");
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

    private static String shortMsg(String m) {
        if (m == null || m.isBlank()) {
            return "";
        }
        String trimmed = m.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 240 ? trimmed.substring(0, 240) : trimmed;
    }
}
