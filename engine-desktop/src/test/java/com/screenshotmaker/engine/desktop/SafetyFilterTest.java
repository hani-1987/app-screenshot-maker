package com.screenshotmaker.engine.desktop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyFilterTest {

    private final SafetyFilter filter = new SafetyFilter(SafetyFilter.defaultBlockedPatterns());

    @Test
    void blocksKnownDestructiveActionsCaseInsensitively() {
        assertFalse(filter.isSafeToActivate("Delete account"));
        assertFalse(filter.isSafeToActivate("DELETE ACCOUNT"));
        assertFalse(filter.isSafeToActivate("Exit application"));
        assertFalse(filter.isSafeToActivate("Sign Out"));
    }

    @Test
    void allowsOrdinaryNavigationControls() {
        assertTrue(filter.isSafeToActivate("Next"));
        assertTrue(filter.isSafeToActivate("Settings"));
        assertTrue(filter.isSafeToActivate("Profile tab"));
    }

    @Test
    void unnamedElementsAreAllowed() {
        assertTrue(filter.isSafeToActivate(null));
        assertTrue(filter.isSafeToActivate("   "));
    }

    @Test
    void honorsCustomBlockedPatterns() {
        SafetyFilter custom = new SafetyFilter(List.of("dangerous"));
        assertFalse(custom.isSafeToActivate("Do Something Dangerous"));
        assertTrue(custom.isSafeToActivate("Delete"), "custom list replaces, doesn't merge with, the default list");
    }
}
