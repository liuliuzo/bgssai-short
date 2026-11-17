package com.bgssai.shortai.login.sms;

/**
 * 腾讯云短信手机号区号路由与归一化。
 *
 * <ul>
 *   <li>中国大陆 {@code MAINLAND}：匹配 {@code 1[3-9]\d{9}} 或 {@code (+86|0086)1[3-9]\d{9}}，
 *       库表与 OTP 键归一为 11 位数字，腾讯云入参为 {@code +86<11位>}。</li>
 *   <li>国际/港澳台 {@code INTERNATIONAL}：匹配非 {@code +86} 的 E.164 {@code +[1-9]\d{6,14}}，
 *       库表、OTP 键与腾讯云入参均保持标准 E.164 {@code +<国家码><号码>}。</li>
 * </ul>
 */
public final class TencentSmsPhoneRoute {

    public enum Route {
        MAINLAND,
        INTERNATIONAL
    }

    private static final String MAINLAND_CORE_REGEX = "1[3-9]\\d{9}";
    private static final String E164_REGEX = "\\+[1-9]\\d{6,14}";

    private TencentSmsPhoneRoute() {
    }

    /** 清理空白与连字符。 */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("[\\s-]+", "");
    }

    /** 解析中国大陆 11 位手机号；非大陆号码返回 null。 */
    public static String normalizeMainland(String raw) {
        String value = clean(raw);
        if (value.matches("^" + MAINLAND_CORE_REGEX + "$")) {
            return value;
        }
        if (value.startsWith("+86")) {
            String rest = value.substring(3);
            return rest.matches("^" + MAINLAND_CORE_REGEX + "$") ? rest : null;
        }
        if (value.startsWith("0086")) {
            String rest = value.substring(4);
            return rest.matches("^" + MAINLAND_CORE_REGEX + "$") ? rest : null;
        }
        return null;
    }

    /** 解析非 +86 的国际/港澳台 E.164 号码；大陆号码或无 + 前缀号码返回 null。 */
    public static String normalizeInternational(String raw) {
        String value = clean(raw);
        if (value.startsWith("00") && value.length() > 2) {
            value = "+" + value.substring(2);
        }
        if (!value.matches("^" + E164_REGEX + "$")) {
            return null;
        }
        if (value.startsWith("+86")) {
            return null;
        }
        return value;
    }

    /** 判断号码归属国内通道或国际/港澳台通道；非法格式返回 null。 */
    public static Route routeOf(String raw) {
        if (normalizeMainland(raw) != null) {
            return Route.MAINLAND;
        }
        if (normalizeInternational(raw) != null) {
            return Route.INTERNATIONAL;
        }
        return null;
    }

    /** 统一存储键：大陆返回 11 位数字，国际/港澳台返回 +E.164；非法返回 null。 */
    public static String normalizeForStorage(String raw) {
        String mainland = normalizeMainland(raw);
        if (mainland != null) {
            return mainland;
        }
        return normalizeInternational(raw);
    }

    /** 构造发往腾讯云 SendSms PhoneNumberSet 的标准 E.164 号码。 */
    public static String toTencentE164(String raw) {
        String mainland = normalizeMainland(raw);
        if (mainland != null) {
            return "+86" + mainland;
        }
        return normalizeInternational(raw);
    }

    /** 日志用内联掩码，绝不暴露完整手机号。 */
    public static String maskPhone(String phone) {
        String cleaned = clean(phone);
        if (cleaned.length() < 7) {
            return "****";
        }
        return cleaned.substring(0, 3) + "****" + cleaned.substring(cleaned.length() - 4);
    }
}
