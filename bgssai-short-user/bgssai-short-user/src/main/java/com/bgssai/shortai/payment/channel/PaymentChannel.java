package com.bgssai.shortai.payment.channel;

import java.util.Map;

public interface PaymentChannel {

    String channelCode();

    PrepayResult createPayment(PaymentIntent intent, String tradeNo);

    NotifyResult verifyAndParseNotify(NotifyContext context);

    default NotifyResult verifyAndParseNotify(String rawBody, Map<String, String> headers) {
        return verifyAndParseNotify(NotifyContext.ofBody(rawBody, headers));
    }
}
