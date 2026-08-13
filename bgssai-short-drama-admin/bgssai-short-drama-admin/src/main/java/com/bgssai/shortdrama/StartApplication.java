package com.bgssai.shortdrama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin console backend (control-plane). Runs on port 8080.
 * Shares one MySQL database with the user-app backend (bgssai-short-drama-user, port 8081).
 * Only the admin role is served; protected endpoints are guarded by @NeedAop + AdminAuthGuard.
 */
@SpringBootApplication
public class StartApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }
}
