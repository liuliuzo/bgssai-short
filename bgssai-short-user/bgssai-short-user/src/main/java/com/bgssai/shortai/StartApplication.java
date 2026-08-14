package com.bgssai.shortai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User-app backend (business plane). Remote deploy: HTTPS 443 + HTTP 80 redirect.
 * Shares one MySQL database with the admin-console backend (bgssai-short-admin).
 * Serves the normal user role; protected endpoints are guarded by @NeedAop.
 */
@SpringBootApplication
public class StartApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }
}
