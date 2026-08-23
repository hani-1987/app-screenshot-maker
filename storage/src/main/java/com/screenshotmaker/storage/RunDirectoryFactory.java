package com.screenshotmaker.storage;

import com.screenshotmaker.core.model.CaptureTarget;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Computes where a run's screenshots should live: {@code <outputRoot>/<type>_<target>_<timestamp>/}.
 * Pure path arithmetic &mdash; creating the directory is the sink's job, not this class's.
 */
public final class RunDirectoryFactory {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private RunDirectoryFactory() {
    }

    public static Path resolve(Path outputRoot, CaptureTarget target, Instant startedAt) {
        String typePrefix = switch (target.type()) {
            case WEBSITE -> "web";
            case DESKTOP_APPLICATION -> "desktop";
        };
        String folderName = typePrefix + "_" + FileNameSanitizer.sanitize(target.source())
                + "_" + TIMESTAMP_FORMAT.format(startedAt);
        return outputRoot.resolve(folderName);
    }
}
