package com.screenshotmaker.engine.web;

import com.screenshotmaker.core.model.CaptureConfig;

import java.time.Duration;

/**
 * Settings specific to crawling and screenshotting a website. Only {@link WebCaptureEngine} needs
 * to know about this type &mdash; it is invisible to the desktop engine and to core.
 */
public record WebCaptureConfig(
        int maxItems,
        int maxDepth,
        int concurrency,
        Duration timeout,
        boolean includeSubdomains,
        boolean respectRobotsTxt,
        boolean headless,
        Duration politenessDelay,
        int windowWidth,
        int windowHeight,
        int maxFullPageHeight,
        Browser browser) implements CaptureConfig {

    public enum Browser { CHROME, FIREFOX, EDGE }

    public WebCaptureConfig {
        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems must be positive");
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth must not be negative");
        }
        if (concurrency <= 0) {
            throw new IllegalArgumentException("concurrency must be positive");
        }
        if (windowWidth <= 0 || windowHeight <= 0 || maxFullPageHeight <= 0) {
            throw new IllegalArgumentException("window dimensions must be positive");
        }
    }

    public static WebCaptureConfig defaults() {
        return new WebCaptureConfig(
                200,                    // maxItems
                5,                      // maxDepth
                4,                      // concurrency
                Duration.ofSeconds(20), // timeout
                false,                  // includeSubdomains
                true,                   // respectRobotsTxt
                true,                   // headless
                Duration.ofMillis(250), // politenessDelay
                1440,                   // windowWidth
                900,                    // windowHeight
                15000,                  // maxFullPageHeight
                Browser.CHROME);
    }
}
