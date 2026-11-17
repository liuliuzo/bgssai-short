package com.bgssai.shortai.useraimodel.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;

/**
 * Mints short-lived HS256 JWT tokens from Kling AccessKey + SecretKey per request.
 */
public final class KlingJwtSupport {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private KlingJwtSupport() {
    }

    public static String signJwt(String accessKey, String secretKey) {
        return signJwt(accessKey, secretKey, Instant.now());
    }

    public static String signJwt(String accessKey, String secretKey, Instant now) {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "可灵 Access Key 与 Secret Key 不能为空");
        }
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            String header = encoder.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            long epoch = now.getEpochSecond();
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", accessKey.trim());
            claims.put("exp", epoch + 1800L);
            claims.put("nbf", epoch - 5L);
            String payload = encoder.encodeToString(ShortJson.toJson(claims).getBytes(StandardCharsets.UTF_8));
            String unsigned = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = encoder.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
            return unsigned + "." + signature;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "可灵签名计算失败，请检查 Secret Key 格式");
        }
    }

    public static String resolveBearerToken(String apiKey, String secretKey, String baseUrl) {
        String ak = apiKey == null ? "" : apiKey.trim();
        String sk = secretKey == null ? "" : secretKey.trim();
        if (ak.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "可灵 Access Key 不能为空");
        }
        if (!sk.isBlank()) {
            return signJwt(ak, sk);
        }
        int colon = ak.indexOf(':');
        if (colon > 0 && colon < ak.length() - 1) {
            return signJwt(ak.substring(0, colon), ak.substring(colon + 1));
        }
        String host = baseUrl == null ? "" : baseUrl.trim().toLowerCase(Locale.ROOT);
        if (host.contains("klingai.com") && countDots(ak) != 2) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "可灵官方接口需同时填写 Access Key 与 Secret Key");
        }
        return ak;
    }

    private static int countDots(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }
}
