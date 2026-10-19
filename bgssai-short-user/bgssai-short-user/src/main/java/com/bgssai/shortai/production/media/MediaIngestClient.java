package com.bgssai.shortai.production.media;

import java.util.Map;

public interface MediaIngestClient {

    record IngestResult(String mediaId, String playUrl, String status, Integer httpStatus, int attempts,
            String message) {
        public IngestResult(String mediaId, String playUrl, String status, Integer httpStatus, int attempts) {
            this(mediaId, playUrl, status, httpStatus, attempts, "");
        }

        public IngestResult(String mediaId, String playUrl, String status) {
            this(mediaId, playUrl, status, 200, 1, "");
        }
    }

    IngestResult ingest(Map<String, Object> payload);

    /**
     * Read the receiver row for an existing idempotency key. Must not create a work.
     * {@code null} means the client cannot tell, which the publisher stores as UNKNOWN.
     */
    IngestResult lookup(String idempotencyKey);
}
