package com.bgssai.shortai.aiprovider;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import com.bgssai.shortai.production.support.ShortJson;

/**
 * Interpreter for image-to-video contract v0.1-current-20261111-1053Z.
 * <p>
 * Assumptions, until a later contract revision:
 * submit answers HTTP 202 with {@code id} and relative {@code status_url};
 * status reads stay HTTP 200 for queued, running, completed and failed;
 * {@code content} is a relative path and is fetched only after {@code status}
 * is exactly {@code completed}; 409 means not completed; 429 and 503 are not
 * success; there is no idempotency key, cancel, or TTL. This is not an
 * OpenAI-compatible video API. Provider error text and metrics are never copied
 * into the user-facing message.
 */
public final class ImageConditionedVideoProtocol {

    public static final String CONTRACT = "v0.1-current-20261111-1053Z";
    public static final int STAGE_TIMEOUT_SEC = 7200;
    public static final int DEADLINE_SEC = STAGE_TIMEOUT_SEC * 2;
    public static final int SUBMIT_TIMEOUT_SEC = 60;
    public static final int POLL_TIMEOUT_SEC = 30;
    public static final int CONTENT_TIMEOUT_SEC = 120;
    public static final Pattern JOB_ID = Pattern.compile("i2v_[0-9a-f]{32}");

    public static final String UNAVAILABLE = "图生视频暂不可用。文生视频不受影响。";
    public static final String BUSY = "图生视频正忙，请稍后再试。";
    public static final String FAILED = "图生视频失败，未生成视频。";
    public static final String TIMEOUT = "图生视频超时，未生成视频。请勿重复提交同一请求。";
    public static final String AMBIGUOUS = "图生视频提交结果不确定，请勿重复提交，请到任务中心查看原任务。";
    public static final String BAD_RESULT = "图生视频结果不是有效视频，未入账。";
    public static final String NO_CANCEL = "图生视频已提交，当前服务不支持取消。";
    public static final String NO_RETRY = "图生视频不能自动重新提交，也不会改走文生视频。要生成新视频请重新选择图片后再提交。";
    public static final String NEEDS_IMAGE = "图生视频需要图片，不会改走文生视频。";

    private ImageConditionedVideoProtocol() {
    }

    public enum SubmitKind {
        ACCEPTED, BUSY, UNAVAILABLE, REJECTED, AMBIGUOUS
    }

    public enum PollKind {
        RUNNING, COMPLETED, FAILED, TRANSIENT, TERMINAL
    }

    public enum ContentKind {
        READY, NOT_READY, FAILED
    }

    public record SubmitDecision(SubmitKind kind, String jobId, String taskCode, String userMessage) {
    }

    public record PollDecision(PollKind kind, String taskCode, String userMessage) {
    }

    public record ContentDecision(ContentKind kind, byte[] body, String taskCode, String userMessage) {
    }

    public static long pollDelayMs(int attempt) {
        long[] delays = { 5_000L, 7_000L, 10_000L, 12_000L, 15_000L };
        int index = Math.max(0, attempt);
        return index >= delays.length ? 15_000L : delays[index];
    }

    public static String statusPath(String jobId) {
        requireJobId(jobId);
        return "/v1/videos/" + jobId;
    }

    public static String contentPath(String jobId) {
        requireJobId(jobId);
        return "/v1/videos/" + jobId + "/content";
    }

    public static SubmitDecision interpretSubmit(int status, String body) {
        if (status == 202) {
            Map<String, Object> json = ShortJson.map(body);
            String jobId = ShortJson.text(json.get("id")).trim();
            String statusUrl = ShortJson.text(json.get("status_url")).trim();
            if (JOB_ID.matcher(jobId).matches() && statusPath(jobId).equals(statusUrl)) {
                return new SubmitDecision(SubmitKind.ACCEPTED, jobId, "", "");
            }
            return ambiguous();
        }
        if (status == 429) {
            return new SubmitDecision(SubmitKind.BUSY, "", "I2V_BUSY", BUSY);
        }
        if (status == 401 || status == 503) {
            return new SubmitDecision(SubmitKind.UNAVAILABLE, "", "I2V_UNAVAILABLE", UNAVAILABLE);
        }
        if (status == 400 || status == 413 || status == 422) {
            return new SubmitDecision(SubmitKind.REJECTED, "", "I2V_BAD_IMAGE",
                    status == 413 ? "图片超过 8MB，无法用于图生视频。" : "图片或参数无法用于图生视频。");
        }
        return ambiguous();
    }

    public static PollDecision interpretPoll(String jobId, int status, String body) {
        requireJobId(jobId);
        if (status < 0 || status == 429 || status == 503 || status >= 500) {
            return new PollDecision(PollKind.TRANSIENT, "", "");
        }
        if (status == 401) {
            return new PollDecision(PollKind.TERMINAL, "I2V_UNAVAILABLE", UNAVAILABLE);
        }
        if (status == 404) {
            return new PollDecision(PollKind.TERMINAL, "I2V_FAILED", "图生视频任务不存在。");
        }
        if (status != 200) {
            return new PollDecision(PollKind.TRANSIENT, "", "");
        }
        Map<String, Object> json = ShortJson.map(body);
        String state = ShortJson.text(json.get("status")).trim();
        if ("queued".equals(state) || "running".equals(state)) {
            return new PollDecision(PollKind.RUNNING, "", "");
        }
        if ("failed".equals(state)) {
            return new PollDecision(PollKind.FAILED, "I2V_FAILED", FAILED);
        }
        if ("completed".equals(state)) {
            String content = ShortJson.text(json.get("content")).trim();
            if (contentPath(jobId).equals(content)) {
                return new PollDecision(PollKind.COMPLETED, "", "");
            }
            return new PollDecision(PollKind.RUNNING, "", "");
        }
        return new PollDecision(PollKind.RUNNING, "", "");
    }

    public static ContentDecision interpretContent(int status, String contentType, byte[] body) {
        if (status == 409) {
            return new ContentDecision(ContentKind.NOT_READY, null, "", "");
        }
        if (status == 429 || status == 503 || status >= 500 || status < 0) {
            return new ContentDecision(ContentKind.NOT_READY, null, "", "");
        }
        if (status == 200 && videoMp4(contentType) && mp4(body)) {
            return new ContentDecision(ContentKind.READY, body, "", "");
        }
        return new ContentDecision(ContentKind.FAILED, null, "I2V_FAILED", BAD_RESULT);
    }

    public static boolean mp4(byte[] body) {
        if (body == null || body.length < 12) {
            return false;
        }
        int last = Math.min(body.length - 4, 64);
        for (int i = 0; i <= last; i++) {
            if (body[i] == 'f' && body[i + 1] == 't' && body[i + 2] == 'y' && body[i + 3] == 'p') {
                return true;
            }
        }
        return false;
    }

    public static void requireJobId(String jobId) {
        if (jobId == null || !JOB_ID.matcher(jobId).matches()) {
            throw new IllegalArgumentException("job id rejected");
        }
    }

    static String textBody(byte[] body) {
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private static boolean videoMp4(String contentType) {
        if (contentType == null) {
            return false;
        }
        String value = contentType.toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("video/mp4");
    }

    private static SubmitDecision ambiguous() {
        return new SubmitDecision(SubmitKind.AMBIGUOUS, "", "I2V_AMBIGUOUS", AMBIGUOUS);
    }
}
