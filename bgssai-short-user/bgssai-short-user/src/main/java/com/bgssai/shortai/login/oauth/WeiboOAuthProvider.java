package com.bgssai.shortai.login.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bgssai.shortai.login.oauth.OAuthIds;
import com.bgssai.shortai.common.exception.BizException;

/**
 * 微博开放平台 · OAuth2.0 授权登录。协议按官方文档核实（置信度中等）：
 * <a href="https://open.weibo.com/wiki/Oauth2/authorize">Oauth2/authorize</a>、
 * <a href="https://open.weibo.com/wiki/Oauth2/access_token">Oauth2/access_token</a>。
 * 微博没有 unionid 概念（单平台，不存在"多应用互通标识"这层）。
 *
 * <p><b>已知边界</b>：本仓库无真实 client_id/client_secret，未做过真实联调，接入真实凭证前须在
 * 微博开放平台控制台走一遍完整授权流程核对字段。</p>
 */
@Component
public class WeiboOAuthProvider implements OAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(WeiboOAuthProvider.class);
    private static final String AUTHORIZE_URL = "https://api.weibo.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.weibo.com/oauth2/access_token";
    private static final String USERINFO_URL = "https://api.weibo.com/2/users/show.json";

    @Autowired private OAuthProperties oauthProperties;
    @Autowired private RestTemplate restTemplate;

    @Override
    public String platformCode() {
        return "weibo";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.Weibo cfg = oauthProperties.getWeibo();
        if (oauthProperties.isMockEnabled()) {
            return cfg.getRedirectUri() + "?code=mock-code&state=" + encode(state);
        }
        requireCredentials(cfg.getClientId(), cfg.getClientSecret());
        return AUTHORIZE_URL + "?client_id=" + cfg.getClientId()
                + "&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&response_type=code&state=" + encode(state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String code) {
        if (oauthProperties.isMockEnabled()) {
            return new OAuthUserInfo("mock-weibo-uid", null, "mock微博用户", null);
        }
        OAuthProperties.Weibo cfg = oauthProperties.getWeibo();
        requireCredentials(cfg.getClientId(), cfg.getClientSecret());
        try {
            Map<String, String> tokenForm = new HashMap<>();
            tokenForm.put("client_id", cfg.getClientId());
            tokenForm.put("client_secret", cfg.getClientSecret());
            tokenForm.put("grant_type", "authorization_code");
            tokenForm.put("redirect_uri", cfg.getRedirectUri());
            tokenForm.put("code", code);
            Map<String, Object> tokenResp = restTemplate.postForObject(TOKEN_URL, tokenForm, Map.class);
            if (tokenResp == null || tokenResp.get("uid") == null) {
                log.warn("微博授权换取 access_token 失败");
                throw new BizException("4404", "授权失败，请重试");
            }
            String uid = String.valueOf(tokenResp.get("uid"));
            String accessToken = String.valueOf(tokenResp.get("access_token"));

            String userInfoUrl = USERINFO_URL + "?access_token=" + encode(accessToken) + "&uid=" + encode(uid);
            Map<String, Object> userResp = restTemplate.getForObject(userInfoUrl, Map.class);
            String nickname = userResp == null ? null : (String) userResp.get("screen_name");
            String avatarUrl = userResp == null ? null : (String) userResp.get("profile_image_url");
            return new OAuthUserInfo(uid, null, nickname, avatarUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微博授权登录异常 error={}", e.getClass().getSimpleName());
            throw new BizException("4404", "授权失败，请重试");
        }
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
