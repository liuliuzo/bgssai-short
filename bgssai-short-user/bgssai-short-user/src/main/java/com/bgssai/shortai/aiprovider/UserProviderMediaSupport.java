package com.bgssai.shortai.aiprovider;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bgssai.shortai.billing.UsageScope;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;

/**
 * Shared helpers for user-supplied API key providers (resume tracking, media decoding, STT normalization).
 */
final class UserProviderMediaSupport {

    private UserProviderMediaSupport() {
    }

    static String currentTaskId() {
        UsageScope scope = UsageScope.current();
        return scope == null ? null : scope.taskRef();
    }

    static String findTaskParam(ShortAiTaskMapper mapper, String key) {
        String taskId = currentTaskId();
        if (mapper == null || taskId == null || taskId.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskIdEqualTo(taskId);
        ShortAiTask row = mapper.selectOneByExample(ex);
        if (row == null) {
            return "";
        }
        return ShortJson.text(ShortJson.map(row.getRequestJson()).get(key));
    }

    static void saveTaskParams(ShortAiTaskMapper mapper, Map<String, String> values) {
        String taskId = currentTaskId();
        if (mapper == null || taskId == null || taskId.isBlank() || values == null || values.isEmpty()) {
            return;
        }
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskIdEqualTo(taskId);
        ShortAiTask row = mapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        Map<String, Object> req = new LinkedHashMap<>(ShortJson.map(row.getRequestJson()));
        boolean changed = false;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isBlank()) {
                req.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        ShortAiTask patch = new ShortAiTask();
        patch.setId(row.getId());
        patch.setRequestJson(ShortJson.toJson(req));
        patch.setUpdatedTime(new Date());
        mapper.updateByPrimaryKeySelective(patch);
    }

    static String extractChatContent(Map<String, Object> parsed) {
        if (parsed == null) {
            return "";
        }
        Object choices = parsed.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object msg = first.get("message");
            if (msg instanceof Map<?, ?> msgMap) {
                String content = ShortJson.text(msgMap.get("content"));
                if (!content.isBlank()) {
                    return content.trim();
                }
            }
            String text = ShortJson.text(first.get("text"));
            if (!text.isBlank()) {
                return text.trim();
            }
        }
        return ShortJson.text(parsed.get("output_text")).trim();
    }

    static int nestedInt(Map<String, Object> root, String outerKey, String innerKey, int fallback) {
        if (root == null) {
            return fallback;
        }
        Object outer = root.get(outerKey);
        if (outer instanceof Map<?, ?> map) {
            return ShortJson.integer(map.get(innerKey), fallback);
        }
        return fallback;
    }

    static String nestedText(Map<String, Object> root, String outerKey, String innerKey) {
        if (root == null) {
            return "";
        }
        Object outer = root.get(outerKey);
        if (outer instanceof Map<?, ?> map) {
            return ShortJson.text(map.get(innerKey));
        }
        return "";
    }

    static double numberValue(Object raw) {
        if (raw instanceof Number num) {
            return num.doubleValue();
        }
        try {
            return raw == null ? 0.0 : Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    static byte[] decodeBase64(String raw) {
        String payload = raw.trim();
        int comma = payload.indexOf(',');
        if (payload.startsWith("data:") && comma >= 0) {
            payload = payload.substring(comma + 1);
        }
        return Base64.getDecoder().decode(payload);
    }

    static String stripDataUrlPrefix(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int comma = trimmed.indexOf(',');
        if (trimmed.startsWith("data:") && comma >= 0) {
            return trimmed.substring(comma + 1);
        }
        return trimmed;
    }

    static byte[] decodeHexOrBase64(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model audio missing");
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("data:")) {
            return decodeBase64(trimmed);
        }
        try {
            return decodeHex(trimmed);
        } catch (IllegalArgumentException ignored) {
            try {
                return Base64.getDecoder().decode(trimmed);
            } catch (IllegalArgumentException ex) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "model audio encoding invalid");
            }
        }
    }

    static byte[] decodeHex(String value) {
        if (value == null || (value.length() & 1) == 1) {
            throw new IllegalArgumentException("invalid hex");
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("invalid hex");
            }
            bytes[i] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    static byte[] audioPayload(AiProviderRequest request, ProviderHttp.Client http, int timeoutSec)
            throws Exception {
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

    static byte[] resolveImageBytes(AiProviderRequest request, ProviderHttp.Client http, boolean lastFrame,
            int timeoutSec) throws Exception {
        String b64Key = lastFrame ? "last_image_base64" : "image_base64";
        String urlKey = lastFrame ? "last_image_url" : "image_url";
        String b64 = ShortJson.text(request.param(b64Key));
        if (!b64.isBlank()) {
            return decodeBase64(b64);
        }
        String url = ShortJson.text(request.param(urlKey));
        if (!url.isBlank()) {
            UserAiModelSupport.validatePublicHttpsUrl(url, urlKey);
            return http.getBytes(url, Math.min(120, timeoutSec));
        }
        return new byte[0];
    }

    static String guessAudioMime(byte[] bytes) {
        if (bytes != null && bytes.length >= 4 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F') {
            return "audio/wav";
        }
        return "audio/mpeg";
    }

    static String guessImageMime(String url) {
        String lower = url == null ? "" : url.toLowerCase();
        if (lower.contains(".jpg") || lower.contains(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    static boolean isTransientFailure(BizException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        return msg.contains("暂时不可用") || msg.contains("502") || msg.contains("503") || msg.contains("504");
    }

    static void normalizeSttSentences(Map<String, Object> parsed) {
        if (parsed.get("sentences") instanceof List<?> existing && !existing.isEmpty()) {
            return;
        }
        List<Map<String, Object>> sentences = new ArrayList<>();
        if (parsed.get("segments") instanceof List<?> segments && !segments.isEmpty()) {
            for (Object item : segments) {
                if (!(item instanceof Map<?, ?> seg)) {
                    continue;
                }
                String txt = ShortJson.text(seg.get("text")).trim();
                if (txt.isBlank()) {
                    continue;
                }
                int beginMs = Math.max(0, (int) Math.round(numberValue(seg.get("start")) * 1000.0));
                int endMs = Math.max(beginMs + 50, (int) Math.round(numberValue(seg.get("end")) * 1000.0));
                sentences.add(sentenceItem(beginMs, endMs, txt));
            }
        }
        if (sentences.isEmpty() && parsed.get("words") instanceof List<?> words && !words.isEmpty()) {
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
                if (endsSentence(txt) || buf.length() >= 36) {
                    sentences.add(sentenceItem(segStart, segEnd, buf.toString().trim()));
                    buf.setLength(0);
                    segStart = -1;
                }
            }
            if (buf.length() > 0 && segStart >= 0) {
                sentences.add(sentenceItem(segStart, Math.max(segStart + 50, segEnd), buf.toString().trim()));
            }
        }
        if (sentences.isEmpty()) {
            String fullText = ShortJson.text(parsed.get("text")).trim();
            if (!fullText.isBlank()) {
                int durMs = Math.max(1000, (int) Math.round(numberValue(parsed.get("duration")) * 1000.0));
                sentences.add(sentenceItem(0, durMs, fullText));
            }
        }
        if (!sentences.isEmpty()) {
            parsed.put("sentences", sentences);
        }
    }

    private static Map<String, Object> sentenceItem(int beginMs, int endMs, String text) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("begin_ms", beginMs);
        row.put("end_ms", endMs);
        row.put("text", text);
        return row;
    }

    private static boolean endsSentence(String txt) {
        String s = txt.trim();
        return s.endsWith("。") || s.endsWith("！") || s.endsWith("？")
                || s.endsWith(".") || s.endsWith("!") || s.endsWith("?")
                || s.endsWith("，") || s.endsWith(",");
    }

    static int ceilSeconds(int milliseconds) {
        return milliseconds <= 0 ? 0 : Math.max(1, (milliseconds + 999) / 1000);
    }
}
