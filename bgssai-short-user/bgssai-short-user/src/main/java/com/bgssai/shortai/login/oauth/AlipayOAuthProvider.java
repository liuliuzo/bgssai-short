package com.bgssai.shortai.login.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.bgssai.shortai.login.oauth.OAuthIds;
import com.bgssai.shortai.common.exception.BizException;

/**
 * 支付宝开放平台 · 网页授权登录（scope=auth_user）。接口名确认（{@code alipay.system.oauth.token}
 * 换 token，{@code alipay.user.info.share} 取用户信息），走现有 {@code alipay-sdk-java} SDK 的 RPC
 * 签名调用，不用手写签名——与 {@code payment.channel.AlipayQrPayProvider} 同一套客户端构建方式。
 *
 * <p><b>已知边界</b>：授权跳转 URL 的域名与参数按公开资料核实，置信度中等；本仓库无真实
 * AppID/私钥，未做过真实联调，接入真实凭证前须在支付宝开放平台控制台走一遍完整授权流程核对。</p>
 */
@Component
public class AlipayOAuthProvider implements OAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(AlipayOAuthProvider.class);
    private static final String AUTHORIZE_URL = "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm";
    private static final String GATEWAY_URL = "https://openapi.alipay.com/gateway.do";

    @Autowired private OAuthProperties oauthProperties;
    @Autowired private LoginOAuthSecrets loginOAuthSecrets;

    @Override
    public String platformCode() {
        return "alipay";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        OAuthProperties.Alipay cfg = oauthProperties.getAlipay();
        String appId = loginOAuthSecrets.alipayAppId();
        String appPrivateKey = loginOAuthSecrets.alipayAppPrivateKey();
        requireCredentials(appId, appPrivateKey);
        return AUTHORIZE_URL + "?app_id=" + appId
                + "&scope=auth_user&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&state=" + encode(state);
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code) {
        String appId = loginOAuthSecrets.alipayAppId();
        String appPrivateKey = loginOAuthSecrets.alipayAppPrivateKey();
        String alipayPublicKey = loginOAuthSecrets.alipayPublicKey();
        requireCredentials(appId, appPrivateKey);
        try {
            AlipayClient client = new DefaultAlipayClient(GATEWAY_URL, appId,
                    appPrivateKey, "json", "UTF-8", alipayPublicKey, "RSA2");

            AlipaySystemOauthTokenRequest tokenRequest = new AlipaySystemOauthTokenRequest();
            tokenRequest.setGrantType("authorization_code");
            tokenRequest.setCode(code);
            AlipaySystemOauthTokenResponse tokenResponse = client.execute(tokenRequest);
            if (!tokenResponse.isSuccess()) {
                log.warn("支付宝授权换取 access_token 失败 subCode={}", tokenResponse.getSubCode());
                throw new BizException("4404", "授权失败，请重试");
            }
            String accessToken = tokenResponse.getAccessToken();
            String userId = tokenResponse.getUserId();

            AlipayUserInfoShareRequest userInfoRequest = new AlipayUserInfoShareRequest();
            AlipayUserInfoShareResponse userInfoResponse = client.execute(userInfoRequest, accessToken);
            String nickname = userInfoResponse.isSuccess() ? userInfoResponse.getNickName() : null;
            String avatarUrl = userInfoResponse.isSuccess() ? userInfoResponse.getAvatar() : null;
            return new OAuthUserInfo(userId, null, nickname, avatarUrl);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("支付宝授权登录异常 error={}", e.getClass().getSimpleName());
            throw new BizException("4404", "授权失败，请重试");
        }
    }


    private static void requireCredentials(String appId, String appPrivateKey) {
        if (OAuthIds.blank(appId) || OAuthIds.blank(appPrivateKey)) {
            throw new BizException("4403", "该登录方式暂不可用");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
