package com.bgssai.shortai.login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.login.dto.req.ReqOAuthAuthorizeUrl;
import com.bgssai.shortai.login.dto.resp.RespOAuthAuthorizeUrl;
import com.bgssai.shortai.login.oauth.OAuthProvider;
import com.bgssai.shortai.login.oauth.OAuthProviderFactory;
import com.bgssai.shortai.login.oauth.OAuthStateUtil;

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
