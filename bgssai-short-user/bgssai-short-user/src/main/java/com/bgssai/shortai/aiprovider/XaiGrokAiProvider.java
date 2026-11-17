package com.bgssai.shortai.aiprovider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.billing.UsageScope;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;

/**
 * provider_code=XAI (xAI Grok API) for user-configured API keys.
 * Supports TEXT (/chat/completions), IMAGE (/images/generations & /images/edits),
 * VIDEO (/videos/generations & /videos/{request_id}), VOICE (/tts), and ASR (/stt).
 */
@Component
public class XaiGrokAiProvider implements AiProvider {

    public static final String CODE = "XAI";

    private static final Logger log = LoggerFactory.getLogger(XaiGrokAiProvider.class);
    private static final long VIDEO_POLL_INTERVAL_MS = 4_000L;
    private static final long VIDEO_POLL_FLOOR_MS = 900_000L;

    @Autowired
    private UserAiModelKeyService userAiModelKeyService;

    @Autowired(required = false)
    private ShortAiTaskMapper shortAiTaskMapper;

    private final ProviderHttp.Client http;
    private final ProviderHttp.Sleeper sleeper;

    public XaiGrokAiProvider() {
        this.http = new ProviderHttp.JdkClient();
        this.sleeper = ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        };
    }

    XaiGrokAiProvider(UserAiModelKeyService userAiModelKeyService, ShortAiTaskMapper shortAiTaskMapper,
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
        Map<String, Object> cfg = ShortJson.map(request.getModel().getConfigJson());
        String userId = ShortJson.text(cfg.get("user_id"));
        String keyId = ProviderHttp.firstNonBlank(ShortJson.text(cfg.get("user_key_id")),
                request.getModel().getModelId());
        String baseUrl = xaiApiBase(UserAiModelSupport.normalizeBaseUrl(ShortJson.text(cfg.get("base_url")), CODE));
        String apiKey = userAiModelKeyService.requirePlainApiKey(userId, keyId);
        String modelCode = ProviderHttp.firstNonBlank(request.getModel().getModelCode(), "grok-4.7");
        String ability = request.getAbilityType() == null ? ""
                : request.getAbilityType().trim().toUpperCase(Locale.ROOT);
        int timeoutSec = ProviderHttp.timeoutSec(request, 180);

        log.info("调用 Grok 模型 userId={} keyId={} ability={} model={}", userId, keyId, ability, modelCode);
        try {
            return switch (ability) {
                case "TEXT" -> chat(baseUrl, apiKey, modelCode, request, timeoutSec);
                case "IMAGE" -> image(baseUrl, apiKey, modelCode, cfg, request, timeoutSec);
                case "VIDEO" -> video(baseUrl, apiKey, modelCode, cfg, request, timeoutSec);
                case "VOICE" -> tts(baseUrl, apiKey, request, timeoutSec);
                case "ASR" -> stt(baseUrl, apiKey, modelCode, request, timeoutSec);
                case "MUSIC" -> throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                        "Grok 暂不支持背景音乐生成，请先配置其他音乐模型或跳过背景音乐直接合成");
                default -> throw new BizException(CommonErrorCode.RULE_REJECT, "unsupported ability: " + ability);
            };
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "Grok 模型调用被中断，请稍后重试");
        } catch (Exception ex) {
            log.warn("Grok 模型调用异常 userId={} keyId={} ability={} model={} errType={}",
                    userId, keyId, ability, modelCode, ex.getClass().getSimpleName());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "Grok 模型请求失败，请检查密钥与网络后重试");
        }
    }

    private AiProviderResult chat(String baseUrl, String apiKey, String modelCode,
            AiProviderRequest request, int timeoutSec) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("messages", List.of(Map.of("role", "user", "content", ProviderHttp.promptOf(request))));
        body.put("stream", false);
        String raw = http.postJson(baseUrl + "/chat/completions", authHeaders(apiKey),
                ShortJson.toJson(body), Math.min(180, timeoutSec));
        Map<String, Object> parsed = ShortJson.map(raw);
        String content = extractChatContent(parsed);
        if (content.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "Grok 文本返回内容为空");
        }
        AiProviderResult result = AiProviderResult.text(content);
        int tokens = nestedInt(parsed, "usage", "total_tokens", 1);
        result.setUsageAmount(Math.max(1, tokens));
        return result;
    }

    private AiProviderResult image(String baseUrl, String apiKey, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        String refImage = ProviderHttp.imagePayload(request);
        String aspect = ProviderHttp.aspectRatio(cfg, request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        body.put("prompt", ProviderHttp.promptOf(request));
        body.put("response_format", "b64_json");

        String endpoint;
        if (!refImage.isBlank()) {
            endpoint = baseUrl + "/images/edits";
            Map<String, Object> imageObj = new LinkedHashMap<>();
            imageObj.put("url", refImage);
            imageObj.put("type", "image_url");
            body.put("image", imageObj);
            if (!aspect.isBlank()) {
                body.put("aspect_ratio", aspect);
            }
        } else {
            endpoint = baseUrl + "/images/generations";
            body.put("n", 1);
            if (!aspect.isBlank()) {
                body.put("aspect_ratio", aspect);
            }
        }

        String raw;
        try {
            raw = http.postJson(endpoint, authHeaders(apiKey), ShortJson.toJson(body), Math.max(180, timeoutSec));
        } catch (BizException ex) {
            if (body.containsKey("aspect_ratio")) {
                body.remove("aspect_ratio");
                raw = http.postJson(endpoint, authHeaders(apiKey), ShortJson.toJson(body), Math.max(180, timeoutSec));
            } else {
                throw ex;
            }
        }
        Map<String, Object> parsed = ShortJson.map(raw);
        return extractImageResult(parsed, timeoutSec);
    }

    private AiProviderResult video(String baseUrl, String apiKey, String modelCode,
            Map<String, Object> cfg, AiProviderRequest request, int timeoutSec) throws Exception {
        int duration = Math.max(1, Math.min(15, ShortJson.integer(request.param("duration_sec"), 5)));
        String requestId = findSavedXaiVideoRequestId();

        if (requestId.isBlank()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelCode);
            body.put("prompt", ProviderHttp.promptOf(request));
            body.put("duration", duration);
            body.put("aspect_ratio", ProviderHttp.aspectRatio(cfg, request));
            body.put("resolution", "720p");

            String refImage = ProviderHttp.imagePayload(request);
            if (!refImage.isBlank()) {
                Map<String, Object> imageObj = new LinkedHashMap<>();
                imageObj.put("url", refImage);
                body.put("image", imageObj);
            }
            String submitRaw = http.postJson(baseUrl + "/videos/generations", authHeaders(apiKey),
                    ShortJson.toJson(body), Math.min(90, timeoutSec));
            Map<String, Object> submitted = ShortJson.map(submitRaw);
            requestId = ProviderHttp.firstNonBlank(ShortJson.text(submitted.get("request_id")),
                    ShortJson.text(submitted.get("id")));
            if (requestId.isBlank()) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "Grok 视频任务提交未返回 request_id");
            }
            saveXaiVideoRequestId(requestId);
        }

        String pollUrl = baseUrl + "/videos/" + URLEncoder.encode(requestId, StandardCharsets.UTF_8);
        long budgetMs = Math.max(VIDEO_POLL_FLOOR_MS, Math.max(120, timeoutSec) * 1000L);
        long deadline = System.currentTimeMillis() + budgetMs;

        while (true) {
            Map<String, Object> statusBody;
            try {
                String raw = http.getJson(pollUrl, authHeaders(apiKey), 60);
                statusBody = ShortJson.map(raw);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            } catch (BizException ex) {
                if (!isTransientFailure(ex) || System.currentTimeMillis() >= deadline) {
                    throw ex;
                }
                sleeper.sleep(VIDEO_POLL_INTERVAL_MS);
                continue;
            }

            String status = ShortJson.text(statusBody.get("status")).trim().toLowerCase(Locale.ROOT);
            if ("done".equals(status) || "succeeded".equals(status) || "completed".equals(status)) {
                return extractVideoResult(statusBody, duration, timeoutSec);
            }
            if ("failed".equals(status) || "expired".equals(status) || "error".equals(status)
                    || "cancelled".equals(status) || "canceled".equals(status)) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "Grok 视频生成失败");
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, "Grok 视频生成超时，请稍后重试");
            }
            sleeper.sleep(VIDEO_POLL_INTERVAL_MS);
        }
    }

    private AiProviderResult tts(String baseUrl, String apiKey, AiProviderRequest request, int timeoutSec)
            throws Exception {
        String rawVoice = ProviderHttp.firstNonBlank(ShortJson.text(request.param("voice_id")),
                ShortJson.text(request.param("voice")), "ara");
        String voiceId = mapGrokVoiceId(rawVoice);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", ProviderHttp.promptOf(request));
        body.put("voice_id", voiceId);
        body.put("language", "zh");

        byte[] audioBytes = http.postBytes(baseUrl + "/tts", authHeaders(apiKey),
                ShortJson.toJson(body), Math.min(120, timeoutSec));
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "Grok 配音返回音频为空");
        }
        String mime = audioBytes.length >= 4 && audioBytes[0] == 'R' && audioBytes[1] == 'I'
                && audioBytes[2] == 'F' && audioBytes[3] == 'F' ? "audio/wav" : "audio/mpeg";
        return AiProviderResult.binary(audioBytes, mime);
    }

    private AiProviderResult stt(String baseUrl, String apiKey, String modelCode,
            AiProviderRequest request, int timeoutSec) throws Exception {
        byte[] audio = audioPayload(request, timeoutSec);
        if (audio == null || audio.length == 0) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "audio input required");
        }
        String mime = ProviderHttp.firstNonBlank(ShortJson.text(request.param("audio_mime")),
                audio.length >= 4 && audio[0] == 'R' && audio[1] == 'I' && audio[2] == 'F' && audio[3] == 'F'
                        ? "audio/wav" : "audio/mpeg");
        String filename = mime.contains("wav") ? "audio.wav" : "audio.mp3";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", modelCode);
        fields.put("format", "true");
        fields.put("language", ProviderHttp.firstNonBlank(ShortJson.text(request.param("language")), "zh"));

        String raw = http.postMultipart(baseUrl + "/stt", authHeaders(apiKey),
                audio, filename, mime, fields, Math.min(300, Math.max(60, timeoutSec)));
        Map<String, Object> parsed = new LinkedHashMap<>(ShortJson.map(raw));
        normalizeSttSentences(parsed);
        AiProviderResult result = AiProviderResult.text(ShortJson.toJson(parsed));
        double dur = numberValue(parsed.get("duration"));
        if (dur > 0) {
            int sec = (int) Math.ceil(dur);
            result.setDurationSec(sec);
            result.setUsageAmount(Math.max(1, sec));
        }
        return result;
    }

    private AiProviderResult extractImageResult(Map<String, Object> resp, int timeoutSec) throws Exception {
        Object dataObj = resp.get("data");
        if (dataObj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            String b64 = ShortJson.text(first.get("b64_json"));
            if (!b64.isBlank()) {
                return AiProviderResult.binary(decodeBase64(b64), "image/png");
            }
            String url = ShortJson.text(first.get("url"));
            if (!url.isBlank()) {
                UserAiModelSupport.validatePublicHttpsUrl(url, "image_url");
                byte[] bytes = http.getBytes(url, Math.min(180, timeoutSec));
                return AiProviderResult.binary(bytes, "image/png");
            }
        }
        throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "Grok 图片生成返回为空");
    }

    private AiProviderResult extractVideoResult(Map<String, Object> statusBody, int fallbackDuration, int timeoutSec)
            throws Exception {
        String url = "";
        int duration = fallbackDuration;
        Object videoObj = statusBody.get("video");
        if (videoObj instanceof Map<?, ?> videoMap) {
            url = ShortJson.text(videoMap.get("url"));
            int parsedDur = ShortJson.integer(videoMap.get("duration"), 0);
            if (parsedDur > 0) {
                duration = parsedDur;
            }
        }
        if (url.isBlank()) {
            url = ProviderHttp.firstNonBlank(ShortJson.text(statusBody.get("video_url")),
                    ShortJson.text(statusBody.get("url")));
        }
        if (url.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "Grok 视频完成但缺少下载链接");
        }
        UserAiModelSupport.validatePublicHttpsUrl(url, "video_url");
        byte[] bytes = http.getBytes(url, Math.min(300, timeoutSec));
        AiProviderResult res = AiProviderResult.binary(bytes, "video/mp4");
        res.setDurationSec(duration);
        res.setUsageAmount(duration);
        return res;
    }

    private byte[] audioPayload(AiProviderRequest request, int timeoutSec) throws Exception {
        Object direct = request.param("audio_bytes");
        if (direct instanceof byte[] bytes && bytes.length > 0) {
            return bytes;
        }
        String b64 = ShortJson.text(request.param("audio_base64"));
        if (!b64.isBlank()) {
            return decodeBase64(b64);
        }
        String inputUrl = ShortJson.text(request.param("input_url"));
        if (!inputUrl.isBlank()) {
            UserAiModelSupport.validatePublicHttpsUrl(inputUrl, "input_url");
            return http.getBytes(inputUrl, Math.min(180, timeoutSec));
        }
        return new byte[0];
    }

    private static String currentTaskId() {
        UsageScope scope = UsageScope.current();
        return scope == null ? null : scope.taskRef();
    }

    private String findSavedXaiVideoRequestId() {
        String taskId = currentTaskId();
        if (shortAiTaskMapper == null || taskId == null || taskId.isBlank()) {
            return "";
        }
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskIdEqualTo(taskId);
        ShortAiTask row = shortAiTaskMapper.selectOneByExample(ex);
        if (row == null) {
            return "";
        }
        return ShortJson.text(ShortJson.map(row.getRequestJson()).get("xai_video_request_id"));
    }

    private void saveXaiVideoRequestId(String requestId) {
        String taskId = currentTaskId();
        if (shortAiTaskMapper == null || taskId == null || taskId.isBlank() || requestId == null || requestId.isBlank()) {
            return;
        }
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskIdEqualTo(taskId);
        ShortAiTask row = shortAiTaskMapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        Map<String, Object> req = new LinkedHashMap<>(ShortJson.map(row.getRequestJson()));
        req.put("xai_video_request_id", requestId);
        ShortAiTask patch = new ShortAiTask();
        patch.setId(row.getId());
        patch.setRequestJson(ShortJson.toJson(req));
        patch.setUpdatedTime(new Date());
        shortAiTaskMapper.updateByPrimaryKeySelective(patch);
    }

    private static void normalizeSttSentences(Map<String, Object> parsed) {
        if (parsed.get("sentences") instanceof List<?> existing && !existing.isEmpty()) {
            return;
        }
        List<Map<String, Object>> sentences = new ArrayList<>();
        if (parsed.get("words") instanceof List<?> words && !words.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            int segStart = -1;
            int segEnd = 0;
            for (Object item : words) {
                if (!(item instanceof Map<?, ?> w)) {
                    continue;
                }
                String txt = ShortJson.text(w.get("text"));
                if (txt.isBlank()) {
                    continue;
                }
                int startMs = Math.max(0, (int) Math.round(numberValue(w.get("start")) * 1000.0));
                int endMs = Math.max(startMs + 50, (int) Math.round(numberValue(w.get("end")) * 1000.0));
                if (segStart < 0) {
                    segStart = startMs;
                }
                buf.append(txt);
                segEnd = endMs;
                boolean sentenceBreak = txt.endsWith("。") || txt.endsWith("！") || txt.endsWith("？")
                        || txt.endsWith(".") || txt.endsWith("!") || txt.endsWith("?")
                        || buf.length() >= 24;
                if (sentenceBreak) {
                    sentences.add(sentenceRow(segStart, segEnd, buf.toString().trim()));
                    buf.setLength(0);
                    segStart = -1;
                }
            }
            if (buf.length() > 0 && segStart >= 0) {
                sentences.add(sentenceRow(segStart, Math.max(segStart + 100, segEnd), buf.toString().trim()));
            }
        } else {
            String fullText = ShortJson.text(parsed.get("text")).trim();
            if (!fullText.isBlank()) {
                double dur = numberValue(parsed.get("duration"));
                int endMs = dur > 0 ? Math.max(500, (int) Math.round(dur * 1000.0)) : 3000;
                sentences.add(sentenceRow(0, endMs, fullText));
            }
        }
        if (!sentences.isEmpty()) {
            parsed.put("sentences", sentences);
        }
    }

    private static Map<String, Object> sentenceRow(int beginMs, int endMs, String text) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("begin_ms", beginMs);
        row.put("end_ms", Math.max(beginMs + 100, endMs));
        row.put("text", text);
        return row;
    }

    private static String mapGrokVoiceId(String voice) {
        String lower = voice == null ? "" : voice.trim().toLowerCase(Locale.ROOT);
        if ("eve".equals(lower) || "ara".equals(lower) || "rex".equals(lower)
                || "sal".equals(lower) || "leo".equals(lower)) {
            return lower;
        }
        if (lower.contains("cold") || lower.contains("female-01")) {
            return "eve";
        }
        if (lower.contains("warm") || lower.contains("female")) {
            return "ara";
        }
        if (lower.contains("deep")) {
            return "rex";
        }
        if (lower.contains("bright") || lower.contains("male")) {
            return "leo";
        }
        if (lower.contains("narrator")) {
            return "sal";
        }
        return "ara";
    }

    private static Map<String, String> authHeaders(String apiKey) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    private static String extractChatContent(Map<String, Object> resp) {
        Object choices = resp.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> choice)) {
            return "";
        }
        Object msgObj = choice.get("message");
        if (msgObj instanceof Map<?, ?> msg && msg.get("content") != null) {
            return String.valueOf(msg.get("content")).trim();
        }
        Object text = choice.get("text");
        return text == null ? "" : String.valueOf(text).trim();
    }

    private static int nestedInt(Map<String, Object> root, String outerKey, String innerKey, int fallback) {
        if (root == null) {
            return fallback;
        }
        Object outer = root.get(outerKey);
        if (outer instanceof Map<?, ?> map) {
            return ShortJson.integer(map.get(innerKey), fallback);
        }
        return fallback;
    }

    private static double numberValue(Object raw) {
        if (raw instanceof Number num) {
            return num.doubleValue();
        }
        try {
            return raw == null ? 0.0 : Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static byte[] decodeBase64(String raw) {
        String payload = raw.trim();
        int comma = payload.indexOf(',');
        if (payload.startsWith("data:") && comma >= 0) {
            payload = payload.substring(comma + 1);
        }
        return Base64.getDecoder().decode(payload);
    }

    private static boolean isTransientFailure(BizException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        return msg.contains("暂时不可用") || msg.contains("502") || msg.contains("503") || msg.contains("504");
    }

    private static String xaiApiBase(String normalizedBaseUrl) {
        String path = java.net.URI.create(normalizedBaseUrl).getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return normalizedBaseUrl + "/v1";
        }
        return normalizedBaseUrl;
    }
}
