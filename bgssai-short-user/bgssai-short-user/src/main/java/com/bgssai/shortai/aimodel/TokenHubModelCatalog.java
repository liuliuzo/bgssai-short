package com.bgssai.shortai.aimodel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * Reads gateway model ids for server-side alignment. User pickers receive product aliases, not these ids.
 */
@Component
public class TokenHubModelCatalog {

    public record HubModel(String id, List<String> capabilities, Map<String, Object> pricing, String ownedBy) {
        public HubModel(String id, List<String> capabilities) {
            this(id, capabilities, com.bgssai.shortai.billing.ModelPricing.from(Map.of()), "");
        }

        public HubModel(String id, List<String> capabilities, Map<String, Object> pricing) {
            this(id, capabilities, pricing, "");
        }

        public HubModel {
            if (capabilities == null) {
                capabilities = List.of();
            }
            if (ownedBy == null) {
                ownedBy = "";
            }
        }
    }

    private static final long SUCCESS_TTL_MS = 10 * 60 * 1000L;
    private static final long FAILURE_TTL_MS = 60 * 1000L;

    private final java.util.concurrent.ConcurrentMap<String,PriceCache> prices = new java.util.concurrent.ConcurrentHashMap<>();
    private record PriceCache(long until,java.util.Map<String,java.util.Map<String,Object>> rows) {}
    public Map<String,Object> pricingFor(String endpoint,String model) {
        String base=endpoint==null?"":endpoint.replaceAll("/+$","");
        if(base.isBlank())return com.bgssai.shortai.billing.ModelPricing.from(Map.of());
        PriceCache cache=prices.get(base);
        if(cache==null || cache.until()<System.currentTimeMillis()) {
            try {
                var request=HttpRequest.newBuilder(URI.create(base+"/v1/models")).timeout(Duration.ofSeconds(3)).GET().build();
                var response=http.send(request,HttpResponse.BodyHandlers.ofString());
                Map<String,Map<String,Object>> rows=new java.util.LinkedHashMap<>();
                if(response.statusCode()==200) for(var row:parse(response.body())) rows.put(row.id(),row.pricing());
                cache=new PriceCache(System.currentTimeMillis()+(response.statusCode()==200?SUCCESS_TTL_MS:FAILURE_TTL_MS),rows);
            } catch(InterruptedException ex){Thread.currentThread().interrupt();cache=new PriceCache(System.currentTimeMillis()+FAILURE_TTL_MS,Map.of());}
            catch(Exception ex){cache=new PriceCache(System.currentTimeMillis()+FAILURE_TTL_MS,Map.of());}
            prices.put(base,cache);
        }
        return cache.rows().getOrDefault(model,com.bgssai.shortai.billing.ModelPricing.from(Map.of()));
    }

    public String endpointFor(java.util.Map<String,Object> config,String model) {
        // The environment profile is the gateway. A host stored on the model row is ignored
        // once that profile is an official hub, so DEV does not follow a production URL.
        if(com.bgssai.shortai.aiprovider.TokenHubClientSupport.isOfficialHub(baseUrl))return baseUrl;
        String configured=ShortJson.text(config==null?null:config.get("base_url")).replaceAll("/+$","");
        if(com.bgssai.shortai.aiprovider.TokenHubClientSupport.isOfficialHub(configured))return configured;
        return baseUrl==null||baseUrl.isBlank()?configured:baseUrl;
    }

    private final String baseUrl;
    private final SysSecretReader secrets;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private volatile List<HubModel> cached = List.of();
    private volatile long freshUntil;

    public TokenHubModelCatalog(String baseUrl) {
        this(baseUrl, null);
    }

    @Autowired
    public TokenHubModelCatalog(
            @Value("${bgssai.llm.tokenhub-cn-base-url:${bgssai.llm.gateway-base-url:https://www.bgssai-tokenhub.cn}}")
            String baseUrl,
            SysSecretReader secrets) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
        this.secrets = secrets;
    }

    /** Models from the last successful catalog read. Empty when the gateway has not answered. */
    public List<HubModel> snapshot() {
        refresh();
        return cached;
    }

    /** Keeps a catalog another caller just read, so the next default pick uses the same list. */
    public void remember(List<HubModel> models) {
        if (models == null || models.isEmpty()) {
            return;
        }
        synchronized (this) {
            cached = List.copyOf(models);
            freshUntil = System.currentTimeMillis() + SUCCESS_TTL_MS;
        }
    }

    public List<HubModel> modelsFor(String abilityType) {
        String capability = capabilityOf(abilityType);
        if (capability == null) {
            return List.of();
        }
        refresh();
        List<HubModel> matched = new ArrayList<>();
        for (HubModel model : cached) {
            if (model.capabilities().contains(capability)) {
                matched.add(model);
            }
        }
        return matched;
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now < freshUntil) {
            return;
        }
        synchronized (this) {
            if (System.currentTimeMillis() < freshUntil) {
                return;
            }
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/models"))
                        .timeout(Duration.ofSeconds(3));
                String apiKey = apiKey();
                if (!apiKey.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + apiKey);
                }
                HttpRequest request = requestBuilder.GET().build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    freshUntil = System.currentTimeMillis() + FAILURE_TTL_MS;
                    return;
                }
                cached = parse(response.body());
                freshUntil = System.currentTimeMillis() + SUCCESS_TTL_MS;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                freshUntil = System.currentTimeMillis() + FAILURE_TTL_MS;
            } catch (Exception ex) {
                freshUntil = System.currentTimeMillis() + FAILURE_TTL_MS;
            }
        }
    }

    static List<HubModel> parse(String body) {
        Map<String, Object> root = ShortJson.map(body);
        Object data = root.get("data");
        if (!(data instanceof List<?> rows)) {
            return List.of();
        }
        List<HubModel> models = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            Object id = map.get("id");
            if (id == null || String.valueOf(id).isBlank()) {
                continue;
            }
            List<String> capabilities = new ArrayList<>();
            Object rawCaps = map.get("capabilities");
            if (rawCaps instanceof List<?> caps) {
                for (Object cap : caps) {
                    if (cap != null && !String.valueOf(cap).isBlank()) {
                        capabilities.add(String.valueOf(cap).trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
            Object owned = map.get("owned_by");
            String ownedBy = owned == null ? "" : String.valueOf(owned).trim();
            models.add(new HubModel(String.valueOf(id).trim(), List.copyOf(capabilities),
                    com.bgssai.shortai.billing.ModelPricing.from(map), ownedBy));
        }
        return List.copyOf(models);
    }

    private String apiKey() {
        if (secrets == null) {
            return "";
        }
        return TokenHubClientSupport.resolveApiKey(secrets, TokenHubClientSupport.HUB_CN, null);
    }

    private static String capabilityOf(String abilityType) {
        if (abilityType == null || abilityType.isBlank()) {
            return null;
        }
        return switch (abilityType.trim().toUpperCase(Locale.ROOT)) {
            case "TEXT" -> "chat";
            case "IMAGE" -> "image_generation";
            case "VIDEO" -> "video_generation";
            case "VOICE" -> "audio_speech";
            case "MUSIC" -> "audio_music";
            default -> null;
        };
    }
}
