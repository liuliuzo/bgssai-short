package com.bgssai.shortai.aiprovider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;

/**
 * provider_code=PIXVERSE (PixVerse OpenAPI v2) for user-configured API keys.
 * Supports VIDEO generation (text-to-video, image-to-video, and first/last frame transition).
 */
@Component
public class PixVerseAiProvider implements AiProvider {

    public static final String CODE = "PIXVERSE";

    private static final Logger log = LoggerFactory.getLogger(PixVerseAiProvider.class);
    private static final long VIDEO_POLL_INTERVAL_MS = 4_000L;
    private static final long VIDEO_POLL_FLOOR_MS = 900_000L;

    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;

    @Autowired(required = false)
    private ShortAiTaskMapper shortAiTaskMapper;

    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;

    public PixVerseAiProvider() {
        this.http = new ProviderHttp.JdkClient();
        this.sleeper = ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        };
    }

    PixVerseAiProvider(UserAiModelKeyService userAiModelKeyService, ShortAiTaskMapper shortAiTaskMapper,
            ProviderHttp.Client http, ProviderHttp.Sleeper sleeper) {
        this.userAiModelKeyService = userAiModelKeyService;
        this.shortAiTaskMapper = shortAiTaskMapper;
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
        if (!UserAiModelSupport.isUserKeyModel(request.getModel())) {
            throw DirectVendorGuard.rejected(CODE);
        }
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        if (!"VIDEO".equals(ability)) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    "PixVerse 仅支持视频模型，请为当前能力配置其他供应商密钥或切换混合模式");
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String userId = ShortJson.text(cfg.get("user_id"));
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                request.getModel().getModelId());
        String baseUrl = UserAiModelSupport.normalizeBaseUrl(ShortJson.text(cfg.get("base_url")), CODE);
        String apiRoot = UserAiModelSupport.stripTrailingPath(baseUrl, "/openapi/v2");
        String apiKey = userAiModelKeyService.requirePlainApiKey(userId, keyId);
        String modelCode = ProviderHttp.firstNonBlank(request.getModel().getModelCode(), "v6");
        int timeoutSec = ProviderHttp.timeoutSec(request, 900);

        log.info("调用 PixVerse 视频模型 userId={} keyId={} model={}", userId, keyId, modelCode);
        try {
            return generateVideo(apiRoot, apiKey, modelCode, cfg, request, timeoutSec);
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "PixVerse 视频生成被中断，请稍后重试");
        } catch (Exception ex) {
            log.warn("PixVerse 视频调用异常 userId={} keyId={} model={} errType={}",
                    userId, keyId, modelCode, ex.getClass().getSimpleName());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "PixVerse 视频请求失败，请检查密钥与网络后重试");
        }
    }

    private AiProviderResult generateVideo(String apiRoot, String apiKey, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        int duration = durationForModel(modelCode, request, cfg);
        String aspect = ProviderHttp.firstNonBlank(ShortJson.text(request.param("aspect_ratio")),
                ShortJson.text(cfg.get("aspect_ratio")), "9:16");
        String quality = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("quality")), "720p");

        String videoId = UserProviderMediaSupport.findTaskParam(shortAiTaskMapper, "pixverse_video_id");
        if (videoId.isBlank()) {
            long firstImgId = uploadImageIfPresent(apiRoot, apiKey, request, false, timeoutSec);
            long lastImgId = firstImgId > 0 ? uploadImageIfPresent(apiRoot, apiKey, request, true, timeoutSec) : 0L;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelCode);
            body.put("prompt", ProviderHttp.promptOf(request));
            body.put("duration", duration);
            body.put("quality", quality);

            String endpoint;
            if (firstImgId > 0 && lastImgId > 0) {
                endpoint = apiRoot + "/openapi/v2/video/transition/generate";
                body.put("first_frame_img", firstImgId);
                body.put("last_frame_img", lastImgId);
            } else if (firstImgId > 0) {
                endpoint = apiRoot + "/openapi/v2/video/img/generate";
                body.put("img_id", firstImgId);
            } else {
                endpoint = apiRoot + "/openapi/v2/video/text/generate";
                body.put("aspect_ratio", aspect);
            }

            String raw = http.postJson(endpoint, headers(apiKey), ShortJson.toJson(body), 60);
            Map<String, Object> resp = ShortJson.map(raw);
            assertZero(resp, "PixVerse 视频提交失败");
            videoId = UserProviderMediaSupport.nestedText(resp, "Resp", "video_id");
            if (videoId.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "PixVerse 未返回 video_id");
            }
            UserProviderMediaSupport.saveTaskParams(shortAiTaskMapper, Map.of("pixverse_video_id", videoId));
        }

        long intervalMs = Math.max(100L, ShortJson.integer(cfg.get("poll_interval_ms"), (int) VIDEO_POLL_INTERVAL_MS));
        long deadline = System.currentTimeMillis() + Math.max(VIDEO_POLL_FLOOR_MS, Math.max(60, timeoutSec) * 1000L);
        String queryUrl = apiRoot + "/openapi/v2/video/result/" + videoId;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> pollResp;
            try {
                pollResp = ShortJson.map(http.getJson(queryUrl, headers(apiKey), 60));
            } catch (BizException ex) {
                if (!UserProviderMediaSupport.isTransientFailure(ex) || System.currentTimeMillis() >= deadline) {
                    throw ex;
                }
                sleeper.sleep(intervalMs);
                continue;
            }
            assertZero(pollResp, "PixVerse 视频查询失败");
            int status = UserProviderMediaSupport.nestedInt(pollResp, "Resp", "status", 5);
            if (status == 1) {
                String url = UserProviderMediaSupport.nestedText(pollResp, "Resp", "url");
                if (url.isBlank()) {
                    throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "PixVerse 视频完成但缺少下载地址");
                }
                UserAiModelSupport.validatePublicHttpsUrl(url, "video_url");
                byte[] bytes = http.getBytes(url, Math.min(300, timeoutSec));
                AiProviderResult res = AiProviderResult.binary(bytes, "video/mp4");
                res.setDurationSec(duration);
                res.setUsageAmount(duration);
                return res;
            }
            if (status == 7) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "PixVerse 视频内容审核未通过，请调整提示词后重试");
            }
            if (status != 5) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "PixVerse 视频生成失败");
            }
            sleeper.sleep(intervalMs);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "PixVerse 视频生成超时，请稍后重试");
    }

    private long uploadImageIfPresent(String apiRoot, String apiKey, AiProviderRequest request, boolean lastFrame,
            int timeoutSec) throws Exception {
        byte[] bytes = UserProviderMediaSupport.resolveImageBytes(request, http, lastFrame, timeoutSec);
        if (bytes == null || bytes.length == 0) {
            return 0L;
        }
        String raw = http.postMultipartField(apiRoot + "/openapi/v2/image/upload", headers(apiKey),
                "image", bytes, "frame.png", "image/png", Map.of(), 60);
        Map<String, Object> resp = ShortJson.map(raw);
        assertZero(resp, "PixVerse 参考图上传失败");
        long imgId = Long.parseLong(ProviderHttp.firstNonBlank(
                UserProviderMediaSupport.nestedText(resp, "Resp", "img_id"), "0"));
        if (imgId <= 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "PixVerse 参考图上传未返回 img_id");
        }
        return imgId;
    }

    private static int durationForModel(String modelCode, AiProviderRequest request, Map<String, Object> cfg) {
        int sec = ShortJson.integer(request.param("duration_sec"), ShortJson.integer(cfg.get("duration"), 5));
        String lower = modelCode.toLowerCase(Locale.ROOT);
        if (lower.startsWith("v6")) {
            return Math.max(5, Math.min(10, sec <= 0 ? 5 : sec));
        }
        return sec > 5 ? 8 : 5;
    }

    private static Map<String, String> headers(String apiKey) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("API-KEY", apiKey);
        h.put("Ai-trace-id", UUID.randomUUID().toString());
        return h;
    }

    private static void assertZero(Map<String, Object> resp, String fallback) {
        Object code = resp.get("ErrCode");
        if (code != null && ShortJson.integer(code, -1) != 0) {
            String msg = ProviderHttp.firstNonBlank(ShortJson.text(resp.get("ErrMsg")), fallback);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, msg);
        }
    }
}
