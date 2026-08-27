package com.bgssai.shortai.aiprovider;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * provider_code=VEO — Google Gemini API 的 Veo 视频（predictLongRunning）。
 */
@Component
public class VeoAiProvider implements AiProvider {

    public static final String CODE = "VEO";

    static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private static final Logger log = LoggerFactory.getLogger(VeoAiProvider.class);

    private final SysSecretReader secretReader;
    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;

    @Autowired
    public VeoAiProvider(SysSecretReader secretReader) {
        this(secretReader, new ProviderHttp.JdkClient(), ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        });
    }

    VeoAiProvider(SysSecretReader secretReader, ProviderHttp.Client http, ProviderHttp.Sleeper sleeper) {
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
            throw new BizException(CommonErrorCode.RULE_REJECT, "VEO unsupported ability: " + request.getAbilityType());
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String ref = ShortJson.text(cfg.get("secret_ref"));
        if (ref.isBlank()) {
            ref = SysSecretReader.AI_VEO_API_KEY;
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
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "veo interrupted");
        } catch (Exception ex) {
            log.warn("VEO call failed", ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, safe(ex));
        }
    }

    private AiProviderResult video(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("prompt", ProviderHttp.promptOf(request));
        String image = rawBase64(request);
        if (!image.isBlank()) {
            Map<String, Object> img = new LinkedHashMap<>();
            img.put("bytesBase64Encoded", image);
            img.put("mimeType", ProviderHttp.firstNonBlank(
                    ShortJson.text(request.param("image_mime")), "image/png"));
            instance.put("image", img);
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("aspectRatio", ProviderHttp.aspectRatio(cfg, request));
        parameters.put("durationSeconds", durationSec(cfg, request));
        parameters.put("personGeneration", "allow_adult");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("instances", List.of(instance));
        body.put("parameters", parameters);
        String model = request.getModel().getModelCode();
        String submitUrl = base + "/models/" + model + ":predictLongRunning";
        Map<String, String> headers = Map.of("x-goog-api-key", apiKey);
        Map<String, Object> submitted = ShortJson.map(
                http.postJson(submitUrl, headers, ShortJson.toJson(body), Math.min(60, timeoutSec)));
        String name = ShortJson.text(submitted.get("name"));
        if (name.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "veo submit missing operation name");
        }
        Map<String, Object> done = poll(base, apiKey, name, timeoutSec, cfg);
        return download(done, timeoutSec);
    }

    private Map<String, Object> poll(String base, String apiKey, String name, int timeoutSec,
            Map<String, Object> cfg) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        long interval = ProviderHttp.pollIntervalMs(cfg);
        String query = name.startsWith("http") ? name : base + "/" + name.replaceFirst("^/+", "");
        Map<String, String> headers = Map.of("x-goog-api-key", apiKey);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> last = ShortJson.map(http.getJson(query, headers, Math.min(60, timeoutSec)));
            Object done = last.get("done");
            if (Boolean.TRUE.equals(done) || "true".equalsIgnoreCase(String.valueOf(done))) {
                if (last.get("error") instanceof Map<?, ?> err) {
                    throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                            ProviderHttp.firstNonBlank(ShortJson.text(err.get("message")), "veo failed"));
                }
                return last;
            }
            sleeper.sleep(interval);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "veo operation timeout");
    }

    @SuppressWarnings("unchecked")
    private AiProviderResult download(Map<String, Object> done, int timeoutSec) throws Exception {
        Object response = done.get("response");
        if (!(response instanceof Map<?, ?> resp)) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "veo response missing");
        }
        Object generate = resp.get("generateVideoResponse");
        if (!(generate instanceof Map<?, ?> gen)) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "veo generateVideoResponse missing");
        }
        Object samples = gen.get("generatedSamples");
        if (!(samples instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> first)) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "veo samples missing");
        }
        Object video = first.get("video");
        if (!(video instanceof Map<?, ?> vmap)) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "veo video missing");
        }
        String b64 = ShortJson.text(vmap.get("bytesBase64Encoded"));
        byte[] bytes;
        if (!b64.isBlank()) {
            bytes = Base64.getDecoder().decode(b64);
        } else {
            String uri = ShortJson.text(vmap.get("uri"));
            if (uri.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "veo video uri missing");
            }
            bytes = http.getBytes(uri, Math.min(180, timeoutSec));
        }
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        result.setWidth(1080);
        result.setHeight(1920);
        result.setDurationSec(8);
        return result;
    }

    private static String rawBase64(AiProviderRequest request) {
        String b64 = ShortJson.text(request.param("image_base64"));
        if (b64.startsWith("data:") && b64.contains(",")) {
            return b64.substring(b64.indexOf(',') + 1);
        }
        return b64;
    }

    private static int durationSec(Map<String, Object> cfg, AiProviderRequest request) {
        int sec = ShortJson.integer(request.param("duration_sec"), 0);
        if (sec <= 0) {
            sec = ShortJson.integer(cfg.get("duration"), 8);
        }
        if (sec <= 4) {
            return 4;
        }
        if (sec <= 6) {
            return 6;
        }
        return 8;
    }

    private static String safe(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "veo call failed";
        }
        return msg.length() > 240 ? msg.substring(0, 240) : msg;
    }
}
