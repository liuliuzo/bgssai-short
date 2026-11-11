package com.bgssai.shortai.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class WelcomeIllustrationResourceTest {
    @Test
    void welcomeIllustrationIsIncludedInApplicationResources() {
        assertNotNull(getClass().getResource("/static/brand/illustrations/admin-welcome.svg"),
                "The sign-in illustration must be copied into the backend static resources");
    }
}
