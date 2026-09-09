package com.bgssai.shortai.payment.channel;

/**
 * Result of a channel order query used for notify-miss compensation.
 * Both trade no and amount (fen) are required before grant.
 */
public final class ChannelPaidQuery {

    private final String channelTradeNo;
    private final Integer amountCent;

    private ChannelPaidQuery(String channelTradeNo, Integer amountCent) {
        this.channelTradeNo = channelTradeNo;
        this.amountCent = amountCent;
    }

    public static ChannelPaidQuery of(String channelTradeNo, Integer amountCent) {
        if (channelTradeNo == null || channelTradeNo.isBlank() || amountCent == null) {
            return null;
        }
        return new ChannelPaidQuery(channelTradeNo, amountCent);
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public Integer getAmountCent() {
        return amountCent;
    }
}
