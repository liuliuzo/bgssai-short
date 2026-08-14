package com.bgssai.shortai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin console backend (control-plane). Remote deploy: HTTPS 443 + HTTP 80 redirect.
 * Shares one MySQL database with the user-app backend (bgssai-short-user).
 * Only the admin role is served; protected endpoints are guarded by @NeedAop + AdminAuthGuard.
 */
@SpringBootApplication
public class StartApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }
}
