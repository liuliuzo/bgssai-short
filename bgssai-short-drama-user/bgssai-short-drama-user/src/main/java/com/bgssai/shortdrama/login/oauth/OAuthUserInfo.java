package com.bgssai.shortdrama.login.oauth;

/** 三方平台换回的用户身份信息，Provider 返回的统一形状。 */
public class OAuthUserInfo {
    private final String openId;
    private final String unionId;
    private final String nickname;
    private final String avatarUrl;

    public OAuthUserInfo(String openId, String unionId, String nickname, String avatarUrl) {
        this.openId = openId;
        this.unionId = unionId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public String getOpenId() { return openId; }
    public String getUnionId() { return unionId; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
}
