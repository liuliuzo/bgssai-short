package com.bgssai.shortai.storage;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;

/**
 * Huawei Cloud OBS via esdk-obs-java.
 */
public class HuaweiObsObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(HuaweiObsObjectStorage.class);

    private final ObsClient client;
    private final String bucket;

    public HuaweiObsObjectStorage(String endpoint, String accessKey, String secretKey, String bucket,
            int connectTimeoutMs, int socketTimeoutMs, int maxErrorRetry) {
        // 必须显式设超时：SDK 默认值在 OBS 不可达时会让 putObject 长时间不返回，
        // 调用它的 AI 任务就一直停在 RUNNING。
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endpoint);
        config.setConnectionTimeout(connectTimeoutMs);
        config.setSocketTimeout(socketTimeoutMs);
        config.setConnectionRequestTimeout(connectTimeoutMs);
        config.setMaxErrorRetry(maxErrorRetry);
        this.client = new ObsClient(accessKey, secretKey, config);
        this.bucket = bucket;
    }

    @Override
    public String put(String objectKey, byte[] content, String mimeType) {
        byte[] body = content == null ? new byte[0] : content;
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength((long) body.length);
        if (mimeType != null && !mimeType.isBlank()) {
            meta.setContentType(mimeType.trim());
        }
        client.putObject(bucket, objectKey, new ByteArrayInputStream(body), meta);
        return objectKey;
    }

    @Override
    public byte[] get(String objectKey) {
        ObsObject obj = client.getObject(bucket, objectKey);
        if (obj == null || obj.getObjectContent() == null) {
            return null;
        }
        try (java.io.InputStream in = obj.getObjectContent()) {
            return in.readAllBytes();
        } catch (Exception ex) {
            throw new IllegalStateException("obs get failed: " + objectKey, ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.deleteObject(bucket, objectKey);
        } catch (Exception ex) {
            log.warn("obs delete failed key={}", objectKey, ex);
        }
    }

    @Override
    public PresignResult presignGet(String objectKey, int expireSeconds) {
        int ttl = Math.max(60, expireSeconds);
        TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.GET, ttl);
        req.setBucketName(bucket);
        req.setObjectKey(objectKey);
        Map<String, String> headers = new HashMap<>();
        req.setHeaders(headers);
        TemporarySignatureResponse resp = client.createTemporarySignature(req);
        Instant expireAt = Instant.now().plusSeconds(ttl);
        return new PresignResult(resp.getSignedUrl(), expireAt);
    }
}
