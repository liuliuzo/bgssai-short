package com.bgssai.shortai.payment.channel;

import java.util.Map;

public interface PaymentChannel {

    String channelCode();

    PrepayResult createPayment(PaymentIntent intent, String tradeNo);

    NotifyResult verifyAndParseNotify(NotifyContext context);

    default NotifyResult verifyAndParseNotify(String rawBody, Map<String, String> headers) {
        return verifyAndParseNotify(NotifyContext.ofBody(rawBody, headers));
    }

    /**
     * Query whether the channel already paid this out_trade_no.
     * Returns the channel trade no, or null. Sandbox / missing creds / exceptions must not throw.
     */
    default String queryPaidChannelTradeNo(String outTradeNo) {
        return null;
    }

}
