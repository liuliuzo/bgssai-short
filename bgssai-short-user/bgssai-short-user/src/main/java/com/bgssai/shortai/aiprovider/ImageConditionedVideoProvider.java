package com.bgssai.shortai.aiprovider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.ContentDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitKind;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * Image-conditioned video adapter. The browser never sees the bearer or the
 * base URL. Calls use multipart image bytes plus the prompt; the image is not
 * appended to the prompt and a text-to-video route is not used as a fallback.
 */
@Component
public class ImageConditionedVideoProvider implements AiProvider, ImageConditionedVideoClient {

    public static final String CODE = "IMAGE_CONDITIONED_VIDEO";
    public static final String MODEL_ID = "m-bgssai-image-to-video";
    public static final String MODEL_CODE = "bgssai-video-i2v";

    private final SysSecretReader secrets;
    private final Transport transport;
    private final Sleeper sleeper;

    @Autowired
    public ImageConditionedVideoProvider(SysSecretReader secrets) {
        this(secrets, new JdkTransport(), Thread::sleep);
    }

    ImageConditionedVideoProvider(SysSecretReader secrets, Transport transport, Sleeper sleeper) {
        this.secrets = secrets;
        this.transport = transport;
        this.sleeper = sleeper == null ? ms -> {
        } : sleeper;
    }

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public boolean ready(AiModelConfig model) {
        return callable(model) && Integer.valueOf(1).equals(model.getEnabledFlag())
                && !Integer.valueOf(1).equals(model.getDeletedFlag());
    }

    @Override
    public boolean callable(AiModelConfig model) {
        return origin(model).isPresent() && !bearer(model).isBlank();
    }

    @Override
    public SubmitDecision submit(AiModelConfig model, byte[] image, String mime, String prompt,
            int width, int height, int frames, int fps, int steps, int seed) {
        Optional<ImageConditionEndpoint.Origin> base = origin(model);
        String key = bearer(model);
        if (base.isEmpty() || key.isBlank()) {
            return new SubmitDecision(SubmitKind.UNAVAILABLE, "", "I2V_UNAVAILABLE",
                    ImageConditionedVideoProtocol.UNAVAILABLE);
        }
        if (image == null || image.length == 0) {
            return new SubmitDecision(SubmitKind.REJECTED, "", "I2V_BAD_IMAGE",
                    ImageConditionedVideoProtocol.NEEDS_IMAGE);
        }
        String fileType = mime == null || !mime.contains("jpeg") ? "image/png" : "image/jpeg";
        String filename = fileType.contains("jpeg") ? "image.jpg" : "image.png";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("prompt", prompt == null ? "" : prompt);
        fields.put("width", Integer.toString(width));
        fields.put("height", Integer.toString(height));
        fields.put("num_frames", Integer.toString(frames));
        fields.put("fps", Integer.toString(fps));
        fields.put("steps", Integer.toString(steps));
        fields.put("seed", Integer.toString(seed));
        byte[] body = multipart(fields, image, filename, fileType);
        try {
            HttpReply reply = transport.send(new HttpCall("POST", base.get().value() + "/v1/videos", key, body,
                    "multipart/form-data; boundary=" + boundaryOf(body), ImageConditionedVideoProtocol.SUBMIT_TIMEOUT_SEC));
            return ImageConditionedVideoProtocol.interpretSubmit(reply.status(),
                    ImageConditionedVideoProtocol.textBody(reply.body()));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ImageConditionedVideoProtocol.interpretSubmit(-1, "");
        }
    }

    @Override
    public PollDecision poll(AiModelConfig model, String jobId) {
        Optional<ImageConditionEndpoint.Origin> base = origin(model);
        String key = bearer(model);
        if (base.isEmpty() || key.isBlank()) {
            return new PollDecision(ImageConditionedVideoProtocol.PollKind.TERMINAL, "I2V_UNAVAILABLE",
                    ImageConditionedVideoProtocol.UNAVAILABLE);
        }
        try {
            HttpReply reply = transport.send(new HttpCall("GET", base.get().value()
                    + ImageConditionedVideoProtocol.statusPath(jobId), key, null, "",
                    ImageConditionedVideoProtocol.POLL_TIMEOUT_SEC));
            return ImageConditionedVideoProtocol.interpretPoll(jobId, reply.status(),
                    ImageConditionedVideoProtocol.textBody(reply.body()));
        } catch (IOException | InterruptedException | IllegalArgumentException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ImageConditionedVideoProtocol.interpretPoll(safeJob(jobId), -1, "");
        }
    }

    @Override
    public ContentDecision fetch(AiModelConfig model, String jobId) {
        Optional<ImageConditionEndpoint.Origin> base = origin(model);
        String key = bearer(model);
        if (base.isEmpty() || key.isBlank()) {
            return new ContentDecision(ImageConditionedVideoProtocol.ContentKind.FAILED, null, "I2V_UNAVAILABLE",
                    ImageConditionedVideoProtocol.UNAVAILABLE);
        }
        try {
            HttpReply reply = transport.send(new HttpCall("GET", base.get().value()
                    + ImageConditionedVideoProtocol.contentPath(jobId), key, null, "",
                    ImageConditionedVideoProtocol.CONTENT_TIMEOUT_SEC));
            return ImageConditionedVideoProtocol.interpretContent(reply.status(), reply.contentType(), reply.body());
        } catch (IOException | InterruptedException | IllegalArgumentException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ContentDecision(ImageConditionedVideoProtocol.ContentKind.NOT_READY, null, "", "");
        }
    }

    /**
     * The shot worker must not drive this provider. A multi-hour job would block the
     * shared task pool, and a missing image must not fall through to text-to-video.
     * Image-to-video uses {@link #submit}, {@link #poll} and {@link #fetch}.
     */
    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (imageBytes(request) == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, ImageConditionedVideoProtocol.NEEDS_IMAGE);
        }
        throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, ImageConditionedVideoProtocol.UNAVAILABLE);
    }

    static byte[] multipart(Map<String, String> fields, byte[] image, String filename, String mime) {
        String boundary = "b" + java.util.UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                out.write(field.getValue().getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"image\"; filename=\"" + filename + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(image);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("multipart", ex);
        }
        return out.toByteArray();
    }

    private static String boundaryOf(byte[] body) {
        String text = new String(body, 0, Math.min(body.length, 80), StandardCharsets.ISO_8859_1);
        int start = text.indexOf("--");
        int end = text.indexOf("\r\n");
        if (start == 0 && end > 2) {
            return text.substring(2, end);
        }
        return "bgssaiI2vBoundary";
    }

    private Optional<ImageConditionEndpoint.Origin> origin(AiModelConfig model) {
        if (model == null) {
            return Optional.empty();
        }
        return ImageConditionEndpoint.admit(ShortJson.text(ShortJson.map(model.getConfigJson()).get("base_url")));
    }

    private String bearer(AiModelConfig model) {
        if (secrets == null || model == null) {
            return "";
        }
        String ref = ShortJson.text(ShortJson.map(model.getConfigJson()).get("secret_ref")).trim();
        if (ref.isBlank()) {
            ref = SysSecretReader.AI_IMAGE_TO_VIDEO_API_KEY;
        }
        if (!SysSecretReader.AI_IMAGE_TO_VIDEO_API_KEY.equals(ref)) {
            return "";
        }
        String value = secrets.get(ref);
        return value == null ? "" : value.trim();
    }

    private static byte[] imageBytes(AiProviderRequest request) {
        if (request == null) {
            return null;
        }
        String encoded = ShortJson.text(request.param("image_base64"));
        if (encoded.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int bound(AiProviderRequest request, String key, int fallback) {
        return ShortJson.integer(request.param(key), fallback);
    }

    private static String safeJob(String jobId) {
        try {
            ImageConditionedVideoProtocol.requireJobId(jobId);
            return jobId;
        } catch (IllegalArgumentException ex) {
            return "i2v_" + "0".repeat(32);
        }
    }

    private void sleep(long ms) {
        try {
            sleeper.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorCode.UPSTREAM_TIMEOUT, ImageConditionedVideoProtocol.TIMEOUT);
        }
    }

    private static BizException failure(String code, String message) {
        String safe = message == null || message.isBlank() ? ImageConditionedVideoProtocol.FAILED : message;
        if ("I2V_BUSY".equals(code)) {
            return new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED, safe);
        }
        if ("I2V_UNAVAILABLE".equals(code)) {
            return new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, safe);
        }
        if ("I2V_AMBIGUOUS".equals(code)) {
            return new BizException("I2V_AMBIGUOUS", safe);
        }
        if ("I2V_BAD_IMAGE".equals(code)) {
            return new BizException(CommonErrorCode.INVALID_PARAM, safe);
        }
        return new BizException(CommonErrorCode.UPSTREAM_ERROR, safe);
    }

    interface Transport {
        HttpReply send(HttpCall call) throws IOException, InterruptedException;
    }

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    record HttpCall(String method, String url, String bearer, byte[] body, String contentType, int timeoutSec) {
    }

    record HttpReply(int status, String contentType, byte[] body) {
    }

    static final HttpClient.Redirect REDIRECTS = HttpClient.Redirect.NEVER;

    static final class JdkTransport implements Transport {
        private final HttpClient http = HttpClient.newBuilder()
                .followRedirects(REDIRECTS)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        @Override
        public HttpReply send(HttpCall call) throws IOException, InterruptedException {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(call.url()))
                    .timeout(Duration.ofSeconds(Math.max(5, call.timeoutSec())));
            if (call.bearer() != null && !call.bearer().isBlank()) {
                builder.header("Authorization", "Bearer " + call.bearer());
            }
            if ("POST".equals(call.method())) {
                builder.header("Content-Type", call.contentType());
                builder.POST(HttpRequest.BodyPublishers.ofByteArray(call.body() == null ? new byte[0] : call.body()));
            } else {
                builder.GET();
            }
            HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            String type = response.headers().firstValue("content-type").orElse("");
            byte[] body = response.body() == null ? new byte[0] : response.body();
            return new HttpReply(response.statusCode(), type, body);
        }
    }
}
