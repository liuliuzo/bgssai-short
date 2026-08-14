package com.bgssai.shortdrama.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

/**
 * Unit tests for SslConfig's connector wiring (user backend).
 *
 * When server.ssl.enabled=true the main Tomcat connector is HTTPS on server.port,
 * so a second plain-HTTP connector on server.http.port must only be added when the
 * ports differ. Remote deploy uses 80 / 443; test profile uses a single plaintext port.
 */
class SslConfigTest {

    @Test
    void singlePortLayout_addsNoExtraConnector() {
        TomcatServletWebServerFactory factory = new SslConfig(8081, 8081).servletContainer();

        assertThat(factory.getAdditionalConnectors()).isEmpty();
    }

    @Test
    void distinctPorts_addsSingleHttpRedirectConnector() {
        TomcatServletWebServerFactory factory = new SslConfig(80, 443).servletContainer();

        List<Connector> additional = factory.getAdditionalConnectors();
        assertThat(additional).hasSize(1);

        Connector http = additional.get(0);
        assertThat(http.getScheme()).isEqualTo("http");
        assertThat(http.getPort()).isEqualTo(80);
        assertThat(http.getRedirectPort()).isEqualTo(443);
        assertThat(http.getSecure()).isFalse();
    }
}
