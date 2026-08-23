package com.screenshotmaker.storage.manifest;

import java.time.Instant;

/** One non-fatal problem recorded during the run. */
public record ManifestErrorEntry(String sourceRef, String message, Instant occurredAt) {
}
