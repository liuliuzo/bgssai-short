package com.bgssai.shortdrama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User-app backend (business plane). Runs on port 8081.
 * Shares one MySQL database with the admin-console backend (bgssai-short-drama-admin, port 8080).
 * Serves the normal user role; protected endpoints are guarded by @NeedAop.
 */
@SpringBootApplication
public class StartApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }
}
