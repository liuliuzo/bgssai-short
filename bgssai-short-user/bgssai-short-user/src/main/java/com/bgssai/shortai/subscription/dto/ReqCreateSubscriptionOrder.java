package com.bgssai.shortai.subscription.dto;

public class ReqCreateSubscriptionOrder {
    private String planCode;
    private String paymentChannel;

    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }

    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }

}
