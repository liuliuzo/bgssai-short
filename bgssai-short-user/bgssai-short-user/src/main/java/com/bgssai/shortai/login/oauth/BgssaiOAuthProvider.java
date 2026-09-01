package com.bgssai.shortai.login.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bgssai.shortai.common.exception.BizException;

/**
 * BGSSAI 官网开放授权登录（授权码 + 服务端 client_secret，PKCE 可选省略）。
 * client_id / client_secret 读 {@link LoginOAuthSecrets}；redirect_uri 与 IdP 基址写 properties。
 */
@Component
public class BgssaiOAuthProvider implements OAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(BgssaiOAuthProvider.class);

    @Autowired private OAuthProperties oauthProperties;
    @Autowired private LoginOAuthSecrets loginOAuthSecrets;
    @Autowired private RestTemplate restTemplate;
    @Value("${bgssai.idp.base-url}") private String idpBaseUrl;

    @Override
    public String platformCode() {
        return "bgssai";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.Bgssai cfg = oauthProperties.getBgssai();
        String clientId = loginOAuthSecrets.bgssaiClientId();
        String clientSecret = loginOAuthSecrets.bgssaiClientSecret();
        requireCredentials(clientId, clientSecret);
        return trimSlash(idpBaseUrl) + "/oauth/authorize?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&response_type=code&scope=" + encode("profile email")
                + "&state=" + encode(state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String code) {
        String clientId = loginOAuthSecrets.bgssaiClientId();
        String clientSecret = loginOAuthSecrets.bgssaiClientSecret();
        requireCredentials(clientId, clientSecret);
        try {
            Map<String, String> tokenBody = new LinkedHashMap<>();
            tokenBody.put("grant_type", "authorization_code");
            tokenBody.put("code", code);
            tokenBody.put("client_id", clientId);
            tokenBody.put("client_secret", clientSecret);
            tokenBody.put("redirect_uri", oauthProperties.getBgssai().getRedirectUri());
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> tokenWrap = restTemplate.postForObject(
                    trimSlash(idpBaseUrl) + "/api/user/oauth/token",
                    new HttpEntity<>(tokenBody, jsonHeaders),
                    Map.class);
            Map<String, Object> tokenData = unwrapWebsiteData(tokenWrap);
            if (tokenData == null || OAuthIds.blank(text(tokenData, "access_token"))) {
                log.warn("BGSSAI 授权换取 access_token 失败");
                throw new BizException("4404", "授权失败，请重试");
            }
            String accessToken = text(tokenData, "access_token");

            HttpHeaders bearer = new HttpHeaders();
            bearer.setBearerAuth(accessToken);
            ResponseEntity<Map> userResp = restTemplate.exchange(
                    trimSlash(idpBaseUrl) + "/api/user/oauth/userinfo",
                    HttpMethod.GET,
                    new HttpEntity<>(bearer),
                    Map.class);
            Map<String, Object> userData = unwrapWebsiteData(userResp.getBody());
            String sub = userData == null ? null : text(userData, "sub");
            if (OAuthIds.blank(sub)) {
                log.warn("BGSSAI 授权 userinfo 缺少 sub");
                throw new BizException("4404", "授权失败，请重试");
            }
            String nickname = userData.get("nickname") == null ? null : String.valueOf(userData.get("nickname"));
            return new OAuthUserInfo(sub, null, nickname, null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("BGSSAI 授权登录异常 error={}", e.getClass().getSimpleName());
            throw new BizException("4404", "授权失败，请重试");
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> unwrapWebsiteData(Map<String, Object> wrap) {
        if (wrap == null || !isOk(wrap)) {
            return null;
        }
        Object data = wrap.get("data");
        return data instanceof Map ? (Map<String, Object>) data : null;
    }

    private static boolean isOk(Map<String, Object> wrap) {
        Object code = wrap.get("code");
        if (code instanceof Number) {
            return ((Number) code).intValue() == 200;
        }
        return code != null && "200".equals(String.valueOf(code));
    }

    private static String text(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static void requireCredentials(String clientId, String clientSecret) {
        if (OAuthIds.blank(clientId) || OAuthIds.blank(clientSecret)) {
            throw new BizException("4403", "该登录方式暂不可用");
        }
    }

    private static String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
