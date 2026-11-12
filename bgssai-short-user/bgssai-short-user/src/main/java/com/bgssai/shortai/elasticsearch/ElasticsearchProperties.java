package com.bgssai.shortai.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 开发共用池 Elasticsearch。连接参数只写各环境 properties，不入库、不进管理端。
 */
@ConfigurationProperties(prefix = "bgssai.elasticsearch")
public class ElasticsearchProperties {

    private boolean enabled = false;

    private String uris = "";

    private String username = "";

    private String password = "";

    private boolean skipTlsVerification = true;

    private String indexPrefix = "short-local";

    private int connectTimeoutMs = 2000;

    private int socketTimeoutMs = 5000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUris() {
        return uris;
    }

    public void setUris(String uris) {
        this.uris = uris;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSkipTlsVerification() {
        return skipTlsVerification;
    }

    public void setSkipTlsVerification(boolean skipTlsVerification) {
        this.skipTlsVerification = skipTlsVerification;
    }

    public String getIndexPrefix() {
        return indexPrefix;
    }

    public void setIndexPrefix(String indexPrefix) {
        this.indexPrefix = indexPrefix;
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

    public String lookIndex() {
        String prefix = indexPrefix == null ? "" : indexPrefix.trim();
        if (prefix.isEmpty()) {
            prefix = "short-local";
        }
        return prefix + "-look";
    }
}
