package com.bgssai.shortai.login.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.login.dto.resp.RespOAuthChannels;
import com.bgssai.shortai.login.oauth.LoginOAuthSecrets;

/**
 * 哪些三方登录渠道真的能用：凭证两项都填齐才算开通。
 * 登录页据此决定画哪几个入口，而不是先把入口都画出来、点下去才报「该登录方式暂不可用」。
 */
@Service
public class OAuthChannelsService {

    @Autowired private LoginOAuthSecrets secrets;

    public RespOAuthChannels list() {
        List<String> open = new ArrayList<>();
        if (filled(secrets.wechatAppId(), secrets.wechatAppSecret())) open.add("wechat");
        if (filled(secrets.alipayAppId(), secrets.alipayAppPrivateKey())) open.add("alipay");
        if (filled(secrets.douyinClientKey(), secrets.douyinClientSecret())) open.add("douyin");
        if (filled(secrets.baiduClientId(), secrets.baiduClientSecret())) open.add("baidu");
        if (filled(secrets.bgssaiClientId(), secrets.bgssaiClientSecret())) open.add("bgssai");
        RespOAuthChannels resp = new RespOAuthChannels();
        resp.setChannels(open);
        return resp;
    }

    private static boolean filled(String a, String b) {
        return a != null && !a.isBlank() && b != null && !b.isBlank();
    }
}
