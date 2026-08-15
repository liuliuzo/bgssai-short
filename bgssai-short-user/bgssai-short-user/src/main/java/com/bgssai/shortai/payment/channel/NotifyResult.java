package com.bgssai.shortai.payment.channel;

public class NotifyResult {

    private final boolean verified;
    private final boolean success;
    private final String orderNo;
    private final String channelTradeNo;
    private final Integer amountCent;

    public NotifyResult(boolean verified, boolean success, String orderNo, String channelTradeNo, Integer amountCent) {
        this.verified = verified;
        this.success = success;
        this.orderNo = orderNo;
        this.channelTradeNo = channelTradeNo;
        this.amountCent = amountCent;
    }

    public static NotifyResult rejected() {
        return new NotifyResult(false, false, null, null, null);
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public Integer getAmountCent() {
        return amountCent;
    }
}
