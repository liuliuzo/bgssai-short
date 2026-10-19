package com.bgssai.shortai.configuration;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Admin console SPA (React Router history mode) deep-link fallback.
 * <p>
 * Each top-level front-end route prefix is forwarded to {@code /index.html} so deep links
 * and refreshes do not 404.
 * <p>
 * A whitelist of prefixes is used rather than {@code /**}, to avoid forwarding hashed static
 * assets ({@code /assets/*.js} / {@code /assets/*.css}) to HTML (a browser parsing HTML as JS
 * would throw SyntaxError).
 * <p>
 * {@code /bgssai/**} (business API and login endpoints) goes to Controllers and must return a
 * 404 JSON on a miss, so it is not in this list. The front-end route list is in
 * bgssai-short-admin-react src/App.jsx — when a new top-level route prefix is added there,
 * add it here too.
 */
@Configuration
public class SpaFallbackConfig implements WebMvcConfigurer {

    private final Environment environment;

    public SpaFallbackConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Top-level front-end route prefix whitelist.
     * Never add {@code /bgssai} (API) or {@code /assets} (static resources).
     */
    private static final List<String> SPA_ROUTE_PREFIXES = Arrays.asList(
            "/download",
            "/login",
            "/oauth-callback",
            "/home",
            "/login-logs",
            "/mcp-tokens",
            "/subscription",
            "/account",
            "/usage",
            "/works",
            "/tasks"
    );

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String prefix : SPA_ROUTE_PREFIXES) {
            registry.addViewController(prefix).setViewName("forward:/index.html");
            registry.addViewController(prefix + "/**").setViewName("forward:/index.html");
        }
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        Path downloadDirectory = Path.of(environment.getProperty(
                "bgssai.bot.download.directory", "downloads/bot")).toAbsolutePath().normalize();
        registry.addResourceHandler("/downloads/bot/**")
                .addResourceLocations(downloadDirectory.toUri().toString());
    }
}
