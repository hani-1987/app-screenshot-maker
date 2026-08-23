package com.screenshotmaker.engine.desktop;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DesktopCaptureConfigTest {

    @Test
    void rejectsConcurrencyOtherThanOne() {
        assertThrows(IllegalArgumentException.class, () -> new DesktopCaptureConfig(
                10, 4, 2, Duration.ofSeconds(15), URI.create("http://127.0.0.1:4723"),
                Duration.ofMillis(400), 60, List.of()));
    }

    @Test
    void rejectsNonPositiveMaxElementsPerScreen() {
        assertThrows(IllegalArgumentException.class, () -> new DesktopCaptureConfig(
                10, 4, 1, Duration.ofSeconds(15), URI.create("http://127.0.0.1:4723"),
                Duration.ofMillis(400), 0, List.of()));
    }

    @Test
    void rejectsNonPositiveMaxItems() {
        assertThrows(IllegalArgumentException.class, () -> new DesktopCaptureConfig(
                0, 4, 1, Duration.ofSeconds(15), URI.create("http://127.0.0.1:4723"),
                Duration.ofMillis(400), 60, List.of()));
    }
}
