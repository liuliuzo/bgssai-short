package com.bgssai.shortai.loginlog.detail;

/**
 * Turns the account identifier typed at login into a display value.
 * Emails and phones are masked. Passwords, OTP codes, and tokens are dropped.
 */
public final class LoginIdentifierMask {

    public static final class Masked {
        private final String type;
        private final String value;

        Masked(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() { return type; }
        public String getValue() { return value; }
    }

    private LoginIdentifierMask() {
    }

    public static Masked mask(String raw, String kind) {
        if ("oauth".equals(kind)) {
            return new Masked("oauth", oauthLabel(raw));
        }
        if (raw == null || raw.isBlank()) {
            return new Masked(null, null);
        }
        String id = raw.trim();
        if (id.length() > 128) {
            id = id.substring(0, 128);
        }
        if (looksSecret(id)) {
            return new Masked(null, null);
        }
        if (looksEmail(id)) {
            return new Masked("email", maskEmail(id));
        }
        if (looksPhone(id)) {
            return new Masked("phone", maskPhone(id));
        }
        return new Masked("username", id.length() > 64 ? id.substring(0, 64) : id);
    }

    public static String oauthLabel(String platform) {
        if (platform == null) return "第三方账号";
        return switch (platform.trim().toLowerCase()) {
            case "wechat" -> "微信";
            case "alipay" -> "支付宝";
            case "douyin" -> "抖音";
            case "baidu" -> "百度";
            case "bgssai" -> "BGSSAI 账号";
            default -> "第三方账号";
        };
    }

    static boolean looksSecret(String value) {
        if (value.matches("\\d{4,8}")) return true;
        if (value.startsWith("eyJ")) return true;
        return value.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    }

    static boolean looksEmail(String value) {
        int at = value.indexOf('@');
        return at > 0 && at < value.length() - 1 && value.indexOf('@', at + 1) < 0 && !value.contains(" ");
    }

    static boolean looksPhone(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 7 || digits.length() > 15) return false;
        return value.matches("\\+?[0-9][0-9\\- ]{6,18}");
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String keep = local.substring(0, 1);
        return keep + "***@" + domain;
    }

    static String maskPhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return "***";
        return digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
    }
}
