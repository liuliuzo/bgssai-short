package com.bgssai.shortai.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.obs.services.ObsClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Runs the real OBS SDK against loopback, including its HTTP request construction. */
class HuaweiObsObjectStorageHttpTest {
    @Test
    void actualObsClientUploadsBytesWithTheApplicationDependencyGraph() throws Exception {
        AtomicReference<byte[]> uploaded = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> objectPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.getResponseHeaders().add("x-obs-api", "3.0");
                exchange.getResponseHeaders().add("x-obs-request-id", "local-obs-contract");
                if ("PUT".equals(exchange.getRequestMethod())) {
                    uploaded.set(exchange.getRequestBody().readAllBytes());
                    contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                    objectPath.set(exchange.getRequestURI().getPath());
                    exchange.getResponseHeaders().add("ETag", "\"local-etag\"");
                }
                exchange.sendResponseHeaders(200, -1);
            } finally {
                exchange.close();
            }
        });
        server.start();
        HuaweiObsObjectStorage storage = null;
        try {
            storage = new HuaweiObsObjectStorage("http://127.0.0.1:" + server.getAddress().getPort(),
                    "local-test-access", "local-test-secret", "test-bucket", 2000, 2000, 0);
            // IP endpoints use path style and skip negotiation. A real HEAD exercises the
            // same body-less request builder used by production's auth negotiation.
            ((ObsClient) ReflectionTestUtils.getField(storage, "client"))
                    .getBucketMetadata(new com.obs.services.model.BucketMetadataInfoRequest("test-bucket"));
            byte[] content = "real-sdk-upload-contract".getBytes(StandardCharsets.UTF_8);
            assertEquals("short/test/image.png", storage.put("short/test/image.png", content, "image/png"));
            assertArrayEquals(content, uploaded.get());
            assertEquals("image/png", contentType.get());
            assertTrue(objectPath.get().endsWith("/short/test/image.png"));
        } finally {
            if (storage != null) ((ObsClient) ReflectionTestUtils.getField(storage, "client")).close();
            server.stop(0);
        }
    }
}
