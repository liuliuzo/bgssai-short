package com.bgssai.shortai.payment.util;

public final class PemSupport {

    private PemSupport() {
        throw new IllegalStateException("Utility class");
    }

    public static String toPkcs8Pem(String privateKeyMaterial) {
        if (privateKeyMaterial == null) {
            return "";
        }
        String trimmed = privateKeyMaterial.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.contains("BEGIN")) {
            return trimmed.replace("\\n", "\n");
        }
        String body = trimmed.replaceAll("\\s+", "");
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < body.length(); i += 64) {
            int end = Math.min(i + 64, body.length());
            pem.append(body, i, end).append('\n');
        }
        pem.append("-----END PRIVATE KEY-----");
        return pem.toString();
    }

    public static String toAlipayKey(String keyMaterial) {
        if (keyMaterial == null) {
            return "";
        }
        String trimmed = keyMaterial.trim().replace("\\n", "\n");
        return trimmed
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
