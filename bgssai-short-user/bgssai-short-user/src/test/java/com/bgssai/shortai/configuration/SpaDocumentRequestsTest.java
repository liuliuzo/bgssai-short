package com.bgssai.shortai.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpaDocumentRequestsTest {

    @Test
    void htmlGetOutsideApiPrefixesServesTheSpa() {
        assertTrue(SpaDocumentRequests.shouldServeIndex("GET", "/no-such-page-qa", "text/html"));
        assertTrue(SpaDocumentRequests.shouldServeIndex("GET", "/account", "text/html,application/xhtml+xml"));
        assertTrue(SpaDocumentRequests.shouldServeIndex("GET", "/works/w-1/script", "text/html"));
    }

    @Test
    void apiAndNonDocumentRequestsStayJson() {
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/bgssai/user/login", "text/html"));
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/api/download/bot", "text/html"));
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/no-such-page-qa", "application/json"));
        assertFalse(SpaDocumentRequests.shouldServeIndex("POST", "/no-such-page-qa", "text/html"));
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/index.html", "text/html"));
        assertFalse(SpaDocumentRequests.shouldServeIndex("GET", "/assets/app.js", "*/*"));
    }
}
