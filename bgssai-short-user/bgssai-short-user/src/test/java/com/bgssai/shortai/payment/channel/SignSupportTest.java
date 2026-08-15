package com.bgssai.shortai.payment.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class SignSupportTest {

    @Test
    void parseFormSplitsPairs() {
        Map<String, String> map = SignSupport.parseForm("out_trade_no=O-1&amount=1990");
        assertEquals("O-1", map.get("out_trade_no"));
        assertEquals("1990", map.get("amount"));
    }

    @Test
    void canonicalSortsAndExcludesSign() {
        Map<String, String> params = new TreeMap<>();
        params.put("b", "2");
        params.put("a", "1");
        params.put("sign", "should-be-excluded");
        assertEquals("a=1&b=2", SignSupport.canonical(params));
    }

    @Test
    void verifyAcceptsGoodSignAndRejectsBad() {
        Map<String, String> params = new TreeMap<>();
        params.put("out_trade_no", "O-1");
        params.put("amount", "1990");
        params.put("trade_state", "SUCCESS");
        String key = "sandbox-key";
        String sign = SignSupport.hmacSha256Hex(SignSupport.canonical(params), key);
        params.put("sign", sign);
        assertTrue(SignSupport.verify(params, key));
        params.put("sign", "deadbeef");
        assertFalse(SignSupport.verify(params, key));
    }

    @Test
    void verifyRejectsMissingSign() {
        Map<String, String> params = new TreeMap<>();
        params.put("out_trade_no", "O-1");
        assertFalse(SignSupport.verify(params, "k"));
    }
}
