package com.screenshotmaker.storage.manifest;

import java.time.Instant;
import java.util.List;

/**
 * Full, self-describing record of one capture run. Written as {@code manifest.json} alongside the
 * screenshots so the output folder is understandable without re-running the tool or reading logs.
 */
public record ManifestDocument(
        String targetType,
        String targetSource,
        Instant startedAt,
        Instant finishedAt,
        int totalScreens,
        int totalErrors,
        List<ManifestScreenEntry> screens,
        List<ManifestErrorEntry> errors) {
}
