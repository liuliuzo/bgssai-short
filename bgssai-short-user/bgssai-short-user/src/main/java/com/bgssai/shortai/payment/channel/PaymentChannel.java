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
     * Returns trade no + amount (fen), or null. Sandbox / missing creds / exceptions must not throw.
     * Amount must be present for grant compensation (fail-closed).
     */
    default ChannelPaidQuery queryPaidOrder(String outTradeNo) {
        return null;
    }

}
