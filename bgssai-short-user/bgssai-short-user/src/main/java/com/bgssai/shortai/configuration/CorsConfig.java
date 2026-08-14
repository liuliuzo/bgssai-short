package com.bgssai.shortai.configuration;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS configuration. Allowed origins are configured per profile via bgssai.cors.allowed-origins.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${bgssai.cors.allowed-origins:https://app.example.com}")
    private String allowedOrigins;

    private static final List<String> METHODS = Arrays.asList("GET", "POST", "OPTIONS");

    private static final List<String> HEADERS = Arrays.asList(
        "Content-Type",
        "Accept",
        "Authorization",
        "jwtToken",
        "Jwttoken",
        "X-Requested-With"
    );

    @Bean
    CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split("\\s*,\\s*"));
        for (String origin : origins) {
            if (origin == null || origin.isEmpty()) continue;
            if ("*".equals(origin)) {
                throw new IllegalStateException(
                    "bgssai.cors.allowed-origins must not be '*' — use explicit origin whitelist");
            }
            config.addAllowedOrigin(origin);
        }
        config.setAllowedMethods(METHODS);
        config.setAllowedHeaders(HEADERS);
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        log.info("[CorsConfig] CORS configured allowedOrigins={} methods={} headers={}",
                origins, METHODS, HEADERS);
        return new CorsFilter(source);
    }
}
