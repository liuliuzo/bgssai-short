package com.bgssai.shortai.aiprovider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * provider_code=SEEDANCE — 火山方舟 Seedance 视频（文生 / 图生）。
 */
@Component
public class SeedanceAiProvider implements AiProvider {

    public static final String CODE = "SEEDANCE";

    static final String DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    private static final Logger log = LoggerFactory.getLogger(SeedanceAiProvider.class);

    private final SysSecretReader secretReader;
    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;

    public SeedanceAiProvider(SysSecretReader secretReader) {
        this(secretReader, new ProviderHttp.JdkClient(), ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        });
    }

    SeedanceAiProvider(SysSecretReader secretReader, ProviderHttp.Client http, ProviderHttp.Sleeper sleeper) {
        this.secretReader = secretReader;
        this.http = http;
        this.sleeper = sleeper;
    }

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (request == null || request.getModel() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model required");
        }
        if (!"VIDEO".equals(request.getAbilityType())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "SEEDANCE unsupported ability: " + request.getAbilityType());
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String ref = ShortJson.text(cfg.get("secret_ref"));
        if (ref.isBlank()) {
            ref = SysSecretReader.AI_SEEDANCE_API_KEY;
        }
        String apiKey = secretReader.get(ref);
        if (apiKey.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "AI api key missing for " + ref);
        }
        String base = ProviderHttp.trimSlash(ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("base_url")), DEFAULT_BASE_URL));
        int timeoutSec = ProviderHttp.timeoutSec(request, 600);
        try {
            return video(base, apiKey, cfg, request, timeoutSec);
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "seedance interrupted");
        } catch (Exception ex) {
            log.warn("SEEDANCE call failed", ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, safe(ex));
        }
    }

    private AiProviderResult video(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("type", "text");
        text.put("text", ProviderHttp.promptOf(request));
        content.add(text);
        String image = ProviderHttp.imagePayload(request);
        if (!image.isBlank()) {
            Map<String, Object> img = new LinkedHashMap<>();
            img.put("type", "image_url");
            img.put("image_url", Map.of("url", image));
            img.put("role", "first_frame");
            content.add(img);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("content", content);
        body.put("ratio", ProviderHttp.aspectRatio(cfg, request));
        body.put("duration", durationSec(cfg, request));
        body.put("watermark", false);
        String raw = http.postJson(base + "/contents/generations/tasks", bearer(apiKey), ShortJson.toJson(body),
                Math.min(60, timeoutSec));
        Map<String, Object> submitted = ShortJson.map(raw);
        String taskId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("id")));
        if (taskId.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "seedance submit missing id");
        }
        Map<String, Object> done = poll(base, apiKey, taskId, timeoutSec, cfg);
        String url = videoUrl(done);
        byte[] bytes = http.getBytes(url, Math.min(180, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        result.setWidth(1080);
        result.setHeight(1920);
        result.setDurationSec(ShortJson.integer(done.get("duration"), durationSec(cfg, request)));
        return result;
    }

    private Map<String, Object> poll(String base, String apiKey, String taskId, int timeoutSec,
            Map<String, Object> cfg) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        long interval = ProviderHttp.pollIntervalMs(cfg);
        String query = base + "/contents/generations/tasks/" + taskId;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> last = ShortJson.map(http.getJson(query, bearer(apiKey), Math.min(60, timeoutSec)));
            String status = ShortJson.text(last.get("status")).toLowerCase();
            if ("succeeded".equals(status) || "success".equals(status)) {
                return last;
            }
            if ("failed".equals(status) || "cancelled".equals(status) || "canceled".equals(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        ProviderHttp.firstNonBlank(errorOf(last), "seedance task failed"));
            }
            sleeper.sleep(interval);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "seedance task timeout id=" + taskId);
    }

    @SuppressWarnings("unchecked")
    private static String videoUrl(Map<String, Object> done) {
        Object content = done.get("content");
        if (content instanceof Map<?, ?> map) {
            String url = ShortJson.text(map.get("video_url"));
            if (!url.isBlank()) {
                return url;
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "seedance video url missing");
    }

    @SuppressWarnings("unchecked")
    private static String errorOf(Map<String, Object> last) {
        Object err = last.get("error");
        if (err instanceof Map<?, ?> map) {
            return ShortJson.text(map.get("message"));
        }
        return ShortJson.text(last.get("message"));
    }

    private static int durationSec(Map<String, Object> cfg, AiProviderRequest request) {
        int sec = ShortJson.integer(request.param("duration_sec"), 0);
        if (sec <= 0) {
            sec = ShortJson.integer(cfg.get("duration"), 5);
        }
        if (sec < 4) {
            return 5;
        }
        return Math.min(sec, 10);
    }

    private static Map<String, String> bearer(String apiKey) {
        return Map.of("Authorization", "Bearer " + apiKey);
    }

    private static String safe(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "seedance call failed";
        }
        return msg.length() > 240 ? msg.substring(0, 240) : msg;
    }
}
