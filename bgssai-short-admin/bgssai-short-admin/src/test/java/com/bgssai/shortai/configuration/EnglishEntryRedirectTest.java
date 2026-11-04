package com.bgssai.shortai.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class EnglishEntryRedirectTest {

    @Test
    void englishPrefixGoesToChineseLoginAndKeepsTheQuery() {
        assertEquals("/login", EnglishEntryRedirect.location("/en", null));
        assertEquals("/login", EnglishEntryRedirect.location("/en/", ""));
        assertEquals("/login", EnglishEntryRedirect.location("/en/login", null));
        assertEquals("/login?from=en", EnglishEntryRedirect.location("/en", "from=en"));
        assertEquals("/login?redirect=%2Fhome", EnglishEntryRedirect.location("/en/login", "redirect=%2Fhome"));
        assertEquals("/login", EnglishEntryRedirect.location("/en", "bad\r\nLocation: https://evil.example"));
    }

    @Test
    void lookalikeAndApiPathsStayPut() {
        for (String path : new String[] {"/login", "/home", "/english", "/end", "/enterprise", "/privacy", "/bgssai/admin/login"}) {
            assertFalse(EnglishEntryRedirect.isEnglishPrefix(path));
            assertNull(EnglishEntryRedirect.location(path, "from=en"));
        }
        assertEquals("/en", EnglishEntryRedirect.pathWithinApplication("/app/en;jsessionid=1", "/app"));
    }

    @Test
    void filterRedirectsGetAndHeadWithoutServingTheSpa() throws Exception {
        EnglishEntryRedirectFilter filter = new EnglishEntryRedirectFilter();
        for (String method : new String[] {"GET", "HEAD"}) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, "/en/");
            request.setQueryString("from=en");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean chained = new AtomicBoolean(false);
            FilterChain chain = (req, res) -> chained.set(true);
            filter.doFilter(request, response, chain);
            assertFalse(chained.get());
            assertEquals(302, response.getStatus());
            assertEquals("/login?from=en", response.getHeader("Location"));
            assertEquals("no-store", response.getHeader("Cache-Control"));
        }
    }

    @Test
    void filterLeavesOtherPathsAndMethodsAlone() throws Exception {
        EnglishEntryRedirectFilter filter = new EnglishEntryRedirectFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/en");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chained = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> chained.set(true));
        assertTrue(chained.get());

        MockHttpServletRequest login = new MockHttpServletRequest("GET", "/login");
        AtomicBoolean loginChained = new AtomicBoolean(false);
        filter.doFilter(login, new MockHttpServletResponse(), (req, res) -> loginChained.set(true));
        assertTrue(loginChained.get());
    }
}
