package com.bgssai.shortai.payment.channel;

public class PrepayResult {

    private final String prepayInfo;
    private final String channelTradeNo;
    private final String payMerchantNo;

    public PrepayResult(String prepayInfo, String channelTradeNo, String payMerchantNo) {
        this.prepayInfo = prepayInfo;
        this.channelTradeNo = channelTradeNo;
        this.payMerchantNo = payMerchantNo;
    }

    public String getPrepayInfo() {
        return prepayInfo;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public String getPayMerchantNo() {
        return payMerchantNo;
    }
}
