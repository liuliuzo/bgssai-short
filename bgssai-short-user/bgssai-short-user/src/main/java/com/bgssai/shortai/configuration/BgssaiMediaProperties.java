package com.bgssai.shortai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bgssai.media")
public class BgssaiMediaProperties {

    private String baseUrl = "";

    private String ingestToken = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public String getIngestToken() {
        return ingestToken;
    }

    public void setIngestToken(String ingestToken) {
        this.ingestToken = ingestToken == null ? "" : ingestToken.trim();
    }
}
