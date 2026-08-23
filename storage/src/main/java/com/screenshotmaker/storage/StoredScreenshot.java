package com.screenshotmaker.storage;

import com.screenshotmaker.core.model.CapturedScreen;

import java.nio.file.Path;

/** A screenshot's metadata paired with the file it was actually written to. */
public record StoredScreenshot(CapturedScreen screen, Path file) {
}
