package com.bgssai.shortai.production.media;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared ingest contract with bgssai-tube (short).
 */
public final class MediaIngestContract {

    public static final String SOURCE_SYSTEM = "bgssai-short";

    public static final String ASPECT_RATIO = "9:16";

    public static final String LANGUAGE = "zh-CN";

    public static final String INGEST_PATH = "/bgssai/user/tube/ingest/short";

    /**
     * @deprecated Retired endpoint path, use {@link #INGEST_PATH} instead.
     */
    @Deprecated
    public static final String DEPRECATED_INGEST_PATH = "/bgssai/user/tube/ingest/short-drama";

    public static final String TOKEN_HEADER = "X-Bgssai-Ingest-Token";

    /**
     * Extra HTTP header (body {@code idempotency_key} remains the tube UNIQUE key).
     * Tube twin currently keys on the body field; this header is sent so a future
     * header-based gate can reuse the same value.
     */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    public static final String SECRET_KEY = "bgssai.tube.ingest-token";

    private MediaIngestContract() {
    }

    public static String idempotencyKey(String workId, String episodeId, String filmId) {
        return "short:" + trim(workId) + ":" + trim(episodeId) + ":" + trim(filmId);
    }

    public static boolean isHttpUrl(String url) {
        if (url == null) {
            return false;
        }
        String v = url.trim();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }

    public static Map<String, Object> buildPayload(String workId, String episodeId, String filmId,
            String title, String coverUrl, String videoUrl, Integer durationSec, List<String> tags) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_system", SOURCE_SYSTEM);
        body.put("source_work_id", workId);
        body.put("source_episode_id", episodeId);
        body.put("source_film_id", filmId);
        // Tube catalog: Short productions land under the short directory (Long uses long).
        body.put("catalog_dir", "short");
        body.put("product_line", "short");
        body.put("publish_version", filmId);
        body.put("title", title);
        if (coverUrl != null && !coverUrl.isBlank()) {
            body.put("cover_url", coverUrl);
        }
        body.put("video_url", videoUrl);
        body.put("duration_sec", durationSec == null ? 0 : durationSec);
        body.put("aspect_ratio", ASPECT_RATIO);
        body.put("language", LANGUAGE);
        body.put("tags", tags == null ? List.of() : tags);
        // Tube packager (ShortDramaPublishJob) sends an explicit READY approved pack.
        // Omitted fields are still accepted upstream; sending them keeps the gate honest.
        body.put("status", "READY");
        body.put("approved", Boolean.TRUE);
        body.put("idempotency_key", idempotencyKey(workId, episodeId, filmId));
        return body;
    }
}
