package com.bgssai.shortai.login.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bgssai.shortai.login.oauth.OAuthIds;
import com.bgssai.shortai.common.exception.BizException;

/**
 * 微信开放平台 · 网站应用微信登录（scope=snsapi_login）。协议按官方文档核实（置信度高）：
 * <a href="https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html">
 * 网站应用微信登录文档</a>。
 *
 * <p><b>已知边界</b>：本仓库无真实 AppID/AppSecret，未做过真实联调；接入真实凭证前须在微信开放平台
 * 控制台走一遍完整授权流程核对字段。</p>
 */
@Component
public class WechatOAuthProvider implements OAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(WechatOAuthProvider.class);
    private static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USERINFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    @Autowired private OAuthProperties oauthProperties;
    @Autowired private RestTemplate restTemplate;

    @Override
    public String platformCode() {
        return "wechat";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.Wechat cfg = oauthProperties.getWechat();
        // mock 模式直接拼一个指向本应用回调地址、带假 code 的 URL——开发环境没有真实账号可跳转到
        // 微信授权页，让前端点击后直接落到 callback 页面即可跑通整条登录链路。
        if (oauthProperties.isMockEnabled()) {
            return cfg.getRedirectUri() + "?code=mock-code&state=" + encode(state);
        }
        requireCredentials(cfg.getAppId(), cfg.getAppSecret());
        return AUTHORIZE_URL + "?appid=" + cfg.getAppId()
                + "&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&response_type=code&scope=snsapi_login&state=" + encode(state)
                + "#wechat_redirect";
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code) {
        if (oauthProperties.isMockEnabled()) {
            return new OAuthUserInfo("mock-wechat-openid", "mock-wechat-unionid", "mock微信用户", null);
        }
        OAuthProperties.Wechat cfg = oauthProperties.getWechat();
        requireCredentials(cfg.getAppId(), cfg.getAppSecret());
        try {
            String tokenUrl = TOKEN_URL + "?appid=" + cfg.getAppId() + "&secret=" + cfg.getAppSecret()
                    + "&code=" + encode(code) + "&grant_type=authorization_code";
            Map<String, Object> tokenResp = restTemplate.getForObject(tokenUrl, Map.class);
            if (tokenResp == null || tokenResp.get("openid") == null || tokenResp.containsKey("errcode")) {
                log.warn("微信授权换取 access_token 失败 errcode={}", tokenResp == null ? null : tokenResp.get("errcode"));
                throw new BizException("4404", "授权失败，请重试");
            }
            String openId = String.valueOf(tokenResp.get("openid"));
            String unionId = tokenResp.get("unionid") == null ? null : String.valueOf(tokenResp.get("unionid"));
            String accessToken = String.valueOf(tokenResp.get("access_token"));

            String userInfoUrl = USERINFO_URL + "?access_token=" + encode(accessToken)
                    + "&openid=" + encode(openId) + "&lang=zh_CN";
            Map<String, Object> userResp = restTemplate.getForObject(userInfoUrl, Map.class);
            String nickname = userResp == null ? null : (String) userResp.get("nickname");
            String avatarUrl = userResp == null ? null : (String) userResp.get("headimgurl");
            return new OAuthUserInfo(openId, unionId, nickname, avatarUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微信授权登录异常 error={}", e.getClass().getSimpleName());
            throw new BizException("4404", "授权失败，请重试");
        }
    }

    private static void requireCredentials(String appId, String appSecret) {
        if (OAuthIds.blank(appId) || OAuthIds.blank(appSecret)) {
            throw new BizException("4403", "该登录方式暂不可用");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
