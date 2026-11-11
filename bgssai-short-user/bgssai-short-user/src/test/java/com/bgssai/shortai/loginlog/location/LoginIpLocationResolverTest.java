package com.bgssai.shortai.loginlog.location;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoginIpLocationResolverTest {
    private final LoginIpLocationResolver resolver = new LoginIpLocationResolver();

    LoginIpLocationResolverTest() throws Exception {
    }

    @Test
    void bundledDatabaseResolvesPublicIpv4WithoutNetwork() {
        assertTrue(resolver.resolve("114.114.114.114").startsWith("中国"));
        assertNotEquals("未知", resolver.resolve("8.8.8.8"));
    }

    @Test
    void localPrivateAndNonPublicAddressesAreNotGeolocated() {
        for (String ip : new String[] {"127.1.2.3", "::1"}) {
            assertEquals("本地", resolver.resolve(ip));
        }
        for (String ip : new String[] {"10.1.2.3", "172.16.0.5", "192.168.1.1",
                "169.254.1.1", "fc00::1", "fe80::1", "::ffff:192.168.1.1"}) {
            assertEquals("内网", resolver.resolve(ip));
        }
        for (String ip : new String[] {"0.0.0.0", "203.0.113.7", "198.51.100.9",
                "192.0.2.1", "100.64.0.1", "198.18.0.1", "224.0.0.1", "255.255.255.255"}) {
            assertEquals("非公网地址", resolver.resolve(ip));
        }
    }

    @Test
    void unsupportedIpv6AndInvalidValuesRemainHonest() {
        assertEquals("未知（IPv6 暂未覆盖）", resolver.resolve("2606:4700:4700::1111"));
        for (String value : new String[] {null, "", "example.com", "127.1", "8.8.8.8, 1.1.1.1",
                "1.2.3.256", "01.2.3.4", "8.8.8.8:80", "fe80::1%eth0"}) {
            assertEquals("未知", resolver.resolve(value));
        }
    }

    @Test
    void displayUsesAvailableCountryProvinceCityWithoutInventingMissingFields() {
        assertEquals("中国 / 江苏省 / 南京市",
                LoginIpLocationResolver.format("中国|0|江苏省|南京市|电信"));
        assertEquals("美国", LoginIpLocationResolver.format("美国|0|0|0|Google"));
        assertEquals("中国 / 北京市", LoginIpLocationResolver.format("中国|0|北京市|北京市|联通"));
        assertEquals("未知", LoginIpLocationResolver.format("0|0|0|0|0"));
        assertEquals("未知", LoginIpLocationResolver.format("bad data"));
    }
}
