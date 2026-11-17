package com.bgssai.shortai.login.sms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class SmsTemplateParamsTest {

    @Test
    void count2IsCodeAndMinutes() {
        assertArrayEquals(
                new String[] { "839201", "5" },
                SmsTemplateParams.build(2, "Short", 42L, "839201", 5));
    }

    @Test
    void count3IsProductCodeAndMinutes() {
        assertArrayEquals(
                new String[] { "Short", "839201", "5" },
                SmsTemplateParams.build(3, "Short", 42L, "839201", 5));
    }

    @Test
    void count4SplitsProductAndSeq() {
        assertArrayEquals(
                new String[] { "Short", "42", "839201", "5" },
                SmsTemplateParams.build(4, "Short", 42L, "839201", 5));
    }

    @Test
    void invalidCountFallsBackTo2() {
        assertArrayEquals(
                new String[] { "123456", "5" },
                SmsTemplateParams.build(5, "Short", 1L, "123456", 5));
    }
}
