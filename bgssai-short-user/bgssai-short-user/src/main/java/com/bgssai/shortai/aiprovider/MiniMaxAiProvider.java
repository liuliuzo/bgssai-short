package com.bgssai.shortai.aiprovider;

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
 * provider_code=MINIMAX — 海螺视频 / image-01 文生图。
 * 国内默认 https://api.minimaxi.com
 */
@Component
public class MiniMaxAiProvider implements AiProvider {

    public static final String CODE = "MINIMAX";

    static final String DEFAULT_BASE_URL = "https://api.minimaxi.com";

    private static final Logger log = LoggerFactory.getLogger(MiniMaxAiProvider.class);

    private final SysSecretReader secretReader;
    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;

    @Autowired
    public MiniMaxAiProvider(SysSecretReader secretReader) {
        this(secretReader, new ProviderHttp.JdkClient(), ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        });
    }

    MiniMaxAiProvider(SysSecretReader secretReader, ProviderHttp.Client http, ProviderHttp.Sleeper sleeper) {
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
        Map<String, Object> cfg = require(request);
        String apiKey = apiKey(cfg);
        String base = ProviderHttp.trimSlash(ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("base_url")), DEFAULT_BASE_URL));
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);
        String ability = request.getAbilityType();
        try {
            if ("IMAGE".equals(ability)) {
                return image(base, apiKey, cfg, request, timeoutSec);
            }
            if ("VIDEO".equals(ability)) {
                return video(base, apiKey, cfg, request, timeoutSec);
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "MINIMAX unsupported ability: " + ability);
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "minimax interrupted");
        } catch (Exception ex) {
            log.warn("MINIMAX call failed ability={}", ability, ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, safe(ex));
        }
    }

    private AiProviderResult image(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("aspect_ratio", ProviderHttp.aspectRatio(cfg, request));
        body.put("n", 1);
        body.put("response_format", "url");
        String raw = http.postJson(base + "/v1/image_generation", bearer(apiKey), ShortJson.toJson(body),
                Math.min(60, timeoutSec));
        Map<String, Object> resp = ShortJson.map(raw);
        assertBaseOk(resp);
        String url = firstImageUrl(resp);
        byte[] bytes = http.getBytes(url, Math.min(120, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "image/png");
        result.setWidth(1080);
        result.setHeight(1920);
        return result;
    }

    private AiProviderResult video(String base, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel().getModelCode());
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("duration", durationSec(cfg, request));
        body.put("resolution", ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("resolution")), "1080P"));
        String image = ProviderHttp.imagePayload(request);
        if (!image.isBlank()) {
            body.put("first_frame_image", image);
            String lastImage = ProviderHttp.lastImagePayload(request);
            if (!lastImage.isBlank()) {
                body.put("last_frame_image", lastImage);
            }
        }
        String raw = http.postJson(base + "/v1/video_generation", bearer(apiKey), ShortJson.toJson(body),
                Math.min(60, timeoutSec));
        Map<String, Object> submitted = ShortJson.map(raw);
        assertBaseOk(submitted);
        String taskId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("task_id")));
        if (taskId.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "minimax submit missing task_id");
        }
        Map<String, Object> done = pollVideo(base, apiKey, taskId, timeoutSec, cfg);
        String fileId = ShortJson.text(done.get("file_id"));
        String download = ShortJson.text(done.get("download_url"));
        if (download.isBlank() && !fileId.isBlank()) {
            String fileRaw = http.getJson(base + "/v1/files/retrieve?file_id=" + fileId, bearer(apiKey),
                    Math.min(60, timeoutSec));
            Map<String, Object> fileResp = ShortJson.map(fileRaw);
            assertBaseOk(fileResp);
            Object file = fileResp.get("file");
            if (file instanceof Map<?, ?> fm) {
                download = ShortJson.text(fm.get("download_url"));
            }
        }
        if (download.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "minimax video url missing");
        }
        byte[] bytes = http.getBytes(download, Math.min(180, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        result.setWidth(1080);
        result.setHeight(1920);
        result.setDurationSec(ShortJson.integer(done.get("video_duration"), durationSec(cfg, request)));
        return result;
    }

    private Map<String, Object> pollVideo(String base, String apiKey, String taskId, int timeoutSec,
            Map<String, Object> cfg) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        long interval = ProviderHttp.pollIntervalMs(cfg);
        String query = base + "/v1/query/video_generation?task_id=" + taskId;
        Map<String, Object> last = new LinkedHashMap<>();
        while (System.currentTimeMillis() < deadline) {
            String raw = http.getJson(query, bearer(apiKey), Math.min(60, timeoutSec));
            last = ShortJson.map(raw);
            assertBaseOk(last);
            String status = ShortJson.text(last.get("status")).toLowerCase();
            if ("success".equals(status) || "succeed".equals(status)) {
                return last;
            }
            if ("fail".equals(status) || "failed".equals(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        ProviderHttp.firstNonBlank(statusMsg(last), "minimax video failed"));
            }
            sleeper.sleep(interval);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "minimax task timeout task_id=" + taskId);
    }

    @SuppressWarnings("unchecked")
    private static String firstImageUrl(Map<String, Object> resp) {
        Object data = resp.get("data");
        if (data instanceof Map<?, ?> map) {
            Object urls = map.get("image_urls");
            if (urls instanceof List<?> list && !list.isEmpty()) {
                return String.valueOf(list.get(0));
            }
            String one = ShortJson.text(map.get("image_url"));
            if (!one.isBlank()) {
                return one;
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "minimax image url missing");
    }

    private static String statusMsg(Map<String, Object> last) {
        Object base = last.get("base_resp");
        if (base instanceof Map<?, ?> map) {
            String msg = ShortJson.text(map.get("status_msg"));
            if (!msg.isBlank()) {
                return msg;
            }
        }
        return ShortJson.text(last.get("status_msg"));
    }

    private static void assertBaseOk(Map<String, Object> resp) {
        Object base = resp.get("base_resp");
        if (base instanceof Map<?, ?> map) {
            Object code = map.get("status_code");
            if (code != null && !"0".equals(String.valueOf(code))) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        ProviderHttp.firstNonBlank(ShortJson.text(map.get("status_msg")), "minimax failed"));
            }
        }
    }

    private static int durationSec(Map<String, Object> cfg, AiProviderRequest request) {
        int sec = ShortJson.integer(request.param("duration_sec"), 0);
        if (sec <= 0) {
            sec = ShortJson.integer(cfg.get("duration"), 6);
        }
        return sec <= 6 ? 6 : 10;
    }

    private Map<String, Object> require(AiProviderRequest request) {
        if (request == null || request.getModel() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model required");
        }
        return ShortJson.map(request.getModel().getConfigJson());
    }

    private String apiKey(Map<String, Object> cfg) {
        String ref = ShortJson.text(cfg.get("secret_ref"));
        if (ref.isBlank()) {
            ref = SysSecretReader.AI_MINIMAX_API_KEY;
        }
        String key = secretReader.get(ref);
        if (key.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "AI api key missing for " + ref);
        }
        return key;
    }

    private static Map<String, String> bearer(String apiKey) {
        return Map.of("Authorization", "Bearer " + apiKey);
    }

    private static String safe(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "minimax call failed";
        }
        return msg.length() > 240 ? msg.substring(0, 240) : msg;
    }
}
