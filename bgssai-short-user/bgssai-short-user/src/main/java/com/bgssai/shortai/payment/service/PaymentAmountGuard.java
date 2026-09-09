package com.bgssai.shortai.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bgssai.shortai.common.exception.BizException;

/**
 * Fail-closed amount comparison for notify and channel-query settlement.
 * Missing amount is never treated as a match.
 */
public final class PaymentAmountGuard {

    private static final Logger log = LoggerFactory.getLogger(PaymentAmountGuard.class);

    private PaymentAmountGuard() {
        throw new IllegalStateException("Utility class");
    }

    public static void requireMatch(String orderNo, Integer orderAmountCent, Integer channelAmountCent) {
        if (orderAmountCent == null || channelAmountCent == null
                || !orderAmountCent.equals(channelAmountCent)) {
            log.warn("payment amount mismatch orderNo={} orderFen={} channelFen={}",
                    orderNo, orderAmountCent, channelAmountCent);
            throw new BizException("60005", "notify amount mismatch");
        }
    }

    public static boolean matches(Integer orderAmountCent, Integer channelAmountCent) {
        return orderAmountCent != null
                && channelAmountCent != null
                && orderAmountCent.equals(channelAmountCent);
    }
}
