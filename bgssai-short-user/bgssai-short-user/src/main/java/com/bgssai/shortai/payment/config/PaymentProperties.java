package com.bgssai.shortai.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentProperties {

    @Value("${bgssai.payment.notify-base-url:http://localhost:8080}")
    private String notifyBaseUrl;

    @Value("${bgssai.payment.order-timeout-minutes:30}")
    private int orderTimeoutMinutes;

    public String getNotifyBaseUrl() {
        return notifyBaseUrl;
    }

    public int getOrderTimeoutMinutes() {
        return orderTimeoutMinutes;
    }
}
