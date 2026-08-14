package com.bgssai.shortai.login.oauth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;

@Component
public class OAuthProviderFactory {

    private final Map<String, OAuthProvider> providers = new HashMap<>();

    public OAuthProviderFactory(List<OAuthProvider> providerList) {
        for (OAuthProvider provider : providerList) {
            providers.put(provider.platformCode(), provider);
        }
    }

    public OAuthProvider get(String platform) {
        OAuthProvider provider = providers.get(platform);
        if (provider == null) throw new BizException("4402", "不支持的登录方式");
        return provider;
    }
}
