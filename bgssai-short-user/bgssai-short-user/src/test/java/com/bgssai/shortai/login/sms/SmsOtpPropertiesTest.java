package com.bgssai.shortai.login.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SmsOtpPropertiesTest {

    @Test
    void defaultsToShortAndCount2() {
        SmsOtpProperties props = new SmsOtpProperties("", 2);
        assertEquals("Short", props.getProductLabel());
        assertEquals(2, props.getTemplateParamCount());
    }

    @Test
    void acceptsConfiguredLabelAndCount() {
        SmsOtpProperties props = new SmsOtpProperties(" ShortDrama ", 3);
        assertEquals("ShortDrama", props.getProductLabel());
        assertEquals(3, props.getTemplateParamCount());
    }

    @Test
    void rejectsInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> new SmsOtpProperties("Short", 5));
    }
}
