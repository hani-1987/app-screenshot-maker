package com.screenshotmaker.engine.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WebCaptureConfigTest {

    @Test
    void rejectsNonPositiveMaxItems() {
        assertThrows(IllegalArgumentException.class, () -> new WebCaptureConfig(
                0, 5, 4, Duration.ofSeconds(20), false, true, true,
                Duration.ofMillis(250), 1440, 900, 15000, WebCaptureConfig.Browser.CHROME));
    }

    @Test
    void rejectsNegativeMaxDepth() {
        assertThrows(IllegalArgumentException.class, () -> new WebCaptureConfig(
                10, -1, 4, Duration.ofSeconds(20), false, true, true,
                Duration.ofMillis(250), 1440, 900, 15000, WebCaptureConfig.Browser.CHROME));
    }

    @Test
    void rejectsNonPositiveConcurrency() {
        assertThrows(IllegalArgumentException.class, () -> new WebCaptureConfig(
                10, 5, 0, Duration.ofSeconds(20), false, true, true,
                Duration.ofMillis(250), 1440, 900, 15000, WebCaptureConfig.Browser.CHROME));
    }
}
