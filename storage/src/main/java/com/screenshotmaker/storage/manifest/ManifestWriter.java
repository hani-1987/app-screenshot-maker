package com.screenshotmaker.storage.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.screenshotmaker.core.model.CaptureError;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.storage.StoredScreenshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes a run's {@link ManifestDocument} as both {@code manifest.json} (machine-readable) and
 * {@code index.html} (a browsable gallery) into the run's output directory.
 */
public final class ManifestWriter {

    private static final String JSON_FILE_NAME = "manifest.json";
    private static final String HTML_FILE_NAME = "index.html";

    private final ObjectMapper objectMapper;

    public ManifestWriter() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Builds the manifest from the run's result and stored files, and writes both output files. */
    public Path write(Path rootDirectory, CaptureResult result, List<StoredScreenshot> stored) throws IOException {
        ManifestDocument document = toDocument(rootDirectory, result, stored);

        Path jsonPath = rootDirectory.resolve(JSON_FILE_NAME);
        objectMapper.writeValue(jsonPath.toFile(), document);

        Path htmlPath = rootDirectory.resolve(HTML_FILE_NAME);
        Files.writeString(htmlPath, GalleryHtmlRenderer.render(document), StandardCharsets.UTF_8);

        return jsonPath;
    }

    private ManifestDocument toDocument(Path rootDirectory, CaptureResult result, List<StoredScreenshot> stored) {
        List<ManifestScreenEntry> screenEntries = stored.stream()
                .sorted((a, b) -> Integer.compare(a.screen().sequence(), b.screen().sequence()))
                .map(entry -> new ManifestScreenEntry(
                        entry.screen().sequence(),
                        entry.screen().label(),
                        entry.screen().sourceRef(),
                        entry.screen().breadcrumb(),
                        relativize(rootDirectory, entry.file()),
                        entry.screen().capturedAt()))
                .toList();

        List<ManifestErrorEntry> errorEntries = result.errors().stream()
                .map(this::toErrorEntry)
                .toList();

        return new ManifestDocument(
                result.target().type().name(),
                result.target().source(),
                result.startedAt(),
                result.finishedAt(),
                screenEntries.size(),
                errorEntries.size(),
                screenEntries,
                errorEntries);
    }

    private ManifestErrorEntry toErrorEntry(CaptureError error) {
        return new ManifestErrorEntry(error.sourceRef(), error.message(), error.occurredAt());
    }

    private static String relativize(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
