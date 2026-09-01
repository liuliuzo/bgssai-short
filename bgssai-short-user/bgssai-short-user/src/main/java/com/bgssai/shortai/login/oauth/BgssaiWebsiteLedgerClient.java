package com.bgssai.shortai.login.oauth;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.bgssai.shortai.mapper.OauthBindingMapper;
import com.bgssai.shortai.model.OauthBinding;
import com.bgssai.shortai.model.OauthBindingExample;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 已绑定 BGSSAI 官网身份的用户：本地额度扣减成功后，向官网中央账本上报用量。
 * 未绑定则跳过。官网不可达时只打日志，不回滚本地扣减。
 */
@Component
public class BgssaiWebsiteLedgerClient {

    private static final Logger log = LoggerFactory.getLogger(BgssaiWebsiteLedgerClient.class);
    private static final RestClient HTTP = RestClient.create();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final OauthBindingMapper oauthBindingMapper;
    private final LoginOAuthSecrets loginOAuthSecrets;
    private final String idpBaseUrl;

    public BgssaiWebsiteLedgerClient(OauthBindingMapper oauthBindingMapper,
                                     LoginOAuthSecrets loginOAuthSecrets,
                                     @Value("${bgssai.idp.base-url}") String idpBaseUrl) {
        this.oauthBindingMapper = oauthBindingMapper;
        this.loginOAuthSecrets = loginOAuthSecrets;
        this.idpBaseUrl = BgssaiOAuthProvider.trimSlash(idpBaseUrl);
    }

    public void reportDebit(String userId, long amount, String usageKind) {
        if (userId == null || userId.isBlank() || amount < 1 || amount > Integer.MAX_VALUE) {
            return;
        }
        OauthBindingExample ex = new OauthBindingExample();
        ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andPlatformEqualTo("bgssai")
                .andDelFlagEqualTo(Boolean.FALSE);
        OauthBinding binding = oauthBindingMapper.selectOneByExample(ex);
        if (binding == null || OAuthIds.blank(binding.getOpenId())) {
            return;
        }
        String clientId = loginOAuthSecrets.bgssaiClientId();
        String clientSecret = loginOAuthSecrets.bgssaiClientSecret();
        if (OAuthIds.blank(clientId) || OAuthIds.blank(clientSecret)) {
            log.warn("BGSSAI 中央账本跳过：oauth.bgssai 凭证未配置 userId={}", userId);
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("user_id", binding.getOpenId());
        body.put("usage_points", (int) amount);
        if (usageKind != null && !usageKind.isBlank()) {
            body.put("usage_kind", usageKind);
        }
        body.put("remark", "short debit " + (usageKind == null ? "" : usageKind));
        try {
            String json = JSON.writeValueAsString(body);
            String resp = HTTP.post()
                    .uri(URI.create(idpBaseUrl + "/api/user/oauth/usage"))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(json)
                    .retrieve()
                    .body(String.class);
            JsonNode wrap = resp == null ? null : JSON.readTree(resp);
            if (wrap != null && wrap.has("code") && wrap.get("code").asInt(0) != 200) {
                log.warn("BGSSAI 中央账本上报被拒 userId={} code={} message={}",
                        userId, wrap.get("code").asInt(), wrap.path("message").asText(""));
                return;
            }
            log.info("BGSSAI 中央账本已上报 userId={} points={} kind={}", userId, amount, usageKind);
        } catch (RestClientException e) {
            log.warn("BGSSAI 中央账本上报失败 userId={} error={}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("BGSSAI 中央账本上报异常 userId={} error={}", userId, e.getMessage());
        }
    }
}
