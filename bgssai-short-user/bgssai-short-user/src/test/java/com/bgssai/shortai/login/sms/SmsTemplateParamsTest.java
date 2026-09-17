package com.bgssai.shortai.login.sms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;

class SmsTemplateParamsTest {

    @Test
    void count2IsCodeAndMinutes() {
        assertArrayEquals(
                new String[] { "839201", "5" },
                SmsTemplateParams.build(2, "Short", 42L, "839201", 5));
    }

    @Test
    void count3JoinsProductAndSeq() {
        assertArrayEquals(
                new String[] { "Short#42", "839201", "5" },
                SmsTemplateParams.build(3, "Short", 42L, "839201", 5));
    }

    @Test
    void count4SplitsProductAndSeq() {
        assertArrayEquals(
                new String[] { "Short", "42", "839201", "5" },
                SmsTemplateParams.build(4, "Short", 42L, "839201", 5));
    }

    @Test
    void invalidCountFails() {
        BizException ex = assertThrows(
                BizException.class,
                () -> SmsTemplateParams.build(5, "Short", 1L, "123456", 5));
        assertEquals("5203", ex.getCode());
    }
}
