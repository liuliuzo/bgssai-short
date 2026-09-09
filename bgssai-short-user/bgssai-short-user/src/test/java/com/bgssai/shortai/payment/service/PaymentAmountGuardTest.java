package com.bgssai.shortai.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;

class PaymentAmountGuardTest {

    @Test
    void nullChannelAmountIsNotMatch() {
        assertFalse(PaymentAmountGuard.matches(9900, null));
        BizException ex = assertThrows(BizException.class,
                () -> PaymentAmountGuard.requireMatch("o1", 9900, null));
        assertEquals("60005", ex.getCode());
    }

    @Test
    void equalAmountsMatch() {
        assertTrue(PaymentAmountGuard.matches(9900, 9900));
        PaymentAmountGuard.requireMatch("o1", 9900, 9900);
    }
}
