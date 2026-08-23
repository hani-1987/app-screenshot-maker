package com.screenshotmaker.storage;

import com.screenshotmaker.core.model.CapturedScreen;
import com.screenshotmaker.core.spi.ScreenshotSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Writes screenshots as PNG files under a single run directory, named so they sort in capture
 * order. Safe to call from multiple engine worker threads concurrently.
 */
public final class FileSystemScreenshotSink implements ScreenshotSink {

    private static final Logger log = LoggerFactory.getLogger(FileSystemScreenshotSink.class);

    private final Path rootDirectory;
    private final List<StoredScreenshot> stored = new CopyOnWriteArrayList<>();

    public FileSystemScreenshotSink(Path rootDirectory) throws IOException {
        Files.createDirectories(rootDirectory);
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Path store(CapturedScreen screen, byte[] pngBytes) throws IOException {
        String fileName = FileNameSanitizer.sequencePrefix(screen.sequence())
                + "_" + FileNameSanitizer.sanitize(screen.label()) + ".png";
        Path target = rootDirectory.resolve(fileName);
        Files.write(target, pngBytes);
        stored.add(new StoredScreenshot(screen, target));
        log.debug("Stored screenshot {} ({} bytes)", target, pngBytes.length);
        return target;
    }

    @Override
    public Path rootDirectory() {
        return rootDirectory;
    }

    /** Snapshot of everything stored so far, in the order it was stored. */
    public List<StoredScreenshot> stored() {
        return Collections.unmodifiableList(stored);
    }
}
