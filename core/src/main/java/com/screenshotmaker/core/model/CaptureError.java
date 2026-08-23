package com.screenshotmaker.core.model;

import java.time.Instant;

/**
 * A non-fatal problem encountered while capturing one item. Engines collect these instead of
 * aborting the whole run, so one broken page/dialog does not stop the rest of the traversal.
 */
public record CaptureError(String sourceRef, String message, Instant occurredAt) {

    public static CaptureError now(String sourceRef, String message) {
        return new CaptureError(sourceRef, message, Instant.now());
    }
}
