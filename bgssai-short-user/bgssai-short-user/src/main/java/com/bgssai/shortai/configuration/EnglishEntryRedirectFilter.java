package com.bgssai.shortai.configuration;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 在 SPA 把 {@code /en} 以 200 交给中文登录页之前，先 302 到 {@code /login}。
 * 前端路由仍保留，页面契约继续把该别名记在登录页上。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class EnglishEntryRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = EnglishEntryRedirect.pathWithinApplication(request.getRequestURI(), request.getContextPath());
        String location = EnglishEntryRedirect.location(path, request.getQueryString());
        if (location == null) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", location);
        response.setHeader("Cache-Control", "no-store");
    }
}
