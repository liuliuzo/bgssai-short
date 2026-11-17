package com.bgssai.shortai.aiprovider;

import java.util.Base64;
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
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

/**
 * provider_code=MINIMAX — 海螺视频 / 图像 / 文本 / 语音 / 识别 / 音乐。
 * 平台直连禁行，用户自有密钥模式支持 https://api.minimaxi.com 全能力调用。
 */
@Component
public class MiniMaxAiProvider implements AiProvider {

    public static final String CODE = "MINIMAX";

    static final String DEFAULT_BASE_URL = "https://api.minimaxi.com";
    private static final long VIDEO_POLL_INTERVAL_MS = 4_000L;
    private static final long VIDEO_POLL_FLOOR_MS = 900_000L;

    private static final Logger log = LoggerFactory.getLogger(MiniMaxAiProvider.class);

    private final SysSecretReader secretReader;

    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;

    void setUserAiProviderKeyService(UserAiProviderKeyService userAiProviderKeyService) {
        this.userAiProviderKeyService = userAiProviderKeyService;
    }
    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;

    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;

    @Autowired(required = false)
    private ShortAiTaskMapper shortAiTaskMapper;

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
        if (request == null || request.getModel() == null) {
            throw DirectVendorGuard.rejected(CODE);
        }
        Optional<ResolvedProviderKey> providerByok = DirectVendorGuard.isExplicitUserModelKey(request.getModel())
                ? Optional.empty()
                : DirectVendorGuard.resolveProviderByok(request, userAiProviderKeyService);
        if (providerByok.isPresent() && providerByok.get().apiKey() != null
                && !providerByok.get().apiKey().isBlank()) {
            return executeWithCredentials(request, providerByok.get());
        }
        if (!UserAiModelSupport.isUserKeyModel(request.getModel())) {
            throw DirectVendorGuard.rejected(CODE);
        }
        return executeUserKey(request);
    }

    private AiProviderResult executeWithCredentials(AiProviderRequest request, ResolvedProviderKey key) {
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String effectiveUrl = UserAiProviderCatalog.resolveEffectiveBaseUrl(key.baseUrl(), CODE);
        String apiRoot = UserAiModelSupport.stripTrailingPath(effectiveUrl, "/v1");
        return dispatchUserKey(request, cfg, key.userId(),
                UserAiModelSupport.PROVIDER_KEY_PREFIX + "minimax", apiRoot, key.apiKey());
    }

    private AiProviderResult executeUserKey(AiProviderRequest request) {
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String userId = ShortJson.text(cfg.get("user_id"));
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                request.getModel().getModelId());
        String baseUrl = UserAiModelSupport.normalizeBaseUrl(ShortJson.text(cfg.get("base_url")), CODE);
        String apiRoot = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        String apiKey = userAiModelKeyService.requirePlainApiKey(userId, keyId);
        return dispatchUserKey(request, cfg, userId, keyId, apiRoot, apiKey);
    }

    private AiProviderResult dispatchUserKey(AiProviderRequest request, Map<String, Object> cfg,
            String userId, String keyId, String apiRoot, String apiKey) {
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        String modelCode = ProviderHttp.firstNonBlank(request.getModel().getModelCode(), defaultModel(ability));
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);

        log.info("调用 MiniMax 用户密钥模型 userId={} keyId={} ability={} model={}",
                userId, keyId, ability, modelCode);
        try {
            return switch (ability) {
                case "TEXT" -> userChat(apiRoot, apiKey, modelCode, request, timeoutSec);
                case "IMAGE" -> userImage(apiRoot, apiKey, modelCode, cfg, request, timeoutSec);
                case "VIDEO" -> userVideo(apiRoot, apiKey, modelCode, cfg, request, timeoutSec);
                case "VOICE" -> userVoice(apiRoot, apiKey, modelCode, cfg, request, timeoutSec);
                case "ASR" -> userAsr(apiRoot, apiKey, modelCode, request, timeoutSec);
                case "MUSIC" -> userMusic(apiRoot, apiKey, modelCode, request, timeoutSec);
                default -> throw new BizException(CommonErrorCode.RULE_REJECT,
                        "MINIMAX unsupported ability: " + ability);
            };
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "MiniMax 模型调用被中断，请稍后重试");
        } catch (Exception ex) {
            log.warn("MiniMax 用户密钥调用异常 userId={} keyId={} ability={} model={} errType={}",
                    userId, keyId, ability, modelCode, ex.getClass().getSimpleName());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "MiniMax 模型请求失败，请检查密钥与网络后重试");
        }
    }

    private AiProviderResult userChat(String apiRoot, String apiKey, String modelCode,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
        body.put("stream", false);
        String raw = http.postJson(apiRoot + "/v1/chat/completions", bearer(apiKey),
                ShortJson.toJson(body), Math.min(180, timeoutSec));
        Map<String, Object> parsed = ShortJson.map(raw);
        assertBaseOk(parsed);
        String content = UserProviderMediaSupport.extractChatContent(parsed);
        if (content.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "MiniMax 文本返回内容为空");
        }
        AiProviderResult result = AiProviderResult.text(content);
        int tokens = UserProviderMediaSupport.nestedInt(parsed, "usage", "total_tokens", 1);
        result.setUsageAmount(Math.max(1, tokens));
        return result;
    }

    private AiProviderResult userImage(String apiRoot, String apiKey, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        String aspect = ProviderHttp.aspectRatio(cfg, request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("aspect_ratio", aspect);
        body.put("n", 1);
        body.put("response_format", "base64");

        String refImage = ProviderHttp.imagePayload(request);
        boolean withSubjectRef = false;
        if (!refImage.isBlank()) {
            validateHttpRefIfUrl(refImage, "image_url");
            body.put("subject_reference", List.of(Map.of("type", "character", "image_file", refImage)));
            withSubjectRef = true;
        }

        String raw;
        try {
            raw = http.postJson(apiRoot + "/v1/image_generation", bearer(apiKey),
                    ShortJson.toJson(body), Math.min(180, timeoutSec));
        } catch (BizException ex) {
            if (!withSubjectRef) {
                throw ex;
            }
            body.remove("subject_reference");
            raw = http.postJson(apiRoot + "/v1/image_generation", bearer(apiKey),
                    ShortJson.toJson(body), Math.min(180, timeoutSec));
        }
        Map<String, Object> resp = ShortJson.map(raw);
        assertBaseOk(resp);
        byte[] bytes = extractImageBytes(resp, timeoutSec);
        AiProviderResult result = AiProviderResult.binary(bytes, "image/png");
        result.setWidth(aspect.startsWith("16:") ? 1920 : 1080);
        result.setHeight(aspect.startsWith("16:") ? 1080 : 1920);
        return result;
    }

    private byte[] extractImageBytes(Map<String, Object> resp, int timeoutSec) throws Exception {
        Object dataObj = resp.get("data");
        if (dataObj instanceof Map<?, ?> data) {
            Object b64Obj = data.get("image_base64");
            if (b64Obj instanceof List<?> list && !list.isEmpty()) {
                String firstB64 = ShortJson.text(list.get(0));
                if (!firstB64.isBlank()) {
                    return UserProviderMediaSupport.decodeBase64(firstB64);
                }
            } else if (b64Obj instanceof String s && !s.isBlank()) {
                return UserProviderMediaSupport.decodeBase64(s);
            }
            Object urlsObj = data.get("image_urls");
            if (urlsObj instanceof List<?> list && !list.isEmpty()) {
                String url = ShortJson.text(list.get(0));
                if (!url.isBlank()) {
                    UserAiModelSupport.validatePublicHttpsUrl(url, "image_url");
                    return http.getBytes(url, Math.min(180, timeoutSec));
                }
            }
            String singleUrl = ShortJson.text(data.get("image_url"));
            if (!singleUrl.isBlank()) {
                UserAiModelSupport.validatePublicHttpsUrl(singleUrl, "image_url");
                return http.getBytes(singleUrl, Math.min(180, timeoutSec));
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "MiniMax 图片生成返回为空");
    }

    private AiProviderResult userVideo(String apiRoot, String apiKey, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        int sec = durationSec(cfg, request);
        String taskId = UserProviderMediaSupport.findTaskParam(shortAiTaskMapper, "minimax_video_task_id");
        if (taskId.isBlank()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelCode);
            body.put("prompt", ProviderHttp.promptOf(request));
            if (modelCode.toLowerCase(Locale.ROOT).contains("hailuo")) {
                body.put("duration", sec);
                body.put("resolution", ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("resolution")), "768P"));
            }
            String firstImage = ProviderHttp.imagePayload(request);
            if (!firstImage.isBlank()) {
                validateHttpRefIfUrl(firstImage, "image_url");
                body.put("first_frame_image", firstImage);
                String lastImage = ProviderHttp.lastImagePayload(request);
                if (!lastImage.isBlank()) {
                    validateHttpRefIfUrl(lastImage, "last_image_url");
                    body.put("last_frame_image", lastImage);
                }
            }
            String raw = http.postJson(apiRoot + "/v1/video_generation", bearer(apiKey),
                    ShortJson.toJson(body), 60);
            Map<String, Object> submitted = ShortJson.map(raw);
            assertBaseOk(submitted);
            taskId = ShortJson.text(submitted.get("task_id"));
            if (taskId.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "MiniMax 视频提交未返回 task_id");
            }
            UserProviderMediaSupport.saveTaskParams(shortAiTaskMapper, Map.of("minimax_video_task_id", taskId));
        }

        Map<String, Object> done = pollUserVideo(apiRoot, apiKey, taskId, timeoutSec, cfg);
        String fileId = ShortJson.text(done.get("file_id"));
        String download = ShortJson.text(done.get("download_url"));
        if (download.isBlank() && !fileId.isBlank()) {
            String fileRaw = http.getJson(apiRoot + "/v1/files/retrieve?file_id=" + fileId,
                    bearer(apiKey), 60);
            Map<String, Object> fileResp = ShortJson.map(fileRaw);
            assertBaseOk(fileResp);
            download = UserProviderMediaSupport.nestedText(fileResp, "file", "download_url");
        }
        if (download.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "MiniMax 视频完成但缺少下载链接");
        }
        UserAiModelSupport.validatePublicHttpsUrl(download, "video_url");
        byte[] bytes = http.getBytes(download, Math.min(300, timeoutSec));
        AiProviderResult result = AiProviderResult.binary(bytes, "video/mp4");
        int parsedDur = ShortJson.integer(done.get("video_duration"), 0);
        int finalDur = parsedDur > 0 ? parsedDur : sec;
        result.setDurationSec(finalDur);
        result.setUsageAmount(finalDur);
        return result;
    }

    private Map<String, Object> pollUserVideo(String apiRoot, String apiKey, String taskId,
            int timeoutSec, Map<String, Object> cfg) throws Exception {
        long interval = Math.max(100L, ShortJson.integer(cfg.get("poll_interval_ms"), (int) VIDEO_POLL_INTERVAL_MS));
        long deadline = System.currentTimeMillis() + Math.max(VIDEO_POLL_FLOOR_MS, Math.max(60, timeoutSec) * 1000L);
        String queryUrl = apiRoot + "/v1/query/video_generation?task_id=" + taskId;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> resp;
            try {
                resp = ShortJson.map(http.getJson(queryUrl, bearer(apiKey), 60));
            } catch (BizException ex) {
                if (!UserProviderMediaSupport.isTransientFailure(ex) || System.currentTimeMillis() >= deadline) {
                    throw ex;
                }
                sleeper.sleep(interval);
                continue;
            }
            assertBaseOk(resp);
            String status = ShortJson.text(resp.get("status")).toLowerCase(Locale.ROOT);
            if ("success".equals(status) || "succeed".equals(status)) {
                return resp;
            }
            if ("fail".equals(status) || "failed".equals(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                        ProviderHttp.firstNonBlank(statusMsg(resp), "MiniMax 视频生成失败"));
            }
            sleeper.sleep(interval);
        }
        throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "MiniMax 视频生成超时，请稍后重试");
    }

    private AiProviderResult userVoice(String apiRoot, String apiKey, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        String rawVoice = ProviderHttp.firstNonBlank(ShortJson.text(request.param("voice_id")),
                ShortJson.text(request.param("voice")), ShortJson.text(cfg.get("default_voice_id")));
        Map<String, Object> voiceSetting = new LinkedHashMap<>();
        voiceSetting.put("voice_id", mapVoiceId(rawVoice));
        voiceSetting.put("speed", 1.0);
        voiceSetting.put("vol", 1.0);
        voiceSetting.put("pitch", 0);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("text", ProviderHttp.promptOf(request));
        body.put("stream", false);
        body.put("output_format", "hex");
        body.put("voice_setting", voiceSetting);
        body.put("audio_setting", Map.of(
                "sample_rate", 32000,
                "bitrate", 128000,
                "format", "mp3",
                "channel", 1));

        String raw = http.postJson(apiRoot + "/v1/t2a_v2", bearer(apiKey),
                ShortJson.toJson(body), Math.min(120, timeoutSec));
        Map<String, Object> resp = ShortJson.map(raw);
        assertBaseOk(resp);
        String audio = ProviderHttp.firstNonBlank(
                UserProviderMediaSupport.nestedText(resp, "data", "audio"),
                ShortJson.text(resp.get("audio")));
        int durationMs = UserProviderMediaSupport.nestedInt(resp, "extra_info", "audio_length", 0);
        return buildAudioResult(audio, durationMs, timeoutSec);
    }

    private AiProviderResult userAsr(String apiRoot, String apiKey, String modelCode,
            AiProviderRequest request, int timeoutSec) throws Exception {
        byte[] audio = UserProviderMediaSupport.audioPayload(request, http, timeoutSec);
        if (audio == null || audio.length == 0) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "audio input required");
        }
        String mime = ProviderHttp.firstNonBlank(ShortJson.text(request.param("audio_mime")),
                UserProviderMediaSupport.guessAudioMime(audio));
        String filename = mime.contains("wav") ? "audio.wav" : "audio.mp3";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", modelCode);
        fields.put("response_format", "verbose_json");
        fields.put("timestamp_level", "word");

        String raw = http.postMultipart(apiRoot + "/v1/speech_to_text", bearer(apiKey),
                audio, filename, mime, fields, Math.min(300, Math.max(60, timeoutSec)));
        Map<String, Object> parsed = new LinkedHashMap<>(ShortJson.map(raw));
        UserProviderMediaSupport.normalizeSttSentences(parsed);
        AiProviderResult result = AiProviderResult.text(ShortJson.toJson(parsed));
        double dur = UserProviderMediaSupport.numberValue(parsed.get("duration"));
        if (dur > 0) {
            int sec = (int) Math.ceil(dur);
            result.setDurationSec(sec);
            result.setUsageAmount(Math.max(1, sec));
        }
        return result;
    }

    private AiProviderResult userMusic(String apiRoot, String apiKey, String modelCode,
            AiProviderRequest request, int timeoutSec) throws Exception {
        boolean instrumental = !Boolean.FALSE.equals(request.param("is_instrumental"))
                && !"false".equalsIgnoreCase(ShortJson.text(request.param("is_instrumental")))
                && !"0".equals(ShortJson.text(request.param("is_instrumental")));
        String lyrics = ShortJson.text(request.param("lyrics"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("is_instrumental", instrumental);
        if (!lyrics.isBlank()) {
            body.put("lyrics", lyrics);
        } else if (modelCode.contains("2.0")) {
            body.put("lyrics", "[Instrumental]");
        }
        body.put("output_format", "hex");
        body.put("audio_setting", Map.of(
                "sample_rate", 44100,
                "bitrate", 128000,
                "format", "mp3"));

        String raw = http.postJson(apiRoot + "/v1/music_generation", bearer(apiKey),
                ShortJson.toJson(body), Math.min(240, Math.max(60, timeoutSec)));
        Map<String, Object> resp = ShortJson.map(raw);
        assertBaseOk(resp);
        String audio = ProviderHttp.firstNonBlank(
                UserProviderMediaSupport.nestedText(resp, "data", "audio"),
                ShortJson.text(resp.get("audio")));
        int durationMs = UserProviderMediaSupport.nestedInt(resp, "extra_info", "music_duration", 0);
        if (durationMs <= 0) {
            durationMs = UserProviderMediaSupport.nestedInt(resp, "extra_info", "audio_length", 0);
        }
        if (durationMs <= 0) {
            durationMs = Math.max(5, ShortJson.integer(request.param("duration_sec"), 15)) * 1000;
        }
        return buildAudioResult(audio, durationMs, timeoutSec);
    }

    private AiProviderResult buildAudioResult(String audio, int durationMs, int timeoutSec) throws Exception {
        if (audio == null || audio.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "MiniMax 音频生成返回为空");
        }
        byte[] bytes;
        if (audio.startsWith("https://")) {
            UserAiModelSupport.validatePublicHttpsUrl(audio, "audio_url");
            bytes = http.getBytes(audio, Math.min(180, timeoutSec));
        } else {
            bytes = UserProviderMediaSupport.decodeHexOrBase64(audio);
        }
        AiProviderResult result = AiProviderResult.binary(bytes, UserProviderMediaSupport.guessAudioMime(bytes));
        int sec = UserProviderMediaSupport.ceilSeconds(durationMs);
        if (sec > 0) {
            result.setDurationSec(sec);
            result.setUsageAmount(sec);
        }
        return result;
    }

    private static void validateHttpRefIfUrl(String ref, String fieldName) {
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            UserAiModelSupport.validatePublicHttpsUrl(ref, fieldName);
        }
    }

    private static String defaultModel(String ability) {
        return switch (ability) {
            case "IMAGE" -> "image-01";
            case "VIDEO" -> "MiniMax-Hailuo-02";
            case "VOICE" -> "speech-2.8-hd";
            case "ASR" -> "asr-1.0";
            case "MUSIC" -> "music-2.5";
            default -> "MiniMax-M2.5";
        };
    }

    private static String mapVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            return "female-shaonv";
        }
        return switch (voiceId.trim()) {
            case "v-female-cold-01" -> "female-yujie";
            case "v-female-warm-01" -> "female-shaonv";
            case "v-male-deep-01" -> "male-qn-jingying";
            case "v-male-bright-01" -> "male-qn-qingse";
            case "v-narrator-01" -> "presenter_male";
            default -> voiceId.trim();
        };
    }

    AiProviderResult executeLegacy(AiProviderRequest request) {
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
        Map<String, Object> last;
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
