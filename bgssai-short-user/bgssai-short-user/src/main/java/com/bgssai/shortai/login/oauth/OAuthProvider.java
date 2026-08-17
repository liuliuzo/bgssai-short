package com.bgssai.shortai.login.oauth;

/**
 * 三方授权登录策略接口（Standards §7 策略隔离，仿 {@code payment.channel.PaymentChannelProvider}）：
 * 平台间的协议差异（授权 URL 形状、换取用户信息的字段名）不外泄到 {@code OAuthLoginService}。
 */
public interface OAuthProvider {

    String platformCode();

    /**
     * 拼出跳转到该平台授权页的完整 URL；{@code state} 由 {@link OAuthStateUtil} 签发。
     * {@code redirect_uri} 恒取服务端自己在 {@link OAuthProperties} 里配置的固定值，
     * 不接受调用方传入——防止被构造出跳到任意地址的开放重定向。
     */
    String buildAuthorizeUrl(String state);

    /** 用授权码换用户信息；向开放平台换 token 并拉取资料。 */
    OAuthUserInfo fetchUserInfo(String code);
}
