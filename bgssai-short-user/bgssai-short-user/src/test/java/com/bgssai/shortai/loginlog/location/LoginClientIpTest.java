package com.bgssai.shortai.loginlog.location;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class LoginClientIpTest {
    @Test
    void directClientsCannotSpoofForwardedHeaders() {
        MockHttpServletRequest request = request("8.8.8.8", "1.1.1.1");
        request.addHeader("X-Real-IP", "9.9.9.9");
        assertEquals("8.8.8.8", LoginClientIp.resolve(request));
    }

    @Test
    void localNginxAppendedPeerWinsOverClientSuppliedPrefix() {
        assertEquals("8.8.8.8", LoginClientIp.resolve(request("127.0.0.1", "1.1.1.1, 8.8.8.8")));
        assertEquals("8.8.8.8", LoginClientIp.resolve(request("::1", "8.8.8.8")));
    }

    @Test
    void invalidLastHopNeverFallsBackToAnAttackerControlledEarlierHop() {
        assertEquals("127.0.0.1", LoginClientIp.resolve(request("127.0.0.1", "8.8.8.8, forged")));
        assertEquals("127.0.0.1", LoginClientIp.resolve(request("127.0.0.1", "8.8.8.8,")));
        assertEquals("127.0.0.1", LoginClientIp.resolve(request("127.0.0.1", "a".repeat(4097))));
        assertNull(LoginClientIp.resolve(null));
    }

    private MockHttpServletRequest request(String remote, String forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remote);
        request.addHeader("X-Forwarded-For", forwarded);
        return request;
    }
}
