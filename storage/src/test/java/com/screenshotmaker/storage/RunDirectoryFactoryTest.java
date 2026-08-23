package com.screenshotmaker.storage;

import com.screenshotmaker.core.model.CaptureTarget;
import com.screenshotmaker.core.model.CaptureTargetType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunDirectoryFactoryTest {

    @Test
    void buildsAPredictableTypePrefixedFolderName() {
        CaptureTarget target = new CaptureTarget(CaptureTargetType.WEBSITE, "https://example.com/");
        Instant fixedInstant = Instant.parse("2026-01-02T03:04:05Z");

        Path resolved = RunDirectoryFactory.resolve(Path.of("out"), target, fixedInstant);

        assertEquals(Path.of("out", "web_https-example.com_20260102-030405"), resolved);
    }

    @Test
    void desktopTargetsGetADesktopPrefix() {
        CaptureTarget target = new CaptureTarget(CaptureTargetType.DESKTOP_APPLICATION, "C:\\apps\\notepad.exe");
        Path resolved = RunDirectoryFactory.resolve(Path.of("out"), target, Instant.now());

        assertTrue(resolved.getFileName().toString().startsWith("desktop_"));
    }
}
