package com.bgssai.shortai.storage;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ObjectStorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageConfiguration.class);

    @Bean
    public ObjectStorage objectStorage(ObsProperties props, Environment env) {
        boolean localProfile = Arrays.stream(env.getActiveProfiles())
                .anyMatch(p -> "local".equalsIgnoreCase(p));
        String ak = trim(props.getAccessKey());
        String sk = trim(props.getSecretKey());
        String endpoint = trim(props.getEndpoint());
        String bucket = trim(props.getBucket());
        if (!ak.isEmpty() && !sk.isEmpty() && !endpoint.isEmpty() && !bucket.isEmpty()) {
            try {
                log.info("ObjectStorage=HuaweiObs bucket={} connectMs={} socketMs={} retry={}",
                        bucket, props.getConnectTimeoutMs(), props.getSocketTimeoutMs(),
                        props.getMaxErrorRetry());
                return new HuaweiObsObjectStorage(endpoint, ak, sk, bucket,
                        props.getConnectTimeoutMs(), props.getSocketTimeoutMs(),
                        props.getMaxErrorRetry());
            } catch (Throwable ex) {
                if (!localProfile) {
                    throw new IllegalStateException(
                            "Huawei OBS init failed and LocalFile fallback is disabled outside local profile"
                                    + " (commercial gate SHORT-01)",
                            ex);
                }
                log.warn("Huawei OBS init failed, fallback to LocalFile (local profile only): {}", ex.toString());
            }
        } else if (!localProfile) {
            throw new IllegalStateException(
                    "OBS not configured (bgssai.obs.*). LocalFileObjectStorage is only allowed when"
                            + " spring.profiles.active includes local. Refusing silent local fallback"
                            + " that would look like commercial OBS success (SHORT-01).");
        }
        String base = props.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "";
        }
        log.info("ObjectStorage=LocalFile root={} (local profile only)", props.getLocalRoot());
        String sign = env.getProperty("jwt.secret", "bgssai-local-object-sign");
        return new LocalFileObjectStorage(props.getLocalRoot(), base, sign);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
