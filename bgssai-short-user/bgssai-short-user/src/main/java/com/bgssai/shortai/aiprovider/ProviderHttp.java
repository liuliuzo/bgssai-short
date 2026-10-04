package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.bgssai.shortai.common.CommercialOutcomeGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

final class ProviderHttp {

    interface Client {
        String postJson(String url, Map<String, String> headers, String body, int timeoutSec) throws Exception;

        String getJson(String url, Map<String, String> headers, int timeoutSec) throws Exception;

        byte[] getBytes(String url, int timeoutSec) throws Exception;

        default byte[] getBytes(String url, Map<String, String> headers, int timeoutSec) throws Exception {
            return getBytes(url, timeoutSec);
        }

        default byte[] postBytes(String url, Map<String, String> headers, String body, int timeoutSec) throws Exception {
            return postJson(url, headers, body, timeoutSec).getBytes(StandardCharsets.UTF_8);
        }
    }

    interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    static final class JdkClient implements Client {
        private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

        @Override
        public String postJson(String url, Map<String, String> headers, String body, int timeoutSec) throws Exception {
            return new String(postBytes(url, headers, body, timeoutSec), StandardCharsets.UTF_8);
        }

        @Override
        public byte[] postBytes(String url, Map<String, String> headers, String body, int timeoutSec) throws Exception {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            apply(b, headers);
            HttpResponse<byte[]> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "http " + resp.statusCode());
            }
            return resp.body() == null ? new byte[0] : resp.body();
        }

        @Override
        public String getJson(String url, Map<String, String> headers, int timeoutSec) throws Exception {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .GET();
            apply(b, headers);
            return handleJson(httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) throws Exception {
            return getBytes(url, Map.of(), timeoutSec);
        }

        @Override
        public byte[] getBytes(String url, Map<String, String> headers, int timeoutSec) throws Exception {
            URI target = URI.create(url);
            Map<String, String> forwarded = headers;
            for (int redirect = 0; redirect <= 5; redirect++) {
                CommercialOutcomeGuard.rejectFakeUrl(target.toString(), "provider download url");
                if (!"https".equalsIgnoreCase(target.getScheme()) && !"http".equalsIgnoreCase(target.getScheme())) {
                    throw new BizException(CommonErrorCode.RULE_REJECT,
                            "invalid download scheme (refusing stub/placeholder commercial success): "
                                    + target.getScheme());
                }
                HttpRequest.Builder request = HttpRequest.newBuilder(target)
                        .timeout(Duration.ofSeconds(Math.max(15, timeoutSec))).GET();
                apply(request, forwarded);
                HttpResponse<byte[]> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String location = response.headers().firstValue("location").orElse("");
                    if (location.isBlank()) throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "download redirect missing location");
                    URI next = target.resolve(location);
                    if ("https".equalsIgnoreCase(target.getScheme()) && !"https".equalsIgnoreCase(next.getScheme())) {
                        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "download cannot downgrade HTTPS");
                    }
                    if (!java.util.Objects.equals(target.getHost(), next.getHost()) || target.getPort() != next.getPort()
                            || !java.util.Objects.equals(target.getScheme(), next.getScheme())) forwarded = Map.of();
                    target = next;
                    continue;
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "download status " + response.statusCode());
                }
                byte[] body = response.body();
                if (body == null || body.length == 0) throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "download empty");
                return body;
            }
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "too many download redirects");
        }

        private static void apply(HttpRequest.Builder b, Map<String, String> headers) {
            if (headers == null) {
                return;
            }
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    b.header(e.getKey(), e.getValue());
                }
            }
        }

        private static String handleJson(HttpResponse<String> resp) {
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "http " + resp.statusCode());
            }
            return resp.body() == null ? "" : resp.body();
        }
    }

    static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String v = url.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    static String dataUrl(String base64, String mime) {
        if (base64 == null || base64.isBlank()) {
            return "";
        }
        if (base64.startsWith("data:")) {
            return base64;
        }
        String m = mime == null || mime.isBlank() ? "image/png" : mime;
        return "data:" + m + ";base64," + base64;
    }

    static String promptOf(AiProviderRequest request) {
        String prompt = request.getPrompt();
        if (prompt != null && !prompt.isBlank()) {
            return prompt.trim();
        }
        String title = com.bgssai.shortai.production.support.ShortJson.text(request.param("title"));
        String subtitle = com.bgssai.shortai.production.support.ShortJson.text(request.param("subtitle"));
        String joined = (title + " " + subtitle).trim();
        if (joined.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "prompt required");
        }
        return joined;
    }

    /**
     * 尾帧载荷。与 imagePayload 分开取，是因为首帧与尾帧要同时出现在一次请求里；
     * 复用同一组 key 会让后写的把先写的覆盖掉，而这种覆盖不报错、只是接不好。
     */
    static String lastImagePayload(AiProviderRequest request) {
        String url = com.bgssai.shortai.production.support.ShortJson.text(request.param("last_image_url"));
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String b64 = com.bgssai.shortai.production.support.ShortJson.text(request.param("last_image_base64"));
        if (b64.isBlank()) {
            return "";
        }
        String mime = com.bgssai.shortai.production.support.ShortJson.text(request.param("last_image_mime"));
        return dataUrl(b64, mime);
    }
    static String imagePayload(AiProviderRequest request) {
        String url = com.bgssai.shortai.production.support.ShortJson.text(request.param("image_url"));
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String b64 = com.bgssai.shortai.production.support.ShortJson.text(request.param("image_base64"));
        if (b64.isBlank()) {
            return "";
        }
        String mime = com.bgssai.shortai.production.support.ShortJson.text(request.param("image_mime"));
        return dataUrl(b64, mime);
    }

    static String aspectRatio(java.util.Map<String, Object> cfg, AiProviderRequest request) {
        return firstNonBlank(
                com.bgssai.shortai.production.support.ShortJson.text(request.param("aspect_ratio")),
                com.bgssai.shortai.production.support.ShortJson.text(cfg.get("aspect_ratio")),
                "9:16");
    }

    static int timeoutSec(AiProviderRequest request, int fallback) {
        int t = request.getModel().getTimeoutSec() == null ? fallback : request.getModel().getTimeoutSec();
        return Math.max(30, t);
    }

    static long pollIntervalMs(java.util.Map<String, Object> cfg) {
        int ms = com.bgssai.shortai.production.support.ShortJson.integer(cfg.get("poll_interval_ms"), 3000);
        if (ms < 0) {
            return 0;
        }
        return Math.min(10_000, ms);
    }

    private ProviderHttp() {
    }
}
