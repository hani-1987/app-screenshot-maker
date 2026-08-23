package com.screenshotmaker.cli;

import com.screenshotmaker.core.exception.CaptureException;
import com.screenshotmaker.core.model.CaptureConfig;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.core.model.CaptureTarget;
import com.screenshotmaker.core.spi.CaptureEngine;
import com.screenshotmaker.storage.FileSystemScreenshotSink;
import com.screenshotmaker.storage.RunDirectoryFactory;
import com.screenshotmaker.storage.manifest.ManifestWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Orchestrates a single capture run: resolves the output directory, picks the engine, runs the
 * capture, and writes the manifest. Deliberately has no knowledge of command-line parsing (that is
 * {@link ScreenshotMakerCommand}'s job) or of any specific engine's internals.
 */
final class CaptureRunner {

    private final EngineRegistry engineRegistry = new EngineRegistry();
    private final ManifestWriter manifestWriter = new ManifestWriter();

    RunSummary run(CaptureTarget target, CaptureConfig config, Path outputRoot) throws CaptureException, IOException {
        Path runDirectory = RunDirectoryFactory.resolve(outputRoot, target, Instant.now());
        FileSystemScreenshotSink sink = new FileSystemScreenshotSink(runDirectory);

        CaptureEngine engine = engineRegistry.engineFor(target.type());
        CaptureResult result = engine.capture(target, config, sink);

        manifestWriter.write(runDirectory, result, sink.stored());
        return new RunSummary(runDirectory, result);
    }
}
