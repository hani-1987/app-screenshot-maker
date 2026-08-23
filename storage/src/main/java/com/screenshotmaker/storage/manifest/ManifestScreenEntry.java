package com.screenshotmaker.storage.manifest;

import java.time.Instant;
import java.util.List;

/** One row of the manifest: a captured screen's metadata plus the file it ended up in. */
public record ManifestScreenEntry(
        int sequence,
        String label,
        String sourceRef,
        List<String> breadcrumb,
        String fileName,
        Instant capturedAt) {
}
