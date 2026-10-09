package com.freelancesuite.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void h2ConsoleAccessRequiresDevProfileAndExplicitEnablement() {
        assertTrue(SecurityConfig.shouldPermitH2Console(true, new String[]{"dev"}));
        assertFalse(SecurityConfig.shouldPermitH2Console(false, new String[]{"dev"}));
        assertFalse(SecurityConfig.shouldPermitH2Console(true, new String[]{"prod"}));
        assertFalse(SecurityConfig.shouldPermitH2Console(true, new String[]{"test"}));
    }
}
