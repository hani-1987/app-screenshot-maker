package com.screenshotmaker.engine.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitedStateTrackerTest {

    @Test
    void firstSightingIsNewSubsequentAreNot() {
        VisitedStateTracker tracker = new VisitedStateTracker();

        assertTrue(tracker.markIfNew("Home::Button|Settings|"));
        assertFalse(tracker.markIfNew("Home::Button|Settings|"));
        assertEquals(1, tracker.size());
    }

    @Test
    void differentSignaturesAreTrackedIndependently() {
        VisitedStateTracker tracker = new VisitedStateTracker();

        tracker.markIfNew("Home::Button|Settings|");
        tracker.markIfNew("Home>Settings::Button|Back|");

        assertEquals(2, tracker.size());
    }
}
