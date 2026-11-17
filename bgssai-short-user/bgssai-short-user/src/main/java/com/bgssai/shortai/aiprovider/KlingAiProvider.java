package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;
import com.bgssai.shortai.useraimodel.service.KlingJwtSupport;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService.UserKeyCredentials;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

/**
 * provider_code=KLING — 可灵官方异步任务（文生图 / 图生图 / 文生视频 / 图生视频 / 语音合成）。
 * 平台模型仍走 TokenHub；用户自有密钥支持 AccessKey + SecretKey 即时签发 HS256 JWT 直连。
 */
@Component
public class KlingAiProvider implements AiProvider {

    public static final String CODE = "KLING";

    static final String DEFAULT_BASE_URL = "https://api-beijing.klingai.com";
    static final String DEFAULT_IMAGE_PATH = "/v1/images/generations";
    static final String DEFAULT_TEXT2VIDEO_PATH = "/v1/videos/text2video";
    static final String DEFAULT_IMAGE2VIDEO_PATH = "/v1/videos/image2video";
    static final String DEFAULT_TTS_PATH = "/v1/audio/tts";

    private static final Logger log = LoggerFactory.getLogger(KlingAiProvider.class);
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final long USER_VIDEO_POLL_INTERVAL_MS = 4_000L;
    private static final long USER_VIDEO_POLL_FLOOR_MS = 900_000L;

    private final SysSecretReader secretReader;
    private final Transport transport;
    private final Sleeper sleeper;
    private final ProviderHttp.Client userHttp;

    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;

    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;

    @Autowired(required = false)
    private ShortAiTaskMapper shortAiTaskMapper;

    void setUserAiProviderKeyService(UserAiProviderKeyService userAiProviderKeyService) {
        this.userAiProviderKeyService = userAiProviderKeyService;
    }

    private record KlingAuth(String apiKey, String accessKey, String secretKey, boolean providerKeyMode) {
        String resolveBearerToken(String apiRoot) {
            if (providerKeyMode) {
                return UserAiProviderCatalog.resolveKlingBearerToken(apiKey, accessKey, secretKey);
            }
            return KlingJwtSupport.resolveBearerToken(apiKey, secretKey, apiRoot);
        }
    }

    @Autowired
    public KlingAiProvider(SysSecretReader secretReader) {
        this(secretReader, new JdkTransport(), ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        });
    }

    KlingAiProvider(SysSecretReader secretReader, Transport transport, Sleeper sleeper) {
        this(secretReader, transport, sleeper, new ProviderHttp.JdkClient());
    }

    KlingAiProvider(SysSecretReader secretReader, Transport transport, Sleeper sleeper,
            ProviderHttp.Client userHttp) {
        this.secretReader = secretReader;
        this.transport = transport;
        this.sleeper = sleeper;
        this.userHttp = userHttp;
    }

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (request == null || request.getModel() == null) {
            throw DirectVendorGuard.rejected(CODE);
        }
        Optional<ResolvedProviderKey> providerByok = DirectVendorGuard.isExplicitUserModelKey(request.getModel())
                ? Optional.empty()
                : DirectVendorGuard.resolveProviderByok(request, userAiProviderKeyService);
        if (providerByok.isPresent()) {
            return executeProviderByok(request, providerByok.get());
        }
        if (!UserAiModelSupport.isUserKeyModel(request.getModel())) {
            throw DirectVendorGuard.rejected(CODE);
        }
        return executeUserKey(request);
    }

    private AiProviderResult executeProviderByok(AiProviderRequest request, ResolvedProviderKey key) {
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String effectiveUrl = UserAiProviderCatalog.resolveEffectiveBaseUrl(key.baseUrl(), CODE);
        String apiRoot = UserAiModelSupport.stripTrailingPath(effectiveUrl, "/v1");
        KlingAuth auth = new KlingAuth(key.apiKey(), key.accessKey(), key.secretKey(), true);
        return dispatchUserAbility(request, cfg, key.userId(),
                UserAiModelSupport.PROVIDER_KEY_PREFIX + "kling", apiRoot, auth);
    }

    private AiProviderResult executeUserKey(AiProviderRequest request) {
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String userId = ShortJson.text(cfg.get("user_id"));
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                request.getModel().getModelId());
        String baseUrl = UserAiModelSupport.normalizeBaseUrl(ShortJson.text(cfg.get("base_url")), CODE);
        String apiRoot = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        UserKeyCredentials rawCreds = userAiModelKeyService.requirePlainCredentials(userId, keyId);
        KlingAuth creds = new KlingAuth(rawCreds.apiKey(), null, rawCreds.secretKey(), false);
        return dispatchUserAbility(request, cfg, userId, keyId, apiRoot, creds);
    }

    private AiProviderResult dispatchUserAbility(AiProviderRequest request, Map<String, Object> cfg,
            String userId, String keyId, String apiRoot, KlingAuth creds) {
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        String modelCode = ProviderHttp.firstNonBlank(request.getModel().getModelCode(), "kling-v3");
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);

        log.info("调用可灵用户密钥模型 userId={} keyId={} ability={} model={}",
                userId, keyId, ability, modelCode);
        try {
            return switch (ability) {
                case "IMAGE" -> userImage(apiRoot, creds, modelCode, cfg, request, timeoutSec);
                case "VIDEO" -> userVideo(apiRoot, creds, modelCode, cfg, request, timeoutSec);
                case "VOICE" -> userVoice(apiRoot, creds, cfg, request, timeoutSec);
                case "TEXT", "ASR", "MUSIC" -> throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                        "可灵暂不支持该能力，请为当前能力配置其他供应商密钥或切换混合模式");
                default -> throw new BizException(CommonErrorCode.RULE_REJECT,
                        "KLING unsupported ability: " + ability);
            };
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "可灵模型调用被中断，请稍后重试");
        } catch (Exception ex) {
            log.warn("可灵用户密钥调用异常 userId={} keyId={} ability={} model={} errType={}",
                    userId, keyId, ability, modelCode, ex.getClass().getSimpleName());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "可灵模型请求失败，请检查 Access/Secret Key 与网络后重试");
        }
    }

    private AiProviderResult userImage(String apiRoot, KlingAuth creds, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        String path = DEFAULT_IMAGE_PATH;
        String aspect = aspectRatio(cfg, request);
        String taskId = UserProviderMediaSupport.findTaskParam(shortAiTaskMapper, "kling_image_task_id");
        if (taskId.isBlank()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model_name", modelCode);
            body.put("prompt", promptOf(request));
            String negative = firstNonBlank(ShortJson.text(request.param("negative_prompt")),
                    ShortJson.text(cfg.get("negative_prompt")));
            if (!negative.isBlank()) {
                body.put("negative_prompt", negative);
            }
            body.put("n", 1);
            body.put("aspect_ratio", aspect);
            String image = normalizeKlingImageRef(request, false);
            boolean withRefMeta = false;
            if (!image.isBlank()) {
                body.put("image", image);
                String ref = firstNonBlank(ShortJson.text(request.param("image_reference")), "subject");
                body.put("image_reference", ref);
                body.put("image_fidelity", ShortJson.decimal(request.param("image_fidelity"), 0.75d));
                if ("subject".equals(ref)) {
                    body.put("human_fidelity", ShortJson.decimal(request.param("human_fidelity"), 0.55d));
                }
                withRefMeta = true;
            }
            String raw;
            try {
                raw = postUserJson(apiRoot + path, creds, apiRoot, ShortJson.toJson(body), Math.min(60, timeoutSec));
            } catch (BizException ex) {
                if (!withRefMeta) {
                    throw ex;
                }
                body.remove("image_reference");
                body.remove("image_fidelity");
                body.remove("human_fidelity");
                raw = postUserJson(apiRoot + path, creds, apiRoot, ShortJson.toJson(body), Math.min(60, timeoutSec));
            }
            Map<String, Object> submitted = requireOk(raw, "submit");
            taskId = taskIdOf(submitted);
            if (taskId.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "可灵生图未返回 task_id");
            }
            UserProviderMediaSupport.saveTaskParams(shortAiTaskMapper, Map.of("kling_image_task_id", taskId));
        }

        Map<String, Object> data = pollUserTask(apiRoot + path + "/" + taskId, creds, apiRoot,
                Math.max(600, timeoutSec), cfg, 3_000L);
        String url = firstImageUrl(data);
        UserAiModelSupport.validatePublicHttpsUrl(url, "image_url");
        byte[] bytes = userHttp.getBytes(url, Math.min(180, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, guessImageMime(url));
        result.setWidth(aspect.startsWith("16:") ? 1920 : 1080);
        result.setHeight(aspect.startsWith("16:") ? 1080 : 1920);
        return result;
    }

    private AiProviderResult userVideo(String apiRoot, KlingAuth creds, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        String taskId = UserProviderMediaSupport.findTaskParam(shortAiTaskMapper, "kling_video_task_id");
        String kind = UserProviderMediaSupport.findTaskParam(shortAiTaskMapper, "kling_video_kind");
        String image = normalizeKlingImageRef(request, false);
        if (kind.isBlank()) {
            kind = !image.isBlank() ? "image2video" : "text2video";
        }
        String path = "image2video".equals(kind) ? DEFAULT_IMAGE2VIDEO_PATH : DEFAULT_TEXT2VIDEO_PATH;

        if (taskId.isBlank()) {
            boolean image2video = !image.isBlank();
            kind = image2video ? "image2video" : "text2video";
            path = image2video ? DEFAULT_IMAGE2VIDEO_PATH : DEFAULT_TEXT2VIDEO_PATH;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model_name", modelCode);
            body.put("prompt", promptOf(request));
            String negative = firstNonBlank(ShortJson.text(request.param("negative_prompt")),
                    ShortJson.text(cfg.get("negative_prompt")));
            if (!negative.isBlank()) {
                body.put("negative_prompt", negative);
            }
            body.put("mode", modeOf(cfg, request));
            body.put("duration", durationOf(cfg, request));
            if (image2video) {
                body.put("image", image);
                String tailImage = normalizeKlingImageRef(request, true);
                if (!tailImage.isBlank()) {
                    body.put("image_tail", tailImage);
                }
            } else {
                body.put("aspect_ratio", aspectRatio(cfg, request));
            }
            String raw = postUserJson(apiRoot + path, creds, apiRoot, ShortJson.toJson(body), 60);
            Map<String, Object> submitted = requireOk(raw, "submit");
            taskId = taskIdOf(submitted);
            if (taskId.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "可灵视频提交未返回 task_id");
            }
            UserProviderMediaSupport.saveTaskParams(shortAiTaskMapper, Map.of(
                    "kling_video_task_id", taskId,
                    "kling_video_kind", kind));
        }

        long pollTimeoutMs = Math.max(USER_VIDEO_POLL_FLOOR_MS, Math.max(60, timeoutSec) * 1000L);
        Map<String, Object> data = pollUserTaskMs(apiRoot + path + "/" + taskId, creds, apiRoot,
                pollTimeoutMs, cfg, USER_VIDEO_POLL_INTERVAL_MS);
        String url = firstVideoUrl(data);
        UserAiModelSupport.validatePublicHttpsUrl(url, "video_url");
        byte[] bytes = userHttp.getBytes(url, Math.min(300, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        int durSec = videoDurationSec(data, ShortJson.integer(request.param("duration_sec"), 5));
        result.setDurationSec(durSec);
        result.setUsageAmount(durSec);
        return result;
    }

    private AiProviderResult userVoice(String apiRoot, KlingAuth creds, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String voiceId = firstNonBlank(ShortJson.text(request.param("voice_id")),
                ShortJson.text(request.param("voice")), ShortJson.text(cfg.get("default_voice_id")));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", promptOf(request));
        body.put("voice_id", mapKlingVoiceId(voiceId));
        body.put("voice_language", "zh");
        body.put("voice_speed", 1);

        String raw = postUserJson(apiRoot + DEFAULT_TTS_PATH, creds, apiRoot, ShortJson.toJson(body), 60);
        Map<String, Object> submitted = requireOk(raw, "submit");
        Map<String, Object> data = hasMedia(submitted) ? dataOf(submitted) : null;
        if (data == null) {
            String taskId = taskIdOf(submitted);
            if (taskId.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "可灵配音未返回 task_id");
            }
            data = pollUserTask(apiRoot + DEFAULT_TTS_PATH + "/" + taskId, creds, apiRoot,
                    Math.max(120, timeoutSec), cfg, 2_000L);
        }
        String url = firstAudioUrl(data);
        UserAiModelSupport.validatePublicHttpsUrl(url, "audio_url");
        byte[] bytes = userHttp.getBytes(url, Math.min(180, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, UserProviderMediaSupport.guessAudioMime(bytes));
        int durSec = firstAudioDurationSec(data, 0);
        if (durSec > 0) {
            result.setDurationSec(durSec);
            result.setUsageAmount(durSec);
        }
        return result;
    }

    private Map<String, Object> pollUserTask(String queryUrl, KlingAuth creds, String apiRoot,
            int timeoutSec, Map<String, Object> cfg, long defaultIntervalMs) throws Exception {
        return pollUserTaskMs(queryUrl, creds, apiRoot, Math.max(60, timeoutSec) * 1000L, cfg, defaultIntervalMs);
    }

    private Map<String, Object> pollUserTaskMs(String queryUrl, KlingAuth creds, String apiRoot,
            long budgetMs, Map<String, Object> cfg, long defaultIntervalMs) throws Exception {
        long intervalMs = Math.max(100L, ShortJson.integer(cfg.get("poll_interval_ms"), (int) defaultIntervalMs));
        long deadline = System.currentTimeMillis() + budgetMs;
        while (System.currentTimeMillis() < deadline) {
            sleeper.sleep(intervalMs);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            Map<String, Object> resp;
            try {
                String raw = getUserJson(queryUrl, creds, apiRoot, 60);
                resp = requireOk(raw, "poll");
            } catch (BizException ex) {
                if (!UserProviderMediaSupport.isTransientFailure(ex) || System.currentTimeMillis() >= deadline) {
                    throw ex;
                }
                continue;
            }
            String status = taskStatusOf(resp);
            if (isSuccess(status) && hasMedia(resp)) {
                return dataOf(resp);
            }
            if (isFailed(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, failMessage(resp));
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "可灵任务超时，请稍后重试");
    }

    private String postUserJson(String url, KlingAuth creds, String apiRoot,
            String body, int timeoutSec) throws Exception {
        String token = creds.resolveBearerToken(apiRoot);
        return userHttp.postJson(url, Map.of("Authorization", "Bearer " + token), body, timeoutSec);
    }

    private String getUserJson(String url, KlingAuth creds, String apiRoot, int timeoutSec)
            throws Exception {
        String token = creds.resolveBearerToken(apiRoot);
        return userHttp.getJson(url, Map.of("Authorization", "Bearer " + token), timeoutSec);
    }

    private static String normalizeKlingImageRef(AiProviderRequest request, boolean lastFrame) {
        String url = ShortJson.text(request.param(lastFrame ? "last_image_url" : "image_url"));
        if (url.startsWith("http://") || url.startsWith("https://")) {
            UserAiModelSupport.validatePublicHttpsUrl(url, lastFrame ? "last_image_url" : "image_url");
            return url;
        }
        String rawB64 = ShortJson.text(request.param(lastFrame ? "last_image_base64" : "image_base64"));
        String stripped = UserProviderMediaSupport.stripDataUrlPrefix(rawB64);
        if (stripped.isBlank()) {
            return "";
        }
        int approxBytes = stripped.length() * 3 / 4;
        if (approxBytes > MAX_IMAGE_BYTES) {
            log.warn("KLING skip frame: image exceeds {} bytes", MAX_IMAGE_BYTES);
            return "";
        }
        return stripped;
    }

    private static String firstAudioUrl(Map<String, Object> data) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object audios = map.get("audios");
            if (audios instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                String url = ShortJson.text(first.get("url"));
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "可灵配音缺少音频地址");
    }

    private static int firstAudioDurationSec(Map<String, Object> data, int fallback) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object audios = map.get("audios");
            if (audios instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                int sec = ShortJson.integer(first.get("duration"), 0);
                if (sec > 0) {
                    return sec;
                }
            }
        }
        return fallback;
    }

    private static String mapKlingVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            return "zhinen_xuesheng";
        }
        return switch (voiceId.trim()) {
            case "v-female-cold-01" -> "chengshu_jiejie";
            case "v-female-warm-01" -> "zhinen_xuesheng";
            case "v-male-deep-01" -> "ai_chenjiahao_712";
            case "v-male-bright-01" -> "genshin_vindi2";
            case "v-narrator-01" -> "ai_laoguowang_712";
            default -> voiceId.trim();
        };
    }

    /** Retained for unit coverage of legacy transport; production path never calls this. */
    AiProviderResult executeLegacy(AiProviderRequest request) {
        if (request == null || request.getModel() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model required");
        }
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String baseUrl = trimSlash(firstNonBlank(ShortJson.text(cfg.get("base_url")), DEFAULT_BASE_URL));
        String secretRef = ShortJson.text(cfg.get("secret_ref"));
        if (secretRef.isBlank()) {
            secretRef = SysSecretReader.AI_KLING_API_KEY;
        }
        String apiKey = secretReader.get(secretRef);
        if (apiKey.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "AI api key missing for " + secretRef);
        }
        int timeoutSec = request.getModel().getTimeoutSec() == null ? 180 : request.getModel().getTimeoutSec();
        if (timeoutSec < 30) {
            timeoutSec = 30;
        }
        String ability = request.getAbilityType();
        try {
            if ("IMAGE".equals(ability)) {
                return generateImage(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            if ("VIDEO".equals(ability)) {
                return generateVideo(baseUrl, apiKey, cfg, request, timeoutSec);
            }
            throw new BizException(CommonErrorCode.RULE_REJECT, "KLING unsupported ability: " + ability);
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "kling interrupted");
        } catch (Exception ex) {
            log.warn("KLING call failed ability={} model={}", ability, request.getModel().getModelCode(), ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, safeMessage(ex));
        }
    }

    private AiProviderResult generateImage(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String path = firstNonBlank(ShortJson.text(cfg.get("image_path")), DEFAULT_IMAGE_PATH);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_name", request.getModel().getModelCode());
        body.put("prompt", promptOf(request));
        String negative = firstNonBlank(ShortJson.text(request.param("negative_prompt")),
                ShortJson.text(cfg.get("negative_prompt")));
        if (!negative.isBlank()) {
            body.put("negative_prompt", negative);
        }
        body.put("n", 1);
        body.put("aspect_ratio", aspectRatio(cfg, request));
        String image = imagePayload(request);
        if (image != null && !image.isBlank()) {
            body.put("image", image);
            String ref = firstNonBlank(ShortJson.text(request.param("image_reference")), "subject");
            body.put("image_reference", ref);
            Double fidelity = ShortJson.decimal(request.param("image_fidelity"), 0.75d);
            body.put("image_fidelity", fidelity);
            if ("subject".equals(ref)) {
                body.put("human_fidelity", ShortJson.decimal(request.param("human_fidelity"), 0.55d));
            }
        }
        Map<String, Object> data = submitAndPoll(baseUrl, path, apiKey, body, timeoutSec, cfg);
        String url = firstImageUrl(data);
        byte[] bytes = transport.getBytes(url, Math.min(120, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, guessImageMime(url));
        result.setWidth(1080);
        result.setHeight(1920);
        return result;
    }

    private AiProviderResult generateVideo(String baseUrl, String apiKey, Map<String, Object> cfg,
            AiProviderRequest request, int timeoutSec) throws Exception {
        String image = imagePayload(request);
        boolean image2video = image != null && !image.isBlank();
        String path = image2video
                ? firstNonBlank(ShortJson.text(cfg.get("image2video_path")), DEFAULT_IMAGE2VIDEO_PATH)
                : firstNonBlank(ShortJson.text(cfg.get("text2video_path")), DEFAULT_TEXT2VIDEO_PATH);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_name", request.getModel().getModelCode());
        body.put("prompt", promptOf(request));
        String negative = firstNonBlank(ShortJson.text(request.param("negative_prompt")),
                ShortJson.text(cfg.get("negative_prompt")));
        if (!negative.isBlank()) {
            body.put("negative_prompt", negative);
        }
        body.put("mode", modeOf(cfg, request));
        body.put("duration", durationOf(cfg, request));
        if (image2video) {
            body.put("image", image);
        } else {
            body.put("aspect_ratio", aspectRatio(cfg, request));
        }
        Map<String, Object> data = submitAndPoll(baseUrl, path, apiKey, body, timeoutSec, cfg);
        String url = firstVideoUrl(data);
        byte[] bytes = transport.getBytes(url, Math.min(180, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        result.setWidth(1080);
        result.setHeight(1920);
        result.setDurationSec(videoDurationSec(data, ShortJson.integer(request.param("duration_sec"), 5)));
        return result;
    }

    private Map<String, Object> submitAndPoll(String baseUrl, String path, String apiKey,
            Map<String, Object> body, int timeoutSec, Map<String, Object> cfg) throws Exception {
        int httpTimeout = Math.min(60, timeoutSec);
        String raw = transport.postJson(baseUrl + path, apiKey, ShortJson.toJson(body), httpTimeout);
        Map<String, Object> submitted = requireOk(raw, "submit");
        String taskId = taskIdOf(submitted);
        if (taskId.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling submit missing task_id");
        }
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        long intervalMs = pollIntervalMs(cfg);
        String queryUrl = baseUrl + path + "/" + taskId;
        Map<String, Object> last = submitted;
        while (System.currentTimeMillis() < deadline) {
            String status = taskStatusOf(last);
            if (isSuccess(status) && hasMedia(last)) {
                return dataOf(last);
            }
            if (isFailed(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, failMessage(last));
            }
            sleeper.sleep(intervalMs);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            raw = transport.getJson(queryUrl, apiKey, httpTimeout);
            last = requireOk(raw, "poll");
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "kling task timeout task_id=" + taskId);
    }

    private static Map<String, Object> requireOk(String raw, String phase) {
        Map<String, Object> resp = ShortJson.map(raw);
        Object code = resp.get("code");
        if (code != null && !"0".equals(String.valueOf(code))) {
            String msg = firstNonBlank(ShortJson.text(resp.get("message")), "kling " + phase + " failed");
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, msg);
        }
        return resp;
    }

    private static Map<String, Object> dataOf(Map<String, Object> resp) {
        Object data = resp.get("data");
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            return out;
        }
        return new LinkedHashMap<>();
    }

    private static String taskIdOf(Map<String, Object> resp) {
        Map<String, Object> data = dataOf(resp);
        return firstNonBlank(ShortJson.text(data.get("task_id")), ShortJson.text(data.get("id")));
    }

    private static String taskStatusOf(Map<String, Object> resp) {
        Map<String, Object> data = dataOf(resp);
        return firstNonBlank(ShortJson.text(data.get("task_status")), ShortJson.text(data.get("status"))).toLowerCase();
    }

    private static boolean isSuccess(String status) {
        return "succeed".equals(status) || "succeeded".equals(status) || "success".equals(status);
    }

    private static boolean isFailed(String status) {
        return "failed".equals(status) || "fail".equals(status) || "error".equals(status) || "canceled".equals(status)
                || "cancelled".equals(status);
    }

    private static boolean hasMedia(Map<String, Object> resp) {
        try {
            firstImageUrl(dataOf(resp));
            return true;
        } catch (BizException ignored) {
            try {
                firstVideoUrl(dataOf(resp));
                return true;
            } catch (BizException ignored2) {
                try {
                    firstAudioUrl(dataOf(resp));
                    return true;
                } catch (BizException ignored3) {
                    return false;
                }
            }
        }
    }

    private static String failMessage(Map<String, Object> resp) {
        Map<String, Object> data = dataOf(resp);
        return firstNonBlank(ShortJson.text(data.get("task_status_msg")), ShortJson.text(data.get("message")),
                "kling task failed");
    }

    @SuppressWarnings("unchecked")
    private static String firstImageUrl(Map<String, Object> data) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object images = map.get("images");
            if (images instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                String url = firstNonBlank(ShortJson.text(first.get("url")), ShortJson.text(first.get("url_1")));
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling image url missing");
    }

    @SuppressWarnings("unchecked")
    private static String firstVideoUrl(Map<String, Object> data) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object videos = map.get("videos");
            if (videos instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                String url = ShortJson.text(first.get("url"));
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling video url missing");
    }

    @SuppressWarnings("unchecked")
    private static int videoDurationSec(Map<String, Object> data, int fallback) {
        Object result = data.get("task_result");
        if (result instanceof Map<?, ?> map) {
            Object videos = map.get("videos");
            if (videos instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                return ShortJson.integer(first.get("duration"), fallback);
            }
        }
        return fallback;
    }

    private static String promptOf(AiProviderRequest request) {
        String prompt = request.getPrompt();
        if (prompt != null && !prompt.isBlank()) {
            return prompt.trim();
        }
        String title = ShortJson.text(request.param("title"));
        String subtitle = ShortJson.text(request.param("subtitle"));
        String joined = (title + " " + subtitle).trim();
        if (joined.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "prompt required");
        }
        return joined;
    }

    private static String imagePayload(AiProviderRequest request) {
        String url = ShortJson.text(request.param("image_url"));
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String b64 = ShortJson.text(request.param("image_base64"));
        if (b64.isBlank()) {
            return "";
        }
        int approxBytes = b64.length() * 3 / 4;
        if (approxBytes > MAX_IMAGE_BYTES) {
            log.warn("KLING skip first frame: image exceeds {} bytes", MAX_IMAGE_BYTES);
            return "";
        }
        return b64;
    }

    private static String aspectRatio(Map<String, Object> cfg, AiProviderRequest request) {
        return firstNonBlank(ShortJson.text(request.param("aspect_ratio")),
                ShortJson.text(cfg.get("aspect_ratio")), "9:16");
    }

    private static String modeOf(Map<String, Object> cfg, AiProviderRequest request) {
        String mode = firstNonBlank(ShortJson.text(request.param("mode")), ShortJson.text(cfg.get("mode")));
        if (!mode.isBlank()) {
            return mode;
        }
        String quality = request.getModel().getQualityLevel();
        if ("HIGH".equalsIgnoreCase(quality) || "PRO".equalsIgnoreCase(quality)) {
            return "pro";
        }
        return "std";
    }

    private static String durationOf(Map<String, Object> cfg, AiProviderRequest request) {
        int sec = ShortJson.integer(request.param("duration_sec"), 0);
        if (sec <= 0) {
            sec = ShortJson.integer(cfg.get("duration"), 5);
        }
        if (sec <= 5) {
            return "5";
        }
        return "10";
    }

    private static long pollIntervalMs(Map<String, Object> cfg) {
        int ms = ShortJson.integer(cfg.get("poll_interval_ms"), 3000);
        if (ms < 0) {
            return 0;
        }
        return Math.min(10_000, ms);
    }

    private static String guessImageMime(String url) {
        String lower = url == null ? "" : url.toLowerCase();
        if (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains("image/jpeg")) {
            return "image/jpeg";
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String v = url.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "kling call failed";
        }
        if (msg.length() > 240) {
            return msg.substring(0, 240);
        }
        return msg;
    }

    interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    interface Transport {
        String postJson(String url, String bearer, String body, int timeoutSec) throws Exception;

        String getJson(String url, String bearer, int timeoutSec) throws Exception;

        byte[] getBytes(String url, int timeoutSec) throws Exception;
    }

    static final class JdkTransport implements Transport {

        private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

        @Override
        public String postJson(String url, String bearer, String body, int timeoutSec) throws Exception {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .header("Authorization", "Bearer " + bearer)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return handleJson(resp);
        }

        @Override
        public String getJson(String url, String bearer, int timeoutSec) throws Exception {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .header("Authorization", "Bearer " + bearer)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return handleJson(resp);
        }

        @Override
        public byte[] getBytes(String url, int timeoutSec) throws Exception {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(15, timeoutSec)))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 400) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling download status " + resp.statusCode());
            }
            byte[] body = resp.body();
            if (body == null || body.length == 0) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling download empty");
            }
            return body;
        }

        private static String handleJson(HttpResponse<String> resp) {
            if (resp.statusCode() >= 400) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "kling http " + resp.statusCode());
            }
            return resp.body() == null ? "" : resp.body();
        }
    }
}
