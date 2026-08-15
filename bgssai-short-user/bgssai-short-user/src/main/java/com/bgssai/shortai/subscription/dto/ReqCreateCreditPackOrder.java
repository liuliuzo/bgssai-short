package com.bgssai.shortai.subscription.dto;

public class ReqCreateCreditPackOrder {
    private String packCode;
    private String paymentChannel;

    public String getPackCode() { return packCode; }
    public void setPackCode(String packCode) { this.packCode = packCode; }

    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }

}
