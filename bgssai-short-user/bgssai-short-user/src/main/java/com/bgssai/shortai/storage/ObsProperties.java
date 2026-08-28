package com.bgssai.shortai.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bgssai.obs")
public class ObsProperties {

    private String endpoint = "";
    private String bucket = "";
    private String region = "";
    private String accessKey = "";
    private String secretKey = "";
    private String localRoot = "./data/obs-local";
    private String publicBaseUrl = "";

    /** 建连超时（毫秒）。中间件参数走 properties 分层，不入库。 */
    private int connectTimeoutMs = 10000;

    /** 读写超时（毫秒）。没有它 OBS 不可达时上传会永久挂住，任务停在 RUNNING 不再前进。 */
    private int socketTimeoutMs = 30000;

    /** SDK 自身重试次数，配合上面两个超时把一次上传的最坏耗时钉死。 */
    private int maxErrorRetry = 2;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public void setSocketTimeoutMs(int socketTimeoutMs) {
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    public void setMaxErrorRetry(int maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }
}
