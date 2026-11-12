package com.bgssai.shortai.elasticsearch;

import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PreDestroy;

/**
 * 懒连接现网 Elasticsearch。连不上时返回 null，定妆检索回落到库内名称匹配。
 */
@Component
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchClientHolder {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchClientHolder.class);

    private final ElasticsearchProperties properties;

    private final Object lock = new Object();

    private volatile ElasticsearchClient client;

    private volatile RestClient restClient;

    private volatile boolean initAttempted;

    public ElasticsearchClientHolder(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    public ElasticsearchProperties properties() {
        return properties;
    }

    public ElasticsearchClient client() {
        if (!properties.isEnabled()) {
            return null;
        }
        ElasticsearchClient existing = client;
        if (existing != null) {
            return existing;
        }
        synchronized (lock) {
            if (client != null) {
                return client;
            }
            if (initAttempted) {
                return null;
            }
            initAttempted = true;
            try {
                RestClient built = buildRestClient();
                this.restClient = built;
                this.client = new ElasticsearchClient(new RestClientTransport(built, new JacksonJsonpMapper()));
                log.info("Elasticsearch client ready index={}", properties.lookIndex());
                return this.client;
            } catch (Exception ex) {
                log.warn("Elasticsearch client init failed type={} message={}",
                        ex.getClass().getSimpleName(), ex.getMessage());
                closeQuietly();
                return null;
            }
        }
    }

    @PreDestroy
    public void close() {
        closeQuietly();
    }

    private RestClient buildRestClient() {
        String raw = properties.getUris() == null ? "" : properties.getUris().trim();
        if (raw.isEmpty()) {
            throw new IllegalStateException("bgssai.elasticsearch.uris is blank");
        }
        URI uri = URI.create(raw);
        String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : ("http".equalsIgnoreCase(scheme) ? 80 : 443);
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("bgssai.elasticsearch.uris has no host");
        }
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));
        builder.setRequestConfigCallback(requestConfig -> requestConfig
                .setConnectTimeout(properties.getConnectTimeoutMs())
                .setSocketTimeout(properties.getSocketTimeoutMs()));
        builder.setHttpClientConfigCallback(this::configureHttp);
        return builder.build();
    }

    private HttpAsyncClientBuilder configureHttp(HttpAsyncClientBuilder http) {
        String user = properties.getUsername() == null ? "" : properties.getUsername().trim();
        String pass = properties.getPassword() == null ? "" : properties.getPassword();
        if (!user.isEmpty()) {
            BasicCredentialsProvider creds = new BasicCredentialsProvider();
            creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
            http.setDefaultCredentialsProvider(creds);
        }
        if (properties.isSkipTlsVerification()) {
            try {
                SSLContext ssl = SSLContext.getInstance("TLS");
                ssl.init(null, new TrustManager[] { TRUST_ALL }, new SecureRandom());
                http.setSSLContext(ssl);
                http.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            } catch (Exception ex) {
                throw new IllegalStateException("failed to disable elasticsearch tls verification", ex);
            }
        }
        return http;
    }

    private void closeQuietly() {
        client = null;
        RestClient current = restClient;
        restClient = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception ex) {
                log.warn("Elasticsearch client close failed type={}", ex.getClass().getSimpleName());
            }
        }
    }

    private static final TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
