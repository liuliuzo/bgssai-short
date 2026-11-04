package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.secret.SysSecretReader;

class TokenHubClientSupportTest {

    @Test
    void resolveHubAlwaysPinsCnForShort() {
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of("hub", "global"), "deepseek-chat"));
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of("base_url", "https://www.bgssai-tokenhub.cn"), "x"));
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of("base_url", "https://dev.user.bgssai-tokenhub.cn/v1"), "gpt-4o-mini"));
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of("base_url", "https://dev.user.bgssai-tokenhub.com/v1"), "deepseek-chat"));
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of(), "gpt-4o-mini"));
        assertEquals(TokenHubClientSupport.HUB_CN, TokenHubClientSupport.resolveHub(
                Map.of(), "hy4-preview"));
        assertTrue(TokenHubClientSupport.wouldSelectGlobal(Map.of("hub", "global"), "x"));
        assertTrue(TokenHubClientSupport.wouldSelectGlobal(Map.of(), "gpt-4o-mini"));
        assertFalse(TokenHubClientSupport.wouldSelectGlobal(Map.of("hub", "cn"), "hy4-preview"));
        assertTrue(TokenHubClientSupport.isOfficialHub("https://www.bgssai-tokenhub.cn"));
        assertTrue(TokenHubClientSupport.isOfficialHub("https://dev.user.bgssai-tokenhub.cn"));
        assertFalse(TokenHubClientSupport.isOfficialHub("https://www.bgssai-tokenhub.com"));
        assertFalse(TokenHubClientSupport.isOfficialHub("https://www.bgssai-tokenhub-global.com"));
    }

    @Test
    void defaultBaseUrlIgnoresOverseasHost() {
        assertEquals("https://www.bgssai-tokenhub.cn", TokenHubClientSupport.defaultBaseUrl(
                TokenHubClientSupport.HUB_GLOBAL,
                "https://www.bgssai-tokenhub.cn",
                "https://www.bgssai-tokenhub.com"));
        assertEquals(SysSecretReader.AI_TOKENHUB_CN_API_KEY,
                TokenHubClientSupport.defaultSecretRef(TokenHubClientSupport.HUB_GLOBAL));
    }

    @Test
    void resolveApiKeyNeverReadsOverseasSecret() {
        SysSecretReader secrets = new SysSecretReader(key -> {
            if (SysSecretReader.AI_TOKENHUB_GLOBAL_API_KEY.equals(key)) {
                return "overseas";
            }
            if (SysSecretReader.AI_TOKENHUB_CN_API_KEY.equals(key)) {
                return "domestic";
            }
            return "";
        });
        assertEquals("domestic", TokenHubClientSupport.resolveApiKey(secrets,
                TokenHubClientSupport.HUB_GLOBAL, SysSecretReader.AI_TOKENHUB_GLOBAL_API_KEY));
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
        assertEquals("模型服务暂未开通：deepseek，请稍后重试",
                TokenHubClientSupport.userFacingMessage("Provider is not configured: deepseek"));
        assertEquals("模型服务暂时不可用，请稍后重试", TokenHubClientSupport.userFacingMessage("http 503"));
        assertEquals("该模型暂不可用，请改选其它模型",
                TokenHubClientSupport.userFacingMessage("cross-region hub disabled for short"));
        assertEquals("生成超时，任务可能仍在处理，请到任务中心查看或稍后重试",
                TokenHubClientSupport.userFacingMessage("model task timeout"));
        assertFalse(TokenHubClientSupport.userFacingMessage("tokenhub timeout").toLowerCase().contains("tokenhub"));
        assertEquals("上游拒绝", TokenHubClientSupport.userFacingMessage("上游拒绝"));
        assertFalse(TokenHubClientSupport.scrubBrand("TokenHub DeepSeek").toLowerCase().contains("tokenhub"));
    }
}
