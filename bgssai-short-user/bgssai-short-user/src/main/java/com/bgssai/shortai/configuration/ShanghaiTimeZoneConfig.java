package com.bgssai.shortai.configuration;

import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

/**
 * Unzoned {@code SimpleDateFormat} and {@code Calendar} follow the JVM default.
 * This product's wall clock is Asia/Shanghai even when the process starts in UTC.
 */
@Configuration
public class ShanghaiTimeZoneConfig {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }
}
