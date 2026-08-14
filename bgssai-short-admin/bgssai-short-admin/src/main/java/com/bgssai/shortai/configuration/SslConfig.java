package com.bgssai.shortai.configuration;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP -&gt; HTTPS redirect when SSL is enabled: the app serves HTTPS on {@code server.port}
 * and opens a plain connector on {@code server.http.port} that 302-redirects to HTTPS.
 * Admin and user deploy on separate hosts, so both use the same web/SSL ports (80 / 443).
 * When {@code server.http.port} equals {@code server.port}, skip the extra connector
 * (single-port / local smoke) to avoid BindException.
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class SslConfig {

    private static final Logger log = LoggerFactory.getLogger(SslConfig.class);

    private final int httpPort;
    private final int httpsPort;

    public SslConfig(
            @Value("${server.http.port:80}") int httpPort,
            @Value("${server.port:443}") int httpsPort) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    @Bean
    TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint constraint = new SecurityConstraint();
                constraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                constraint.addCollection(collection);
                context.addConstraint(constraint);
            }
        };
        if (httpPort != httpsPort) {
            tomcat.addAdditionalConnectors(httpConnector());
        } else {
            log.info("[SSL] server.http.port ({}) equals server.port; serving HTTPS only", httpPort);
        }
        return tomcat;
    }

    @Bean
    Connector httpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        return connector;
    }
}
