package com.bgssai.shortdrama.login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortdrama.login.dto.req.ReqOAuthAuthorizeUrl;
import com.bgssai.shortdrama.login.dto.resp.RespOAuthAuthorizeUrl;
import com.bgssai.shortdrama.login.oauth.OAuthProvider;
import com.bgssai.shortdrama.login.oauth.OAuthProviderFactory;
import com.bgssai.shortdrama.login.oauth.OAuthStateUtil;

@Service
public class OAuthAuthorizeUrlService {

    @Autowired private OAuthProviderFactory oauthProviderFactory;
    @Autowired private OAuthStateUtil oauthStateUtil;

    public RespOAuthAuthorizeUrl build(ReqOAuthAuthorizeUrl req) {
        OAuthProvider provider = oauthProviderFactory.get(req.getPlatform());
        String state = oauthStateUtil.issue(req.getPlatform());
        RespOAuthAuthorizeUrl resp = new RespOAuthAuthorizeUrl();
        resp.setAuthorizeUrl(provider.buildAuthorizeUrl(state));
        return resp;
    }
}
