package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.secret.SysSecretReader;

class TokenHubClientSupportTest {

    @Test
    void resolveHubPrefersExplicitHubThenBaseUrlThenModelCode() {
        assertEquals(TokenHubClientSupport.HUB_GLOBAL, TokenHubClientSupport.resolveHub(
                Map.of("hub", "global"), "deepseek-chat"));
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of("base_url", "https://tokenhub-cn.bgssai.com"), "x"));
        assertEquals(TokenHubClientSupport.HUB_GLOBAL, TokenHubClientSupport.resolveHub(
                Map.of(), "gpt-4o-mini"));
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of(), "hy4-preview"));
    }

    @Test
    void resolveApiKeyFallsBackToLegacyCnSecret() {
        SysSecretReader secrets = new SysSecretReader(key -> {
            if (SysSecretReader.AI_TENCENT_TOKENHUB_API_KEY.equals(key)) {
                return "legacy";
            }
            return "";
        });
        assertEquals("legacy", TokenHubClientSupport.resolveApiKey(secrets,
                TokenHubClientSupport.HUB_CN, SysSecretReader.AI_TOKENHUB_CN_API_KEY));
    }

    @Test
    void userFacingMessageScrubsTokenHubBrand() {
        String msg = TokenHubClientSupport.userFacingMessage("tokenhub task failed: AI api key missing for ai.tokenhub_cn.api_key");
        assertFalse(msg.toLowerCase().contains("tokenhub"));
        assertFalse(msg.toLowerCase().contains("api key"));
        assertTrue(msg.contains("模型"));
        assertEquals("写剧本失败，请重试", TokenHubClientSupport.userFacingMessage("写剧本失败，请重试"));
    }
}
