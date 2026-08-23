package com.screenshotmaker.core.model;

import java.time.Instant;
import java.util.List;

/**
 * One captured screen/page: the metadata about it, not the image bytes. The PNG bytes are handed
 * to a {@link com.screenshotmaker.core.spi.ScreenshotSink} separately so engines never need to know
 * how or where images are stored.
 *
 * @param sequence    1-based order in which this screen was captured during the run
 * @param label       short human-readable name (page title, dialog title, control name, ...)
 * @param sourceRef   canonical reference to where this came from (URL, or a window/element path)
 * @param breadcrumb  the traversal path from the root to this screen, e.g. ["Home", "Settings", "Profile"]
 * @param capturedAt  when the screenshot was taken
 */
public record CapturedScreen(
        int sequence,
        String label,
        String sourceRef,
        List<String> breadcrumb,
        Instant capturedAt) {

    public CapturedScreen {
        if (label == null || label.isBlank()) {
            label = sourceRef;
        }
        breadcrumb = List.copyOf(breadcrumb);
    }
}
