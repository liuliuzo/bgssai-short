package com.bgssai.shortai.login.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bgssai.shortai.common.exception.BizException;

/**
 * 百度开放平台 · 网站应用授权登录。协议按官方文档核实：
 * <a href="https://developer.baidu.com/wiki/index.php?title=docs/oauth/authorization">
 * 百度 OAuth2 授权</a>。
 *
 * <p>clientId = API Key，clientSecret = Secret Key，读 {@link LoginOAuthSecrets}。
 * redirect_uri 写在 application-*.properties。portrait 需拼
 * {@code https://himg.bdimg.com/sys/portrait/item/{portrait}}。
 */
@Component
public class BaiduOAuthProvider implements OAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(BaiduOAuthProvider.class);
    private static final String AUTHORIZE_URL = "https://openapi.baidu.com/oauth/2.0/authorize";
    private static final String TOKEN_URL = "https://openapi.baidu.com/oauth/2.0/token";
    private static final String USERINFO_URL = "https://openapi.baidu.com/rest/2.0/passport/users/getLoggedInUser";
    private static final String PORTRAIT_PREFIX = "https://himg.bdimg.com/sys/portrait/item/";

    @Autowired private OAuthProperties oauthProperties;
    @Autowired private LoginOAuthSecrets loginOAuthSecrets;
    @Autowired private RestTemplate restTemplate;

    @Override
    public String platformCode() {
        return "baidu";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.Baidu cfg = oauthProperties.getBaidu();
        String clientId = loginOAuthSecrets.baiduClientId();
        String clientSecret = loginOAuthSecrets.baiduClientSecret();
        requireCredentials(clientId, clientSecret);
        return AUTHORIZE_URL + "?client_id=" + encode(clientId)
                + "&response_type=code&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&scope=basic&display=popup&state=" + encode(state);
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code) {
        String clientId = loginOAuthSecrets.baiduClientId();
        String clientSecret = loginOAuthSecrets.baiduClientSecret();
        requireCredentials(clientId, clientSecret);
        try {
            String tokenUrl = TOKEN_URL + "?grant_type=authorization_code"
                    + "&code=" + encode(code)
                    + "&client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&redirect_uri=" + encode(oauthProperties.getBaidu().getRedirectUri());
            Map<String, Object> tokenResp = restTemplate.getForObject(tokenUrl, Map.class);
            if (tokenResp == null || tokenResp.get("access_token") == null
                    || tokenResp.containsKey("error") || tokenResp.containsKey("error_code")) {
                log.warn("百度授权换取 access_token 失败");
                throw new BizException("4404", "授权失败，请重试");
            }
            String accessToken = String.valueOf(tokenResp.get("access_token"));

            String userInfoUrl = USERINFO_URL + "?access_token=" + encode(accessToken) + "&get_unionid=1";
            Map<String, Object> userResp = restTemplate.getForObject(userInfoUrl, Map.class);
            if (userResp == null || (userResp.containsKey("error_code") && !Integer.valueOf(0).equals(userResp.get("error_code")))) {
                log.warn("百度拉取用户资料失败");
                throw new BizException("4404", "授权失败，请重试");
            }
            String openId = textOrNull(userResp, "openid");
            if (OAuthIds.blank(openId)) {
                openId = textOrNull(userResp, "userid");
            }
            if (OAuthIds.blank(openId)) {
                throw new BizException("4404", "授权失败，请重试");
            }
            String unionId = textOrNull(userResp, "unionid");
            String nickname = textOrNull(userResp, "username");
            String portrait = textOrNull(userResp, "portrait");
            String avatarUrl = OAuthIds.blank(portrait) ? null : PORTRAIT_PREFIX + portrait;
            return new OAuthUserInfo(openId, unionId, nickname, avatarUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("百度授权登录异常 error={}", e.getClass().getSimpleName());
            throw new BizException("4404", "授权失败，请重试");
        }
    }

    private static String textOrNull(Map<String, Object> resp, String key) {
        Object value = resp.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static void requireCredentials(String clientId, String clientSecret) {
        if (OAuthIds.blank(clientId) || OAuthIds.blank(clientSecret)) {
            throw new BizException("4403", "该登录方式暂不可用");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}


