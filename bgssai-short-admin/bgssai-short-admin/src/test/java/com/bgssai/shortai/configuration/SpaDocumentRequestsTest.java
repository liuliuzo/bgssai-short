package com.bgssai.shortai.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpaDocumentRequestsTest {

    @Test
    void htmlGetOutsideApiPrefixesServesTheSpa() {
        assertTrue(SpaDocumentRequests.shouldServeIndex("GET", "/no-such-page-qa", "text/html"));
        assertTrue(SpaDocumentRequests.shouldServeIndex("GET", "/payment-merchants", "text/html"));
    }

    @Test
    void apiRequestsStayJson() {
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/bgssai/admin/login", "text/html"));
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/api/health", "application/json"));
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/no-such-page-qa", null));
    }
}
