package com.bgssai.shortai.storage;

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
        String ak = trim(props.getAccessKey());
        String sk = trim(props.getSecretKey());
        String endpoint = trim(props.getEndpoint());
        String bucket = trim(props.getBucket());
        if (!ak.isEmpty() && !sk.isEmpty() && !endpoint.isEmpty() && !bucket.isEmpty()) {
            try {
                log.info("ObjectStorage=HuaweiObs bucket={}", bucket);
                return new HuaweiObsObjectStorage(endpoint, ak, sk, bucket);
            } catch (Throwable ex) {
                log.warn("Huawei OBS init failed, fallback to LocalFile: {}", ex.toString());
            }
        }
        String base = props.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "";
        }
        log.info("ObjectStorage=LocalFile root={}", props.getLocalRoot());
        String sign = env.getProperty("jwt.secret", "bgssai-local-object-sign");
        return new LocalFileObjectStorage(props.getLocalRoot(), base, sign);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
