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
 * 抖音开放平台 · 网站应用授权登录。协议按官方文档与公开资料核实（置信度中等，字段名以
 * <a href="https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/sdk/web-app/web/permission">
 * 手机号和扫码登录授权</a>为准）：响应体外层包一层 {@code data}，本实现按此形状解析。
 *
 * <p><b>已知边界</b>：本仓库无真实 client_key/client_secret，未做过真实联调，接入真实凭证前须在
 * 抖音开放平台控制台走一遍完整授权流程核对字段（尤其 {@code data} 包裹层是否与实际一致）。</p>
 */
@Component
public class DouyinOAuthProvider implements OAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(DouyinOAuthProvider.class);
    private static final String AUTHORIZE_URL = "https://open.douyin.com/platform/oauth/connect";
    private static final String TOKEN_URL = "https://open.douyin.com/oauth/access_token/";
    private static final String USERINFO_URL = "https://open.douyin.com/oauth/userinfo/";

    @Autowired private OAuthProperties oauthProperties;
    @Autowired private LoginOAuthSecrets loginOAuthSecrets;
    @Autowired private RestTemplate restTemplate;

    @Override
    public String platformCode() {
        return "douyin";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.Douyin cfg = oauthProperties.getDouyin();
        String clientKey = loginOAuthSecrets.douyinClientKey();
        String clientSecret = loginOAuthSecrets.douyinClientSecret();
        requireCredentials(clientKey, clientSecret);
        return AUTHORIZE_URL + "?client_key=" + clientKey
                + "&response_type=code&scope=user_info&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&state=" + encode(state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String code) {
        String clientKey = loginOAuthSecrets.douyinClientKey();
        String clientSecret = loginOAuthSecrets.douyinClientSecret();
        requireCredentials(clientKey, clientSecret);
        try {
            Map<String, String> tokenForm = new HashMap<>();
            tokenForm.put("client_key", clientKey);
            tokenForm.put("client_secret", clientSecret);
            tokenForm.put("code", code);
            tokenForm.put("grant_type", "authorization_code");
            Map<String, Object> tokenResp = restTemplate.postForObject(TOKEN_URL, tokenForm, Map.class);
            Map<String, Object> tokenData = unwrapData(tokenResp);
            if (tokenData == null || tokenData.get("open_id") == null) {
                log.warn("抖音授权换取 access_token 失败");
                throw new BizException("4404", "授权失败，请重试");
            }
            String openId = String.valueOf(tokenData.get("open_id"));
            String unionId = tokenData.get("union_id") == null ? null : String.valueOf(tokenData.get("union_id"));
            String accessToken = String.valueOf(tokenData.get("access_token"));

            String userInfoUrl = USERINFO_URL + "?access_token=" + encode(accessToken) + "&open_id=" + encode(openId);
            Map<String, Object> userResp = restTemplate.getForObject(userInfoUrl, Map.class);
            Map<String, Object> userData = unwrapData(userResp);
            String nickname = userData == null ? null : (String) userData.get("nickname");
            String avatarUrl = userData == null ? null : (String) userData.get("avatar");
            return new OAuthUserInfo(openId, unionId, nickname, avatarUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("抖音授权登录异常 error={}", e.getClass().getSimpleName());
            throw new BizException("4404", "授权失败，请重试");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapData(Map<String, Object> resp) {
        if (resp == null) return null;
        Object data = resp.get("data");
        return data instanceof Map ? (Map<String, Object>) data : resp;
    }

    private static void requireCredentials(String clientKey, String clientSecret) {
        if (OAuthIds.blank(clientKey) || OAuthIds.blank(clientSecret)) {
            throw new BizException("4403", "该登录方式暂不可用");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
