package com.bgssai.shortai.login.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.login.sms.TencentSmsPhoneRoute.Route;

class TencentSmsPhoneRouteTest {

    @Test
    void routesMainlandAndNormalizesStorageVsE164() {
        assertEquals(Route.MAINLAND, TencentSmsPhoneRoute.routeOf("13800138000"));
        assertEquals(Route.MAINLAND, TencentSmsPhoneRoute.routeOf(" +86 138-0013-8000 "));
        assertEquals(Route.MAINLAND, TencentSmsPhoneRoute.routeOf("008613800138000"));

        assertEquals("13800138000", TencentSmsPhoneRoute.normalizeForStorage("+8613800138000"));
        assertEquals("+8613800138000", TencentSmsPhoneRoute.toTencentE164("13800138000"));
        assertEquals("+8613800138000", TencentSmsPhoneRoute.toTencentE164("0086 13800138000"));
    }

    @Test
    void routesInternationalAndHkMoTwWithoutManglingPlusOne() {
        assertEquals(Route.INTERNATIONAL, TencentSmsPhoneRoute.routeOf("+85291234567"));
        assertEquals(Route.INTERNATIONAL, TencentSmsPhoneRoute.routeOf("00853 66123456"));
        assertEquals(Route.INTERNATIONAL, TencentSmsPhoneRoute.routeOf("+886912345678"));
        assertEquals(Route.INTERNATIONAL, TencentSmsPhoneRoute.routeOf("+14155550100"));

        assertEquals("+14155550100", TencentSmsPhoneRoute.normalizeForStorage("+1 415-555-0100"));
        assertEquals("+14155550100", TencentSmsPhoneRoute.toTencentE164("+1 415-555-0100"));
        assertEquals("+85366123456", TencentSmsPhoneRoute.normalizeForStorage("0085366123456"));
    }

    @Test
    void rejectsMalformedStringsAndBarePlusPrefix() {
        assertNull(TencentSmsPhoneRoute.routeOf(null));
        assertNull(TencentSmsPhoneRoute.routeOf(""));
        assertNull(TencentSmsPhoneRoute.routeOf("+"));
        assertNull(TencentSmsPhoneRoute.routeOf("+abc"));
        assertNull(TencentSmsPhoneRoute.routeOf("+86123456789"));
        assertNull(TencentSmsPhoneRoute.routeOf("12800138000"));
        assertNull(TencentSmsPhoneRoute.routeOf("85291234567"));
    }
}
