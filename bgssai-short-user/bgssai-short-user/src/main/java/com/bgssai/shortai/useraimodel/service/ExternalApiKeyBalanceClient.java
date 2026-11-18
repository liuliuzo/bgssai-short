package com.bgssai.shortai.useraimodel.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.dto.ExternalApiKeyBalancePart;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyItem;

/**
 * Reads the remaining balance already exposed by each external provider.
 * Platform wallet and TokenHub quota are not charged for these keys.
 */
@Component
public class ExternalApiKeyBalanceClient {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    public static final String STATUS_FAILED = "FAILED";

    private static final Logger log = LoggerFactory.getLogger(ExternalApiKeyBalanceClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4);

    interface Transport {
        Result get(String url, Map<String, String> headers) throws Exception;
    }

    record Result(int status, String body) {
    }

    record Snapshot(String status, List<ExternalApiKeyBalancePart> parts) {
        Snapshot {
            parts = parts == null ? List.of() : List.copyOf(parts);
        }
    }

    private final Transport transport;

    public ExternalApiKeyBalanceClient() {
        this(jdkTransport());
    }

    ExternalApiKeyBalanceClient(Transport transport) {
        this.transport = transport;
    }

    public void fill(List<UserAiModelKey> rows, List<UserAiModelKeyItem> items) {
        if (rows == null || items == null || rows.isEmpty() || items.isEmpty()) {
            return;
        }
        int count = Math.min(rows.size(), items.size());
        Map<String, Credential> credentials = new LinkedHashMap<>();
        Map<String, List<Integer>> indexes = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            UserAiModelKey row = rows.get(i);
            if (row == null || items.get(i) == null) {
                continue;
            }
            String fingerprint = fingerprint(row);
            credentials.putIfAbsent(fingerprint, Credential.from(row));
            indexes.computeIfAbsent(fingerprint, key -> new ArrayList<>()).add(i);
        }
        Map<String, Snapshot> snapshots = new LinkedHashMap<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<String, Future<Snapshot>> futures = new LinkedHashMap<>();
            for (Map.Entry<String, Credential> entry : credentials.entrySet()) {
                Credential credential = entry.getValue();
                futures.put(entry.getKey(), pool.submit(() -> readQuietly(credential)));
            }
            for (Map.Entry<String, Future<Snapshot>> entry : futures.entrySet()) {
                snapshots.put(entry.getKey(), await(entry.getValue(), credentials.get(entry.getKey())));
            }
        }
        for (Map.Entry<String, List<Integer>> entry : indexes.entrySet()) {
            Snapshot snapshot = snapshots.getOrDefault(entry.getKey(), failed());
            for (Integer index : entry.getValue()) {
                apply(items.get(index), snapshot);
            }
        }
    }

    static void apply(UserAiModelKeyItem item, Snapshot snapshot) {
        if (item == null || snapshot == null) {
            return;
        }
        item.setBalanceStatus(snapshot.status());
        item.setBalanceList(new ArrayList<>(snapshot.parts()));
    }

    private Snapshot readQuietly(Credential credential) {
        try {
            return read(credential);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("外部密钥余额查询失败 provider={} errType={}",
                    credential.providerCode(), ex.getClass().getSimpleName());
            return failed();
        } catch (Exception ex) {
            log.warn("外部密钥余额查询失败 provider={} errType={}",
                    credential.providerCode(), ex.getClass().getSimpleName());
            return failed();
        }
    }

    private Snapshot await(Future<Snapshot> future, Credential credential) {
        try {
            return future.get(REQUEST_TIMEOUT.toSeconds() + 2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            log.warn("外部密钥余额查询超时 provider={} errType={}",
                    credential.providerCode(), ex.getClass().getSimpleName());
            return failed();
        } catch (Exception ex) {
            future.cancel(true);
            log.warn("外部密钥余额查询超时 provider={} errType={}",
                    credential.providerCode(), ex.getClass().getSimpleName());
            return failed();
        }
    }

    Snapshot read(Credential credential) throws Exception {
        String provider = credential.providerCode();
        if (UserAiModelSupport.PROVIDER_XAI.equals(provider)) {
            return unavailable();
        }
        if (credential.apiKey() == null || credential.apiKey().isBlank()) {
            return failed();
        }
        String baseUrl = requireHttpsBase(credential.baseUrl());
        if (UserAiModelSupport.PROVIDER_PIXVERSE.equals(provider)) {
            return readPixVerse(baseUrl, credential.apiKey());
        }
        if (UserAiModelSupport.PROVIDER_DEEPSEEK.equals(provider)) {
            return readDeepSeek(baseUrl, credential.apiKey());
        }
        if (UserAiModelSupport.PROVIDER_KLING.equals(provider)) {
            return readKling(baseUrl, credential.apiKey(), credential.secretKey());
        }
        if (UserAiModelSupport.PROVIDER_MINIMAX.equals(provider)) {
            return readMiniMax(baseUrl, credential.apiKey());
        }
        if (UserAiModelSupport.PROVIDER_BGSSAI_TOKENHUB.equals(provider)) {
            return readBgssaiTokenHub(baseUrl, credential.apiKey());
        }
        return unavailable();
    }

    private Snapshot readPixVerse(String baseUrl, String apiKey) throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/openapi/v2");
        Result result = transport.get(root + "/openapi/v2/account/balance", Map.of(
                "API-KEY", apiKey,
                "Ai-trace-id", UUID.randomUUID().toString()));
        if (result.status() < 200 || result.status() >= 300) {
            log.warn("外部密钥余额查询失败 provider=PIXVERSE host={} http={}", hostOf(baseUrl), result.status());
            return failed();
        }
        return parsePixVerse(result.body());
    }

    private Snapshot readDeepSeek(String baseUrl, String apiKey) throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        Result result = transport.get(root + "/user/balance", Map.of(
                "Authorization", "Bearer " + apiKey,
                "Accept", "application/json"));
        if (result.status() < 200 || result.status() >= 300) {
            log.warn("外部密钥余额查询失败 provider=DEEPSEEK host={} http={}", hostOf(baseUrl), result.status());
            return failed();
        }
        return parseDeepSeek(result.body());
    }

    private Snapshot readKling(String baseUrl, String apiKey, String secretKey) throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        String token = KlingJwtSupport.resolveBearerToken(apiKey, secretKey, root);
        Instant now = Instant.now();
        long start = now.minus(Duration.ofDays(800)).toEpochMilli();
        long end = now.plus(Duration.ofDays(400)).toEpochMilli();
        String url = root + "/account/costs?start_time=" + start + "&end_time=" + end;
        Result result = transport.get(url, Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json"));
        if (result.status() < 200 || result.status() >= 300) {
            log.warn("外部密钥余额查询失败 provider=KLING host={} http={}", hostOf(baseUrl), result.status());
            return failed();
        }
        return parseKling(result.body());
    }

    private Snapshot readMiniMax(String baseUrl, String apiKey) throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + apiKey,
                "Accept", "application/json");
        Result plan = transport.get(root + "/v1/token_plan/remains", headers);
        if (plan.status() >= 200 && plan.status() < 300) {
            Snapshot quota = parseMiniMaxTokenPlan(plan.body());
            if (quota != null) {
                return quota;
            }
        }
        Result payg = transport.get(root + "/account/query_balance", headers);
        if (payg.status() < 200 || payg.status() >= 300) {
            log.warn("外部密钥余额查询失败 provider=MINIMAX host={} http={}", hostOf(baseUrl), payg.status());
            return failed();
        }
        Snapshot money = parseMiniMaxPayg(payg.body(), moneyUnit(baseUrl));
        return money == null ? failed() : money;
    }

    private Snapshot readBgssaiTokenHub(String baseUrl, String apiKey) throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        Result result = transport.get(root + "/v1/account/balance", Map.of(
                "Authorization", "Bearer " + apiKey,
                "Accept", "application/json"));
        if (result.status() < 200 || result.status() >= 300) {
            log.warn("外部密钥余额查询失败 provider=BGSSAI_TOKENHUB host={} http={}", hostOf(baseUrl), result.status());
            return failed();
        }
        return parseBgssaiTokenHub(result.body());
    }

    static Snapshot parseBgssaiTokenHub(String body) {
        Map<String, Object> parsed = ShortJson.map(body);
        Long tokens = longValue(parsed.get("balance_tokens"));
        if (tokens == null) {
            tokens = longValue(parsed.get("quota_remaining_tokens"));
        }
        Long fen = longValue(parsed.get("balance_fen"));
        if (fen == null) {
            fen = longValue(parsed.get("quota_remaining_fen"));
        }
        if (tokens == null && fen == null) {
            return failed();
        }
        List<ExternalApiKeyBalancePart> parts = new ArrayList<>();
        if (tokens != null) {
            parts.add(part("额度", String.valueOf(tokens), "Token"));
        }
        if (fen != null && (fen > 0 || parts.isEmpty())) {
            parts.add(part("钱包", new BigDecimal(fen).movePointLeft(2).stripTrailingZeros().toPlainString(), "元"));
        }
        return ok(parts);
    }

    private static Long longValue(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    static Snapshot parsePixVerse(String body) {
        Map<String, Object> parsed = ShortJson.map(body);
        Object errCode = parsed.get("ErrCode");
        if (errCode != null && ShortJson.integer(errCode, -1) != 0) {
            return failed();
        }
        Object resp = parsed.get("Resp");
        if (!(resp instanceof Map<?, ?> map)) {
            return failed();
        }
        String monthly = amountText(map.get("credit_monthly"));
        String pack = amountText(map.get("credit_package"));
        if (monthly == null && pack == null) {
            return failed();
        }
        List<ExternalApiKeyBalancePart> parts = new ArrayList<>();
        if (monthly != null) {
            parts.add(part("月度", monthly, "积分"));
        }
        if (pack != null) {
            parts.add(part("资源包", pack, "积分"));
        }
        return ok(parts);
    }

    static Snapshot parseDeepSeek(String body) {
        Map<String, Object> parsed = ShortJson.map(body);
        Object raw = parsed.get("balance_infos");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return failed();
        }
        List<ExternalApiKeyBalancePart> parts = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String amount = amountText(map.get("total_balance"));
            if (amount == null) {
                continue;
            }
            parts.add(part("", amount, currencyUnit(ShortJson.text(map.get("currency")))));
        }
        return parts.isEmpty() ? failed() : ok(parts);
    }

    static Snapshot parseKling(String body) {
        Map<String, Object> parsed = ShortJson.map(body);
        Object code = parsed.get("code");
        if (code != null && ShortJson.integer(code, -1) != 0) {
            return failed();
        }
        Object data = parsed.get("data");
        Object raw = data instanceof Map<?, ?> dataMap
                ? dataMap.get("resource_pack_subscribe_infos")
                : parsed.get("resource_pack_subscribe_infos");
        if (!(raw instanceof List<?> list)) {
            return failed();
        }
        List<ExternalApiKeyBalancePart> parts = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String status = ShortJson.text(map.get("status")).trim().toLowerCase(Locale.ROOT);
            if ("expired".equals(status)) {
                continue;
            }
            String amount = amountText(map.get("remaining_quantity"));
            if (amount == null) {
                continue;
            }
            String name = ShortJson.text(map.get("resource_pack_name")).trim();
            if (name.length() > 32) {
                name = name.substring(0, 32);
            }
            parts.add(part(name.isEmpty() ? "资源包" : name, amount, "次"));
        }
        if (parts.isEmpty()) {
            parts.add(part("资源包", "0", "次"));
        }
        return ok(parts);
    }

    static Snapshot parseMiniMaxTokenPlan(String body) {
        Map<String, Object> parsed = ShortJson.map(body);
        if (!miniMaxOk(parsed)) {
            return null;
        }
        Object raw = parsed.get("model_remains");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        List<ExternalApiKeyBalancePart> parts = new ArrayList<>();
        boolean many = list.size() > 1;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String model = ShortJson.text(map.get("model_name")).trim();
            String prefix = many && !model.isEmpty() ? model + " " : "";
            String interval = amountText(map.get("current_interval_remaining_percent"));
            String weekly = amountText(map.get("current_weekly_remaining_percent"));
            if (interval != null) {
                parts.add(part(prefix + "5小时", interval, "%"));
            }
            if (weekly != null) {
                parts.add(part(prefix + "周", weekly, "%"));
            }
        }
        return parts.isEmpty() ? null : ok(parts);
    }

    static Snapshot parseMiniMaxPayg(String body, String unit) {
        Map<String, Object> parsed = ShortJson.map(body);
        if (!miniMaxOk(parsed)) {
            return null;
        }
        String amount = amountText(parsed.get("available_amount"));
        if (amount == null && parsed.get("data") instanceof Map<?, ?> data) {
            amount = amountText(data.get("available_amount"));
        }
        if (amount == null) {
            return null;
        }
        return ok(List.of(part("", amount, unit == null ? "" : unit)));
    }

    private static boolean miniMaxOk(Map<String, Object> parsed) {
        Object raw = parsed.get("base_resp");
        if (!(raw instanceof Map<?, ?> base)) {
            return true;
        }
        Object code = base.get("status_code");
        return code == null || ShortJson.integer(code, -1) == 0;
    }

    static String moneyUnit(String baseUrl) {
        String host = hostOf(baseUrl);
        if (host.endsWith("minimaxi.com") || host.endsWith("minimax.cn")) {
            return "元";
        }
        if (host.endsWith("minimax.io")) {
            return "USD";
        }
        return "";
    }

    static String currencyUnit(String currency) {
        String value = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if ("CNY".equals(value) || "RMB".equals(value)) {
            return "元";
        }
        if ("USD".equals(value)) {
            return "USD";
        }
        return value;
    }

    private static String requireHttpsBase(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("base url missing");
        }
        URI uri = URI.create(baseUrl.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getHost() == null) {
            throw new IllegalArgumentException("base url rejected");
        }
        String value = baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String hostOf(String baseUrl) {
        try {
            String host = URI.create(baseUrl).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            return "";
        }
    }

    static String amountText(Object raw) {
        if (raw == null) {
            return null;
        }
        String text;
        if (raw instanceof String value) {
            text = value.trim();
        } else if (raw instanceof Number number) {
            text = new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        } else {
            text = String.valueOf(raw).trim();
        }
        return text.isEmpty() ? null : text;
    }

    private static ExternalApiKeyBalancePart part(String label, String amount, String unit) {
        ExternalApiKeyBalancePart part = new ExternalApiKeyBalancePart();
        part.setLabel(label == null ? "" : label);
        part.setAmount(amount);
        part.setUnit(unit == null ? "" : unit);
        return part;
    }

    private static Snapshot ok(List<ExternalApiKeyBalancePart> parts) {
        return new Snapshot(STATUS_OK, parts);
    }

    private static Snapshot failed() {
        return new Snapshot(STATUS_FAILED, List.of());
    }

    private static Snapshot unavailable() {
        return new Snapshot(STATUS_UNAVAILABLE, List.of());
    }

    private static String fingerprint(UserAiModelKey row) {
        return text(row.getProviderCode()) + "\0"
                + text(row.getBaseUrl()) + "\0"
                + text(row.getApiKey()) + "\0"
                + text(row.getSecretKey());
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static Transport jdkTransport() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return (url, headers) -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET();
            if (headers != null) {
                headers.forEach((name, value) -> {
                    if (name != null && value != null) {
                        builder.header(name, value);
                    }
                });
            }
            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body() == null ? "" : response.body();
            return new Result(response.statusCode(), responseBody);
        };
    }

    record Credential(String providerCode, String baseUrl, String apiKey, String secretKey) {
        static Credential from(UserAiModelKey row) {
            String provider = row.getProviderCode() == null
                    ? ""
                    : row.getProviderCode().trim().toUpperCase(Locale.ROOT);
            return new Credential(provider, row.getBaseUrl(), row.getApiKey(), row.getSecretKey());
        }
    }
}
