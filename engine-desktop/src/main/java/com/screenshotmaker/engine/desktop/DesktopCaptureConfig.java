package com.screenshotmaker.engine.desktop;

import com.screenshotmaker.core.model.CaptureConfig;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Settings specific to walking a Windows application's UI tree. Only {@link DesktopCaptureEngine}
 * needs to know about this type &mdash; it is invisible to the web engine and to core.
 */
public record DesktopCaptureConfig(
        int maxItems,
        int maxDepth,
        int concurrency,
        Duration timeout,
        URI winAppDriverUrl,
        Duration settleDelay,
        int maxElementsPerScreen,
        List<String> blockedActionPatterns) implements CaptureConfig {

    public DesktopCaptureConfig {
        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems must be positive");
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth must not be negative");
        }
        if (concurrency != 1) {
            throw new IllegalArgumentException(
                    "Desktop UI automation drives a single input stream to one application; concurrency must be 1");
        }
        if (maxElementsPerScreen <= 0) {
            throw new IllegalArgumentException("maxElementsPerScreen must be positive");
        }
        blockedActionPatterns = List.copyOf(blockedActionPatterns);
    }

    public static DesktopCaptureConfig defaults() {
        return new DesktopCaptureConfig(
                150,                          // maxItems
                4,                            // maxDepth
                1,                            // concurrency
                Duration.ofSeconds(15),       // timeout
                URI.create("http://127.0.0.1:4723"), // winAppDriverUrl
                Duration.ofMillis(400),       // settleDelay
                60,                           // maxElementsPerScreen
                SafetyFilter.defaultBlockedPatterns());
    }
}
