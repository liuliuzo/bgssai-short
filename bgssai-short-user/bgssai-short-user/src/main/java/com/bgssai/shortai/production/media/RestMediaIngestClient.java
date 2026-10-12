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

import com.bgssai.shortai.common.CommercialOutcomeGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.configuration.BgssaiMediaProperties;
import com.bgssai.shortai.secret.SysSecretReader;

@Component
public class RestMediaIngestClient implements MediaIngestClient {

    static final int MAX_ATTEMPTS = 2;

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
    public IngestResult ingest(Map<String, Object> payload) {
        String base = properties.getBaseUrl();
        if (base == null || base.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "Media 主机未配置：bgssai.media.base-url 为空。"
                            + "未部署的 Media 不会被写成发布成功，也不会返回假的 media_id。"
                            + "请在 application-*.properties 填写真实地址（不要编造 IP）");
        }
        String token = resolveToken();
        if (token.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "Media 未配置：请设置 bgssai.media.ingest-token，或在 Admin / sys_secret"
                            + " 写入键 bgssai.media.ingest-token（勿把真实密钥提交到仓库）");
        }
        String url = trimSlash(base) + MediaIngestContract.INGEST_PATH;
        MediaPublishException lastRetriable = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                IngestResult result = postOnce(url, token, payload, attempt);
                return result;
            } catch (MediaPublishException ex) {
                if (!isRetriable(ex) || attempt >= MAX_ATTEMPTS) {
                    throw withAttempts(ex, attempt);
                }
                lastRetriable = ex;
            }
        }
        throw withAttempts(lastRetriable, MAX_ATTEMPTS);
    }

    @SuppressWarnings("unchecked")
    private IngestResult postOnce(String url, String token, Map<String, Object> payload, int attempt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(MediaIngestContract.TOKEN_HEADER, token);
        String idempotencyKey = text(payload == null ? null : payload.get("idempotency_key"));
        if (!idempotencyKey.isBlank()) {
            headers.set(MediaIngestContract.IDEMPOTENCY_HEADER, idempotencyKey);
        }
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers),
                    Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                int status = response.getStatusCode().value();
                throw new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                        "Media 发布失败：HTTP " + status, status, attempt);
            }
            return parseBody(response.getBody(), response.getStatusCode().value(), attempt);
        } catch (MediaPublishException ex) {
            throw ex;
        } catch (BizException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            throw mapHttpStatus(ex, attempt);
        } catch (ResourceAccessException ex) {
            throw mapAccess(ex, attempt);
        } catch (RestClientException ex) {
            throw new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 服务不可用：" + shortMsg(ex), null, attempt, ex);
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
    IngestResult parseBody(Map<String, Object> body, int httpStatus, int attempt) {
        String bizCode = text(body.get("code"));
        Map<String, Object> result = unwrapResult(body);
        String statusRaw = text(result.get("status"));
        String classified = statusRaw.isBlank()
                ? ""
                : CommercialOutcomeGuard.classifyMediaStatus(statusRaw);
        String message = firstNonBlank(text(result.get("message")), text(body.get("message")), text(body.get("msg")));
        boolean envelopeFail = !bizCode.isBlank() && !isSuccessCode(bizCode);

        // Media PENDING (probe not verified) is HTTP 200 / code 0 with no media_id and no play_url.
        // That is not a missing-field failure and must not be stored as FAILED.
        if ("PENDING".equals(classified) && !envelopeFail) {
            return pendingResult(result, message, httpStatus, attempt);
        }
        if (envelopeFail || "FAILED".equals(classified)) {
            throw new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                    failureMessage(bizCode, statusRaw, classified, message), httpStatus, attempt);
        }

        String mediaId = text(result.get("media_id"));
        String playUrl = text(result.get("play_url"));
        if (classified.isBlank()) {
            classified = "READY";
        }
        if ("READY".equals(classified) && playableExplicitlyFalse(result.get("playable"))) {
            throw new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 声明 READY 但 playable=false，不能当成可播放成功"
                            + (message.isBlank() ? "" : "：" + message),
                    httpStatus, attempt);
        }
        if (mediaId.isBlank() || playUrl.isBlank()) {
            throw new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 响应缺少 media_id 或 play_url（HTTP " + httpStatus + "）",
                    httpStatus, attempt);
        }
        try {
            CommercialOutcomeGuard.rejectFakeUrl(playUrl, "media play_url");
        } catch (BizException ex) {
            throw new MediaPublishException(CommonErrorCode.RULE_REJECT, ex.getMessage(), httpStatus, attempt);
        }
        // PENDING stays PENDING; READY/SUCCEEDED normalize to READY. Never invent SUCCEEDED.
        return new IngestResult(mediaId, playUrl, classified, httpStatus, attempt, message);
    }

    private static IngestResult pendingResult(Map<String, Object> result, String message, int httpStatus,
            int attempt) {
        String mediaId = text(result.get("media_id"));
        String playUrl = text(result.get("play_url"));
        if (!playUrl.isBlank()) {
            try {
                CommercialOutcomeGuard.rejectFakeUrl(playUrl, "media play_url");
            } catch (BizException ex) {
                throw new MediaPublishException(CommonErrorCode.RULE_REJECT, ex.getMessage(), httpStatus, attempt);
            }
        }
        String note = message.isBlank()
                ? "Media 已受理，素材尚未验证为可播放（PENDING，不是失败，也不是可播放成功）"
                : message;
        return new IngestResult(mediaId, playUrl, "PENDING", httpStatus, attempt, note);
    }

    private static String failureMessage(String bizCode, String statusRaw, String classified, String message) {
        if ("FAILED".equals(classified)) {
            String detail = message.isBlank() ? ("status=" + statusRaw) : message;
            return "Media 返回失败状态（FAIL，不是 PENDING）：" + detail;
        }
        if (looksLike6xxxx(bizCode)) {
            return "Media 业务拒绝（" + bizCode + "）" + (message.isBlank() ? "" : "：" + message);
        }
        if (!bizCode.isBlank()) {
            return "Media 返回错误码 " + bizCode + (message.isBlank() ? "" : "：" + message);
        }
        return message.isBlank() ? "Media 发布失败" : message;
    }

    private static boolean playableExplicitlyFalse(Object playable) {
        if (playable instanceof Boolean flag) {
            return !flag;
        }
        return "false".equalsIgnoreCase(text(playable));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isRetriable(MediaPublishException ex) {
        Integer status = ex.getHttpStatus();
        if (status != null && status >= 500) {
            return true;
        }
        if (CommonErrorCode.UPSTREAM_TIMEOUT.code().equals(ex.getCode())) {
            return true;
        }
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        return msg.contains("不可达") || msg.contains("超时") || msg.contains("服务异常");
    }

    private static MediaPublishException withAttempts(MediaPublishException ex, int attempts) {
        if (ex == null) {
            return new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR, "Media 发布失败", null, attempts);
        }
        if (ex.getAttempts() == attempts) {
            return ex;
        }
        String suffix = attempts > 1 ? "（已重试）" : "";
        String msg = ex.getMessage() == null ? "Media 发布失败" : ex.getMessage();
        if (attempts > 1 && !msg.contains("已重试")) {
            msg = msg + suffix;
        }
        return new MediaPublishException(ex.getCode(), msg, ex.getHttpStatus(), attempts, ex.getCause());
    }

    private MediaPublishException mapHttpStatus(HttpStatusCodeException ex, int attempt) {
        int status = ex.getStatusCode().value();
        String bodySnippet = shortMsg(ex.getResponseBodyAsString());
        if (status >= 500) {
            return new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 服务异常（HTTP " + status + "）"
                            + (bodySnippet.isBlank() ? "" : "：" + bodySnippet),
                    status, attempt, ex);
        }
        if (status == 401 || status == 403) {
            return new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 鉴权失败（HTTP " + status + "）：请检查 ingest-token 是否与 media 侧一致",
                    status, attempt, ex);
        }
        if (status == 404) {
            return new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                    "Media 接口不存在（HTTP 404）：请确认 bgssai.media.base-url 指向正确的 media 服务",
                    status, attempt, ex);
        }
        return new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                "Media 发布失败（HTTP " + status + "）"
                        + (bodySnippet.isBlank() ? "" : "：" + bodySnippet),
                status, attempt, ex);
    }

    private MediaPublishException mapAccess(ResourceAccessException ex, int attempt) {
        if (isTimeout(ex)) {
            return new MediaPublishException(CommonErrorCode.UPSTREAM_TIMEOUT,
                    "Media 请求超时：请确认 media 服务可达后重试", null, attempt, ex);
        }
        return new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                "Media 服务不可达（连接失败）：请确认 media 已启动且 base-url 正确", null, attempt, ex);
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
