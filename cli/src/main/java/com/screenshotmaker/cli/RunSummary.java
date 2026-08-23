package com.screenshotmaker.cli;

import com.screenshotmaker.core.model.CaptureResult;

import java.nio.file.Path;

/** What to report back to the user once a run finishes. */
record RunSummary(Path outputDirectory, CaptureResult result) {
}
