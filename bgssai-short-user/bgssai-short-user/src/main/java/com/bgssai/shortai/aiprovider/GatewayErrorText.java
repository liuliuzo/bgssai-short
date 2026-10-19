package com.bgssai.shortai.aiprovider;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bgssai.shortai.production.support.ShortJson;

/**
 * Turns a gateway error JSON body into a short Chinese sentence.
 * The gateway {@code code} stays in the log and is not shown to the creator.
 */
public final class GatewayErrorText {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorText.class);
    private static final Pattern PROVIDER = Pattern.compile(
            "(?i)provider is not configured:\\s*([a-z0-9._-]{1,40})");

    private GatewayErrorText() {
    }

    /** User-facing sentence, including a retry hint. Logs {@code code} when the body has one. */
    public static String explain(int status, String body) {
        Parsed parsed = parse(body);
        note(status, parsed);
        return withRetry(core(status, parsed));
    }

    /**
     * A readiness probe is ready when the gateway accepted it, or rejected only the
     * empty probe body after the provider check. {@code provider_not_configured} and
     * {@code unsupported_model} are not ready. Does not log.
     */
    public static boolean probeMeansReady(int status, String body) {
        if (status >= 200 && status < 300) {
            return true;
        }
        Parsed parsed = parse(body);
        if (blocked(parsed)) {
            return false;
        }
        if (status == 400) {
            return true;
        }
        return false;
    }

    /** Reason without the retry suffix, for a sentence that already ends with one. */
    public static String reason(int status, String body) {
        return core(status, parse(body));
    }

    public static void note(int status, String body) {
        note(status, parse(body));
    }

    private static void note(int status, Parsed parsed) {
        if (parsed.code.isBlank() && parsed.type.isBlank()) {
            log.warn("gateway error status={}", status);
            return;
        }
        log.warn("gateway error status={} code={} type={}", status, parsed.code, parsed.type);
    }

    private static boolean blocked(Parsed parsed) {
        if ("provider_not_configured".equals(parsed.code) || "unsupported_model".equals(parsed.code)) {
            return true;
        }
        String lower = parsed.message.toLowerCase(Locale.ROOT);
        return lower.contains("not configured") || lower.contains("not registered")
                || lower.contains("unsupported model");
    }

    private static String core(int status, Parsed parsed) {
        String provider = providerName(parsed.message);
        if ("provider_not_configured".equals(parsed.code)
                || parsed.message.toLowerCase(Locale.ROOT).contains("provider is not configured")) {
            return "模型服务暂未开通：" + (provider.isEmpty() ? "当前模型" : provider);
        }
        if ("unsupported_model".equals(parsed.code)
                || parsed.message.toLowerCase(Locale.ROOT).contains("not registered")) {
            return "该模型暂未开放";
        }
        if (status == 401 || status == 403) {
            return "模型网关鉴权失败";
        }
        if (status == 429 || parsed.message.toLowerCase(Locale.ROOT).contains("rate limit")) {
            return "模型请求过于频繁";
        }
        String lower = parsed.message.toLowerCase(Locale.ROOT);
        if (lower.contains("quota") || lower.contains("insufficient")) {
            return "模型额度不足";
        }
        if (status >= 500 || status <= 0) {
            return "模型服务暂时不可用";
        }
        return "模型暂时不可用";
    }

    static String providerName(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        Matcher matcher = PROVIDER.matcher(message);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).toLowerCase(Locale.ROOT);
    }

    private static String withRetry(String reason) {
        if (reason == null || reason.isBlank()) {
            return "模型暂时不可用，请稍后重试";
        }
        if (reason.contains("请稍后重试")) {
            return reason;
        }
        return reason + "，请稍后重试";
    }

    private static Parsed parse(String body) {
        Map<String, Object> root = ShortJson.map(body);
        Object error = root.get("error");
        if (!(error instanceof Map<?, ?> map)) {
            return new Parsed("", "", ShortJson.text(root.get("message")));
        }
        return new Parsed(ShortJson.text(map.get("code")), ShortJson.text(map.get("type")),
                ShortJson.text(map.get("message")));
    }

    private record Parsed(String code, String type, String message) {
    }
}
