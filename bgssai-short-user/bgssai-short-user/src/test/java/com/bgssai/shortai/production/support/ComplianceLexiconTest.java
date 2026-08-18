package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ComplianceLexiconTest {

    @Test
    void suicideIsBlock() {
        Map<String, Object> scan = ComplianceLexicon.scan("角色企图自杀离开", 1, 1, null);
        assertEquals("BLOCK", scan.get("risk_level"));
    }

    @Test
    void revengeIsWarn() {
        Map<String, Object> scan = ComplianceLexicon.scan("她决定复仇", 1, 1, null);
        assertEquals("WARN", scan.get("risk_level"));
    }
}
