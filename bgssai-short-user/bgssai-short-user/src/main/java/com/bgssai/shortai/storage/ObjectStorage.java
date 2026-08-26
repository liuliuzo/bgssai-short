package com.bgssai.shortai.storage;

import java.time.Instant;

public interface ObjectStorage {

    /**
     * Store bytes; returns the object key actually written.
     */
    String put(String objectKey, byte[] content, String mimeType);

    byte[] get(String objectKey);

    void delete(String objectKey);

    PresignResult presignGet(String objectKey, int expireSeconds);

    record PresignResult(String downloadUrl, Instant expireAt) {
    }
}
