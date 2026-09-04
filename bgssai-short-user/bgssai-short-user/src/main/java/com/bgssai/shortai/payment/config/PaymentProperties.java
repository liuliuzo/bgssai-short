package com.bgssai.shortai.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentProperties {

    @Value("${bgssai.payment.sandbox-enabled:true}")
    private boolean sandboxEnabled;

    @Value("${bgssai.payment.notify-base-url:http://localhost:8080}")
    private String notifyBaseUrl;

    @Value("${bgssai.payment.order-timeout-minutes:30}")
    private int orderTimeoutMinutes;

    @Value("${bgssai.payment.wechat.sign-key:sandbox-wechat-sign-key}")
    private String wechatSignKey;

    @Value("${bgssai.payment.alipay.sign-key:sandbox-alipay-sign-key}")
    private String alipaySignKey;

    public boolean isSandboxEnabled() {
        return sandboxEnabled;
    }

    public String getNotifyBaseUrl() {
        return notifyBaseUrl;
    }

    public int getOrderTimeoutMinutes() {
        return orderTimeoutMinutes;
    }

    public String getWechatSignKey() {
        return wechatSignKey;
    }

    public String getAlipaySignKey() {
        return alipaySignKey;
    }
}
