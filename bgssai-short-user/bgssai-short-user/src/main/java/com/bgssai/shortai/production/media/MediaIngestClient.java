package com.bgssai.shortai.production.media;

import java.util.Map;

public interface MediaIngestClient {

    record IngestResult(String mediaId, String playUrl, String status) {
    }

    IngestResult ingest(Map<String, Object> payload);
}
