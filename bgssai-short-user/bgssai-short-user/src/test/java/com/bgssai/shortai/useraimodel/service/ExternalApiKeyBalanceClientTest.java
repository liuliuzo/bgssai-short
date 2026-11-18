package com.bgssai.shortai.useraimodel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.useraimodel.dto.ExternalApiKeyBalancePart;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyItem;
import com.bgssai.shortai.useraimodel.service.ExternalApiKeyBalanceClient.Credential;
import com.bgssai.shortai.useraimodel.service.ExternalApiKeyBalanceClient.Result;
import com.bgssai.shortai.useraimodel.service.ExternalApiKeyBalanceClient.Snapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

class ExternalApiKeyBalanceClientTest {

    private static final String PIXVERSE_KEY = "pv-test-key";
    private static final String DEEPSEEK_KEY = "sk-test-deepseek";
    private static final String KLING_AK = "kling-access-sample";
    private static final String KLING_SK = "kling-secret-sample";
    private static final String MINIMAX_KEY = "mm-test-key";

    @Test
    void parsesProviderBalancesWithoutEchoingTheKey() {
        Snapshot pix = ExternalApiKeyBalanceClient.parsePixVerse(
                "{\"ErrCode\":0,\"Resp\":{\"account_id\":9,\"credit_monthly\":0,\"credit_package\":10000}}");
        assertEquals(ExternalApiKeyBalanceClient.STATUS_OK, pix.status());
        assertEquals("月度", pix.parts().get(0).getLabel());
        assertEquals("0", pix.parts().get(0).getAmount());
        assertEquals("积分", pix.parts().get(0).getUnit());
        assertEquals("10000", pix.parts().get(1).getAmount());
        assertFalse(joined(pix).contains("account_id"));

        Snapshot deepseek = ExternalApiKeyBalanceClient.parseDeepSeek(
                "{\"is_available\":true,\"balance_infos\":[{\"currency\":\"CNY\",\"total_balance\":\"110.00\"}]}");
        assertEquals("110.00", deepseek.parts().get(0).getAmount());
        assertEquals("元", deepseek.parts().get(0).getUnit());

        Snapshot usd = ExternalApiKeyBalanceClient.parseDeepSeek(
                "{\"balance_infos\":[{\"currency\":\"USD\",\"total_balance\":\"2.50\"}]}");
        assertEquals("USD", usd.parts().get(0).getUnit());

        Snapshot kling = ExternalApiKeyBalanceClient.parseKling(
                "{\"code\":0,\"data\":{\"resource_pack_subscribe_infos\":["
                        + "{\"resource_pack_name\":\"视频包\",\"remaining_quantity\":118.0,\"status\":\"online\"},"
                        + "{\"resource_pack_name\":\"过期包\",\"remaining_quantity\":9,\"status\":\"expired\"}]}}");
        assertEquals(1, kling.parts().size());
        assertEquals("视频包", kling.parts().get(0).getLabel());
        assertEquals("118", kling.parts().get(0).getAmount());
        assertEquals("次", kling.parts().get(0).getUnit());

        Snapshot plan = ExternalApiKeyBalanceClient.parseMiniMaxTokenPlan(
                "{\"base_resp\":{\"status_code\":0},\"model_remains\":[{"
                        + "\"current_interval_remaining_percent\":72.5,"
                        + "\"current_weekly_remaining_percent\":80}]}");
        assertEquals("5小时", plan.parts().get(0).getLabel());
        assertEquals("72.5", plan.parts().get(0).getAmount());
        assertEquals("%", plan.parts().get(0).getUnit());

        Snapshot payg = ExternalApiKeyBalanceClient.parseMiniMaxPayg(
                "{\"base_resp\":{\"status_code\":0},\"available_amount\":\"13.91\"}", "元");
        assertEquals("13.91", payg.parts().get(0).getAmount());
        assertEquals("元", payg.parts().get(0).getUnit());
        assertEquals("元", ExternalApiKeyBalanceClient.moneyUnit("https://api.minimaxi.com"));
        assertEquals("USD", ExternalApiKeyBalanceClient.moneyUnit("https://api.minimax.io"));
    }

    @Test
    void readsEachSuppliedKeyOnceAndKeepsBalancesApart() throws Exception {
        List<String> urls = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger calls = new AtomicInteger();
        ExternalApiKeyBalanceClient client = new ExternalApiKeyBalanceClient((url, headers) -> {
            calls.incrementAndGet();
            urls.add(url);
            if (url.contains("pixverse")) {
                assertEquals(PIXVERSE_KEY, headers.get("API-KEY"));
                assertTrue(headers.get("Ai-trace-id") != null && !headers.get("Ai-trace-id").isBlank());
                return new Result(200,
                        "{\"ErrCode\":0,\"Resp\":{\"credit_monthly\":1,\"credit_package\":2}}");
            }
            if (url.contains("/user/balance")) {
                assertEquals("Bearer " + DEEPSEEK_KEY, headers.get("Authorization"));
                return new Result(200,
                        "{\"balance_infos\":[{\"currency\":\"CNY\",\"total_balance\":\"8.00\"}]}");
            }
            throw new AssertionError(url);
        });

        UserAiModelKey pix = row("PIXVERSE", "https://app-api.pixverse.ai", PIXVERSE_KEY, null);
        UserAiModelKey pixAgain = row("PIXVERSE", "https://app-api.pixverse.ai", PIXVERSE_KEY, null);
        UserAiModelKey deepseek = row("DEEPSEEK", "https://api.deepseek.com/v1", DEEPSEEK_KEY, null);
        UserAiModelKey grok = row("XAI", "https://api.x.ai/v1", "xai-test-key", null);
        List<UserAiModelKeyItem> items = List.of(new UserAiModelKeyItem(), new UserAiModelKeyItem(),
                new UserAiModelKeyItem(), new UserAiModelKeyItem());

        client.fill(List.of(pix, pixAgain, deepseek, grok), items);

        assertEquals(2, calls.get());
        assertTrue(urls.stream().anyMatch(url -> url.endsWith("/openapi/v2/account/balance")));
        assertTrue(urls.stream().anyMatch(url -> url.equals("https://api.deepseek.com/user/balance")));
        assertEquals("1", items.get(0).getBalanceList().get(0).getAmount());
        assertEquals(items.get(0).getBalanceList().get(1).getAmount(),
                items.get(1).getBalanceList().get(1).getAmount());
        assertEquals("8.00", items.get(2).getBalanceList().get(0).getAmount());
        assertEquals("元", items.get(2).getBalanceList().get(0).getUnit());
        assertEquals(ExternalApiKeyBalanceClient.STATUS_UNAVAILABLE, items.get(3).getBalanceStatus());
        assertTrue(items.get(3).getBalanceList().isEmpty());
        for (UserAiModelKeyItem item : items) {
            String rendered = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .writeValueAsString(item);
            assertTrue(rendered.contains("balance_status"));
            assertTrue(rendered.contains("balance_list"));
            assertFalse(rendered.contains(PIXVERSE_KEY));
            assertFalse(rendered.contains(DEEPSEEK_KEY));
            assertFalse(rendered.contains("xai-test-key"));
        }
    }

    @Test
    void sameProviderAndMaskCanHaveDistinctBalances() {
        String first = "sk-1-account-a-tail";
        String second = "sk-1-account-b-tail";
        assertEquals(UserAiModelSupport.maskApiKey(first), UserAiModelSupport.maskApiKey(second));
        ExternalApiKeyBalanceClient client = new ExternalApiKeyBalanceClient((url, headers) -> {
            String balance = switch (headers.get("Authorization")) {
                case "Bearer sk-1-account-a-tail" -> "12.50";
                case "Bearer sk-1-account-b-tail" -> "0.00";
                default -> throw new AssertionError("Unexpected credential");
            };
            return new Result(200, "{\"balance_infos\":[{\"currency\":\"CNY\",\"total_balance\":\"" + balance + "\"}]}");
        });
        List<UserAiModelKeyItem> items = List.of(new UserAiModelKeyItem(), new UserAiModelKeyItem());
        client.fill(List.of(row("DEEPSEEK", "https://api.deepseek.com", first, null),
                row("DEEPSEEK", "https://api.deepseek.com", second, null)), items);
        assertEquals("12.50", items.get(0).getBalanceList().get(0).getAmount());
        assertEquals("0.00", items.get(1).getBalanceList().get(0).getAmount());
    }

    @Test
    void klingAndMiniMaxUseTheProviderBalanceRoutes() throws Exception {
        List<String> urls = new ArrayList<>();
        ExternalApiKeyBalanceClient client = new ExternalApiKeyBalanceClient((url, headers) -> {
            urls.add(url);
            if (url.contains("/account/costs")) {
                assertTrue(headers.get("Authorization").startsWith("Bearer "));
                assertFalse(headers.get("Authorization").contains(KLING_SK));
                return new Result(200, "{\"code\":0,\"data\":{\"resource_pack_subscribe_infos\":[]}}");
            }
            if (url.endsWith("/v1/token_plan/remains")) {
                return new Result(200, "{\"base_resp\":{\"status_code\":1004}}");
            }
            if (url.endsWith("/account/query_balance")) {
                assertEquals("Bearer " + MINIMAX_KEY, headers.get("Authorization"));
                return new Result(200, "{\"data\":{\"available_amount\":\"4.20\"}}");
            }
            throw new AssertionError(url);
        });

        Snapshot kling = client.read(new Credential("KLING", "https://api-beijing.klingai.com", KLING_AK, KLING_SK));
        assertEquals("0", kling.parts().get(0).getAmount());
        assertEquals("次", kling.parts().get(0).getUnit());
        assertTrue(urls.get(0).startsWith("https://api-beijing.klingai.com/account/costs?"));
        assertFalse(joined(kling).contains(KLING_AK));
        assertFalse(joined(kling).contains(KLING_SK));

        urls.clear();
        Snapshot mini = client.read(new Credential("MINIMAX", "https://api.minimaxi.com", MINIMAX_KEY, null));
        assertEquals("https://api.minimaxi.com/v1/token_plan/remains", urls.get(0));
        assertEquals("https://api.minimaxi.com/account/query_balance", urls.get(1));
        assertEquals("4.20", mini.parts().get(0).getAmount());
        assertEquals("元", mini.parts().get(0).getUnit());

        ExternalApiKeyBalanceClient refused = new ExternalApiKeyBalanceClient((url, headers) -> {
            throw new AssertionError("must not call " + url);
        });
        assertThrows(IllegalArgumentException.class,
                () -> refused.read(new Credential("DEEPSEEK", "http://127.0.0.1/v1", DEEPSEEK_KEY, null)));
    }

    @Test
    void parsesBgssaiTokenHubSummaryIntoTokensAndWallet() {
        Snapshot both = ExternalApiKeyBalanceClient.parseBgssaiTokenHub(
                "{\"balance_fen\":1391,\"quota_remaining_fen\":1391,"
                        + "\"balance_tokens\":418912345,\"quota_remaining_tokens\":418912345,"
                        + "\"plan_code\":\"prepaid\",\"plan_name\":\"预付费余额\"}");
        assertEquals(ExternalApiKeyBalanceClient.STATUS_OK, both.status());
        assertEquals("额度", both.parts().get(0).getLabel());
        assertEquals("418912345", both.parts().get(0).getAmount());
        assertEquals("Token", both.parts().get(0).getUnit());
        assertEquals("钱包", both.parts().get(1).getLabel());
        assertEquals("13.91", both.parts().get(1).getAmount());
        assertEquals("元", both.parts().get(1).getUnit());

        Snapshot tokensOnly = ExternalApiKeyBalanceClient.parseBgssaiTokenHub(
                "{\"balance_fen\":0,\"balance_tokens\":0}");
        assertEquals(ExternalApiKeyBalanceClient.STATUS_OK, tokensOnly.status());
        assertEquals("0", tokensOnly.parts().get(0).getAmount());

        assertEquals(ExternalApiKeyBalanceClient.STATUS_FAILED,
                ExternalApiKeyBalanceClient.parseBgssaiTokenHub("{\"plan_code\":\"prepaid\"}").status());
    }

    @Test
    void bgssaiTokenHubReadsTheGatewayBalanceRoute() throws Exception {
        List<String> urls = new ArrayList<>();
        ExternalApiKeyBalanceClient client = new ExternalApiKeyBalanceClient((url, headers) -> {
            urls.add(url);
            assertEquals("Bearer th-test-key", headers.get("Authorization"));
            return new Result(200, "{\"balance_fen\":1200,\"balance_tokens\":9000}");
        });
        Snapshot snap = client.read(new Credential(
                "BGSSAI_TOKENHUB", "https://www.bgssai-tokenhub.cn", "th-test-key", null));
        assertEquals("https://www.bgssai-tokenhub.cn/v1/account/balance", urls.get(0));
        assertEquals(ExternalApiKeyBalanceClient.STATUS_OK, snap.status());
        assertEquals("9000", snap.parts().get(0).getAmount());
        assertFalse(joined(snap).contains("th-test-key"));

        ExternalApiKeyBalanceClient refused = new ExternalApiKeyBalanceClient(
                (url, headers) -> new Result(401, "{\"error\":{\"code\":\"invalid_api_key\"}}"));
        Snapshot denied = refused.read(new Credential(
                "BGSSAI_TOKENHUB", "https://www.bgssai-tokenhub.cn/v1", "th-test-key", null));
        assertEquals(ExternalApiKeyBalanceClient.STATUS_FAILED, denied.status());
        assertTrue(denied.parts().isEmpty());
    }

    @Test
    void failedProviderReadDoesNotInventAZero() {
        assertEquals(ExternalApiKeyBalanceClient.STATUS_FAILED,
                ExternalApiKeyBalanceClient.parsePixVerse("{\"ErrCode\":500}").status());
        assertTrue(ExternalApiKeyBalanceClient.parsePixVerse("{\"ErrCode\":500}").parts().isEmpty());
        assertEquals(ExternalApiKeyBalanceClient.STATUS_FAILED,
                ExternalApiKeyBalanceClient.parseDeepSeek("{\"balance_infos\":[]}").status());
        assertEquals(ExternalApiKeyBalanceClient.STATUS_FAILED,
                ExternalApiKeyBalanceClient.parseKling("{\"code\":1000}").status());
    }

    private static UserAiModelKey row(String provider, String baseUrl, String apiKey, String secretKey) {
        UserAiModelKey row = new UserAiModelKey();
        row.setProviderCode(provider);
        row.setBaseUrl(baseUrl);
        row.setApiKey(apiKey);
        row.setSecretKey(secretKey);
        return row;
    }

    private static String joined(Snapshot snapshot) {
        StringBuilder text = new StringBuilder();
        for (ExternalApiKeyBalancePart part : snapshot.parts()) {
            text.append(part.getLabel()).append(part.getAmount()).append(part.getUnit());
        }
        return text.toString();
    }
}
