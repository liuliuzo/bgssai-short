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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.production.support.ShortJson;

/**
 * Reads the model ids TokenHub can run, so the user picker matches the gateway.
 */
@Component
public class TokenHubModelCatalog {

    public record HubModel(String id, List<String> capabilities) {
    }

    private static final long SUCCESS_TTL_MS = 10 * 60 * 1000L;
    private static final long FAILURE_TTL_MS = 60 * 1000L;

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private volatile List<HubModel> cached = List.of();
    private volatile long freshUntil;

    public TokenHubModelCatalog(
            @Value("${bgssai.llm.tokenhub-cn-base-url:${bgssai.llm.gateway-base-url:https://www.bgssai-tokenhub.cn}}")
            String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
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
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/models"))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();
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

    private static List<HubModel> parse(String body) {
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
            models.add(new HubModel(String.valueOf(id).trim(), List.copyOf(capabilities)));
        }
        return List.copyOf(models);
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
            default -> null;
        };
    }
}
