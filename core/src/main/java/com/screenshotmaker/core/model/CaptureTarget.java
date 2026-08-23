package com.screenshotmaker.core.model;

/**
 * What to capture: a website (source = seed URL) or a Windows desktop application
 * (source = executable path, or {@code window:<title>} to attach to an already-running window).
 */
public record CaptureTarget(CaptureTargetType type, String source) {

    public CaptureTarget {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
    }
}
