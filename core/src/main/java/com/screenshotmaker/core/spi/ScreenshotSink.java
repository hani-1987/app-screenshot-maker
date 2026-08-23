package com.screenshotmaker.core.spi;

import com.screenshotmaker.core.model.CapturedScreen;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Destination for captured screenshots. Engines only ever talk to this interface, never to the
 * filesystem directly &mdash; where and how images are laid out on disk is entirely the storage
 * module's concern.
 */
public interface ScreenshotSink {

    /**
     * Persists one screenshot's PNG bytes and returns the path it was written to.
     */
    Path store(CapturedScreen screen, byte[] pngBytes) throws IOException;

    /** The root directory this sink is writing the current run into, for reporting purposes. */
    Path rootDirectory();
}
